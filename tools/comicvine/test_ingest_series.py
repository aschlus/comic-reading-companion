import argparse
import tempfile
import unittest
import io
import json
from contextlib import redirect_stdout
from pathlib import Path
from unittest.mock import patch

from ingest_series import (
    build_ingest_config,
    choose_candidate,
    run_ingest,
)


class IngestSeriesTest(
    unittest.TestCase
):

    def test_choose_candidate_returns_selected_volume_id(
        self
    ):
        entry = {
            "candidates": [
                {
                    "id": 111,
                    "name":
                        "Wrong Series",
                    "start_year":
                        2002,
                    "count_of_issues":
                        12,
                    "publisher": {
                        "name":
                            "Other Publisher"
                    },
                    "matchScore":
                        70
                },
                {
                    "id": 222,
                    "name":
                        "Correct Series",
                    "start_year":
                        2003,
                    "count_of_issues":
                        145,
                    "publisher": {
                        "name":
                            "Image Comics"
                    },
                    "matchScore":
                        150
                }
            ]
        }

        selected = choose_candidate(
                entry,
                input_func=
                    lambda _: "2"
            )

        self.assertEqual(
            222,
            selected
        )


    def test_build_ingest_config_creates_series_config(
        self
    ):
        config = build_ingest_config(
                title=
                    "Invincible",
                volume=
                    1,
                start_year=
                    2003,
                end_year=
                    2018,
                publisher=
                    "Image Comics",
                comic_vine_volume_id=
                    12345,
                expected_issue_count=
                    145,
                universe_designation=
                    None,
                universe_name=
                    None,
                issue_type=
                    "REGULAR"
            )

        self.assertEqual(
            "Image Comics",
            config["publisher"]
        )

        series = config["series"][0]

        self.assertEqual(
            "Invincible",
            series["title"]
        )

        self.assertEqual(
            12345,
            series[
                "comicVineVolumeId"
            ]
        )

        self.assertEqual(
            145,
            series[
                "expectedIssueCount"
            ]
        )


    def test_run_ingest_known_volume_builds_preview_without_promoting(
        self
    ):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)

            config_path = root / "config.json"

            preview_path = root / "preview.json"

            arguments = argparse.Namespace(
                    title=
                        "Test Series",
                    publisher=
                        "Image Comics",
                    volume=
                        1,
                    start_year=
                        2003,
                    end_year=
                        2003,
                    expected_issue_count=
                        1,
                    comic_vine_volume_id=
                        12345,
                    universe_designation=
                        None,
                    universe_name=
                        None,
                    issue_type=
                        "REGULAR",
                    config_output=
                        config_path,
                    preview_output=
                        preview_path,
                    promote=
                        False,
                    base_catalog=
                        None
                )

            cached_volumes = {
                12345: {
                    "volume_id":
                        12345,
                    "results": [
                        {
                            "id":
                                1001,
                            "issue_number":
                                "1",
                            "name":
                                "Issue One",
                            "cover_date":
                                "2003-01-01",
                            "store_date":
                                None,
                            "image": {
                                "super_url":
                                    "https://example.com/1.jpg"
                            },
                            "site_detail_url":
                                "https://example.com/issue/1001",
                            "volume": {
                                "site_detail_url":
                                    "https://example.com/volume/12345"
                            }
                        }
                    ]
                }
            }

            with patch(
                "ingest_series.cache_volume"
            ) as cache_mock, patch(
                "ingest_series.load_cached_volumes",
                return_value=
                    cached_volumes
            ):
                result = run_ingest(arguments)

            cache_mock.assert_called_once_with(
                12345
            )

            self.assertTrue(
                config_path.exists()
            )

            self.assertTrue(
                preview_path.exists()
            )

            self.assertEqual(
                12345,
                result[
                    "comicVineVolumeId"
                ]
            )

            self.assertEqual(
                1,
                result[
                    "report"
                ][
                    "series"
                ][0][
                    "actualIssueCount"
                ]
            )

    def test_run_ingest_known_volume_builds_preview_without_promoting(
        self
    ):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)

            config_path = root / "config.json"
            preview_path = root / "preview.json"

            arguments = argparse.Namespace(
                title="Test Series",
                publisher="Image Comics",
                volume=1,
                start_year=2003,
                end_year=2003,
                expected_issue_count=1,
                comic_vine_volume_id=12345,
                universe_designation=None,
                universe_name=None,
                issue_type="REGULAR",
                config_output=config_path,
                preview_output=preview_path,
                promote=False,
                base_catalog=None,
                refresh=True
            )

            cached_volumes = {
                12345: {
                    "volume_id": 12345,
                    "results": [
                        {
                            "id": 1001,
                            "issue_number": "1",
                            "name": "Issue One",
                            "cover_date": "2003-01-01",
                            "store_date": None,
                            "image": {
                                "super_url":
                                    "https://example.com/1.jpg"
                            },
                            "site_detail_url":
                                "https://example.com/issue/1001",
                            "volume": {
                                "site_detail_url":
                                    "https://example.com/volume/12345"
                            }
                        }
                    ]
                }
            }

            with patch(
                "ingest_series.cache_volume"
            ) as cache_mock, patch(
                "ingest_series.load_cached_volumes",
                return_value=cached_volumes
            ):
                result = run_ingest(arguments)

            cache_mock.assert_called_once_with(
                12345,
                refresh=True
            )

            self.assertTrue(config_path.exists())
            self.assertTrue(preview_path.exists())

            self.assertEqual(
                12345,
                result["comicVineVolumeId"]
            )

            self.assertEqual(
                1,
                result["report"]["series"][0]["actualIssueCount"]
            )

    def test_run_ingest_compares_preview_against_base_catalog(
        self
    ):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)

            config_path = root / "config.json"
            preview_path = root / "preview.json"
            base_catalog_path = root / "base_catalog.json"

            catalog = {
                "publisher": "Image Comics",
                "universes": [],
                "series": [
                    {
                        "title": "Test Series",
                        "volume": 1,
                        "startYear": 2003,
                        "endYear": None,
                        "externalIds": [
                            {
                                "source": "COMIC_VINE",
                                "externalId": "12345",
                                "url": None,
                            }
                        ],
                        "issues": [
                            {
                                "number": "1",
                                "title": "Issue One",
                                "publicationDate": "2003-01",
                                "coverUrl": "https://example.com/1.jpg",
                                "description": None,
                                "type": "REGULAR",
                                "universeDesignation": None,
                                "externalIds": [],
                            }
                        ],
                    }
                ],
            }

            base_catalog_path.write_text(
                json.dumps(catalog, indent=2),
                encoding="utf-8",
            )

            arguments = argparse.Namespace(
                title="Test Series",
                publisher="Image Comics",
                volume=1,
                start_year=2003,
                end_year=2003,
                expected_issue_count=1,
                comic_vine_volume_id=12345,
                universe_designation=None,
                universe_name=None,
                issue_type="REGULAR",
                config_output=config_path,
                preview_output=preview_path,
                promote=False,
                base_catalog=base_catalog_path,
                refresh=False,
            )

            output = io.StringIO()

            with patch(
                "ingest_series.cache_volume"
            ) as cache_mock, patch(
                "ingest_series.load_cached_volumes",
                return_value={12345: {"volume_id": 12345, "results": []}},
            ), patch(
                "ingest_series.build_catalog",
                return_value=catalog,
            ), patch(
                "ingest_series.build_anomaly_report",
                return_value={"series": []},
            ), patch(
                "ingest_series.format_anomaly_report",
                return_value="No anomalies",
            ), redirect_stdout(output):
                result = run_ingest(arguments)

            cache_mock.assert_called_once_with(
                12345,
                refresh=False,
            )

            self.assertTrue(config_path.exists())
            self.assertTrue(preview_path.exists())

            self.assertEqual(
                12345,
                result["comicVineVolumeId"],
            )

            self.assertIn(
                "CATALOG COMPARISON",
                output.getvalue(),
            )

            self.assertIn(
                "Series status: No changes",
                output.getvalue(),
            )

            self.assertIn(
                "No differences detected.",
                output.getvalue(),
            )

            self.assertIn(
                "Preview only.",
                output.getvalue(),
            )


if __name__ == "__main__":
    unittest.main()