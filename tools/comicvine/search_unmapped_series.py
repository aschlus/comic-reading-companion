from __future__ import annotations

import json
import os
import re
import time
from collections import defaultdict
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


BASE_URL = "https://comicvine.gamespot.com/api"

TOOL_DIR = Path(__file__).resolve().parent

PROJECT_DIR = (
        TOOL_DIR /
        ".." /
        ".."
).resolve()

READING_LIST_FILE = (
        PROJECT_DIR /
        "app" /
        "src" /
        "main" /
        "assets" /
        "reading_lists" /
        "spider_man_volume_2.json"
)

MAPPINGS_FILE = (
        TOOL_DIR /
        "series_mappings.json"
)

API_KEY_FILE = (
        TOOL_DIR /
        ".comicvine_api_key"
)

CACHE_DIR = (
        TOOL_DIR /
        "cache" /
        "series_candidates"
)

OUTPUT_DIR = (
        TOOL_DIR /
        "output"
)

REPORT_FILE = (
        OUTPUT_DIR /
        "series_candidate_report.json"
)


def load_json(
        path: Path
):
    return json.loads(
        path.read_text(
            encoding="utf-8"
        )
    )


def load_api_key() -> str:
    environment_key = os.environ.get(
        "COMIC_VINE_API_KEY"
    )

    if environment_key:
        return environment_key.strip()

    if API_KEY_FILE.exists():
        api_key = API_KEY_FILE.read_text(
            encoding="utf-8"
        ).strip()

        if api_key:
            return api_key

    raise RuntimeError(
        "Comic Vine API key not found."
    )


def normalize_title(
        title: str
) -> str:
    normalized = title.lower().strip()

    if normalized.startswith("the "):
        normalized = normalized[4:]

    normalized = normalized.replace(
        "&",
        "and"
    )

    normalized = re.sub(
        r"[^a-z0-9]+",
        "",
        normalized
    )

    return normalized


def series_key(
        title: str,
        volume,
        start_year
) -> tuple:
    return (
        title.strip(),
        volume,
        start_year
    )


def cache_name(
        title: str,
        volume,
        start_year
) -> str:
    slug = re.sub(
        r"[^a-z0-9]+",
        "_",
        title.lower()
    ).strip("_")

    volume_text = (
        str(volume)
        if volume is not None
        else "none"
    )

    year_text = (
        str(start_year)
        if start_year is not None
        else "none"
    )

    return (
        f"{slug}_"
        f"v{volume_text}_"
        f"{year_text}.json"
    )


def get_unmapped_series() -> list[dict]:
    reading_list = load_json(
        READING_LIST_FILE
    )

    mappings = load_json(
        MAPPINGS_FILE
    )

    mapped_keys = {
        series_key(
            mapping["localTitle"],
            mapping.get("localVolume"),
            mapping.get("localStartYear")
        )
        for mapping in mappings
    }

    issue_counts = defaultdict(int)

    for item in reading_list.get(
            "items",
            []
    ):
        series = item["series"]

        key = series_key(
            series["title"],
            series.get("volume"),
            series.get("startYear")
        )

        issue_counts[key] += 1

    result = []

    for (
            title,
            volume,
            start_year
    ), issue_count in issue_counts.items():

        key = series_key(
            title,
            volume,
            start_year
        )

        if key in mapped_keys:
            continue

        result.append(
            {
                "title": title,
                "volume": volume,
                "startYear": start_year,
                "localIssueCount": issue_count
            }
        )

    return sorted(
        result,
        key=lambda item: (
            item["startYear"] or 0,
            item["title"].lower(),
            item["volume"] or 0
        )
    )


def search_comic_vine(
        local_series: dict
) -> dict:
    title = local_series["title"]

    cache_file = (
            CACHE_DIR /
            cache_name(
                title,
                local_series["volume"],
                local_series["startYear"]
            )
    )

    if cache_file.exists():
        print(
            f"Cache hit: {title}"
        )

        return load_json(
            cache_file
        )

    parameters = {
        "api_key": load_api_key(),
        "format": "json",
        "resources": "volume",
        "query": title,
        "field_list": (
            "id,"
            "name,"
            "start_year,"
            "count_of_issues,"
            "publisher,"
            "site_detail_url"
        ),
        "limit": 50
    }

    url = (
        f"{BASE_URL}/search/?"
        f"{urlencode(parameters)}"
    )

    print(
        f"Searching Comic Vine: {title}"
    )

    request = Request(
        url,
        headers={
            "User-Agent": (
                "ComicReadingCompanion/0.1 "
                "(personal non-commercial "
                "metadata enrichment)"
            )
        }
    )

    try:
        with urlopen(
                request,
                timeout=30
        ) as response:
            data = json.load(response)

    except HTTPError as error:
        raise RuntimeError(
            f"HTTP {error.code} "
            f"while searching for {title}"
        ) from error

    except URLError as error:
        raise RuntimeError(
            f"Connection error while "
            f"searching for {title}: "
            f"{error.reason}"
        ) from error

    if data.get("status_code") != 1:
        raise RuntimeError(
            f"Comic Vine error for "
            f"{title}: "
            f"{data.get('error')}"
        )

    CACHE_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    cache_file.write_text(
        json.dumps(
            data,
            indent=2
        ),
        encoding="utf-8"
    )

    time.sleep(1.5)

    return data


