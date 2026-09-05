from __future__ import annotations

import json
from collections import defaultdict
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


def load_json(
        path: Path
):
    return json.loads(
        path.read_text(
            encoding="utf-8"
        )
    )


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


def main() -> None:
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

    all_series = sorted(
        issue_counts.items(),
        key=lambda entry: (
            entry[0][2] or 0,
            entry[0][0].lower(),
            entry[0][1] or 0
        )
    )

    unmapped = [
        (key, count)
        for key, count in all_series
        if key not in mapped_keys
    ]

    print(
        f"Unique local series: {len(all_series)}"
    )

    print(
        f"Already mapped:      "
        f"{len(all_series) - len(unmapped)}"
    )

    print(
        f"Still unmapped:      {len(unmapped)}"
    )

    print()
    print("=" * 72)

    for (
            title,
            volume,
            start_year
    ), issue_count in unmapped:

        volume_text = (
            f"Vol. {volume}"
            if volume is not None
            else "Vol. ?"
        )

        year_text = (
            str(start_year)
            if start_year is not None
            else "Year ?"
        )

        print(
            f"{title} | "
            f"{volume_text} | "
            f"{year_text} | "
            f"{issue_count} local issue(s)"
        )


if __name__ == "__main__":
    main()