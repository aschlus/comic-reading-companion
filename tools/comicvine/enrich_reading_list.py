from __future__ import annotations

import json
from pathlib import Path


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

CACHE_DIR = (
        TOOL_DIR /
        "cache" /
        "issues"
)

OUTPUT_DIR = (
        TOOL_DIR /
        "output"
)

OUTPUT_FILE = (
        OUTPUT_DIR /
        "spider_man_volume_2.enriched.preview.json"
)

COMIC_VINE_SOURCE = "COMIC_VINE"


def load_json(
        path: Path
):
    if not path.exists():
        raise RuntimeError(
            f"File not found: {path}"
        )

    return json.loads(
        path.read_text(
            encoding="utf-8"
        )
    )


def get_cache_file(
        volume_id: int
) -> Path:
    return (
            CACHE_DIR /
            f"volume_{volume_id}.json"
    )


def load_cached_volume(
        volume_id: int
) -> dict:
    cache_file = get_cache_file(
        volume_id
    )

    cached_volume = load_json(
        cache_file
    )

    if (
            cached_volume.get("volume_id")
            != volume_id
    ):
        raise RuntimeError(
            "Cached volume ID does not match "
            f"{volume_id}"
        )

    return cached_volume


def normalize_issue_number(
        issue_number
) -> str:
    return str(
        issue_number
    ).strip().lower()


def get_mapping_key(
        title: str,
        volume,
        start_year
) -> tuple:
    return (
        title.strip(),
        volume,
        start_year
    )


def load_mappings() -> dict:
    mappings = load_json(
        MAPPINGS_FILE
    )

    result = {}

    for mapping in mappings:
        key = get_mapping_key(
            mapping["localTitle"],
            mapping.get("localVolume"),
            mapping.get("localStartYear")
        )

        if key in result:
            raise RuntimeError(
                "Duplicate local series mapping: "
                f"{key}"
            )

        result[key] = mapping

    return result


def build_comic_vine_issue_index(
        cached_volume: dict
) -> tuple[dict, dict]:
    issues_by_number = {}
    duplicates = {}

    for issue in cached_volume.get(
            "results",
            []
    ):
        issue_number = (
            normalize_issue_number(
                issue.get(
                    "issue_number",
                    ""
                )
            )
        )

        if not issue_number:
            continue

        if issue_number in issues_by_number:
            duplicates.setdefault(
                issue_number,
                [
                    issues_by_number[
                        issue_number
                    ]
                ]
            ).append(issue)

            continue

        issues_by_number[
            issue_number
        ] = issue

    return (
        issues_by_number,
        duplicates
    )


def choose_cover_url(
        comic_vine_issue: dict
) -> str | None:
    image = (
            comic_vine_issue.get("image")
            or {}
    )

    return (
            image.get("super_url")
            or image.get("original_url")
    )


def update_external_ids(
        local_issue: dict,
        comic_vine_issue: dict
) -> None:
    comic_vine_id = str(
        comic_vine_issue["id"]
    )

    comic_vine_url = (
        comic_vine_issue.get(
            "site_detail_url"
        )
    )

    external_ids = local_issue.setdefault(
        "externalIds",
        []
    )

    existing = next(
        (
            external_id
            for external_id
            in external_ids
            if external_id.get("source")
               == COMIC_VINE_SOURCE
        ),
        None
    )

    if existing is None:
        external_ids.append(
            {
                "source":
                    COMIC_VINE_SOURCE,
                "externalId":
                    comic_vine_id,
                "url":
                    comic_vine_url
            }
        )

        return

    existing["externalId"] = (
        comic_vine_id
    )

    existing["url"] = (
        comic_vine_url
    )


def enrich_local_issue(
        local_issue: dict,
        comic_vine_issue: dict
) -> bool:
    cover_url = choose_cover_url(
        comic_vine_issue
    )

    missing_cover = (
            cover_url is None
    )

    if cover_url is not None:
        local_issue[
            "coverUrl"
        ] = cover_url

    update_external_ids(
        local_issue =
        local_issue,
        comic_vine_issue =
        comic_vine_issue
    )

    return missing_cover