def parse_year(
        value
) -> int | None:
    if value is None:
        return None

    try:
        return int(value)
    except (
            TypeError,
            ValueError
    ):
        return None


def score_candidate(
        local_series: dict,
        candidate: dict
) -> int:
    score = 0

    local_title = normalize_title(
        local_series["title"]
    )

    candidate_title = normalize_title(
        candidate.get("name") or ""
    )

    if local_title == candidate_title:
        score += 100

    elif (
            local_title in candidate_title
            or candidate_title in local_title
    ):
        score += 45

    local_year = parse_year(
        local_series["startYear"]
    )

    candidate_year = parse_year(
        candidate.get("start_year")
    )

    if (
            local_year is not None
            and candidate_year is not None
    ):
        difference = abs(
            local_year - candidate_year
        )

        if difference == 0:
            score += 30

        elif difference == 1:
            score += 20

        elif difference == 2:
            score += 10

    publisher = (
            candidate.get("publisher")
            or {}
    )

    publisher_name = (
            publisher.get("name")
            or ""
    ).lower()

    if "marvel" in publisher_name:
        score += 15

    candidate_issue_count = (
            candidate.get(
                "count_of_issues"
            )
            or 0
    )

    local_issue_count = (
        local_series[
            "localIssueCount"
        ]
    )

    if (
            candidate_issue_count
            >= local_issue_count
    ):
        score += 5
    else:
        score -= 20

    return score


def build_report_entry(
        local_series: dict,
        response: dict
) -> dict:
    candidates = []

    for candidate in response.get(
            "results",
            []
    ):
        candidate_copy = dict(
            candidate
        )

        candidate_copy[
            "matchScore"
        ] = score_candidate(
            local_series,
            candidate
        )

        candidates.append(
            candidate_copy
        )

    candidates.sort(
        key=lambda candidate:
        candidate["matchScore"],
        reverse=True
    )

    return {
        "localSeries":
            local_series,
        "candidates":
            candidates
    }


def print_summary(
        entry: dict
) -> None:
    local = entry[
        "localSeries"
    ]

    candidates = entry[
        "candidates"
    ]

    print()
    print("=" * 72)

    print(
        f"{local['title']} | "
        f"Vol. {local['volume']} | "
        f"{local['startYear']} | "
        f"{local['localIssueCount']} "
        "local issue(s)"
    )

    print("-" * 72)

    if not candidates:
        print(
            "NO COMIC VINE CANDIDATES"
        )
        return

    for candidate in candidates[:3]:
        publisher = (
                candidate.get(
                    "publisher"
                )
                or {}
        )

        print(
            f"Score {candidate['matchScore']:>3} | "
            f"ID {candidate.get('id')} | "
            f"{candidate.get('name')} | "
            f"{candidate.get('start_year')} | "
            f"{candidate.get('count_of_issues')} issues | "
            f"{publisher.get('name')}"
        )


def main() -> None:
    unmapped_series = (
        get_unmapped_series()
    )

    print(
        f"Searching {len(unmapped_series)} "
        "unmapped series"
    )

    report = []

    for index, local_series in enumerate(
            unmapped_series,
            start=1
    ):
        print()
        print(
            f"[{index}/"
            f"{len(unmapped_series)}] "
            f"{local_series['title']}"
        )

        response = search_comic_vine(
            local_series
        )

        entry = build_report_entry(
            local_series,
            response
        )

        report.append(
            entry
        )

    OUTPUT_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    REPORT_FILE.write_text(
        json.dumps(
            report,
            indent=2,
            ensure_ascii=False
        )
        + "\n",
        encoding="utf-8"
    )

    print()
    print()
    print(
        "CANDIDATE SUMMARY"
    )

    for entry in report:
        print_summary(
            entry
        )

    print()
    print("=" * 72)

    print(
        "Detailed report written to:"
    )

    print(
        f"  {REPORT_FILE}"
    )


if __name__ == "__main__":
    main()