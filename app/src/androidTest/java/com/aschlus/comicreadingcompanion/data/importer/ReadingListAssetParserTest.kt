package com.aschlus.comicreadingcompanion.data.importer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingListAssetParserTest {

    private val testContext =
        InstrumentationRegistry
            .getInstrumentation()
            .context

    private val parser =
        ReadingListAssetParser(
            context = testContext
        )

    @Test
    fun parseValidAsset_returnsReadingListData() {
        val result = parser.parse("reading_lists/valid_test.json")

        assertEquals(
            "Parser Test",
            result.title
        )

        assertEquals(
            "Test Publisher",
            result.publisher
        )

        assertEquals(
            "Earth-Test",
            result.universe.designation
        )

        assertEquals(
            1,
            result.items.size
        )

        assertEquals(
            "1",
            result.items.first().issue.number
        )
    }

    @Test
    fun parseMalformedAssert_includesAssetPathInError() {
        try {
            parser.parse("reading_lists/malformed_test.json")

            throw AssertionError("Expected malformed JSON to fail parsing")
        } catch (exception: IllegalArgumentException) {
            assertTrue(
                exception.message?.contains(
                    "Could not parse reading-list asset"
                ) == true
            )

            assertTrue(
                exception.message?.contains(
                    "reading_lists/malformed_test.json"
                ) == true
            )
        }
    }

    @Test
    fun parseAssetMissingRequiredField_includeAssetPathInError() {
        try {
            parser.parse("reading_lists/missing_required_field_test.json")

            throw AssertionError("Expected missing required field to fail parsing")
        } catch (exception: IllegalArgumentException) {
            assertTrue(
                exception.message?.contains(
                    "Could not parse reading-list asset"
                ) == true
            )

            assertTrue(
                exception.message?.contains(
                    "reading_lists/missing_required_field_test.json"
                ) == true
            )
        }
    }

    @Test
    fun parseMissingAsset_includesAssetPathInError() {
        try{
            parser.parse("reading_lists/does_not_exist.json")

            throw AssertionError("Expected missing asset to fail reading")
        } catch (exception: IllegalArgumentException) {
            assertTrue(
                exception.message?.contains(
                    "Could not read reading-list asset"
                ) == true
            )

            assertTrue(
                exception.message?.contains(
                    "reading_lists/does_not_exist.json"
                ) == true
            )
        }
    }

    @Test
    fun parseAssetWithUnknownFields_ignoresUnknownFields() {
        val result = parser.parse("reading_lists/unknown_fields_test.json")

        assertEquals(
            "Unknown Fields Test",
            result.title
        )

        assertEquals(
            "Test Publisher",
            result.publisher
        )
    }

    @Test
    fun listReadingListAssets_returnsOnlyJsonFiles() {
        val assets = parser.listReadingListAssets()

        assertTrue(
            assets.isNotEmpty()
        )

        assertTrue(
            assets.all { assetPath ->
                assetPath.endsWith(
                    ".json",
                    ignoreCase = true
                )
            }
        )
    }

    @Test
    fun listReadingListAssets_returnsSortedPaths() {
        val assets = parser.listReadingListAssets()

        assertEquals(
            assets.sorted(),
            assets
        )
    }
}