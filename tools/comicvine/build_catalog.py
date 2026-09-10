from __future__ import annotations
import argparse
import json
import re
from pathlib import Path

COMIC_VINE_SOURCE = "COMIC_VINE"

VALID_ISSUE_TYPES = {
    "REGULAR",
    "ANNUAL",
    "SPECIAL",
    "ONE_SHOT",
    "GIANT_SIZE",
    "PREVIEW",
}

TOOL_DIR = Path(__file__).resolve().parent
CACHE_DIR = (TOOL_DIR / "cache" / "issues")
OUTPUT_DIR = (TOOL_DIR / "output")


def choose_cover_url(comic_vine_issue: dict) -> str | None:
    image = (comic_vine_issue.get("image") or {})
    return (image.get("super_url") or image.get("original_url"))


def choose_publication_month(comic_vine_issue: dict) -> str | None:
    raw_date = (comic_vine_issue.get("cover_date") or comic_vine_issue.get("store_date"))

    if raw_date is None:
        return None

    date_text = str(raw_date).strip()

    if len(date_text) < 7:
        return None

    candidate = date_text[:7]

    if(len(candidate) != 7
       or candidate[4] != "-"
       or not candidate[:4].isdigit()
       or not candidate[5:7].isdigit()
       ):
        return None

    month = int(candidate[5:7])

    if month < 1 or month > 12:
        return None

    return candidate


def choose_series_url(cached_volume: dict) -> str | None:
    for comic_vine_issue in cached_volume.get("results", []):
        volume = (comic_vine_issue.get("volume") or {})

        if not isinstance(volume, dict):
            continue

        site_detail_url = volume.get("site_detail_url")

        if site_detail_url:
            return site_detail_url

    return None


def build_issue(comic_vine_issue: dict, universe_designation: str | None, issue_type: str) -> dict:
    issue_number = str(comic_vine_issue.get("issue_number", "")).strip()

    if not issue_number:
        raise ValueError(
            "Comic Vine issue is missing an issue number"
        )

    comic_vine_issue_id = (comic_vine_issue.get("id"))

    if comic_vine_issue_id is None:
        raise ValueError(
            f"Comic Vine issue #{issue_number} is missing an ID"
        )

    title = comic_vine_issue.get("name")

    if title is not None:
        title = str(title).strip()

        if not title:
            title = None

    return {
        "number": issue_number,
        "title": title,
        "publicationDate": choose_publication_month(comic_vine_issue),
        "coverUrl": choose_cover_url(comic_vine_issue),
        "description": None,
        "type": issue_type.upper(),
        "universeDesignation": universe_designation,
        "externalIds": [
            {
                "source": COMIC_VINE_SOURCE,
                "externalId": str(comic_vine_issue_id),
                "url": comic_vine_issue.get("site_detail_url")
            }
        ]
    }


def issue_number_sort_key(issue_number: str) -> tuple:
    text = str(issue_number).strip().casefold()
    parts = re.split(r"(\d+)", text)

    return tuple(
        (0,int(part))
        if part.isdigit()
        else (1, part)
        for part in parts
        if part
    )


def issue_sort_key(issue: dict) -> tuple:
    publication_date = (issue.get("publicationDate"))

    return(
        publication_date is None,
        publication_date or "",
        issue_number_sort_key(issue["number"])
    )


def build_series(series_config: dict, cached_volume: dict) -> dict:
    volume_id = int(series_config["comicVineVolumeId"])
    cached_volume_id = (cached_volume.get("volume_id"))

    if cached_volume_id != volume_id:
        raise ValueError(
            f"Cached volume ID {cached_volume_id} does not match configured volume {volume_id}"
        )

    universe_designation = (series_config.get("defaultUniverseDesignation"))

    issue_type = (series_config.get("defaultIssueType", "REGULAR"))

    issue_overrides = (series_config.get("issueOverrides", {}))

    issues = []

    for comic_vine_issue in cached_volume.get("results", []):
        issue_number = str(comic_vine_issue.get("issue_number", "")).strip()
        override = (issue_overrides.get(issue_number, {}))

        issues.append(
            build_issue(
                        comic_vine_issue=comic_vine_issue,
                        universe_designation=override.get("universeDesignation", universe_designation),
                        issue_type=override.get("type", issue_type)
                    )
        )

    issues.sort(key=issue_sort_key)

    return {
        "title": series_config["title"],
        "volume": series_config.get("volume"),
        "startYear": series_config.get("startYear"),
        "endYear": series_config.get("endYear"),
        "externalIds": [
            {
                "source": COMIC_VINE_SOURCE,
                "externalId": str(volume_id),
                "url": choose_series_url(cached_volume)
            }
        ],
        "issues": issues
    }


