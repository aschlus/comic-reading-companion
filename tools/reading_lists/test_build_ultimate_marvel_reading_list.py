import unittest

from build_ultimate_marvel_reading_list import (
    DEFAULT_CATALOG,
    DEFAULT_MANIFEST,
    build_reading_list,
    build_universe_override,
    load_json
)


class BuildUltimateMarvelReadingListTest(unittest.TestCase):

    def test_matching_universe_needs_no_override(self):
        override = build_universe_override(
            list_universe={
                "name": "Ultimate Marvel Universe",
                "designation": "Earth-1610"
            },
            catalog={
                "universes": [
                    {
                        "name": "Ultimate Marvel Universe",
                        "designation": "Earth-1610"
                    }
                ]
            },
            issue={
                "universeDesignation": "Earth-1610"
            }
        )

        self.assertIsNone(override)


    def test_null_universe_emits_none_override(self):
        override = build_universe_override(
            list_universe={
                "name": "Ultimate Marvel Universe",
                "designation": "Earth-1610"
            },
            catalog={
                "universes": [
                    {
                        "name": "Ultimate Marvel Universe",
                        "designation": "Earth-1610"
                    }
                ]
            },
            issue={
                "universeDesignation": None
            }
        )

        self.assertEqual({ "mode": "NONE" }, override)


    def test_production_reading_list_emits_expected_overrides(self):
        catalog = load_json(DEFAULT_CATALOG)
        manifest = load_json(DEFAULT_MANIFEST)
        reading_list = build_reading_list(
            manifest=manifest,
            catalog=catalog
        )

        overrides = [
            item
            for item
            in reading_list["items"]
            if "universeOverride" in item
        ]

        self.assertEqual(20, len(overrides))
        self.assertTrue(
            all(
                item["universeOverride"] == { "mode": "NONE" }
                for item in overrides
            )
        )
        self.assertEqual(
            {"Spider-Men", "Secret Wars", "Ultimate End"},
            {
                item["series"]["title"]
                for item in overrides
            }
        )
        self.assertEqual(127, len(reading_list["sections"]))
        self.assertEqual(714, len(reading_list["items"]))


if __name__ == "__main__":
    unittest.main()