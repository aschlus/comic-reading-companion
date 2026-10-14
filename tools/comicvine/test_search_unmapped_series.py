import json
import tempfile
import unittest
from pathlib import Path

from search_unmapped_series import (
    get_manifest_series,
    score_candidate
)


class SearchUnmappedSeriesTest(
    unittest.TestCase
):

    def test_score_candidate_rewards_requested_publisher(self):
        local_series = {
            "title": "Invincible",
            "volume": 1,
            "startYear": 2003,
            "localIssueCount": 145,
            "publisher": "Image Comics",
        }

        image_candidate = {
            "name": "Invincible",
            "start_year": 2003,
            "count_of_issues": 145,
            "publisher": {
                "name": "Image Comics"
            },
        }

        marvel_candidate = {
            "name": "Invincible",
            "start_year": 2003,
            "count_of_issues": 145,
            "publisher": {
                "name": "Marvel"
            },
        }

        self.assertGreater(
            score_candidate(local_series, image_candidate),
            score_candidate(local_series, marvel_candidate)
        )

    def test_score_candidate_has_no_publisher_bias_when_unspecified(self):
        local_series = {
            "title": "Test Series",
            "volume": 1,
            "startYear": 2000,
            "localIssueCount": 12,
            "publisher": None,
        }

        image_candidate = {
            "name": "Test Series",
            "start_year": 2000,
            "count_of_issues": 12,
            "publisher": {
                "name": "Image Comics"
            },
        }

        marvel_candidate = {
            "name": "Test Series",
            "start_year": 2000,
            "count_of_issues": 12,
            "publisher": {
                "name": "Marvel Comics"
            },
        }

        self.assertEqual(
            score_candidate(local_series, image_candidate),
            score_candidate(local_series, marvel_candidate)
        )

    def test_manifest_series_inherits_manifest_publisher(self):
        manifest = {
            "publisher": "Image Comics",
            "series": [
                {
                    "title": "Invincible",
                    "volume": 1,
                    "startYear": 2003,
                    "expectedIssueCount": 145,
                }
            ],
        }

        with tempfile.TemporaryDirectory() as directory:
            manifest_file = Path(directory) / "manifest.json"

            manifest_file.write_text(
                json.dumps(manifest),
                encoding="utf-8"
            )

            series = get_manifest_series(manifest_file)

        self.assertEqual(
            "Image Comics",
            series[0]["publisher"]
        )


if __name__ == "__main__":
    unittest.main()