def enrich_series(
        reading_list: dict,
        mapping: dict
) -> dict:
    title = mapping["localTitle"]

    volume = mapping.get(
        "localVolume"
    )

    start_year = mapping.get(
        "localStartYear"
    )

    mapping_key = get_mapping_key(
        title,
        volume,
        start_year
    )

    local_items = []

    for item in reading_list.get(
            "items",
            []
    ):
        series = item.get(
            "series",
            {}
        )

        local_key = get_mapping_key(
            series.get(
                "title",
                ""
            ),
            series.get("volume"),
            series.get("startYear")
        )

        if local_key == mapping_key:
            local_items.append(
                item
            )

    matched = []
    unmatched = []
    ambiguous = []
    missing_cover = []

    issue_mappings = mapping.get(
        "issueMappings"
    )

    if issue_mappings is not None:
        for item in local_items:
            local_issue = item.get(
                "issue",
                {}
            )

            local_issue_number = (
                normalize_issue_number(
                    local_issue.get(
                        "number",
                        ""
                    )
                )
            )

            issue_mapping = (
                issue_mappings.get(
                    local_issue_number
                )
            )

            if issue_mapping is None:
                unmatched.append(
                    local_issue_number
                )

                continue

            comic_vine_volume_id = (
                issue_mapping[
                    "comicVineVolumeId"
                ]
            )

            comic_vine_issue_number = (
                normalize_issue_number(
                    issue_mapping.get(
                        "comicVineIssueNumber",
                        local_issue_number
                    )
                )
            )

            cached_volume = (
                load_cached_volume(
                    comic_vine_volume_id
                )
            )

            (
                issues_by_number,
                duplicates
            ) = build_comic_vine_issue_index(
                cached_volume
            )

            if (
                    comic_vine_issue_number
                    in duplicates
            ):
                ambiguous.append(
                    local_issue_number
                )

                continue

            comic_vine_issue = (
                issues_by_number.get(
                    comic_vine_issue_number
                )
            )

            if comic_vine_issue is None:
                unmatched.append(
                    local_issue_number
                )

                continue

            has_missing_cover = (
                enrich_local_issue(
                    local_issue =
                    local_issue,
                    comic_vine_issue =
                    comic_vine_issue
                )
            )

            if has_missing_cover:
                missing_cover.append(
                    local_issue_number
                )

            matched.append(
                {
                    "number":
                        local_issue_number,
                    "comicVineVolumeId":
                        comic_vine_volume_id,
                    "comicVineIssueNumber":
                        comic_vine_issue_number,
                    "comicVineIssueId":
                        comic_vine_issue.get(
                            "id"
                        ),
                    "coverDate":
                        comic_vine_issue.get(
                            "cover_date"
                        )
                }
            )

        return {
            "series": title,
            "volume": volume,
            "mappingType":
                "PER_ISSUE",
            "comicVineVolumeId":
                None,
            "matched": matched,
            "unmatched": unmatched,
            "ambiguous": ambiguous,
            "missingCover":
                missing_cover
        }

    comic_vine_volume_id = mapping[
        "comicVineVolumeId"
    ]

    cached_volume = load_cached_volume(
        comic_vine_volume_id
    )

    (
        issues_by_number,
        duplicates
    ) = build_comic_vine_issue_index(
        cached_volume
    )

    for item in local_items:
        local_issue = item.get(
            "issue",
            {}
        )

        issue_number = (
            normalize_issue_number(
                local_issue.get(
                    "number",
                    ""
                )
            )
        )

        if issue_number in duplicates:
            ambiguous.append(
                issue_number
            )

            continue

        comic_vine_issue = (
            issues_by_number.get(
                issue_number
            )
        )

        if comic_vine_issue is None:
            unmatched.append(
                issue_number
            )

            continue

        has_missing_cover = (
            enrich_local_issue(
                local_issue =
                local_issue,
                comic_vine_issue =
                comic_vine_issue
            )
        )

        if has_missing_cover:
            missing_cover.append(
                issue_number
            )

        matched.append(
            {
                "number":
                    issue_number,
                "comicVineVolumeId":
                    comic_vine_volume_id,
                "comicVineIssueNumber":
                    issue_number,
                "comicVineIssueId":
                    comic_vine_issue.get(
                        "id"
                    ),
                "coverDate":
                    comic_vine_issue.get(
                        "cover_date"
                    )
            }
        )

    return {
        "series": title,
        "volume": volume,
        "mappingType":
            "VOLUME",
        "comicVineVolumeId":
            comic_vine_volume_id,
        "matched": matched,
        "unmatched": unmatched,
        "ambiguous": ambiguous,
        "missingCover":
            missing_cover
    }


