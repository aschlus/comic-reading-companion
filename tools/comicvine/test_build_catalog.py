import json
import tempfile
import unittest
from pathlib import Path

from build_catalog import (
    build_catalog,
    load_cached_volumes,
    validate_catalog,
    write_catalog
)


class BuildCatalogTest(
    unittest.TestCase
):

    def test_build_catalog_maps_cached_volume_into_app_schema(self):
        config = {
            "publisher": "Marvel Comics",
            "universes": [
                {
                    "name": "Marvel Universe",
                    "designation": "Earth-616",
                    "description": None
                }
            ],
            "series": [
                {
                    "title": "Slingers",
                    "volume": 1,
                    "startYear": 1998,
                    "endYear": 1999,
                    "comicVineVolumeId": 11305,
                    "defaultUniverseDesignation": "Earth-616",
                    "defaultIssueType": "REGULAR"
                }
            ]
        }

        cached_volumes = {
            11305: {
                "volume_id": 11305,
                "results": [
                    {
                        "id": 99239,
                        "issue_number": "1",
                        "name": "So Whose Idea Was This Anyway?",
                        "cover_date": "1998-12-01",
                        "store_date": None,
                        "image": {
                            "super_url": "https://example.com/slingers1.jpg",
                            "original_url": "https://example.com/original.jpg"
                        },
                        "site_detail_url": "https://example.com/issue/99239",
                        "volume": {
                            "id": 11305,
                            "name": "Slingers",
                            "site_detail_url": "https://example.com/volume/11305"
                        }
                    }
                ]
            }
        }

        catalog = build_catalog(
            config=config,
            cached_volumes=cached_volumes
        )

        self.assertEqual("Marvel Comics", catalog["publisher"])
        self.assertEqual("Earth-616", catalog["universes"][0]["designation"])

        series = catalog["series"][0]

        self.assertEqual("Slingers", series["title"])
        self.assertEqual(1, series["volume"])
        self.assertEqual("11305", series["externalIds"][0]["externalId"])

        issue = series["issues"][0]

        self.assertEqual("1", issue["number"])
        self.assertEqual("So Whose Idea Was This Anyway?", issue["title"])
        self.assertEqual("1998-12", issue["publicationDate"])
        self.assertEqual("https://example.com/slingers1.jpg", issue["coverUrl"])
        self.assertEqual("REGULAR", issue["type"])
        self.assertEqual("Earth-616", issue["universeDesignation"])
        self.assertEqual("99239", issue["externalIds"][0]["externalId"])


    def test_build_catalog_rejects_wrong_cached_volume(self):
        config = {
            "publisher": "Marvel Comics",
            "universes": [],
            "series": [
                {
                    "title": "Slingers",
                    "volume": 1,
                    "startYear": 1998,
                    "endYear": 1999,
                    "comicVineVolumeId": 11305
                }
            ]
        }

        cached_volumes = {
            11305: {
                "volume_id": 99999,
                "results": []
            }
        }

        with self.assertRaisesRegex(
            ValueError,
            "does not match configured volume 11305"
        ):
            build_catalog(
                config=config,
                cached_volumes=cached_volumes
            )


    def test_load_cached_volumes_reads_configured_cache(self):
        config = {
                    "publisher": "Marvel Comics",
                    "series": [
                        {
                            "title": "Slingers",
                            "comicVineVolumeId": 11305
                        }
                    ]
                }

        with tempfile.TemporaryDirectory() as directory:
            cache_dir = Path(directory)
            cache_file = (cache_dir / "volume_11305.json")

            cache_file.write_text(
                json.dumps(
                    {
                        "volume_id": 11305,
                        "results": [
                            {
                                "id": 99239,
                                "issue_number": "1"
                            }
                        ]
                    }
                ),
                encoding="utf-8"
            )

            cached_volumes = (load_cached_volumes(
                config=config,
                cache_dir=cache_dir
            ))

            self.assertEqual({11305}, set(cached_volumes.keys()))
            self.assertEqual(99239, cached_volumes[11305]["results"][0]["id"])


    def test_write_catalog_writes_formatted_json(self):
        catalog = {
            "publisher": "Marvel Comics",
            "universes": [],
            "series": []
        }

        with tempfile.TemporaryDirectory() as directory:
            output_file = (Path(directory) / "catalog.json")

            write_catalog(
                catalog=catalog,
                output_file=output_file
            )

            self.assertTrue(output_file.exists())

            loaded = json.loads(output_file.read_text(encoding="utf-8"))

            self.assertEqual(catalog, loaded)

            output_text = (output_file.read_text(encoding="utf-8"))

            self.assertTrue(output_text.endswith("\n"))


    def test_build_catalog_sorts_issues_by_publication_date(self):
        config = {
                    "publisher": "Marvel Comics",
                    "universes": [
                        {
                            "name": "Marvel Universe",
                            "designation": "Earth-616",
                            "description": None
                        }
                    ],
                    "series": [
                        {
                            "title": "Slingers",
                            "volume": 1,
                            "startYear": 1998,
                            "endYear": 1999,
                            "comicVineVolumeId": 11305,
                            "defaultUniverseDesignation": "Earth-616"
                        }
                    ]
                }
        
        cached_volumes = {
                    11305: {
                        "volume_id": 11305,
                        "results": [
                            {
                                "id": 2,
                                "issue_number": "1",
                                "name": None,
                                "cover_date": "1998-12-01",
                                "image": {},
                                "site_detail_url": None,
                                "volume": {}
                            },
                            {
                                "id": 3,
                                "issue_number": "2",
                                "name": None,
                                "cover_date": "1999-01-01",
                                "image": {},
                                "site_detail_url": None,
                                "volume": {}
                            },
                            {
                                "id": 1,
                                "issue_number": "0",
                                "name": None,
                                "cover_date": "1998-09-01",
                                "image": {},
                                "site_detail_url": None,
                                "volume": {}
                            }
                        ]
                    }
                }

        catalog = build_catalog(
            config=config,
            cached_volumes=cached_volumes
        )

        numbers = [issue["number"] for issue in catalog["series"][0]["issues"]]

        self.assertEqual(["0", "1", "2"], numbers)


    def test_validate_catalog_rejects_duplicate_issue_numbers(self):
        catalog = {
                    "publisher": "Marvel Comics",
                    "universes": [
                        {
                            "name": "Marvel Universe",
                            "designation": "Earth-616",
                            "description": None
                        }
                    ],
                    "series": [
                        {
                            "title": "Test Series",
                            "volume": 1,
                            "startYear": 2000,
                            "endYear": 2000,
                            "externalIds": [],
                            "issues": [
                                {
                                    "number": "1",
                                    "title": None,
                                    "publicationDate": "2000-01",
                                    "coverUrl": None,
                                    "description": None,
                                    "type": "REGULAR",
                                    "universeDesignation": "Earth-616",
                                    "externalIds": []
                                },
                                {
                                    "number": "1",
                                    "title": None,
                                    "publicationDate": "2000-02",
                                    "coverUrl": None,
                                    "description": None,
                                    "type": "REGULAR",
                                    "universeDesignation": "Earth-616",
                                    "externalIds": []
                                }
                            ]
                        }
                    ]
                }

        with self.assertRaisesRegex(ValueError, "Duplicate issue number '1'"):
            validate_catalog(catalog)


    def test_validate_catalog_rejects_unkown_universe(self):
        catalog = {
                    "publisher": "Marvel Comics",
                    "universes": [
                        {
                            "name": "Marvel Universe",
                            "designation": "Earth-616",
                            "description": None
                        }
                    ],
                    "series": [
                        {
                            "title": "Test Series",
                            "volume": 1,
                            "startYear": 2000,
                            "endYear": 2000,
                            "externalIds": [],
                            "issues": [
                                {
                                    "number": "1",
                                    "title": None,
                                    "publicationDate": "2000-01",
                                    "coverUrl": None,
                                    "description": None,
                                    "type": "REGULAR",
                                    "universeDesignation": "Earth-999",
                                    "externalIds": []
                                }
                            ]
                        }
                    ]
                }

        with self.assertRaisesRegex(ValueError, "Unknown universe 'Earth-999'"):
            validate_catalog(catalog)


    def test_validate_catalog_rejects_duplicate_issue_external_ids(self):
        catalog = {
                    "publisher": "Marvel Comics",
                    "universes": [
                        {
                            "name": "Marvel Universe",
                            "designation": "Earth-616",
                            "description": None
                        }
                    ],
                    "series": [
                        {
                            "title": "Test Series",
                            "volume": 1,
                            "startYear": 2000,
                            "endYear": 2000,
                            "externalIds": [],
                            "issues": [
                                {
                                    "number": "1",
                                    "title": None,
                                    "publicationDate": "2000-01",
                                    "coverUrl": None,
                                    "description": None,
                                    "type": "REGULAR",
                                    "universeDesignation": "Earth-616",
                                    "externalIds": []
                                },
                                {
                                    "number": "1",
                                    "title": None,
                                    "publicationDate": "2000-02",
                                    "coverUrl": None,
                                    "description": None,
                                    "type": "REGULAR",
                                    "universeDesignation": "Earth-616",
                                    "externalIds": []
                                }
                            ]
                        }
                    ]
                }
        
        with self.assertRaisesRegex(ValueError, "Duplicate issue number '1'"):
            validate_catalog(catalog)
        
        
    def test_validate_catalog_rejects_unkown_universe(self):
        catalog = {
                    "publisher": "Marvel Comics",
                    "universes": [
                        {
                            "name": "Marvel Universe",
                            "designation": "Earth-616",
                            "description": None
                        }
                    ],
                    "series": [
                        {
                            "title": "Test Series",
                            "volume": 1,
                            "startYear": 2000,
                            "endYear": 2000,
                            "externalIds": [],
                            "issues": [
                                {
                                    "number": "1",
                                    "title": None,
                                    "publicationDate": "2000-01",
                                    "coverUrl": None,
                                    "description": None,
                                    "type": "REGULAR",
                                    "universeDesignation": "Earth-616",
                                    "externalIds": [
                                        {
                                            "source": "COMIC_VINE",
                                            "externalId": "12345",
                                            "url": None
                                        }
                                    ]
                                },
                                {
                                    "number": "2",
                                    "title": None,
                                    "publicationDate": "2000-02",
                                    "coverUrl": None,
                                    "description": None,
                                    "type": "REGULAR",
                                    "universeDesignation": "Earth-616",
                                    "externalIds": [
                                        {
                                            "source": "COMIC_VINE",
                                            "externalId": "12345",
                                            "url": None
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }

        with self.assertRaisesRegex(ValueError, "Duplicate issue external ID COMIC_VINE:12345"):
            validate_catalog(catalog)


if __name__ == "__main__":
    unittest.main()