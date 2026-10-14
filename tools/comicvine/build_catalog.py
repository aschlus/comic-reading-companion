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


def build_issue(comic_vine_issue: dict, universe_designation: str | None, issue_type: str, output_number: str | None = None) -> dict:
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
        "number": output_number or issue_number,
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


def get_configured_volume_ids(series_config: dict) -> list[int]:
    issue_mappings = series_config.get("issueMappings")

    if issue_mappings is None:
        return [int(series_config["comicVineVolumeId"])]

    volume_ids = []

    for issue_mapping in issue_mappings.values():
        volume_id = int(issue_mapping["comicVineVolumeId"])

        if volume_id not in volume_ids:
            volume_ids.append(volume_id)

    if not volume_ids:
        raise ValueError(
            "issueMappings must contain at least one issue"
        )

    return volume_ids


def build_mapped_series(series_config: dict, cached_volumes: dict[int, dict]) -> dict:
    issue_mappings = series_config["issueMappings"]
    universe_designation = series_config.get("defaultUniverseDesignation")
    default_issue_type = series_config.get("defaultIssueType", "REGULAR")
    issue_overrides = series_config.get("issueOverrides", {})

    issues = []
    series_external_ids = []

    for local_number, issue_mapping in issue_mappings.items():
        volume_id = int(issue_mapping["comicVineVolumeId"])
        cached_volume = cached_volumes.get(volume_id)

        if cached_volume is None:
            raise ValueError(
                f"No cached Comic Vine volume found for {volume_id}"
            )

        cached_volume_id = cached_volume.get("volume_id")

        if cached_volume_id != volume_id:
            raise ValueError(
                f"Cached volume ID {cached_volume_id} does not match configured volume {volume_id}"
            )

        if not any(external_id["externalId"] == str(volume_id) for external_id in series_external_ids):
            series_external_ids.append(
                {
                    "source": COMIC_VINE_SOURCE,
                    "externalId": str(volume_id),
                    "url": choose_series_url(cached_volume)
                }
            )

        comic_vine_number = str(issue_mapping.get("comicVineIssueNumber", local_number)).strip()

        matches = [comic_vine_issue for comic_vine_issue in cached_volume.get("results", [])
                   if str(comic_vine_issue.get("issue_number", "")).strip().casefold() == comic_vine_number.casefold()]

        if not matches:
            raise ValueError(
                f"Comic Vine issue #{comic_vine_number} not found in volume {volume_id}"
            )

        if len(matches) > 1:
            raise ValueError(
                f"Multiple Comic Vine issues #{comic_vine_number} found in volume {volume_id}"
            )

        override = issue_overrides.get(str(local_number), {})

        issues.append(
            build_issue(
                comic_vine_issue=matches[0],
                universe_designation=override.get("universeDesignation", universe_designation),
                issue_type=override.get("type", default_issue_type),
                output_number=str(local_number)
            )
        )

    issues.sort(key=issue_sort_key)

    return {
        "title": series_config["title"],
        "volume": series_config.get("volume"),
        "startYear": series_config.get("startYear"),
        "endYear": series_config.get("endYear"),
        "externalIds": series_external_ids,
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


def is_nonstandard_issue_number(issue_number: str) -> bool:
    text = str(issue_number).strip()

    return(
        re.fullmatch(r"\d+(?:\.\d+)?", text) is None
    )


def find_duplicates(values: list[str]) -> list[str]:
    seen = set()
    duplicates = []

    for value in values:
        if (value in seen and value not in duplicates):
            duplicates.append(value)

        seen.add(value)

    return duplicates


def build_anomaly_report(config: dict, catalog: dict) -> dict:
    config_series = config.get("series", [])
    catalog_series = catalog.get("series", [])

    series_reports = []

    all_external_ids = []

    for series_index, series in enumerate(catalog_series):
        if series_index < len(config_series):
            series_config = config_series[series_index]
        else:
            series_config = {}

        issues = series.get("issues", [])

        missing_covers = []
        missing_titles = []
        missing_dates = []
        nonstandard_numbers = []
        issue_numbers = []
        review_numbers = []

        for issue in issues:
            number = str(issue.get("number", ""))

            issue_numbers.append(number)

            needs_review = False

            if not issue.get("coverUrl"):
                missing_covers.append(number)
                needs_review = True

            if not issue.get("title"):
                missing_titles.append(number)
                needs_review = True

            if not issue.get("publicationDate"):
                missing_dates.append(number)
                needs_review = True

            if is_nonstandard_issue_number(number):
                nonstandard_numbers.append(number)
                needs_review = True

            if needs_review:
                review_numbers.append(number)

            for external_id in issue.get("externalIds", []):
                all_external_ids.append(
                    (
                        external_id.get("source"),
                        external_id.get("externalId")
                    )
                )

        expected_count = series_config.get("expectedIssueCount")

        series_reports.append(
            {
                "title": series.get("title"),
                "volume": series.get("volume"),
                "startYear": series.get("startYear"),
                "expectedIssueCount": expected_count,
                "actualIssueCount": len(issues),
                "missingCovers": missing_covers,
                "missingTitles": missing_titles,
                "missingPublicationDates": missing_dates,
                "nonstandardIssueNumbers": nonstandard_numbers,
                "duplicateIssueNumbers": find_duplicates(issue_numbers),
                "reviewIssueNumbers": review_numbers
            }
        )

    duplicate_external_ids = []

    seen_external_ids = set()

    for external_id in all_external_ids:
        if (external_id in seen_external_ids and external_id not in duplicate_external_ids):
            duplicate_external_ids.append(external_id)

        seen_external_ids.add(external_id)

    return {
        "publisher": catalog.get("publisher"),
        "series": series_reports,
        "duplicateIssueExternalIds": duplicate_external_ids
    }


def format_anomaly_report(report: dict) -> str:
    lines = [
        "",
        "CATALOG REVIEW",
        "=" * 72
    ]

    for series in report.get("series", []):
        volume = series.get("volume")

        volume_text = (
            f"Vol. {volume}"
            if volume is not None
            else "No volume"
        )

        year = series.get("startYear")

        year_text = (
            str(year)
            if year is not None
            else "Unknown year"
        )

        lines.extend(
            [
                "",
                (
                    f"{series['title']} "
                    f"| {volume_text} "
                    f"| {year_text}"
                ),
                "-" * 72
            ]
        )

        expected = series.get("expectedIssueCount")
        actual = series["actualIssueCount"]

        if expected is None:
            lines.append(
                f"Issues: {actual}"
            )
        else:
            lines.append(
                "Issues: "
                f"{actual} / "
                f"{expected} expected"
            )

        fields = [
            (
                "Missing covers",
                "missingCovers"
            ),
            (
                "Missing titles",
                "missingTitles"
            ),
            (
                "Missing publication dates",
                "missingPublicationDates"
            ),
            (
                "Nonstandard issue numbers",
                "nonstandardIssueNumbers"
            ),
            (
                "Duplicate issue numbers",
                "duplicateIssueNumbers"
            )
        ]

        for label, key in fields:
            values = series.get(key, [])

            line = f"{label}: {len(values)}"

            if values:
                line += (
                    " ["
                    + ", ".join(
                        f"#{value}"
                        for value in values
                    )
                    + "]"
                )

            lines.append(line)

        lines.append(
            "Issues requiring review: "
            f"{len(series['reviewIssueNumbers'])}"
        )

    duplicate_external_ids = report.get("duplicateIssueExternalIds", [])

    lines.extend(
        [
            "",
            (
                "Duplicate issue external IDs: "
                f"{len(duplicate_external_ids)}"
            )
        ]
    )

    if duplicate_external_ids:
        lines.extend(
            (
                f"  {source}:{external_id}"
                for source, external_id
                in duplicate_external_ids
            )
        )

    return "\n".join(lines)


def validate_expected_issue_count(series_config: dict, built_series: dict) -> None:
    expected_count = series_config.get("expectedIssueCount")

    if expected_count is None:
        return

    expected_count = int(expected_count)

    actual_count = len(built_series.get("issues", []))

    if actual_count != expected_count:
        raise ValueError(
            f"Expected {expected_count} issues "
            f"for '{series_config['title']}' "
            f"but built {actual_count}"
        )


def build_catalog(config: dict, cached_volumes: dict[int, dict]) -> dict:
    built_series = []

    for series_config in config.get("series", []):
        if series_config.get("issueMappings") is not None:
            generated_series = build_mapped_series(
                series_config=series_config,
                cached_volumes=cached_volumes
            )
        else:
            volume_id = int(series_config["comicVineVolumeId"])

            cached_volume = cached_volumes.get(volume_id)

            if cached_volume is None:
                raise ValueError(
                    f"No cached Comic Vine volume found for {volume_id}"
                )

            generated_series = build_series(
                series_config=series_config,
                cached_volume=cached_volume
            )

        validate_expected_issue_count(
            series_config=series_config,
            built_series=generated_series
        )

        built_series.append(generated_series)


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
        for volume_id in get_configured_volume_ids(series_config):

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