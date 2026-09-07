package com.aschlus.comicreadingcompanion.data.progress

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListItem
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingProgressIntegrationTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database =
            Room.inMemoryDatabaseBuilder(
                context,
                ComicDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()

        comicDao = database.comicDao()
        repository = ComicRepository(
            comicDao = comicDao,
            database = database
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sameIssueInTwoReadingLists_hasOneGlobalReadingStatus() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Global Progress Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Shared Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val firstListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "First List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val secondListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Second List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 2000L,
                        updatedAt = 2000L
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = firstListId,
                    sectionId = null,
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = secondListId,
                    sectionId = null,
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            repository.markIssueAsRead(issueId)
            val firstListIssues = repository.getReadingListIssues(firstListId).first()
            val secondListIsses = repository.getReadingListIssues(secondListId).first()
            assertEquals(ReadingStatus.READ, firstListIssues.first().readingStatus)
            assertEquals(ReadingStatus.READ, secondListIsses.first().readingStatus)
        }

    @Test
    fun readingListSummaries_reactToGlobalProgressChanges() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Summary Progress Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Shared Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val firstListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "First List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val secondListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Second List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 2000L,
                        updatedAt = 2000L
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = firstListId,
                    sectionId = null,
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = secondListId,
                    sectionId = null,
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            val before = repository.getReadingListSummaries().first()
            val firstBefore = before.first { it.readingListId == firstListId }
            val secondBefore = before.first { it.readingListId == secondListId }
            assertEquals(1, firstBefore.totalCount)
            assertEquals(0, firstBefore.readCount)
            assertEquals(1, secondBefore.totalCount)
            assertEquals(0, secondBefore.readCount)
            repository.markIssueAsRead(issueId)
            val after = repository.getReadingListSummaries().first()
            val firstAfter = after.first { it.readingListId == firstListId }
            val secondAfter = after.first { it.readingListId == secondListId }
            assertEquals(1, firstAfter.readCount)
            assertEquals(1, secondAfter.readCount)
        }

    @Test
    fun readingIssue_isStillIncomplete() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Reading Status Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "In Progress",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Reading List Test",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = null,
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            repository.markIssueAsReading(issueId)

            val summary = repository.getReadingListSummaries()
                .first().first { it.readingListId == readingListId }
            assertEquals(1, summary.totalCount)
            assertEquals(0, summary.readCount)
            val continueItems = repository.getUnreadReadingListItems().first()
            assertEquals(1, continueItems.size)
            assertEquals(issueId, continueItems.first().issueId)
        }

    @Test
    fun continueChoosesFirstIssueThatIsNotRead() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Continue Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val firstIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "First",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val secondIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "2",
                        title = "Second",
                        publicationDate = "2000-02",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val thirdIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "3",
                        title = "Third",
                        publicationDate = "2000-03",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Continue Test List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            listOf(firstIssueId, secondIssueId, thirdIssueId).forEachIndexed { index, issueId ->
                comicDao.insertReadingListItem(
                    ReadingListItem(
                        readingListId = readingListId,
                        sectionId = null,
                        issueId = issueId,
                        position = index + 1,
                        required = true,
                        notes = null
                    )
                )
            }

            repository.markIssueAsRead(firstIssueId)
            repository.markIssueAsReading(secondIssueId)
            val continueItems = repository.getUnreadReadingListItems().first()
            val firstContinueItem = continueItems.first { it.readingListId == readingListId }
            assertEquals(secondIssueId, firstContinueItem.issueId)
            assertEquals(2, firstContinueItem.position)
        }

    @Test
    fun continueAdvancesAsIssuesAreMarkedRead() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Continue Advancement Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val firstIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "First",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val secondIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "2",
                        title = "Second",
                        publicationDate = "2000-02",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val thirdIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "3",
                        title = "Third",
                        publicationDate = "2000-03",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Continue Advancement List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            listOf(firstIssueId, secondIssueId, thirdIssueId).forEachIndexed { index, issueId ->
                comicDao.insertReadingListItem(
                    ReadingListItem(
                        readingListId = readingListId,
                        sectionId = null,
                        issueId = issueId,
                        position = index + 1,
                        required = true,
                        notes = null
                    )
                )
            }

            val initialContinueItem = repository.getUnreadReadingListItems()
                .first().first { it.readingListId == readingListId }
            assertEquals(firstIssueId, initialContinueItem.issueId)
            repository.markIssueAsRead(firstIssueId)
            val afterFirstRead = repository.getUnreadReadingListItems()
                .first().first { it.readingListId == readingListId }
            assertEquals(secondIssueId, afterFirstRead.issueId)
            repository.markIssueAsRead(secondIssueId)
            val afterSecondRead = repository.getUnreadReadingListItems()
                .first().first { it.readingListId == readingListId }
            assertEquals(thirdIssueId, afterSecondRead.issueId)
        }

    @Test
    fun markingSharedIssueUnread_clearsGlobalStatusAcrossReadingLists() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Global Unread Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Shared Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val firstListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "First List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val secondListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Second List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 2000L,
                        updatedAt = 2000L
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = firstListId,
                    sectionId = null,
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = secondListId,
                    sectionId = null,
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            repository.markIssueAsRead(issueId)
            repository.markIssueAsUnread(issueId)
            val firstListIssue =
                repository.getReadingListIssues(firstListId).first().first()
            val secondListIssue =
                repository.getReadingListIssues(secondListId).first().first()
            assertEquals(null, firstListIssue.readingStatus)
            assertEquals(null, secondListIssue.readingStatus)
            val summaries = repository.getReadingListSummaries().first()
            assertEquals(
                0,
                summaries.first {it.readingListId == firstListId}.readCount
            )
            assertEquals(
                0,
                summaries.first {it.readingListId == secondListId}.readCount
            )
        }

    @Test
    fun batchRead_updatesSummaryAndRemovesContinueItems() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Batch Read Integration Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueIds =
                (1..3).map { number ->
                    comicDao.insertIssue(
                        Issue(
                            seriesId = seriesId,
                            universeId = null,
                            issueNumber = number.toString(),
                            title = "Issue $number",
                            publicationDate = "2000-0$number",
                            coverUrl = null,
                            description = null,
                            issueType = IssueType.REGULAR
                        )
                    )
                }

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Batch Read List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )
            
            issueIds.forEachIndexed { index, issueId ->
                comicDao.insertReadingListItem(
                    ReadingListItem(
                        readingListId = readingListId,
                        sectionId = null,
                        issueId = issueId,
                        position = index + 1,
                        required = true,
                        notes = null
                    )
                )
            }

            repository.markIssuesAsRead(issueIds)
            val summary = repository.getReadingListSummaries()
                    .first().first { it.readingListId == readingListId }
            assertEquals(3, summary.totalCount)
            assertEquals(3, summary.readCount)
            val continueItems = repository.getUnreadReadingListItems()
                .first().filter { it.readingListId == readingListId }
            assertEquals(0, continueItems.size)
        }

    @Test
    fun batchUnread_restoresSummaryAndContinuePosition() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Batch Unread Integration Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueIds =
                (1..3).map { number ->
                    comicDao.insertIssue(
                        Issue(
                            seriesId = seriesId,
                            universeId = null,
                            issueNumber = number.toString(),
                            title = "Issue $number",
                            publicationDate = "2000-0$number",
                            coverUrl = null,
                            description = null,
                            issueType = IssueType.REGULAR
                        )
                    )
                }

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Batch Unread List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            issueIds.forEachIndexed { index, issueId ->
                comicDao.insertReadingListItem(
                    ReadingListItem(
                        readingListId = readingListId,
                        sectionId = null,
                        issueId = issueId,
                        position = index + 1,
                        required = true,
                        notes = null
                    )
                )
            }

            repository.markIssuesAsRead(issueIds)
            repository.markIssuesAsUnread(listOf(issueIds[1], issueIds[2]))
            val summary = repository.getReadingListSummaries()
                .first().first { it.readingListId == readingListId }
            assertEquals(3, summary.totalCount)
            assertEquals(1, summary.readCount)
            val continueItems = repository.getUnreadReadingListItems()
                .first().first { it.readingListId == readingListId }
            assertEquals(issueIds[1], continueItems.issueId)
            assertEquals(2, continueItems.position)
        }
}