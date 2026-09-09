package com.aschlus.comicreadingcompanion.data.importer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComicCatalogAssetParserTest {

    private val testContext = InstrumentationRegistry.getInstrumentation().context

    private val parser = ComicCatalogAssetParser(context = testContext)

    @Test
    fun parseValidCatalog_returnsCatalogData() {
        val result = parser.parse("catalogs/valid_catalog_test.json")

        assertEquals("Marvel Comics", result.publisher)
        assertEquals(1, result.universes.size)
        assertEquals("Earth-616", result.universes.first().designation)
        assertEquals(1, result.series.size)

        val series = result.series.first()

        assertEquals("Amazing Spider-Man", series.title)
        assertEquals(2, series.volume)
        assertEquals("2127", series.externalIds.first().externalId)
        assertEquals(1, series.issues.size)

        val issue = series.issues.first()

        assertEquals("1", issue.number)
        assertEquals("Earth-616", issue.universeDesignation)
        assertEquals("100001", issue.externalIds.first().externalId)
    }

    @Test
    fun parseMalformedCatalog_includesAssetPathInError() {
        try {
            parser.parse("catalogs/malformed_catalog_test.json")
            throw AssertionError(
                "Expected malformed catalog JSON " +
                "to fail parsing"
            )
        } catch (
            exception: IllegalArgumentException
        ) {
            assertTrue(
                exception.message?.contains(
                "Could not parse comic-catalog asset"
                ) == true
            )
            assertTrue(
                exception.message?.contains(
                    "catalogs/malformed_catalog_test.json"
                ) == true
            )
        }
    }
}