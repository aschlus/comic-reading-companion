from __future__ import annotations

import argparse
import json
import os
import tempfile
from pathlib import Path

COMIC_VINE_SOURCE = "COMIC_VINE"


def load_json(path: Path) -> dict:
    if not path.exists():
        raise RuntimeError(
            f"File not found: {path}"
        )

    data = json.loads(path.read_text(encoding="utf-8"))

    if not isinstance(data, dict):
        raise RuntimeError(
            f"Expected JSON object: {path}"
        )

    return data


def get_external_id(series: dict, source: str) -> str | None:
    for external_id in series.get("externalIds", []):
        if (external_id.get("source") == source):
            value = external_id.get("externalId")

            if value is not None:
                return str(value)

    return None


def series_identity(series: dict) -> tuple:
    comic_vine_id = get_external_id(
        series=series,
        source=COMIC_VINE_SOURCE
    )

    if comic_vine_id is not None:
        return (COMIC_VINE_SOURCE, comic_vine_id)

    return ("METADATA", series.get("title"), series.get("volume"))


def merge_universes(base_catalog: dict, generated_catalog: dict) -> None:
    base_universes = base_catalog.setdefault("universes", [])

    by_designation = { universe["designation"]: universe for universe in base_universes}

    for generated_universe in generated_catalog.get("universes", []):
        designation = generated_universe["designation"]
        existing = by_designation.get(designation)

        if existing is None:
            base_universes.append(generated_universe)

            by_designation[designation] = generated_universe

            continue

        if existing != generated_universe:
            raise ValueError(
                f"Conflicting universe definition for '{designation}'"
            )


def merge_catalog(base_catalog: dict, generated_catalog:dict) -> dict:
    if (base_catalog.get("publisher") != generated_catalog.get("publisher")):
        raise ValueError(
            "Catalog publishers do not match"
        )

    merged = json.loads(json.dumps(base_catalog))

    merge_universes(base_catalog=merged, generated_catalog=generated_catalog)

    merged_series = merged.setdefault("series", [])

    index_by_identity = { series_identity(series): index for index, series in enumerate(merged_series) }

    for generated_series in generated_catalog.get("series", []):
        identity = series_identity(generated_series)
        existing_index = index_by_identity.get(identity)

        if existing_index is None:
            merged_series.append(generated_series)
            index_by_identity[identity] = (len(merged_series) - 1)
        else:
            merged_series[existing_index] = generated_series

    return merged


def write_atomically(destination: Path, data: dict) -> None:
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
        temp_file.write(serialized)
        temp_path = Path(temp_file.name)

    os.replace(temp_path, destination)


def main() -> None:
    parser = argparse.ArgumentParser(
        description=(
            "Promote generated comic catalog data into an app catalog."
        )
    )

    parser.add_argument(
        "--base",
        required=True,
        type=Path
    )

    parser.add_argument(
        "--preview",
        required=True,
        type=Path
    )

    parser.add_argument(
        "--output",
        type=Path
    )

    arguments = parser.parse_args()

    base_catalog = load_json(arguments.base)
    generated_catalog = load_json(arguments.preview)
    merged_catalog = merge_catalog(base_catalog=base_catalog, generated_catalog=generated_catalog)
    destination = arguments.output or arguments.base
    write_atomically(destination=destination, data=merged_catalog)

    generated_issue_count = sum(
        len(series.get("issues", []))
        for series
        in generated_catalog.get("series", [])
    )

    print(f"Promoted {len(generated_catalog.get('series', []))} series with {generated_issue_count} issues.")
    print(f"Output: {destination}")


if __name__ == "__main__":
    main()