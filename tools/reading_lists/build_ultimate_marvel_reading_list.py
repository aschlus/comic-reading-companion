from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


TOOL_DIR = Path(__file__).resolve().parent
PROJECT_DIR = TOOL_DIR.parent.parent

DEFAULT_CATALOG = (
        PROJECT_DIR
        / "app"
        / "src"
        / "main"
        / "assets"
        / "catalogs"
        / "ultimate_marvel_2000_2015_catalog.json"
)

DEFAULT_MANIFEST = (
        TOOL_DIR
        / "ultimate_marvel_arc_order.json"
)

DEFAULT_OUTPUT = (
        TOOL_DIR
        / "ultimate_marvel.preview.json"
)


def load_json(path: Path) -> dict:
    if not path.exists():
        raise RuntimeError(
            f"File not found: {path}"
        )

    data = json.loads(
        path.read_text(
            encoding="utf-8"
        )
    )

    if not isinstance(data, dict):
        raise RuntimeError(
            f"Expected JSON object: {path}"
        )

    return data


def series_key(
        title: str,
        volume: int | None
) -> tuple[str, int | None]:
    return (
        title,
        volume
    )


def build_series_index(
        catalog: dict
) -> dict[tuple[str, int | None], dict]:
    index: dict[
        tuple[str, int | None],
        dict
    ] = {}

    for series in catalog.get(
            "series",
            []
    ):
        key = series_key(
            title=series["title"],
            volume=series.get("volume")
        )

        if key in index:
            raise ValueError(
                "Duplicate catalog series: "
                f"{key}"
            )

        index[key] = series

    return index


def build_issue_index(
        series: dict
) -> dict[str, dict]:
    index: dict[str, dict] = {}

    for issue in series.get(
            "issues",
            []
    ):
        number = str(
            issue["number"]
        )

        if number in index:
            raise ValueError(
                "Duplicate issue number "
                f"{series['title']} "
                f"Vol. {series.get('volume')} "
                f"#{number}"
            )

        index[number] = issue

    return index


def expand_issue_numbers(
        values: list[str]
) -> list[str]:
    expanded: list[str] = []

    for value in values:
        text = str(value).strip()

        match = re.fullmatch(
            r"(\d+)-(\d+)",
            text
        )

        if match is None:
            expanded.append(text)
            continue

        start = int(
            match.group(1)
        )
        end = int(
            match.group(2)
        )

        if end < start:
            raise ValueError(
                "Invalid descending issue range: "
                f"{text}"
            )

        expanded.extend(
            str(number)
            for number
            in range(
                start,
                end + 1
            )
        )

    return expanded


def expand_manifest_entries(entries: list[dict]) -> list[tuple[tuple[str, int | None, str], dict]]:
    expanded: list[tuple[tuple[str, int | None, str], dict]] = []

    for entry in entries:
        title = entry["series"]
        volume = entry.get("volume")
        issue_numbers = expand_issue_numbers(entry.get("issues", []))

        if not issue_numbers:
            raise ValueError(
                f"Entry contains no issues: {title} Vol, {volume}"
            )

        for issue_number in issue_numbers:
            expanded.append(((title, volume, issue_number), entry))

    return expanded