def print_report(
        report: dict
) -> None:
    print()

    volume_text = (
        f"Vol. {report['volume']}"
        if report["volume"] is not None
        else ""
    )

    print(
        f"{report['series']} "
        f"{volume_text}".rstrip()
    )

    print("=" * 72)

    if (
            report["mappingType"]
            == "PER_ISSUE"
    ):
        print(
            "Comic Vine mapping: "
            "per issue"
        )
    else:
        print(
            "Comic Vine volume: "
            f"{report['comicVineVolumeId']}"
        )

    print(
        "Matched:           "
        f"{len(report['matched'])}"
    )

    print(
        "Unmatched:         "
        f"{len(report['unmatched'])}"
    )

    print(
        "Ambiguous:         "
        f"{len(report['ambiguous'])}"
    )

    print(
        "Missing covers:    "
        f"{len(report['missingCover'])}"
    )

    if report["unmatched"]:
        print()
        print(
            "Unmatched issue numbers:"
        )

        print(
            ", ".join(
                report["unmatched"]
            )
        )

    if report["ambiguous"]:
        print()
        print(
            "Ambiguous issue numbers:"
        )

        print(
            ", ".join(
                report["ambiguous"]
            )
        )

    if report["missingCover"]:
        print()
        print(
            "Matched issues without "
            "cover images:"
        )

        print(
            ", ".join(
                report["missingCover"]
            )
        )


def main() -> None:
    reading_list = load_json(
        READING_LIST_FILE
    )

    mappings = load_mappings()

    reports = []

    for mapping in mappings.values():
        report = enrich_series(
            reading_list =
            reading_list,
            mapping = mapping
        )

        reports.append(
            report
        )

    OUTPUT_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    OUTPUT_FILE.write_text(
        json.dumps(
            reading_list,
            indent=2,
            ensure_ascii=False
        )
        + "\n",
        encoding="utf-8"
    )

    for report in reports:
        print_report(
            report
        )

    total_matched = sum(
        len(report["matched"])
        for report in reports
    )

    total_unmatched = sum(
        len(report["unmatched"])
        for report in reports
    )

    total_ambiguous = sum(
        len(report["ambiguous"])
        for report in reports
    )

    total_missing_cover = sum(
        len(report["missingCover"])
        for report in reports
    )

    print()
    print()
    print(
        "OVERALL ENRICHMENT SUMMARY"
    )

    print("=" * 72)

    print(
        f"Matched:           "
        f"{total_matched}"
    )

    print(
        f"Unmatched:         "
        f"{total_unmatched}"
    )

    print(
        f"Ambiguous:         "
        f"{total_ambiguous}"
    )

    print(
        f"Missing covers:    "
        f"{total_missing_cover}"
    )

    print()
    print("=" * 72)

    print(
        "Preview written to:"
    )

    print(
        f"  {OUTPUT_FILE}"
    )


if __name__ == "__main__":
    main()