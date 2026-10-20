package com.aschlus.comicreadingcompanion.data.exporter

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.ExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.IssueOwnership
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListItem
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSection
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSource
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListStyle
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingProgress
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.SeriesExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComicAppBackupExporterTest {

    private lateinit var database: ComicDatabase
    private lateinit var exporter: ComicAppBackupExporter

    @Before
    fun setUp() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        database =
            Room.inMemoryDatabaseBuilder(
                context,
                ComicDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()

        exporter = ComicAppBackupExporter(
            comicDao = database.comicDao(),
            database = database
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun export_includesAllRecordsAndPreservesRelationships() =
        runBlocking {
            val dao = database.comicDao()

            val publisherId =
                dao.insertPublisher(
                    Publisher(name = "Marvel Comics")
                )

            val universeId =
                dao.insertUniverse(
                    Universe(
                        publisherId = publisherId,
                        name = "Marvel Universe",
                        designation = "Earth-616",
                        description = "Primary continuity"
                    )
                )

            val seriesId =
                dao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Amazing Spider-Man",
                        volume = 2,
                        startYear = 1999,
                        endYear = 2003
                    )
                )

            dao.insertSeriesExternalId(
                SeriesExternalId(
                    seriesId = seriesId,
                    source = "COMIC_VINE",
                    externalId = "12345",
                    url = "https://example.com/series"
                )
            )

            val issueId =
                dao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = universeId,
                        issueNumber = "1",
                        title = "Origin",
                        publicationDate = "1999-01",
                        coverUrl = "https://example.com/cover.jpg",
                        description = "Existing description",
                        issueType = IssueType.REGULAR
                    )
                )

            dao.insertExternalId(
                ExternalId(
                    issueId = issueId,
                    source = "COMIC_VINE",
                    externalId = "67890",
                    url = "https://example.com/issue"
                )
            )

            val now = System.currentTimeMillis()

            val readingListId =
                dao.insertReadingList(
                    ReadingList(
                        title = "My Reading Order",
                        description = "Personal list",
                        publisherId = publisherId,
                        universeId = universeId,
                        style = ReadingListStyle.BLUE,
                        source = ReadingListSource.USER,
                        sourceKey = null,
                        createdAt = now,
                        updatedAt = now
                    )
                )

            val sectionId =
                dao.insertReadingListSection(
                    ReadingListSection(
                        readingListId = readingListId,
                        title = "Beginning",
                        description = null,
                        position = 1
                    )
                )

            dao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = sectionId,
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = "Start here"
                )
            )

            dao.insertReadingProgress(
                ReadingProgress(
                    issueId = issueId,
                    status = ReadingStatus.READ,
                    startedAt = 100L,
                    completedAt = 200L,
                    notes = "Finished"
                )
            )

            dao.markIssueOwned(
                IssueOwnership(issueId = issueId)
            )

            val backup = exporter.export()

            assertEquals(
                ComicAppBackupDto.CURRENT_FORMAT_VERSION,
                backup.formatVersion
            )
            assertTrue(backup.exportedAtEpochMillis > 0L)

            assertEquals(1, backup.publishers.size)
            assertEquals(1, backup.universes.size)
            assertEquals(1, backup.series.size)
            assertEquals(1, backup.seriesExternalIds.size)
            assertEquals(1, backup.issues.size)
            assertEquals(1, backup.issueExternalIds.size)
            assertEquals(1, backup.readingLists.size)
            assertEquals(1, backup.readingListSections.size)
            assertEquals(1, backup.readingListItems.size)
            assertEquals(1, backup.readingProgress.size)
            assertEquals(1, backup.issueOwnership.size)

            assertEquals(
                publisherId,
                backup.series.single().publisherId
            )
            assertEquals(
                seriesId,
                backup.issues.single().seriesId
            )
            assertEquals(
                universeId,
                backup.issues.single().universeId
            )
            assertEquals(
                "Existing description",
                backup.issues.single().description
            )
            assertEquals(
                seriesId,
                backup.seriesExternalIds.single().seriesId
            )
            assertEquals(
                issueId,
                backup.issueExternalIds.single().issueId
            )
            assertEquals(
                readingListId,
                backup.readingListSections.single().readingListId
            )
            assertEquals(
                sectionId,
                backup.readingListItems.single().sectionId
            )
            assertEquals(
                issueId,
                backup.readingListItems.single().issueId
            )
            assertEquals(
                ReadingStatus.READ.name,
                backup.readingProgress.single().status
            )
            assertEquals(
                "Finished",
                backup.readingProgress.single().notes
            )
            assertEquals(
                issueId,
                backup.issueOwnership.single().issueId
            )

            val json = exporter.exportJson()

            assertTrue(json.contains("My Reading Order"))
            assertTrue(json.contains("67890"))
            assertTrue(json.contains("Finished"))
        }
}