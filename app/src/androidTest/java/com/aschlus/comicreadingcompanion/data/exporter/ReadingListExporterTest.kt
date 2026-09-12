package com.aschlus.comicreadingcompanion.data.exporter

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.importer.ReadingListImporter
import com.aschlus.comicreadingcompanion.data.importer.ReadingListAssetParser
import com.aschlus.comicreadingcompanion.data.importer.models.ExternalIdImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.IssueImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.ReadingListImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.ReadingListItemImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.ReadingListSectionImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.SeriesImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.UniverseImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.UniverseOverrideImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.UniverseOverrideMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingListExporterTest {

    private lateinit var database:
            ComicDatabase

    private lateinit var importer:
            ReadingListImporter

    private lateinit var exporter:
            ReadingListExporter

    private lateinit var parser:
            ReadingListAssetParser

    @Before
    fun setUp() {
        val context =
            ApplicationProvider
                .getApplicationContext<Context>()

        database =
            Room.inMemoryDatabaseBuilder(
                context,
                ComicDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()

        importer =
            ReadingListImporter(
                comicDao =
                    database.comicDao(),
                database =
                    database
            )

        exporter =
            ReadingListExporter(
                comicDao =
                    database.comicDao()
            )

        parser =
            ReadingListAssetParser(
                context = context
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exportSingleUniverseList_reconstructsImportData() =
        runBlocking {
            val importData =
                ReadingListImportDto(
                    title =
                        "Export Test",
                    description =
                        "Exporter test",
                    publisher =
                        "Test Publisher",
                    universe =
                        UniverseImportDto(
                            name =
                                "Test Universe",
                            designation =
                                "Earth-Test"
                        ),
                    sections =
                        listOf(
                            ReadingListSectionImportDto(
                                position = 1,
                                title = "First Arc",
                                description =
                                    "Arc description"
                            )
                        ),
                    items =
                        listOf(
                            ReadingListItemImportDto(
                                position = 1,
                                sectionPosition = 1,
                                series =
                                    SeriesImportDto(
                                        title =
                                            "Test Series",
                                        volume = 1,
                                        startYear = 2000,
                                        endYear = 2001
                                    ),
                                issue =
                                    IssueImportDto(
                                        number = "1",
                                        title =
                                            "Test Issue",
                                        publicationDate =
                                            "2000-01",
                                        coverUrl =
                                            "https://example.com/cover.jpg",
                                        description =
                                            "Issue description",
                                        type =
                                            "REGULAR",
                                        externalIds =
                                            listOf(
                                                ExternalIdImportDto(
                                                    source =
                                                        "test_provider",
                                                    externalId =
                                                        "12345",
                                                    url =
                                                        "https://example.com/12345"
                                                )
                                            )
                                    ),
                                required = true,
                                notes =
                                    "Read this first"
                            )
                        )
                )

            importer.import(
                importData
            )

            val readingList =
                database.comicDao()
                    .getAllReadingLists()
                    .first()
                    .first()

            val exported =
                exporter.export(
                    readingList.id
                )

            assertEquals(
                importData,
                exported
            )
        }

    @Test
    fun exportWithoutDefaultUniverse_emitsItemUniverseOverride() =
        runBlocking {
            val importData =
                ReadingListImportDto(
                    title =
                        "Multiverse Export Test",
                    description =
                        "Mixed continuity",
                    publisher =
                        "Test Publisher",
                    universe = null,
                    items =
                        listOf(
                            ReadingListItemImportDto(
                                position = 1,
                                series =
                                    SeriesImportDto(
                                        title =
                                            "Alternate Series",
                                        volume = 1,
                                        startYear = 2000,
                                        endYear = null
                                    ),
                                issue =
                                    IssueImportDto(
                                        number = "1",
                                        title =
                                            "Alternate Issue",
                                        publicationDate =
                                            "2000-01",
                                        coverUrl = null,
                                        description = null,
                                        type =
                                            "REGULAR"
                                    ),
                                universeOverride =
                                    UniverseOverrideImportDto(
                                        mode =
                                            UniverseOverrideMode.UNIVERSE,
                                        universe =
                                            UniverseImportDto(
                                                name =
                                                    "Alternate Universe",
                                                designation =
                                                    "Earth-Alternate"
                                            )
                                    ),
                                required = true,
                                notes = null
                            ),
                            ReadingListItemImportDto(
                                position = 2,
                                series =
                                    SeriesImportDto(
                                        title =
                                            "Universe-Free Series",
                                        volume = 1,
                                        startYear = 2001,
                                        endYear = null
                                    ),
                                issue =
                                    IssueImportDto(
                                        number = "1",
                                        title =
                                            "Universe-Free Issue",
                                        publicationDate =
                                            "2001-01",
                                        coverUrl = null,
                                        description = null,
                                        type =
                                            "REGULAR"
                                    ),
                                required = false,
                                notes =
                                    "No universe"
                            )
                        )
                )

            importer.import(
                importData
            )

            val readingList =
                database.comicDao()
                    .getAllReadingLists()
                    .first()
                    .first()

            val exported =
                exporter.export(
                    readingList.id
                )

            assertNull(
                exported.universe
            )

            assertEquals(
                UniverseOverrideMode.UNIVERSE,
                exported.items[0]
                    .universeOverride
                    ?.mode
            )

            assertEquals(
                "Earth-Alternate",
                exported.items[0]
                    .universeOverride
                    ?.universe
                    ?.designation
            )

            assertNull(
                exported.items[1]
                    .universeOverride
            )

            assertEquals(
                importData,
                exported
            )
        }

    @Test
    fun exportJson_roundTripsThroughReadingListParser() =
        runBlocking {
            val importData =
                ReadingListImportDto(
                    title =
                        "JSON Round Trip",
                    description =
                        "Exporter JSON test",
                    publisher =
                        "Test Publisher",
                    universe = null,
                    items =
                        listOf(
                            ReadingListItemImportDto(
                                position = 1,
                                series =
                                    SeriesImportDto(
                                        title =
                                            "Round Trip Series",
                                        volume = 1,
                                        startYear = 2026,
                                        endYear = null
                                    ),
                                issue =
                                    IssueImportDto(
                                        number = "1",
                                        title =
                                            "Round Trip Issue",
                                        publicationDate =
                                            "2026-09",
                                        coverUrl = null,
                                        description =
                                            "Round-trip description",
                                        type =
                                            "REGULAR",
                                        externalIds =
                                            listOf(
                                                ExternalIdImportDto(
                                                    source =
                                                        "test_provider",
                                                    externalId =
                                                        "round-trip-1",
                                                    url = null
                                                )
                                            )
                                    ),
                                universeOverride =
                                    UniverseOverrideImportDto(
                                        mode =
                                            UniverseOverrideMode.UNIVERSE,
                                        universe =
                                            UniverseImportDto(
                                                name =
                                                    "Alternate Universe",
                                                designation =
                                                    "Earth-Alternate"
                                            )
                                    ),
                                required = true,
                                notes =
                                    "Round-trip note"
                            )
                        )
                )

            importer.import(
                importData
            )

            val readingList =
                database.comicDao()
                    .getAllReadingLists()
                    .first()
                    .first()

            val jsonText =
                exporter.exportJson(
                    readingList.id
                )

            val parsed =
                parser.parseJson(
                    jsonText = jsonText,
                    sourceDescription =
                        "exported reading-list JSON"
                )

            assertEquals(
                importData,
                parsed
            )
        }
}