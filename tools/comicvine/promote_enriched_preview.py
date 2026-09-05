from __future__ import annotations

import json
import os
import tempfile
from pathlib import Path


TOOL_DIR = Path(__file__).resolve().parent

PROJECT_DIR = (
        TOOL_DIR /
        ".." /
        ".."
).resolve()

SOURCE_FILE = (
        PROJECT_DIR /
        "app" /
        "src" /
        "main" /
        "assets" /
        "reading_lists" /
        "spider_man_volume_2.json"
)

PREVIEW_FILE = (
        TOOL_DIR /
        "output" /
        "spider_man_volume_2.enriched.preview.json"
)

EXPECTED_ISSUE_COUNT = 219


def load_json(
        path: Path
) -> dict:
    if not path.exists():
        raise RuntimeError(
            f"File not found: {path}"
        )

    return json.loads(
        path.read_text(
            encoding="utf-8"
        )
    )


def validate_preview(
        preview: dict
) -> None:
    items = preview.get(
        "items",
        []
    )

    if len(items) != EXPECTED_ISSUE_COUNT:
        raise RuntimeError(
            "Preview validation failed: "
            f"expected {EXPECTED_ISSUE_COUNT} "
            f"items, found {len(items)}"
        )

    missing_cover = []
    missing_comic_vine_id = []
    spiderfan_urls = []

    for item in items:
        position = item.get(
            "position"
        )

        issue = item.get(
            "issue",
            {}
        )

        cover_url = issue.get(
            "coverUrl"
        )

        if not cover_url:
            missing_cover.append(
                position
            )

        if (
                cover_url
                and "spiderfan.org"
                in cover_url.lower()
        ):
            spiderfan_urls.append(
                position
            )

        external_ids = issue.get(
            "externalIds",
            []
        )

        comic_vine_id = next(
            (
                external_id
                for external_id
                in external_ids
                if external_id.get(
                "source"
            )
                   == "COMIC_VINE"
            ),
            None
        )

        if comic_vine_id is None:
            missing_comic_vine_id.append(
                position
            )

    if missing_cover:
        raise RuntimeError(
            "Preview validation failed: "
            "missing covers at positions "
            + ", ".join(
                str(position)
                for position
                in missing_cover
            )
        )

    if missing_comic_vine_id:
        raise RuntimeError(
            "Preview validation failed: "
            "missing Comic Vine IDs at "
            "positions "
            + ", ".join(
                str(position)
                for position
                in missing_comic_vine_id
            )
        )

    if spiderfan_urls:
        raise RuntimeError(
            "Preview validation failed: "
            "SpiderFan test URLs remain at "
            "positions "
            + ", ".join(
                str(position)
                for position
                in spiderfan_urls
            )
        )


def write_atomically(
        destination: Path,
        data: dict
) -> None:
    destination.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    serialized = (
            json.dumps(
                data,
                indent=2,
                ensure_ascii=False
            )
            + "\n"
    )

    with tempfile.NamedTemporaryFile(
            mode="w",
            encoding="utf-8",
            delete=False,
            dir=destination.parent,
            suffix=".tmp"
    ) as temp_file:
        temp_file.write(
            serialized
        )

        temp_path = Path(
            temp_file.name
        )

    os.replace(
        temp_path,
        destination
    )


def main() -> None:
    preview = load_json(
        PREVIEW_FILE
    )

    validate_preview(
        preview
    )

    print(
        "Preview validation passed:"
    )

    print(
        f"  {EXPECTED_ISSUE_COUNT} issues"
    )

    print(
        f"  {EXPECTED_ISSUE_COUNT} covers"
    )

    print(
        f"  {EXPECTED_ISSUE_COUNT} "
        "Comic Vine IDs"
    )

    print(
        "  0 SpiderFan test URLs"
    )

    write_atomically(
        destination=SOURCE_FILE,
        data=preview
    )

    print()
    print(
        "Updated bundled reading list:"
    )

    print(
        f"  {SOURCE_FILE}"
    )


if __name__ == "__main__":
    main()