def resolve_arc_issue_order(arc_title: str, entries: list[dict], internal_reading_order: list[dict] | None) -> list[tuple[tuple[str, int | None, str], dict]]:
    membership_entries = expand_manifest_entries(entries)
    membership_by_identity: dict[tuple[str, int | None, str], dict] = {}

    for identity, entry in membership_entries:
        if identity in membership_by_identity:
            raise ValueError(
                f"Duplicate issue within arc '{arc_title}': {identity[0]} Vol. {identity[1]} #{identity[2]}"
            )

        membership_by_identity[identity] = entry

    if internal_reading_order is None:
        return membership_entries

    ordered_entries = expand_manifest_entries(internal_reading_order)

    ordered_identities = [
        identity
        for identity, _
        in ordered_entries
    ]

    if len(ordered_identities) != len(set(ordered_identities)):
        raise ValueError(
            f"Arc '{arc_title}' contains duplicate issues in internalReadingOrder"
        )

    membership_identities = set(membership_by_identity)
    ordered_identity_set = set(ordered_identities)

    if membership_identities != ordered_identity_set:
        missing = membership_identities - ordered_identity_set
        extra = ordered_identity_set - membership_identities

        raise ValueError(
            f"Arc '{arc_title}' internalReadingOrder does not match its entries. "
            f"Missing: {sorted(missing)}; Extra: {sorted(extra)}"
        )

    return [(identity, membership_by_identity[identity])
            for identity
            in ordered_identities
            ]


def catalog_issue_identities(
        catalog: dict
) -> set[
    tuple[str, int | None, str]
]:
    identities: set[
        tuple[str, int | None, str]
    ] = set()

    for series in catalog.get(
            "series",
            []
    ):
        for issue in series.get(
                "issues",
                []
        ):
            identities.add(
                (
                    series["title"],
                    series.get("volume"),
                    str(issue["number"])
                )
            )

    return identities


def build_universe_override(list_universe: dict, catalog: dict, issue: dict) -> dict | None:
    list_designation = list_universe["designation"]
    issue_designation = issue.get("universeDesignation")

    if issue_designation == list_designation:
        return None

    if issue_designation is None:
        return { "mode": "NONE" }

    matching_universe = next(
        (
            universe
            for universe
            in catalog.get("universes", [])
            if (
                universe.get("designation") == issue_designation
            )
        ),
        None
    )

    if matching_universe is None:
        raise ValueError(
            f"Issue references universe '{issue_designation}', but that universe is not declared in the catalog"
        )

    return {
        "mode": "UNIVERSE",
        "universe": {
            "name": matching_universe["name"],
            "designation": matching_universe["designation"]
        }
    }


