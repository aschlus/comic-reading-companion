import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import cache_mapped_volume_issues as cache_tool


class CacheMappedVolumeIssuesTest(unittest.TestCase):

    def test_cache_volume_uses_existing_cache_by_default(self):
        with tempfile.TemporaryDirectory() as directory:
            cache_dir = Path(directory)
            cache_file = cache_dir / "volume_12345.json"

            original = {
                "volume_id": 12345,
                "results": [{"id": 1}]
            }

            cache_file.write_text(
                json.dumps(original),
                encoding="utf-8"
            )

            with patch.object(
                cache_tool, "CACHE_DIR", cache_dir
            ), patch.object(
                cache_tool, "request_page"
            ) as request_mock:
                cache_tool.cache_volume(12345)

            request_mock.assert_not_called()

            self.assertEqual(
                original,
                json.loads(cache_file.read_text(encoding="utf-8"))
            )

    def test_cache_volume_refresh_replaces_existing_cache(self):
        with tempfile.TemporaryDirectory() as directory:
            cache_dir = Path(directory)
            cache_file = cache_dir / "volume_12345.json"

            original = {
                "volume_id": 12345,
                "results": [{"id": 1}]
            }

            cache_file.write_text(
                json.dumps(original),
                encoding="utf-8"
            )

            refreshed_issue = {
                "id": 2,
                "issue_number": "2"
            }

            api_page = {
                "number_of_total_results": 1,
                "results": [refreshed_issue]
            }

            with patch.object(
                cache_tool, "CACHE_DIR", cache_dir
            ), patch.object(
                cache_tool, "request_page", return_value=api_page
            ) as request_mock:
                cache_tool.cache_volume(12345, refresh=True)

            request_mock.assert_called_once_with(volume_id=12345, offset=0)

            updated = json.loads(
                cache_file.read_text(encoding="utf-8")
            )

            self.assertEqual(12345, updated["volume_id"])
            self.assertEqual([refreshed_issue], updated["results"])

    def test_failed_refresh_preserves_existing_cache(self):
        with tempfile.TemporaryDirectory() as directory:
            cache_dir = Path(directory)
            cache_file = cache_dir / "volume_12345.json"

            original = {
                "volume_id": 12345,
                "results": [{"id": 1}]
            }

            cache_file.write_text(
                json.dumps(original),
                encoding="utf-8"
            )

            with patch.object(
                cache_tool, "CACHE_DIR", cache_dir
            ), patch.object(
                cache_tool,
                "request_page",
                side_effect=RuntimeError("API unavailable")
            ):
                with self.assertRaisesRegex(
                    RuntimeError, "API unavailable"
                ):
                    cache_tool.cache_volume(12345, refresh=True)

            self.assertEqual(
                original,
                json.loads(cache_file.read_text(encoding="utf-8"))
            )


if __name__ == "__main__":
    unittest.main()