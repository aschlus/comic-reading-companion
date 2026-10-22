
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComicAppBackupRestorerTest {

    private lateinit var database: ComicDatabase
    private lateinit var exporter: ComicAppBackupExporter
    private lateinit var parser: ComicAppBackupParser
    private lateinit var restorer: ComicAppBackupRestorer

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

        parser = ComicAppBackupParser()

        restorer = ComicAppBackupRestorer(
            comicDao = database.comicDao(),
            database = database,
            parser = parser
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun restoreJson_replacesDatabaseAndPreservesAllRecords() =
        runBlocking {
            val dao = database.comicDao()

            // Create the complete set of record types to back up.

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
                        endYear = 2003,
                        universeId = universeId
                    )
                )

            dao.insertSeriesExternalId(
                SeriesExternalId(
                    seriesId = seriesId,
                    source = "COMIC_VINE",
                    externalId = "series-12345",
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
                        description = null,
                        issueType = IssueType.REGULAR,
                        isIssueTypeManuallyOverridden = true
                    )
                )

            dao.insertExternalId(
                ExternalId(
                    issueId = issueId,
                    source = "COMIC_VINE",
                    externalId = "issue-67890",
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

            // Capture the expected snapshot from the exported JSON.

            val backupJson = exporter.exportJson()
            val expected = parser.parseJson(backupJson)

            assertEquals(1, expected.publishers.size)
            assertEquals(1, expected.universes.size)
            assertEquals(1, expected.series.size)
            assertEquals(1, expected.seriesExternalIds.size)
            assertEquals(1, expected.issues.size)
            assertEquals(1, expected.issueExternalIds.size)
            assertEquals(1, expected.readingLists.size)
            assertEquals(1, expected.readingListSections.size)
            assertEquals(1, expected.readingListItems.size)
            assertEquals(1, expected.readingProgress.size)
            assertEquals(1, expected.issueOwnership.size)

            // Add data after the backup so replace behavior is verified.

            dao.insertPublisher(
                Publisher(name = "Temporary Publisher")
            )

            val beforeRestore = exporter.export()

            assertEquals(2, beforeRestore.publishers.size)
            assertTrue(
                beforeRestore.publishers.any {
                    it.name == "Temporary Publisher"
                }
            )

            // Restore the earlier snapshot.

            val restored = restorer.restoreJson(
                jsonText = backupJson,
                sourceDescription = "test backup"
            )

            assertEquals(expected, restored)

            // Read the actual database after restoration.

            val actual = exporter.export()

            // The post-backup publisher must no longer exist.

            assertEquals(1, actual.publishers.size)
            assertFalse(
                actual.publishers.any {
                    it.name == "Temporary Publisher"
                }
            )

            // Every record category must match the original snapshot.

            assertEquals(expected.publishers, actual.publishers)
            assertEquals(expected.universes, actual.universes)
            assertEquals(expected.series, actual.series)
            assertEquals(
                universeId,
                actual.series.single().universeId
            )
            assertEquals(
                expected.seriesExternalIds,
                actual.seriesExternalIds
            )
            assertEquals(expected.issues, actual.issues)
            assertEquals(
                expected.issueExternalIds,
                actual.issueExternalIds
            )
            assertEquals(expected.readingLists, actual.readingLists)
            assertEquals(
                expected.readingListSections,
                actual.readingListSections
            )
            assertEquals(
                expected.readingListItems,
                actual.readingListItems
            )
            assertEquals(
                expected.readingProgress,
                actual.readingProgress
            )
            assertEquals(
                expected.issueOwnership,
                actual.issueOwnership
            )

            // Verify key relationships and user state explicitly.

            assertEquals(
                publisherId,
                actual.series.single().publisherId
            )
            assertEquals(
                seriesId,
                actual.issues.single().seriesId
            )
            assertEquals(
                universeId,
                actual.issues.single().universeId
            )
            assertTrue(
                actual.issues.single().isIssueTypeManuallyOverridden
            )
            assertEquals(
                "series-12345",
                actual.seriesExternalIds.single().externalId
            )
            assertEquals(
                "issue-67890",
                actual.issueExternalIds.single().externalId
            )
            assertEquals(
                sectionId,
                actual.readingListItems.single().sectionId
            )
            assertEquals(
                issueId,
                actual.readingListItems.single().issueId
            )
            assertEquals(
                ReadingStatus.READ.name,
                actual.readingProgress.single().status
            )
            assertEquals(
                "Finished",
                actual.readingProgress.single().notes
            )
            assertEquals(
                issueId,
                actual.issueOwnership.single().issueId
            )
        }
}