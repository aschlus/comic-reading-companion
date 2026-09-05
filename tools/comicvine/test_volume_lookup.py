from __future__ import annotations

import json
import os
import re
import sys
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


BASE_URL = "https://comicvine.gamespot.com/api"
TOOL_DIR = Path(__file__).resolve().parent
API_KEY_FILE = TOOL_DIR / ".comicvine_api_key"
CACHE_DIR = TOOL_DIR / "cache" / "volume_search"


def load_api_key() -> str:
    environment_key = os.environ.get("COMIC_VINE_API_KEY")

    if environment_key:
        return environment_key.strip()

    if API_KEY_FILE.exists():
        api_key = API_KEY_FILE.read_text(
            encoding="utf-8"
        ).strip()

        if api_key:
            return api_key

    raise RuntimeError(
        "Comic Vine API key nut found.\n"
        "Create tools/comicvine/.comicvine_api_key "
        "and put only your API key inside it."
    )


def make_cache_name(title: str) -> str:
    normalized = title.lower()

    normalized = re.sub(
        r"[^a-z0-9]+",
        "_",
        normalized
    )

    normalized = normalized.strip("_")

    return f"{normalized}.json"


def load_cached_response(title: str) -> dict | None:
    cache_file = (
        CACHE_DIR /
        make_cache_name(title)
    )

    if not cache_file.exists():
        return None

    print(f"Cache hit: {cache_file}")

    return json.loads(
        cache_file.read_text(
            encoding="utf-8"
        )
    )


def save_cached_response(title: str, data: dict) -> None:
    CACHE_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    cache_file = (
        CACHE_DIR /
        make_cache_name(title)
    )

    cache_file.write_text(
        json.dumps(
            data,
            indent=2
        ),
        encoding="utf-8"
    )

    print(f"Cached response: {cache_file}")


def search_volumes(
        title: str,
        refresh: bool = False
) -> dict:
    if not refresh:
        cached = load_cached_response(
            title
        )

        if cached is not None:
            return cached

    api_key = load_api_key()

    parameters = {
        "api_key": api_key,
        "format": "json",
        "resources": "volume",
        "query": title,
        "field_list": (
            "id,"
            "name,"
            "start_year,"
            "count_of_issues,"
            "publisher,"
            "site_detail_url"
        ),
        "limit": 100
    }

    url = (
        f"{BASE_URL}/search/?"
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
        f"Searching Comic Vine volumes "
        f"for: {title}"
    )

    try:
        with urlopen(
                request,
                timeout=30
        ) as response:
            data = json.load(response)

    except HTTPError as error:
        raise RuntimeError(
            "Comic Vine request failed with "
            f"HTTP {error.code}"
        ) from error

    except URLError as error:
        raise RuntimeError(
            "Could not connect to Comic Vine: "
            f"{error.reason}"
        ) from error

    status_code = data.get(
        "status_code"
    )

    if status_code != 1:
        raise RuntimeError(
            "Comic Vine returned an error:\n"
            f"{data.get('error')}\n"
            f"Status code: {status_code}"
        )

    save_cached_response(
        title = title,
        data = data
    )

    return data


def print_candidates(data: dict) -> None:
    results = data.get("results", [])

    print()
    print(f"Found {len(results)} candidate volume(s)")
    print("=" * 72)

    for volume in results:
        publisher = (
            volume.get("publisher")
            or {}
        )

        print(f"ID:          {volume.get('id')}")
        print(f"Name:        {volume.get('name')}")
        print(f"Start year:  {volume.get('start_year')}")
        print(f"Issues:      {volume.get('count_of_issues')}")
        print(f"Publisher:   {publisher.get('name')}")
        print(f"Page:        {volume.get('site_detail_url')}")
        print("-" * 72)


def main() -> None:
    title = "The Amazing Spider-Man"
    refresh = ("--refresh" in sys.argv)
    data = search_volumes(
        title = title,
        refresh = refresh
    )

    print_candidates(data)


if __name__ == "__main__":
    main()