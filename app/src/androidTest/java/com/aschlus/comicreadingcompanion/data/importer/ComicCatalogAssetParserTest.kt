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
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val productionCatalogParser = ComicCatalogAssetParser(context = targetContext)
    private val productionReadingListParser =
        ReadingListAssetParser(context = targetContext)

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

    @Test
    fun listCatalogAssets_returnsOnlyJsonFiles() {
        val assets = parser.listCatalogAssets()
        assertTrue(assets.isNotEmpty())
        assertTrue(
            assets.any { assetPath ->
                assetPath.endsWith(
                    ".json",
                    ignoreCase = true
                )
            }
        )
        assertTrue("catalogs/ignore_me.txt" !in assets)
    }

    @Test
    fun listCatalogAssets_returnsSortedPath() {
        val assets = parser.listCatalogAssets()
        assertEquals(assets.sorted(), assets)
    }

    @Test
    fun productionSpiderManCatalog_containsEveryReadingListIssue() {
        val readingList = productionReadingListParser.parse(
            "reading_lists/spider_man_volume_2.json")
        val catalog = productionCatalogParser.parse(
            "catalogs/spider_man_volume_2_catalog.json")
        val readingListIssueIds =
            readingList.items
                .flatMap { item ->
                    item.issue.externalIds
                }
                .filter { externalId ->
                    externalId.source == "COMIC_VINE"
                }
                .map { externalId ->
                    externalId.externalId
                }
                .sorted()

        val catalogIssues =
            catalog.series
                .flatMap { series ->
                    series.issues
                }
        val catalogIssueIds =
            catalogIssues
                .flatMap { issue ->
                    issue.externalIds
                }
                .filter { externalId ->
                    externalId.source == "COMIC_VINE"
                }
                .map { externalId ->
                    externalId.externalId
                }
                .sorted()

        assertEquals(213, readingList.items.size)
        assertTrue(catalogIssues.size >= readingList.items.size)
        assertTrue(catalogIssueIds.containsAll(readingListIssueIds))
    }

    @Test
    fun productionSpiderManCatalog_hasUniqueComicVineIssueIds() {
        val catalog = productionCatalogParser.parse(
            "catalogs/spider_man_volume_2_catalog.json")

        val comicVineIds =
            catalog.series
                .flatMap { series ->
                    series.issues
                }
                .flatMap { issue ->
                    issue.externalIds
                }
                .filter { externalId ->
                    externalId.source == "COMIC_VINE"
                }
                .map { externalId ->
                    externalId.externalId
                }

        assertTrue(comicVineIds.isNotEmpty())
        assertEquals(comicVineIds.size, comicVineIds.distinct().size)
    }

    @Test
    fun productionSpiderManCatalog_hasCoreSeriesExternalIds() {
        val catalog = productionCatalogParser.parse(
            "catalogs/spider_man_volume_2_catalog.json")
        val amazingSpiderMan =
            catalog.series.first { series ->
                series.title == "Amazing Spider-Man" && series.volume == 2
            }
        val amazingSpiderManComicVineId =
            amazingSpiderMan.externalIds.first { externalId ->
                externalId.source == "COMIC_VINE"
            }
        assertEquals("78701", amazingSpiderManComicVineId.externalId)

        val peterParkerSpiderMan =
            catalog.series.first { series ->
                series.title == "Peter Parker: Spider-Man" && series.volume == 2
            }
        val peterParkerComicVineId =
            peterParkerSpiderMan.externalIds.first { externalId ->
                externalId.source == "COMIC_VINE"
            }
        assertEquals("9142", peterParkerComicVineId.externalId)
    }

    @Test
    fun productionSpiderManCatalog_hasUniqueSeriesExternalIds() {
        val catalog = productionCatalogParser.parse(
            "catalogs/spider_man_volume_2_catalog.json")
        val externalIdKeys =
            catalog.series
                .flatMap { series ->
                    series.externalIds
                }
                .map { externalId ->
                    "${externalId.source}:" +
                            externalId.externalId
                }

        assertTrue(externalIdKeys.isNotEmpty())
        assertEquals(externalIdKeys.size, externalIdKeys.distinct().size)
    }

    @Test
    fun productionSpiderManCatalog_hasComicVineIdsForAllSeries() {
        val catalog = productionCatalogParser.parse(
            "catalogs/spider_man_volume_2_catalog.json")
        val seriesWithoutComicVineId =
            catalog.series.filter { series ->
                series.externalIds.none { externalId ->
                    externalId.source == "COMIC_VINE"
                }
            }
        assertEquals(
            emptyList<String>(),
            seriesWithoutComicVineId.map { series -> series.title }
        )
    }
}