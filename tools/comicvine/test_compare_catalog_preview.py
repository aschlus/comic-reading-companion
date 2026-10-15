import unittest

from compare_catalog_preview import (
    compare_catalogs,
    format_catalog_comparison,
)


def make_issue(number, title):
    return {
        "number": str(number),
        "title": title,
        "coverUrl": f"https://example.com/{number}.jpg",
        "publicationDate": "2025-11",
    }


def make_series(issues, **overrides):
    series = {
        "title": "Batman",
        "volume": 4,
        "startYear": 2025,
        "endYear": None,
        "externalIds": [
            {
                "source": "COMIC_VINE",
                "externalId": "166619",
                "url": "https://comicvine.gamespot.com/batman/4050-166619/",
            }
        ],
        "issues": issues,
    }
    series.update(overrides)
    return series


def make_catalog(series):
    return {
        "publisher": "DC Comics",
        "universes": [],
        "series": [series],
    }


class CompareCatalogPreviewTest(unittest.TestCase):

    def test_identical_catalogs_have_no_changes(self):
        issues = [
            make_issue("1", "Issue One"),
            make_issue("2", "Issue Two"),
        ]

        base_catalog = make_catalog(make_series(issues))
        generated_catalog = make_catalog(make_series(issues))

        comparisons = compare_catalogs(
            base_catalog,
            generated_catalog,
        )

        self.assertEqual(1, len(comparisons))

        comparison = comparisons[0]

        self.assertEqual("unchanged", comparison["status"])
        self.assertEqual([], comparison["addedIssues"])
        self.assertEqual([], comparison["removedIssues"])
        self.assertEqual([], comparison["changedIssues"])
        self.assertEqual([], comparison["changedSeriesFields"])

        output = format_catalog_comparison(comparisons)

        self.assertIn("No differences detected.", output)
        self.assertIn("Issues added (0): none", output)

    def test_reports_added_removed_and_changed_issues(self):
        base_catalog = make_catalog(
            make_series([
                make_issue("1", "Old Title"),
                make_issue("2", "Issue Two"),
            ])
        )

        generated_catalog = make_catalog(
            make_series([
                make_issue("1", "Updated Title"),
                make_issue("3", "Issue Three"),
            ])
        )

        comparisons = compare_catalogs(
            base_catalog,
            generated_catalog,
        )

        comparison = comparisons[0]

        self.assertEqual("updated", comparison["status"])
        self.assertEqual(["3"], comparison["addedIssues"])
        self.assertEqual(["2"], comparison["removedIssues"])
        self.assertEqual(
            [
                {
                    "number": "1",
                    "fields": ["title"],
                }
            ],
            comparison["changedIssues"],
        )

        output = format_catalog_comparison(comparisons)

        self.assertIn("Issues added (1): #3", output)
        self.assertIn("Issues removed (1): #2", output)
        self.assertIn("#1: title", output)

    def test_reports_all_issues_when_series_is_new(self):
        unrelated_series = make_series(
            [],
            title="Unrelated Series",
            externalIds=[
                {
                    "source": "COMIC_VINE",
                    "externalId": "99999",
                    "url": None,
                }
            ],
        )

        generated_catalog = make_catalog(
            make_series([
                make_issue("1", "Issue One"),
                make_issue("2", "Issue Two"),
            ])
        )

        comparisons = compare_catalogs(
            make_catalog(unrelated_series),
            generated_catalog,
        )

        comparison = comparisons[0]

        self.assertEqual("new_series", comparison["status"])
        self.assertEqual(["1", "2"], comparison["addedIssues"])
        self.assertEqual([], comparison["removedIssues"])
        self.assertEqual([], comparison["changedIssues"])

        output = format_catalog_comparison(comparisons)

        self.assertIn("Series status: NEW SERIES", output)
        self.assertIn("Issues added (2): #1, #2", output)

    def test_reports_series_metadata_changes(self):
        base_catalog = make_catalog(
            make_series(
                [make_issue("1", "Issue One")],
                endYear=None,
            )
        )

        generated_catalog = make_catalog(
            make_series(
                [make_issue("1", "Issue One")],
                endYear=2026,
            )
        )

        comparisons = compare_catalogs(
            base_catalog,
            generated_catalog,
        )

        comparison = comparisons[0]

        self.assertEqual("updated", comparison["status"])
        self.assertEqual(
            ["endYear"],
            comparison["changedSeriesFields"],
        )
        self.assertEqual([], comparison["changedIssues"])


if __name__ == "__main__":
    unittest.main()