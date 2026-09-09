import unittest

from promote_catalog_preview import (
    merge_catalog,
)

class PromoteCatalogPreviewTest(unittest.TestCase):

    def test_merge_catalog_replaces_matching_series(self):
        base_catalog = {
                    "publisher": "Marvel Comics",
                    "universes": [],
                    "series": [
                        {
                            "title": "Slingers",
                            "volume": 1,
                            "externalIds": [
                                {
                                    "source": "COMIC_VINE",
                                    "externalId": "11305",
                                    "url": None
                                }
                            ],
                            "issues": [
                                {
                                    "number": "1",
                                }
                            ]
                        }
                    ]
                }

        generated_catalog = {
                    "publisher": "Marvel Comics",
                    "universes": [],
                    "series": [
                        {
                            "title": "Slingers",
                            "volume": 1,
                            "externalIds": [
                                {
                                    "source": "COMIC_VINE",
                                    "externalId": "11305",
                                    "url": None
                                }
                            ],
                            "issues": [
                                {
                                    "number": "0",
                                },
                                {
                                    "number": "1",
                                },
                                {
                                    "number": "2",
                                }
                            ]
                        }
                    ]
                }

        merged = merge_catalog(base_catalog=base_catalog, generated_catalog=generated_catalog)
        self.assertEqual(1, len(merged["series"]))
        self.assertEqual(["0", "1", "2"],
                         [
                             issue["number"]
                             for issue in merged["series"][0]["issues"]
                         ])


    def test_merge_catalog_preserves_unrelated_series(self):
        base_catalog = {
                    "publisher": "Marvel Comics",
                    "universes": [],
                    "series": [
                        {
                            "title": "Amazing Spider-Man",
                            "volume": 2,
                            "externalIds": [
                                {
                                    "source": "COMIC_VINE",
                                    "externalId": "78701",
                                    "url": None
                                }
                            ],
                            "issues": [
                                {
                                    "number": "1",
                                }
                            ]
                        },
                        {
                            "title": "Slingers",
                            "volume": 1,
                            "externalIds": [
                                {
                                    "source": "COMIC_VINE",
                                    "externalId": "11305",
                                    "url": None
                                }
                            ],
                            "issues": [
                                {
                                    "number": "1",
                                }
                            ]
                        }
                    ]
                }

        generated_catalog = {
                    "publisher": "Marvel Comics",
                    "universes": [],
                    "series": [
                        {
                            "title": "Slingers",
                            "volume": 1,
                            "externalIds": [
                                {
                                    "source": "COMIC_VINE",
                                    "externalId": "11305",
                                    "url": None
                                }
                            ],
                            "issues": [
                                {
                                    "number": "0",
                                },
                                {
                                    "number": "1",
                                }
                            ]
                        }
                    ]
                }

        merged = merge_catalog(base_catalog=base_catalog, generated_catalog=generated_catalog)
        self.assertEqual(2, len(merged["series"]))
        amazing = (merged["series"][0])
        slingers = (merged["series"][1])
        self.assertEqual("Amazing Spider-Man", amazing["title"])
        self.assertEqual(["1"],
                         [
                             issue["number"]
                             for issue
                             in amazing["issues"]
                         ]
                         )
        self.assertEqual("Slingers", slingers["title"])
        self.assertEqual(["0", "1"],
                         [
                             issue["number"]
                             for issue
                             in slingers["issues"]
                         ]
                         )


if __name__ == "__main__":
    unittest.main()