def validate_catalog(catalog: dict) -> None:
    declared_universes = { universe["designation"] for universe in catalog.get("universes", []) }

    series_external_ids = set()
    issue_external_ids = set()

    for series in catalog.get("series", []):
        series_title = series["title"]

        for external_id in series.get("externalIds", []):
            key = (external_id["source"], external_id["externalId"])

            if key in series_external_ids:
                raise ValueError(
                    f"Duplicate series externalId {key[0]}:{key[1]}"
                )

            series_external_ids.add(key)

        issue_numbers = set()

        for issue in series.get("issues", []):
            issue_number = issue["number"]

            if issue_number in issue_numbers:
                raise ValueError(
                    f"Duplicate issue number '{issue_number}' in series '{series_title}'"
                )

            issue_numbers.add(issue_number)

            issue_type = (issue["type"].upper())

            if issue_type not in VALID_ISSUE_TYPES:
                raise ValueError(
                    f"Invalid issue type '{issue['type']}' for {series_title} #{issue_number}"
                )

            universe_designation = (issue.get("universeDesignation"))

            if (universe_designation is not None and universe_designation not in declared_universes):
                raise ValueError(
                    f"Unknown universe '{universe_designation}' for {series_title} #{issue_number}"
                )

            for external_id in issue.get("externalIds", []):
                key = (external_id["source"], external_id["externalId"])

                if key in issue_external_ids:
                    raise ValueError(
                        f"Duplicate issue external ID {key[0]}:{key[1]}"
                    )

                issue_external_ids.add(key)


def build_catalog(config: dict, cached_volumes: dict[int, dict]) -> dict:
    built_series = []

    for series_config in config.get("series", []):
        volume_id = int(series_config["comicVineVolumeId"])
        cached_volume = (cached_volumes.get(volume_id))

        if cached_volume is None:
            raise ValueError(
                f"No cached Comic Vine volume found for {volume_id}"
            )

        built_series.append(
            build_series(series_config=series_config,
                         cached_volume=cached_volume)
        )

    catalog = {
        "publisher": config["publisher"],
        "universes": config.get("universes", []),
        "series": built_series
    }

    validate_catalog(catalog)

    return catalog


def load_json(path: Path) -> dict:
    if not path.exists():
        raise RuntimeError(
            f"File not found: {path}"
        )

    data = json.loads(path.read_text(encoding="utf-8"))

    if not isinstance(data, dict):
        raise RuntimeError(
            f"Expected JSON object: {path}"
        )

    return data


def get_cache_file(volume_id: int, cache_dir: Path = CACHE_DIR) -> Path:
    return (cache_dir / f"volume_{volume_id}.json")


def load_cached_volume(volume_id: int, cache_dir: Path = CACHE_DIR) -> dict:
    cache_file = get_cache_file(
        volume_id=volume_id,
        cache_dir=cache_dir
    )

    cached_volume = load_json(cache_file)

    cached_volume_id = (cached_volume.get("volume_id"))

    if cached_volume_id != volume_id:
        raise RuntimeError(
            f"Cached volume ID {cached_volume_id} does not match requested volume {volume_id}"
        )

    return cached_volume


def load_cached_volumes(config: dict, cache_dir: Path = CACHE_DIR) -> dict[int, dict]:
    cached_volumes = {}

    for series_config in config.get("series", []):
        volume_id = int(series_config["comicVineVolumeId"])

        if volume_id in cached_volumes:
            continue

        cached_volumes[volume_id] = load_cached_volume(
            volume_id=volume_id,
            cache_dir=cache_dir
        )

    return cached_volumes


def write_catalog(catalog: dict, output_file: Path) -> None:
    output_file.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    output_file.write_text(
        json.dumps(
            catalog,
            indent=2,
            ensure_ascii=False
        ) + "\n",
        encoding="utf-8"
    )


def main() -> None:
    parser = argparse.ArgumentParser(
        description=(
            "Build an app comic catalog from cached Comic Vine data."
        )
    )

    parser.add_argument(
        "--config",
        required=True,
        type=Path,
        help="Catalog build configuration"
    )

    parser.add_argument(
        "--output",
        required=True,
        type=Path,
        help="Generated catalog JSON"
    )

    parser.add_argument(
        "--cache-dir",
        type=Path,
        default=CACHE_DIR,
        help="Comic Vine issue cache directory"
    )

    arguments = parser.parse_args()

    config = load_json(arguments.config)

    cached_volumes = (load_cached_volumes(
        config=config,
        cache_dir=arguments.cache_dir
    ))

    catalog = build_catalog(
        config=config,
        cached_volumes=cached_volumes
    )

    write_catalog(
        catalog=catalog,
        output_file=arguments.output
    )

    issue_count = sum(len(series["issues"]) for series in catalog["series"])

    print(f"Generated {len(catalog['series'])} series with {issue_count} issues.")
    print(f"Output: {arguments.output}")


if __name__ == '__main__':
    main()