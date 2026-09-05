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

PLAN_FILE = (
        TOOL_DIR /
        "section_plan.json"
)

OUTPUT_FILE = (
        TOOL_DIR /
        "spider_man_volume_2.sections.preview.json"
)


def load_json(
        path: Path
):
    return json.loads(
        path.read_text(
            encoding="utf-8"
        )
    )


def validate_plan(
        plan: list[dict]
) -> None:
    used_section_positions = set()
    assigned_item_positions = set()

    for section in plan:
        section_position = section[
            "position"
        ]

        start_position = section[
            "startPosition"
        ]

        end_position = section[
            "endPosition"
        ]

        if section_position in used_section_positions:
            raise RuntimeError(
                "Duplicate section position: "
                f"{section_position}"
            )

        used_section_positions.add(
            section_position
        )

        if start_position > end_position:
            raise RuntimeError(
                f"Section {section_position} "
                "has startPosition greater "
                "than endPosition"
            )

        for item_position in range(
                start_position,
                end_position + 1
        ):
            if (
                    item_position
                    in assigned_item_positions
            ):
                raise RuntimeError(
                    "Reading-list position "
                    f"{item_position} is assigned "
                    "to more than one section"
                )

            assigned_item_positions.add(
                item_position
            )


def main() -> None:
    reading_list = load_json(
        READING_LIST_FILE
    )

    plan = load_json(
        PLAN_FILE
    )

    validate_plan(
        plan
    )

    items = reading_list.get(
        "items",
        []
    )

    item_positions = {
        item["position"]
        for item in items
    }

    sections = []

    assignments = {}

    for section in plan:
        section_position = section[
            "position"
        ]

        sections.append(
            {
                "position":
                    section_position,
                "title":
                    section["title"],
                "description":
                    section.get(
                        "description"
                    )
            }
        )

        for item_position in range(
                section["startPosition"],
                section["endPosition"] + 1
        ):
            if item_position not in item_positions:
                raise RuntimeError(
                    "Section references missing "
                    "reading-list position "
                    f"{item_position}"
                )

            assignments[
                item_position
            ] = section_position

    for item in items:
        position = item[
            "position"
        ]

        section_position = (
            assignments.get(
                position
            )
        )

        if section_position is None:
            item.pop(
                "sectionPosition",
                None
            )
        else:
            item[
                "sectionPosition"
            ] = section_position

    reading_list[
        "sections"
    ] = sections

    OUTPUT_FILE.write_text(
        json.dumps(
            reading_list,
            indent=2,
            ensure_ascii=False
        )
        + "\n",
        encoding="utf-8"
    )

    print(
        f"Sections: "
        f"{len(sections)}"
    )

    print(
        f"Assigned items: "
        f"{len(assignments)}"
    )

    print(
        f"Unsectioned items: "
        f"{len(items) - len(assignments)}"
    )

    print()

    print(
        "Preview written to:"
    )

    print(
        f"  {OUTPUT_FILE}"
    )


if __name__ == "__main__":
    main()