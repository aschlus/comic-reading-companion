package com.aschlus.comicreadingcompanion.data.importer

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingProgress
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.importer.models.CatalogExternalIdImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.CatalogIssueImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.CatalogSeriesImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.CatalogUniverseImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.ComicCatalogImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.IssueImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.ReadingListImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.ReadingListItemImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.SeriesImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.UniverseImportDto
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComicCatalogImporterTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var importer: ComicCatalogImporter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(context, ComicDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        comicDao = database.comicDao()

        importer =
            ComicCatalogImporter(
                comicDao = comicDao,
                database = database
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createCatalog(): ComicCatalogImportDto {
        return ComicCatalogImportDto(
            publisher = "Marvel Comics",
            universes =
                listOf(
                    CatalogUniverseImportDto(
                        name = "Marvel Universe",
                        designation = "Earth-616",
                        description = "Primary Marvel continuity"
                    )
                ),
            series =
                listOf(
                    CatalogSeriesImportDto(
                        title = "Amazing Spider-Man",
                        volume = 2,
                        startYear = 1999,
                        endYear = 2003,
                        externalIds =
                            listOf(
                                CatalogExternalIdImportDto(
                                    source = "COMIC_VINE",
                                    externalId = "78701",
                                    url = "https://example.com/series"
                                )
                            ),
                        issues =
                            listOf(
                                CatalogIssueImportDto(
                                    number = "1",
                                    title = "Test Issue",
                                    publicationDate = "1999-01",
                                    coverUrl = "https://example.com/cover.jpg",
                                    description = "Test description",
                                    type = "REGULAR",
                                    universeDesignation = "Earth-616",
                                    externalIds =
                                        listOf(
                                            CatalogExternalIdImportDto(
                                                source = "COMIC_VINE",
                                                externalId = "100001",
                                                url = "https://example.com/issue"
                                            )
                                        )
                                )
                            )
                    )
                )
        )
    }

    private fun createBundledReadingList(): ReadingListImportDto {
        return ReadingListImportDto(
            title = "Bundled Spider-Man Test",
            description = "Bundled reading list",
            publisher = "Marvel Comics",
            universe =
                UniverseImportDto(
                    name = "Marvel Universe",
                    designation = "Earth-616"
                ),
            sections = emptyList(),
            items =
                listOf(
                    ReadingListItemImportDto(
                        position = 1,
                        sectionPosition = null,
                        series =
                            SeriesImportDto(
                                title = "Amazing Spider-Man",
                                volume = 2,
                                startYear = 1999,
                                endYear = 2003
                            ),
                        issue =
                            IssueImportDto(
                                number = "1",
                                title = "Bundled Issue Title",
                                publicationDate = "1999-01",
                                coverUrl = null,
                                description = "Bundled description",
                                type = "REGULAR",
                                externalIds = emptyList()
                            ),
                        required = true,
                        notes = null
                    )
                )
        )
    }

    @Test
    fun validCatalog_importsCatalogEntitiesAndExternalIds() =
        runBlocking {
            importer.import(createCatalog())

            val publisher = comicDao.getPublisherByName("Marvel Comics")

            assertNotNull(publisher)

            val universe =
                comicDao.getUniverseByDesignation(
                    publisherId = publisher!!.id,
                    designation = "Earth-616"
                )

            assertNotNull(universe)

            val series =
                comicDao.getSeries(
                    publisherId = publisher.id,
                    title = "Amazing Spider-Man",
                    volume = 2
                )

            assertNotNull(series)

            val seriesExternalId =
                comicDao.getSeriesExternalId(
                    source = "COMIC_VINE",
                    externalId = "78701"
                )

            assertNotNull(seriesExternalId)
            assertEquals(series!!.id, seriesExternalId?.seriesId)

            val issue =
                comicDao.getIssue(
                    seriesId = series.id,
                    issueNumber = "1"
                )

            assertNotNull(issue)
            assertEquals(universe?.id, issue?.universeId)
            assertEquals("Test Issue", issue?.title)

            val issueExternalId =
                comicDao.getExternalId(
                    source = "COMIC_VINE",
                    externalId = "100001"
                )

            assertNotNull(issueExternalId)
            assertEquals(issue?.id, issueExternalId?.issueId)
        }

    @Test
    fun sameCatalogImportedTwice_doesNotCreateDuplicates() =
        runBlocking {
            val catalog = createCatalog()

            importer.import(catalog)
            importer.import(catalog)

            val publishers = comicDao.getAllPublishers()
            assertEquals(1, publishers.size)

            val publisher = publishers.first()
            val universes = comicDao.getUniversesForPublisher(publisher.id)
            assertEquals(1, universes.size)

            val series = comicDao.getSeriesForPublisher(publisher.id)
            assertEquals(1, series.size)

            val importedSeries = series.first()
            val seriesExternalIds =
                comicDao.getSeriesExternalIdsForSeries(importedSeries.id)
            assertEquals(1, seriesExternalIds.size)

            val issues = comicDao.getIssuesForSeries(importedSeries.id)
            assertEquals(1, issues.size)

            val issueExternalIds = comicDao.getExternalIdsForIssue(issues.first().id)
            assertEquals(1, issueExternalIds.size)
        }

    @Test
    fun catalogImport_reusesSeriesAndIssueCreatedByBundledReadingList() =
        runBlocking {
            val readingListImporter =
                ReadingListImporter(
                    comicDao = comicDao,
                    database = database
                )

            readingListImporter.import(createBundledReadingList())

            val publisher = comicDao.getPublisherByName("Marvel Comics")
            assertNotNull(publisher)

            val seriesBefore = comicDao.getSeries(
                publisherId = publisher!!.id,
                title = "Amazing Spider-Man",
                volume = 2
            )
            assertNotNull(seriesBefore)

            val issueBefore = comicDao.getIssue(
                seriesId = seriesBefore!!.id,
                issueNumber = "1"
            )
            assertNotNull(issueBefore)

            importer.import(createCatalog())

            val seriesAfter = comicDao.getSeries(
                publisherId = publisher.id,
                title = "Amazing Spider-Man",
                volume = 2
            )
            assertNotNull(seriesAfter)
            assertEquals(seriesBefore.id, seriesAfter?.id)

            val issues = comicDao.getIssuesForSeries(seriesBefore.id)
            assertEquals(1, issues.size)
            assertEquals(issueBefore!!.id, issues.first().id)

            val seriesExternalId = comicDao.getSeriesExternalId(
                source = "COMIC_VINE",
                externalId = "78701"
            )
            assertNotNull(seriesExternalId)
            assertEquals(seriesBefore.id, seriesExternalId?.seriesId)

            val issueExternalId = comicDao.getExternalId(
                source = "COMIC_VINE",
                externalId = "100001"
            )
            assertNotNull(issueExternalId)
            assertEquals(issueBefore.id, issueExternalId?.issueId)
        }

    @Test
    fun catalogImport_updatesMetadataWithoutLosingReadingProgress() =
        runBlocking {
            val originalCatalog = createCatalog()
            importer.import(originalCatalog)

            val publisher = comicDao.getPublisherByName("Marvel Comics")
            assertNotNull(publisher)

            val seriesBefore = comicDao.getSeries(
                publisherId = publisher!!.id,
                title = "Amazing Spider-Man",
                volume = 2
            )
            assertNotNull(seriesBefore)

            val issueBefore = comicDao.getIssue(
                seriesId = seriesBefore!!.id,
                issueNumber = "1"
            )
            assertNotNull(issueBefore)

            val progressId =
                comicDao.insertReadingProgress(
                    ReadingProgress(
                        issueId = issueBefore!!.id,
                        status = ReadingStatus.READ,
                        startedAt = 1000L,
                        completedAt = 2000L,
                        notes = "Keep this progress"
                    )
                )

            val originalSeriesData = originalCatalog.series.first()
            val originalIssueData = originalSeriesData.issues.first()

            val updatedCatalog =
                originalCatalog.copy(
                    series =
                        listOf(
                            originalSeriesData.copy(
                                title = "The Amazing Spider-Man",
                                startYear = 1998,
                                endYear = 2004,
                                issues =
                                    listOf(
                                        originalIssueData.copy(
                                            title = "Updated Issue Title",
                                            coverUrl = "https://example.com/new-cover.jpg",
                                            description = "Updated description"
                                        )
                                    )
                            )
                        )
                )

            importer.import(updatedCatalog)

            val seriesExternalId = comicDao.getSeriesExternalId(
                source = "COMIC_VINE",
                externalId = "78701"
            )
            assertNotNull(seriesExternalId)

            val seriesAfter = comicDao.getSeriesById(seriesExternalId!!.seriesId)
            assertNotNull(seriesAfter)
            assertEquals(seriesBefore.id, seriesAfter?.id)
            assertEquals("The Amazing Spider-Man", seriesAfter?.title)
            assertEquals(1998, seriesAfter?.startYear)
            assertEquals(2004, seriesAfter?.endYear)

            val issueExternalId = comicDao.getExternalId(
                source = "COMIC_VINE",
                externalId = "100001"
            )
            assertNotNull(issueExternalId)

            val issueAfter = comicDao.getIssueById(issueExternalId!!.issueId)
            assertNotNull(issueAfter)
            assertEquals(issueBefore.id, issueAfter?.id)
            assertEquals("Updated Issue Title", issueAfter?.title)
            assertEquals(
                "https://example.com/new-cover.jpg",
                issueAfter?.coverUrl
            )
            assertEquals("Updated description", issueAfter?.description)

            val progressAfter = comicDao.getReadingProgressForIssue(issueBefore.id)
            assertNotNull(progressAfter)
            assertEquals(progressId, progressAfter?.id)
            assertEquals(ReadingStatus.READ, progressAfter?.status)
            assertEquals(1000L, progressAfter?.startedAt)
            assertEquals(2000L, progressAfter?.completedAt)
            assertEquals("Keep this progress", progressAfter?.notes)
        }

    @Test
    fun productionCatalog_importsOverBundledReadingListWithoutDuplicates() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val readingList = ReadingListAssetParser(context = context)
                .parse("reading_lists/spider_man_volume_2.json")
            val catalog = ComicCatalogAssetParser(context = context)
                .parse("catalogs/spider_man_volume_2_catalog.json")
            val readingListImporter =
                ReadingListImporter(
                    comicDao = comicDao,
                    database = database
                )

            readingListImporter.import(readingList)

            val publisher = comicDao.getPublisherByName("Marvel Comics")
            assertNotNull(publisher)

            val seriesBefore = comicDao.getSeriesForPublisher(publisher!!.id)
            val issuesBefore = seriesBefore.flatMap { series ->
                comicDao.getIssuesForSeries(series.id)
            }
            assertEquals(22, seriesBefore.size)
            assertEquals(219, issuesBefore.size)

            val seriesIdsBefore =
                seriesBefore.map { series -> series.id }.toSet()
            val issueIdsBefore =
                issuesBefore.map { issue -> issue.id }.toSet()

            importer.import(catalog)

            val seriesAfter = comicDao.getSeriesForPublisher(publisher.id)
            val issuesAfter = seriesAfter.flatMap { series ->
                comicDao.getIssuesForSeries(series.id)
            }
            assertEquals(22, seriesAfter.size)
            assertEquals(219, issuesAfter.size)
            assertEquals(
                seriesIdsBefore,
                seriesAfter.map { issue -> issue.id }.toSet()
            )
            assertEquals(
                issueIdsBefore,
                issuesAfter.map { issue -> issue.id }.toSet()
            )
        }

    @Test
    fun productionCatalog_importsAllExternalIds() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val readingList = ReadingListAssetParser(context = context)
                .parse("reading_lists/spider_man_volume_2.json")
            val catalog = ComicCatalogAssetParser(context = context)
                .parse("catalogs/spider_man_volume_2_catalog.json")
            val readingListImporter =
                ReadingListImporter(
                    comicDao = comicDao,
                    database = database
                )

            readingListImporter.import(readingList)
            importer.import(catalog)

            val publisher = comicDao.getPublisherByName("Marvel Comics")
            assertNotNull(publisher)

            val series = comicDao.getSeriesForPublisher(publisher!!.id)
            val issues = series.flatMap { storedSeries ->
                comicDao.getIssuesForSeries(storedSeries.id)
            }

            val expectedSeriesExternalIds =
                catalog.series
                    .flatMap { seriesData ->
                        seriesData.externalIds
                    }
                    .map { externalId ->
                        "${externalId.source}:" +
                                externalId.externalId
                    }
                    .toSet()

            val actualSeriesExternalIds =
                series
                    .flatMap { storedSeries ->
                        comicDao.getSeriesExternalIdsForSeries(storedSeries.id)
                    }
                    .map { externalId ->
                        "${externalId.source}:" +
                                externalId.externalId
                    }
                    .toSet()

            assertEquals(expectedSeriesExternalIds, actualSeriesExternalIds)

            val expectedIssueExternalIds =
                catalog.series
                    .flatMap { seriesData ->
                        seriesData.issues
                    }
                    .flatMap { issueData ->
                        issueData.externalIds
                    }
                    .map { externalId ->
                        "${externalId.source}:" +
                                externalId.externalId
                    }
                    .toSet()

            val actualIssueExternalIds =
                issues
                    .flatMap { issue ->
                        comicDao.getExternalIdsForIssue(issue.id)
                    }
                    .map { externalId ->
                        "${externalId.source}:" +
                                externalId.externalId
                    }
                    .toSet()

            assertEquals(expectedIssueExternalIds, actualIssueExternalIds)
        }
}