def build_reading_list(
        manifest: dict,
        catalog: dict
) -> dict:
    if (
            manifest.get("publisher")
            != catalog.get("publisher")
    ):
        raise ValueError(
            "Manifest and catalog publishers "
            "do not match"
        )

    arcs = manifest.get(
        "arcs",
        []
    )

    expected_arc_count = (
        manifest.get(
            "expectedArcCount"
        )
    )

    if (
            expected_arc_count is not None
            and len(arcs)
            != expected_arc_count
    ):
        raise ValueError(
            "Expected "
            f"{expected_arc_count} arcs, "
            f"found {len(arcs)}"
        )

    series_index = (
        build_series_index(
            catalog
        )
    )

    sections: list[dict] = []
    items: list[dict] = []

    seen_issues: set[
        tuple[str, int | None, str]
    ] = set()

    item_position = 1

    for section_position, arc in enumerate(
            arcs,
            start=1
    ):
        arc_title = (
            arc.get("title", "")
            .strip()
        )

        if not arc_title:
            raise ValueError(
                "Arc at position "
                f"{section_position} "
                "has a blank title"
            )

        entries = arc.get(
            "entries",
            []
        )

        if not entries:
            raise ValueError(
                f"Arc '{arc_title}' "
                "contains no entries"
            )

        ordered_arc_entries = (
            resolve_arc_issue_order(
                arc_title=arc_title,
                entries=entries,
                internal_reading_order=arc.get("internalReadingOrder")
            )
        )

        sections.append(
            {
                "position":
                    section_position,
                "title":
                    arc_title,
                "description":
                    arc.get(
                        "description"
                    )
            }
        )

        for identity, entry in ordered_arc_entries:
            (title, volume, issue_number) = identity
            key = series_key(
                title=title,
                volume=volume
            )
            series = series_index.get(key)

            if series is None:
                raise ValueError(
                    f"Series not found in catalog: {title} Vol. {volume}"
                )

            issue_index = build_issue_index(series)
            issue = issue_index.get(issue_number)

            if issue is None:
                raise ValueError(
                    f"Issue not found in catalog: {title} Vol. {volume} #{issue_number}"
                )

            if identity in seen_issues:
                raise ValueError(
                    f"Duplicate reading-list issue: {title} Vol. {volume} #{issue_number}"
                )

            seen_issues.add(identity)

            item = {
                    "position": item_position,
                    "sectionPosition": section_position,
                    "series": {
                        "title": series["title"],
                        "volume": series.get("volume"),
                        "startYear": series.get("startYear"),
                        "endYear": series.get("endYear")
                    },
                    "issue": {
                        "number": issue["number"],
                        "title": issue.get("title"),
                        "publicationDate": issue.get("publicationDate"),
                        "coverUrl": issue.get("coverUrl"),
                        "description": issue.get("description"),
                        "type": issue["type"],
                        "externalIds": issue.get("externalIds", [])
                    },
                    "required": entry.get("required", True),
                    "notes": entry.get("notes")
                }

            universe_override = (
                build_universe_override(
                    list_universe=manifest["universe"],
                    catalog=catalog,
                    issue=issue
                )
            )

            if universe_override is not None:
                item["universeOverride"] = (universe_override)

            items.append(item)

            item_position += 1

    expected_issue_count = (
        manifest.get(
            "expectedIssueCount"
        )
    )

    if (
            expected_issue_count is not None
            and len(items)
            != expected_issue_count
    ):
        raise ValueError(
            "Expected "
            f"{expected_issue_count} issues, "
            f"generated {len(items)}"
        )

    if manifest.get(
            "requireFullCatalogCoverage",
            False
    ):
        catalog_issues = (
            catalog_issue_identities(
                catalog
            )
        )

        missing = (
                catalog_issues
                - seen_issues
        )

        extra = (
                seen_issues
                - catalog_issues
        )

        if missing:
            formatted = "\n".join(
                "  "
                f"{title} Vol. {volume} "
                f"#{number}"
                for (
                    title,
                    volume,
                    number
                )
                in sorted(
                    missing,
                    key=lambda value: (
                        value[0],
                        value[1] or 0,
                        value[2]
                    )
                )
            )

            raise ValueError(
                "Reading order does not cover "
                "the full catalog.\n"
                "Missing:\n"
                f"{formatted}"
            )

        if extra:
            raise ValueError(
                "Reading order contains issues "
                "outside the catalog"
            )

    return {
        "title":
            manifest["title"],
        "description":
            manifest.get(
                "description"
            ),
        "publisher":
            manifest["publisher"],
        "universe":
            manifest["universe"],
        "sections":
            sections,
        "items":
            items
    }


def main() -> None:
    parser = argparse.ArgumentParser(
        description=(
            "Build the bundled Ultimate "
            "Marvel reading list from an "
            "arc-order manifest."
        )
    )

    parser.add_argument(
        "--manifest",
        type=Path,
        default=DEFAULT_MANIFEST
    )

    parser.add_argument(
        "--catalog",
        type=Path,
        default=DEFAULT_CATALOG
    )

    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT
    )

    arguments = parser.parse_args()

    manifest = load_json(
        arguments.manifest
    )

    catalog = load_json(
        arguments.catalog
    )

    reading_list = (
        build_reading_list(
            manifest=manifest,
            catalog=catalog
        )
    )

    arguments.output.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    arguments.output.write_text(
        json.dumps(
            reading_list,
            indent=2,
            ensure_ascii=False
        )
        + "\n",
        encoding="utf-8"
    )

    print(
        "Generated "
        f"{len(reading_list['sections'])} "
        "sections with "
        f"{len(reading_list['items'])} "
        "issues."
    )

    print(
        f"Output: {arguments.output}"
    )


if __name__ == "__main__":
    main()