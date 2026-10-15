from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

from build_catalog import(
    build_anomaly_report,
    build_catalog,
    format_anomaly_report,
    load_cached_volumes,
    write_catalog,
)
from cache_mapped_volume_issues import(
    cache_volume,
)
from promote_catalog_preview import(
    load_json as load_catalog_json,
    merge_catalog,
    write_atomically,
)
from compare_catalog_preview import(
    compare_catalogs,
    format_catalog_comparison,
)
from search_unmapped_series import(
    build_report_entry,
    search_comic_vine,
)


TOOL_DIR = Path(__file__).resolve().parent

CONFIG_DIR = (
    TOOL_DIR /
    "catalog_configs"
)

OUTPUT_DIR = (
    TOOL_DIR /
    "output"
)


def slugify(value: str) -> str:
    return re.sub(
        r"[^a-z0-9]+",
        "_",
        value.lower()
    ).strip("_")


def build_local_series(title: str, volume: int | None, start_year: int | None, expected_issue_count: int | None, publisher: str) -> dict:
    return {
        "title": title,
        "volume": volume,
        "startYear": start_year,
        "localIssueCount": expected_issue_count or 0,
        "publisher": publisher
    }


def choose_candidate(entry: dict, input_func=input) -> int:
    candidates = entry.get("candidates", [])

    if not candidates:
        raise RuntimeError(
            "No Comic Vine candidates found"
        )

    displayed = candidates[:5]

    print()
    print("COMIC VINE CANDIDATES")
    print("=" * 72)

    for index, candidate in enumerate(displayed, start=1):
        publisher = candidate.get("publisher") or {}

        print(
            f"{index}. "
            f"{candidate.get('name')} | "
            f"{candidate.get('start_year')} | "
            f"{candidate.get('count_of_issues')} issues | "
            f"{publisher.get('name')} | "
            f"ID {candidate.get('id')} | "
            f"score {candidate.get('matchScore')}"
        )

    print()

    while True:
        selection = input_func(
            "Select candidate "
            f"[1 - {len(displayed)}]: "
        ).strip()

        try:
            index = int(selection)
        except ValueError:
            continue

        if 1 <= index <= len(displayed):
            candidate = displayed[index - 1]
            candidate_id = candidate.get("id")

            if candidate_id is None:
                raise RuntimeError(
                    "Selected candidate has no Comic Vine ID"
                )

            return int(candidate_id)


def build_ingest_config(
        title: str,
        volume: int | None,
        start_year: int | None,
        end_year: int | None,
        publisher: str,
        comic_vine_volume_id: int,
        expected_issue_count: int | None,
        universe_designation: str | None,
        universe_name: str | None,
        issue_type: str
) -> dict:
    universes = []

    if universe_designation is not None:
        if universe_name is None:
            raise ValueError(
                "--universe-name is required "
                "when --universe-designation "
                "is supplied"
            )

        universes.append(
            {
                "name": universe_name,
                "designation": universe_designation,
                "description": None
            }
        )

    series = {
        "title": title,
        "volume": volume,
        "startYear": start_year,
        "endYear": end_year,
        "comicVineVolumeId": comic_vine_volume_id,
        "defaultIssueType": issue_type
    }

    if expected_issue_count is not None:
        series["expectedIssueCount"] = expected_issue_count

    if universe_designation is not None:
        series["defaultUniverseDesignation"] = universe_designation

    return {
        "publisher": publisher,
        "universes": universes,
        "series": [series]
    }


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    path.write_text(
        json.dumps(
            data,
            indent=2,
            ensure_ascii=False
        )
        + "\n",
        encoding="utf-8"
    )


