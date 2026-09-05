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

COMIC_VINE_CACHE_FILE = (
        TOOL_DIR /
        "cache" /
        "issues" /
        "volume_78701.json"
)

OUTPUT_DIR = (
        TOOL_DIR /
        "output"
)

OUTPUT_FILE = (
        OUTPUT_DIR /
        "spider_man_volume_2.enriched.preview.json"
)


LOCAL_SERIES_TITLE = "Amazing Spider-Man"
LOCAL_SERIES_VOLUME = 2
LOCAL_ISSUE_NUMBER = "1"

COMIC_VINE_VOLUME_ID = 78701
COMIC_VINE_SOURCE = "COMIC_VINE"


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


def find_local_item(
        reading_list: dict
) -> dict:
    matches = []

    for item in reading_list.get(
            "items",
            []
    ):
        series = item.get(
            "series",
            {}
        )

        issue = item.get(
            "issue",
            {}
        )

        if (
                series.get("title")
                == LOCAL_SERIES_TITLE
                and series.get("volume")
                == LOCAL_SERIES_VOLUME
                and str(
            issue.get("number", "")
        ).strip()
                == LOCAL_ISSUE_NUMBER
        ):
            matches.append(item)

    if len(matches) != 1:
        raise RuntimeError(
            "Expected exactly one local match for "
            f"{LOCAL_SERIES_TITLE} "
            f"Vol. {LOCAL_SERIES_VOLUME} "
            f"#{LOCAL_ISSUE_NUMBER}, "
            f"but found {len(matches)}"
        )

    return matches[0]


def find_comic_vine_issue(
        cached_volume: dict
) -> dict:
    matches = [
        issue
        for issue in cached_volume.get(
            "results",
            []
        )
        if str(
            issue.get(
                "issue_number",
                ""
            )
        ).strip()
           == LOCAL_ISSUE_NUMBER
    ]

    if len(matches) != 1:
        raise RuntimeError(
            "Expected exactly one Comic Vine match "
            f"for issue #{LOCAL_ISSUE_NUMBER}, "
            f"but found {len(matches)}"
        )

    return matches[0]


def choose_cover_url(
        comic_vine_issue: dict
) -> str:
    image = (
            comic_vine_issue.get("image")
            or {}
    )

    cover_url = (
            image.get("super_url")
            or image.get("original_url")
    )

    if not cover_url:
        raise RuntimeError(
            "Comic Vine issue has no usable "
            "cover image URL"
        )

    return cover_url


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
            for external_id in external_ids
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


def main() -> None:
    reading_list = load_json(
        READING_LIST_FILE
    )

    cached_volume = load_json(
        COMIC_VINE_CACHE_FILE
    )

    cached_volume_id = cached_volume.get(
        "volume_id"
    )

    if (
            cached_volume_id
            != COMIC_VINE_VOLUME_ID
    ):
        raise RuntimeError(
            "Cached Comic Vine volume does not "
            "match expected volume "
            f"{COMIC_VINE_VOLUME_ID}"
        )

    local_item = find_local_item(
        reading_list
    )

    comic_vine_issue = (
        find_comic_vine_issue(
            cached_volume
        )
    )

    local_issue = local_item["issue"]

    original_cover = local_issue.get(
        "coverUrl"
    )

    comic_vine_cover = choose_cover_url(
        comic_vine_issue
    )

    local_issue["coverUrl"] = (
        comic_vine_cover
    )

    update_external_ids(
        local_issue = local_issue,
        comic_vine_issue =
        comic_vine_issue
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

    print(
        "Matched local issue:"
    )

    print(
        f"  {LOCAL_SERIES_TITLE} "
        f"Vol. {LOCAL_SERIES_VOLUME} "
        f"#{LOCAL_ISSUE_NUMBER}"
    )

    print()

    print(
        "Matched Comic Vine issue:"
    )

    print(
        f"  ID: "
        f"{comic_vine_issue.get('id')}"
    )

    print(
        f"  Title: "
        f"{comic_vine_issue.get('name')}"
    )

    print(
        f"  Cover date: "
        f"{comic_vine_issue.get('cover_date')}"
    )

    print()

    print(
        f"Old cover URL: "
        f"{original_cover}"
    )

    print(
        f"New cover URL: "
        f"{comic_vine_cover}"
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