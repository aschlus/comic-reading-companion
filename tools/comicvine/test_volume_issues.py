from __future__ import annotations

import json
import os
import sys
import time
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


BASE_URL = "https://comicvine.gamespot.com/api"

TOOL_DIR = Path(__file__).resolve().parent

API_KEY_FILE = (
        TOOL_DIR /
        ".comicvine_api_key"
)

CACHE_DIR = (
        TOOL_DIR /
        "cache" /
        "issues"
)

PAGE_LIMIT = 100


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
        "Comic Vine API key not found.\n"
        "Create tools/comicvine/"
        ".comicvine_api_key "
        "and put only your API key inside it."
    )


def get_cache_file(
        volume_id: int
) -> Path:
    return (
            CACHE_DIR /
            f"volume_{volume_id}.json"
    )


def load_cached_issues(
        volume_id: int
) -> dict | None:
    cache_file = get_cache_file(
        volume_id
    )

    if not cache_file.exists():
        return None

    print(
        f"Cache hit: {cache_file}"
    )

    return json.loads(
        cache_file.read_text(
            encoding="utf-8"
        )
    )


def save_cached_issues(
        volume_id: int,
        data: dict
) -> None:
    CACHE_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    cache_file = get_cache_file(
        volume_id
    )

    cache_file.write_text(
        json.dumps(
            data,
            indent=2
        ),
        encoding="utf-8"
    )

    print(
        f"Cached issues: {cache_file}"
    )


def request_issue_page(
        volume_id: int,
        offset: int
) -> dict:
    api_key = load_api_key()

    parameters = {
        "api_key": api_key,
        "format": "json",
        "filter": (
            f"volume:{volume_id}"
        ),
        "field_list": (
            "id,"
            "issue_number,"
            "name,"
            "cover_date,"
            "store_date,"
            "image,"
            "site_detail_url,"
            "volume"
        ),
        "limit": PAGE_LIMIT,
        "offset": offset
    }

    url = (
        f"{BASE_URL}/issues/?"
        f"{urlencode(parameters)}"
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

    print(
        "Requesting Comic Vine issues: "
        f"volume={volume_id}, "
        f"offset={offset}"
    )

    try:
        with urlopen(
                request,
                timeout=30
        ) as response:
            data = json.load(response)

    except HTTPError as error:
        raise RuntimeError(
            "Comic Vine request failed with "
            f"HTTP {error.code}"
        ) from error

    except URLError as error:
        raise RuntimeError(
            "Could not connect to Comic Vine: "
            f"{error.reason}"
        ) from error

    status_code = data.get(
        "status_code"
    )

    if status_code != 1:
        raise RuntimeError(
            "Comic Vine returned an error:\n"
            f"{data.get('error')}\n"
            f"Status code: {status_code}"
        )

    return data


def fetch_all_volume_issues(
        volume_id: int,
        refresh: bool = False
) -> dict:
    if not refresh:
        cached = load_cached_issues(
            volume_id
        )

        if cached is not None:
            return cached

    all_issues = []

    offset = 0
    total_results = None

    while (
            total_results is None
            or offset < total_results
    ):
        page = request_issue_page(
            volume_id = volume_id,
            offset = offset
        )

        page_results = page.get(
            "results",
            []
        )

        if total_results is None:
            total_results = page.get(
                "number_of_total_results",
                len(page_results)
            )

            print(
                "Comic Vine reports "
                f"{total_results} total issue(s)"
            )

        all_issues.extend(
            page_results
        )

        if not page_results:
            break

        offset += len(
            page_results
        )

        if offset < total_results:
            print(
                "Waiting before next "
                "Comic Vine request..."
            )

            time.sleep(1.5)

    combined_data = {
        "volume_id": volume_id,
        "number_of_total_results":
            len(all_issues),
        "results": all_issues
    }

    save_cached_issues(
        volume_id = volume_id,
        data = combined_data
    )

    return combined_data


def print_issue(
        issue: dict
) -> None:
    image = (
            issue.get("image")
            or {}
    )

    volume = (
            issue.get("volume")
            or {}
    )

    print(
        f"Comic Vine issue ID: "
        f"{issue.get('id')}"
    )

    print(
        f"Volume:              "
        f"{volume.get('name')}"
    )

    print(
        f"Issue number:        "
        f"{issue.get('issue_number')}"
    )

    print(
        f"Title:               "
        f"{issue.get('name')}"
    )

    print(
        f"Cover date:          "
        f"{issue.get('cover_date')}"
    )

    print(
        f"Store date:          "
        f"{issue.get('store_date')}"
    )

    print(
        f"Cover super URL:     "
        f"{image.get('super_url')}"
    )

    print(
        f"Cover original URL:  "
        f"{image.get('original_url')}"
    )

    print(
        f"Comic Vine page:     "
        f"{issue.get('site_detail_url')}"
    )


def print_matching_issue(
        data: dict,
        issue_number: str
) -> None:
    matches = [
        issue
        for issue in data.get(
            "results",
            []
        )
        if str(
            issue.get(
                "issue_number",
                ""
            )
        ).strip() == issue_number
    ]

    print()
    print(
        f"Matches for issue "
        f"#{issue_number}: "
        f"{len(matches)}"
    )

    print("=" * 72)

    for issue in matches:
        print_issue(issue)

        print("-" * 72)


def main() -> None:
    volume_id = 78701
    issue_number = "1"

    refresh = (
            "--refresh" in sys.argv
    )

    data = fetch_all_volume_issues(
        volume_id = volume_id,
        refresh = refresh
    )

    print()
    print(
        f"Cached "
        f"{data['number_of_total_results']} "
        "issue(s)"
    )

    print_matching_issue(
        data = data,
        issue_number = issue_number
    )


if __name__ == "__main__":
    main()