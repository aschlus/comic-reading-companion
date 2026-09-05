from __future__ import annotations

import json
import os
import time
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


BASE_URL = "https://comicvine.gamespot.com/api"

TOOL_DIR = Path(__file__).resolve().parent

API_KEY_FILE = (
        TOOL_DIR /
        ".comicvine_api_key"
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

PAGE_LIMIT = 100


def load_json(
        path: Path
):
    return json.loads(
        path.read_text(
            encoding="utf-8"
        )
    )


def load_api_key() -> str:
    environment_key = os.environ.get(
        "COMIC_VINE_API_KEY"
    )

    if environment_key:
        return environment_key.strip()

    if API_KEY_FILE.exists():
        api_key = API_KEY_FILE.read_text(
            encoding="utf-8"
        ).strip()

        if api_key:
            return api_key

    raise RuntimeError(
        "Comic Vine API key not found."
    )


def get_cache_file(
        volume_id: int
) -> Path:
    return (
            CACHE_DIR /
            f"volume_{volume_id}.json"
    )


def request_page(
        volume_id: int,
        offset: int
) -> dict:
    parameters = {
        "api_key": load_api_key(),
        "format": "json",
        "filter": f"volume:{volume_id}",
        "field_list": (
            "id,"
            "issue_number,"
            "name,"
            "cover_date,"
            "store_date,"
            "image,"
            "site_detail_url,"
            "volume"
        ),
        "limit": PAGE_LIMIT,
        "offset": offset
    }

    url = (
        f"{BASE_URL}/issues/?"
        f"{urlencode(parameters)}"
    )

    request = Request(
        url,
        headers={
            "User-Agent": (
                "ComicReadingCompanion/0.1 "
                "(personal non-commercial "
                "metadata enrichment)"
            )
        }
    )

    print(
        f"  Requesting offset {offset}"
    )

    try:
        with urlopen(
                request,
                timeout=30
        ) as response:
            data = json.load(response)

    except HTTPError as error:
        raise RuntimeError(
            f"HTTP {error.code} for "
            f"volume {volume_id}"
        ) from error

    except URLError as error:
        raise RuntimeError(
            f"Connection error for "
            f"volume {volume_id}: "
            f"{error.reason}"
        ) from error

    if data.get("status_code") != 1:
        raise RuntimeError(
            f"Comic Vine error for "
            f"volume {volume_id}: "
            f"{data.get('error')}"
        )

    return data


def cache_volume(
        volume_id: int
) -> None:
    cache_file = get_cache_file(
        volume_id
    )

    if cache_file.exists():
        print(
            f"Cache hit: volume {volume_id}"
        )
        return

    print(
        f"Fetching volume {volume_id}"
    )

    all_issues = []
    offset = 0
    total_results = None

    while (
            total_results is None
            or offset < total_results
    ):
        page = request_page(
            volume_id = volume_id,
            offset = offset
        )

        results = page.get(
            "results",
            []
        )

        if total_results is None:
            total_results = page.get(
                "number_of_total_results",
                len(results)
            )

            print(
                f"  Comic Vine reports "
                f"{total_results} issue(s)"
            )

        all_issues.extend(
            results
        )

        if not results:
            break

        offset += len(
            results
        )

        if offset < total_results:
            time.sleep(1.5)

    combined = {
        "volume_id": volume_id,
        "number_of_total_results":
            len(all_issues),
        "results": all_issues
    }

    CACHE_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    cache_file.write_text(
        json.dumps(
            combined,
            indent=2
        ),
        encoding="utf-8"
    )

    print(
        f"  Cached {len(all_issues)} issue(s)"
    )


def main() -> None:
    mappings = load_json(
        MAPPINGS_FILE
    )

    volume_ids = []

    for mapping in mappings:
        volume_id = mapping.get(
            "comicVineVolumeId"
        )

        if (
                volume_id is not None
                and volume_id not in volume_ids
        ):
            volume_ids.append(
                volume_id
            )

        issue_mappings = mapping.get(
            "issueMappings",
            {}
        )

        for issue_mapping in (
                issue_mappings.values()
        ):
            issue_volume_id = (
                issue_mapping[
                    "comicVineVolumeId"
                ]
            )

            if (
                    issue_volume_id
                    not in volume_ids
            ):
                volume_ids.append(
                    issue_volume_id
                )

    print(
        f"{len(volume_ids)} mapped "
        "Comic Vine volume(s)"
    )

    for index, volume_id in enumerate(
            volume_ids,
            start=1
    ):
        print()
        print(
            f"[{index}/{len(volume_ids)}] "
            f"Volume {volume_id}"
        )

        cache_volume(
            volume_id
        )

        if index < len(volume_ids):
            next_cache = get_cache_file(
                volume_ids[index]
            )

            if not next_cache.exists():
                time.sleep(1.5)

    print()
    print("Done.")


if __name__ == "__main__":
    main()