def run_ingest(arguments, input_func=input) -> dict:
    series_slug = slugify(arguments.title)

    comic_vine_volume_id = arguments.comic_vine_volume_id

    if comic_vine_volume_id is None:
        local_series = build_local_series(
            title=arguments.title,
            volume=arguments.volume,
            start_year=arguments.start_year,
            expected_issue_count=arguments.expected_issue_count,
            publisher=arguments.publisher
        )

        response = search_comic_vine(local_series)

        entry = build_report_entry(local_series, response)

        comic_vine_volume_id = choose_candidate(entry, input_func=input_func)

    config = build_ingest_config(
        title=arguments.title,
        volume=arguments.volume,
        start_year=arguments.start_year,
        end_year=arguments.end_year,
        publisher=arguments.publisher,
        comic_vine_volume_id=comic_vine_volume_id,
        expected_issue_count=arguments.expected_issue_count,
        universe_designation=arguments.universe_designation,
        universe_name=arguments.universe_name,
        issue_type=arguments.issue_type
    )

    config_path = arguments.config_output or (
        CONFIG_DIR /
        f"{series_slug}.json"
    )

    preview_path = arguments.preview_output or (
        OUTPUT_DIR /
        f"{series_slug}_catalog.json"
    )

    write_json(config_path, config)

    print()
    print("Config written:")
    print(f"  {config_path}")

    cache_volume(comic_vine_volume_id, refresh=getattr(arguments, "refresh", False))

    cached_volumes = load_cached_volumes(config=config)

    catalog = build_catalog(
        config=config,
        cached_volumes=cached_volumes
    )

    write_catalog(
        catalog=catalog,
        output_file=preview_path
    )

    print()
    print("Preview written:")
    print(f"  {preview_path}")

    report = build_anomaly_report(
        config=config,
        catalog=catalog
    )

    print(format_anomaly_report(report))

    base_catalog = None

    if arguments.base_catalog is not None:
        base_catalog = load_catalog_json(
            arguments.base_catalog
        )

        comparisons = compare_catalogs(
            base_catalog=base_catalog,
            generated_catalog=catalog
        )

        print()
        print(format_catalog_comparison(comparisons))

    if arguments.promote:
        if (arguments.base_catalog is None):
            raise ValueError(
                "--base-catalog is required "
                "when --promote is used"
            )

        merged_catalog = merge_catalog(
            base_catalog=base_catalog,
            generated_catalog=catalog
        )

        write_atomically(
            destination=arguments.base_catalog,
            data=merged_catalog
        )

        print()
        print("PROMOTED")
        print(f"  {arguments.base_catalog}")

    else:
        print()
        print(
            "Preview only. "
            "No app catalog was modified."
        )

    return {
        "comicVineVolumeId": comic_vine_volume_id,
        "configPath": config_path,
        "previewPath": preview_path,
        "report": report
    }


def build_argument_parser():
    parser = argparse.ArgumentParser(
        description=(
            "Search, cache, build, review, and optionally promote a Comic Vine series."
        )
    )

    parser.add_argument(
        "--title",
        required=True
    )

    parser.add_argument(
        "--publisher",
        required=True
    )

    parser.add_argument(
        "--volume",
        type=int
    )

    parser.add_argument(
        "--start-year",
        type=int
    )

    parser.add_argument(
        "--end-year",
        type=int
    )

    parser.add_argument(
        "--expected-issue-count",
        type=int
    )

    parser.add_argument(
        "--comic-vine-volume-id",
        type=int
    )

    parser.add_argument(
        "--universe-designation"
    )

    parser.add_argument(
        "--universe-name"
    )

    parser.add_argument(
        "--issue-type",
        default="REGULAR",
        choices=[
            "REGULAR",
            "ANNUAL",
            "SPECIAL",
            "ONE_SHOT",
            "GIANT_SIZE",
            "PREVIEW"
        ]
    )

    parser.add_argument(
        "--config-output",
        type=Path
    )

    parser.add_argument(
        "--preview-output",
        type=Path
    )

    parser.add_argument(
        "--promote",
        action="store_true"
    )

    parser.add_argument(
        "--base-catalog",
        type=Path
    )

    parser.add_argument(
        "--refresh",
        action="store_true",
        help="Refresh cached Comic Vine issue data before building the preview."
    )

    return parser


def main() -> None:
    parser = build_argument_parser()
    arguments = parser.parse_args()
    run_ingest(arguments)



if __name__ == "__main__":
    main()