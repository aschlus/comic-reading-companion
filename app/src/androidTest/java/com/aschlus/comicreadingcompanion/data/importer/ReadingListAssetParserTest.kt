package com.aschlus.comicreadingcompanion.data.importer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aschlus.comicreadingcompanion.data.importer.models.UniverseOverrideMode
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

    private val targetContext =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext

    private val productionParser =
        ReadingListAssetParser(
            context = targetContext
        )

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

    @Test
    fun parseUltimateMarvelProductionAsset_hasExpectedStructure() {
        val result =
            productionParser.parse(
                "reading_lists/ultimate_marvel_2000_2015.json"
            )

        assertEquals(
            "Ultimate Marvel (2000–2015)",
            result.title
        )

        assertEquals(
            "Marvel Comics",
            result.publisher
        )

        assertEquals(
            "Earth-1610",
            result.universe.designation
        )

        assertEquals(
            127,
            result.sections.size
        )

        assertEquals(
            714,
            result.items.size
        )

        val universeOverrides =
            result.items.filter { item ->
                item.universeOverride != null
            }

        assertEquals(
            20,
            universeOverrides.size
        )

        assertTrue(
            universeOverrides.all { item ->
                item.universeOverride?.mode ==
                        UniverseOverrideMode.NONE
            }
        )

        assertEquals(
            setOf(
                "Spider-Men",
                "Secret Wars",
                "Ultimate End"
            ),
            universeOverrides
                .map { item ->
                    item.series.title
                }
                .toSet()
        )

        assertEquals(
            1,
            result.sections.first().position
        )

        assertEquals(
            127,
            result.sections.last().position
        )

        assertEquals(
            1,
            result.items.first().position
        )

        assertEquals(
            714,
            result.items.last().position
        )
    }

    @Test
    fun parseAssetWithUniverseOverrides_returnsOverrideData() {
        val result =
            parser.parse(
                "reading_lists/universe_overrides_test.json"
            )

        assertEquals(
            2,
            result.items.size
        )

        val universeOverride =
            result.items[0]
                .universeOverride

        assertEquals(
            UniverseOverrideMode.UNIVERSE,
            universeOverride?.mode
        )

        assertEquals(
            "Earth-Alternate",
            universeOverride
                ?.universe
                ?.designation
        )

        val noneOverride =
            result.items[1]
                .universeOverride

        assertEquals(
            UniverseOverrideMode.NONE,
            noneOverride?.mode
        )

        assertEquals(
            null,
            noneOverride?.universe
        )
    }
}