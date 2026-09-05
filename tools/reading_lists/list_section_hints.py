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


def main() -> None:
    data = json.loads(
        READING_LIST_FILE.read_text(
            encoding="utf-8"
        )
    )

    items = data.get(
        "items",
        []
    )

    for item in items:
        position = item["position"]

        series = item["series"]["title"]
        number = item["issue"]["number"]
        date = (
                item["issue"].get(
                    "publicationDate"
                )
                or "Unknown date"
        )

        notes = (
                item.get("notes")
                or ""
        )

        print(
            f"{position:>3} | "
            f"{date:<7} | "
            f"{series} #{number} | "
            f"{notes}"
        )


if __name__ == "__main__":
    main()