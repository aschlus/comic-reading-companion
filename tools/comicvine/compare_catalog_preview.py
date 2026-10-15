from __future__ import annotations

from promote_catalog_preview import series_identity


def issue_number_sort_key(number: str) -> tuple:
    try:
        return (0, float(number))
    except (TypeError, ValueError):
        return (1, str(number).casefold())


def compare_catalogs(base_catalog: dict, generated_catalog: dict) -> list[dict]:
    base_series_by_identity = {
        series_identity(series): series
        for series in base_catalog.get("series", [])
    }

    comparisons = []

    for generated_series in generated_catalog.get("series", []):
        identity = series_identity(generated_series)
        base_series = base_series_by_identity.get(identity)

        generated_issues = {
            str(issue.get("number", "")): issue
            for issue in generated_series.get("issues", [])
        }

        if base_series is None:
            comparisons.append({
                "title": generated_series.get("title"),
                "volume": generated_series.get("volume"),
                "startYear": generated_series.get("startYear"),
                "status": "new_series",
                "addedIssues": sorted(
                    generated_issues,
                    key=issue_number_sort_key,
                ),
                "removedIssues": [],
                "changedIssues": [],
                "changedSeriesFields": [],
            })
            continue

        base_issues = {
            str(issue.get("number", "")): issue
            for issue in base_series.get("issues", [])
        }

        added_numbers = sorted(
            set(generated_issues) - set(base_issues),
            key=issue_number_sort_key
        )

        removed_numbers = sorted(
            set(base_issues) - set(generated_issues),
            key=issue_number_sort_key
        )

        changed_issues = []

        for number in sorted(
            set(base_issues) & set(generated_issues),
            key=issue_number_sort_key
        ):
            old_issue = base_issues[number]
            new_issue = generated_issues[number]

            changed_fields = sorted(
                key
                for key in set(old_issue) | set(new_issue)
                if old_issue.get(key) != new_issue.get(key)
            )

            if changed_fields:
                changed_issues.append({
                    "number": number,
                    "fields": changed_fields
                })

        changed_series_fields = sorted(
            key
            for key in set(base_series) | set(generated_series)
            if key != "issues"
            and base_series.get(key) != generated_series.get(key)
        )

        has_changes = (
            added_numbers
            or removed_numbers
            or changed_issues
            or changed_series_fields
        )

        comparisons.append({
            "title": generated_series.get("title"),
            "volume": generated_series.get("volume"),
            "startYear": generated_series.get("startYear"),
            "status": "updated" if has_changes else "unchanged",
            "addedIssues": added_numbers,
            "removedIssues": removed_numbers,
            "changedIssues": changed_issues,
            "changedSeriesFields": changed_series_fields,
        })

    return comparisons


def format_catalog_comparison(comparisons: list[dict]) -> str:
    lines = [
        "CATALOG COMPARISON",
        "=" * 72,
    ]

    if not comparisons:
        lines.append("No generated series to compare.")
        return "\n".join(lines)

    for comparison in comparisons:
        title = comparison.get("title") or "Untitled"
        volume = comparison.get("volume")
        start_year = comparison.get("startYear")

        heading = title

        if volume is not None:
            heading += f" | Vol. {volume}"

        if start_year is not None:
            heading += f" | {start_year}"

        lines.extend([
            "",
            heading,
            "-" * 72,
        ])

        if comparison["status"] == "new_series":
            lines.append("Series status: NEW SERIES")
        elif comparison["status"] == "unchanged":
            lines.append("Series status: No changes")
        else:
            lines.append("Series status: UPDATED")

        added = comparison["addedIssues"]
        removed = comparison["removedIssues"]
        changed = comparison["changedIssues"]
        changed_series = comparison["changedSeriesFields"]

        lines.append(
            f"Issues added ({len(added)}): "
            + (", ".join(f"#{number}" for number in added) or "none")
        )

        lines.append(
            f"Issues removed ({len(removed)}): "
            + (", ".join(f"#{number}" for number in removed) or "none")
        )

        lines.append(f"Issues changed ({len(changed)}):")

        if changed:
            for issue in changed:
                fields = ", ".join(issue["fields"])
                lines.append(
                    f"  #{issue['number']}: {fields}"
                )
        else:
            lines.append("  none")

        lines.append(
            "Series metadata changed: "
            + (", ".join(changed_series) or "none")
        )

    if all(
        comparison["status"] == "unchanged"
        for comparison in comparisons
    ):
        lines.extend([
            "",
            "No differences detected.",
        ])

    return "\n".join(lines)