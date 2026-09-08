package com.aschlus.comicreadingcompanion.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
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
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class HomeViewModelTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: HomeViewModel

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

        repository =
            ComicRepository(
                comicDao = comicDao,
                database = database
            )

        viewModel = HomeViewModel(repository = repository)
    }

    @After
    fun tearDown() {
        runBlocking {
            viewModel
                .viewModelScope
                .coroutineContext[Job]
                ?.cancelAndJoin()
        }
        database.close()
    }

    @Test
    fun readingLists_exposesReadingLists() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val firstListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "First Reading List",
                        description = "First",
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val secondListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Second Reading List",
                        description = "Second",
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 2000L,
                        updatedAt = 2000L
                    )
                )

            val readingLists =
                withTimeout(5000L.milliseconds) {
                    viewModel.readingLists.first { it.size == 2 }
                }

            assertEquals(2, readingLists.size)
            assertEquals(secondListId, readingLists[0].id)
            assertEquals("Second Reading List", readingLists[0].title)
            assertEquals(firstListId, readingLists[1].id)
            assertEquals("First Reading List", readingLists[1].title)
        }

    @Test
    fun readingListSummaries_exposesProgressCounts() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Home Summary Test Series",
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

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Home Summary Test List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            listOf(firstIssueId, secondIssueId).forEachIndexed { index, issueId ->
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

            val summary =
                withTimeout(5000L.milliseconds) {
                    viewModel.readingListSummaries.first { summaries ->
                        summaries.any {
                            it.readingListId == readingListId &&
                                it.totalCount == 2 &&
                                it.readCount == 1
                        }
                    }
                }.first { it.readingListId == readingListId }


            assertEquals(2, summary.totalCount)
            assertEquals(1, summary.readCount)
        }

    @Test
    fun continueItems_selectsFirstUnreadIssueForEachList() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Home Continue Test Series",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueIds =
                (1..4).map { number ->
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

            val firstListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "First Continue List",
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
                        title = "Second Continue List",
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
                    issueId = issueIds[0],
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = firstListId,
                    sectionId = null,
                    issueId = issueIds[1],
                    position = 2,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = secondListId,
                    sectionId = null,
                    issueId = issueIds[2],
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = secondListId,
                    sectionId = null,
                    issueId = issueIds[3],
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            repository.markIssueAsRead(issueIds[0])
            repository.markIssueAsReading(issueIds[2])

            val continueItems =
                withTimeout(5000L.milliseconds) {
                    viewModel.continueItems.first { items ->
                        items.size == 2 &&
                            items[firstListId]?.issueId == issueIds[1] &&
                            items[secondListId]?.issueId == issueIds[2]

                    }
                }
            assertEquals(issueIds[1], continueItems[firstListId]?.issueId)
            assertEquals(2, continueItems[firstListId]?.position)
            assertEquals(issueIds[2], continueItems[secondListId]?.issueId)
            assertEquals(1, continueItems[secondListId]?.position)
        }

    @Test
    fun continueItems_reactsToProgressChanges() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Reactive Continue Test Series",
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
                        title = "Reactive Continue List",
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

            val initialContinue =
                withTimeout(5000L.milliseconds) {
                    viewModel.continueItems.first { items ->
                        items[readingListId]?.issueId == issueIds[0]
                    }
                }
            assertEquals(issueIds[0], initialContinue[readingListId]?.issueId)
            repository.markIssueAsRead(issueIds[0])
            val secondContinue =
                withTimeout(5000L.milliseconds) {
                    viewModel.continueItems.first { items ->
                        items[readingListId]?.issueId == issueIds[1]
                    }
                }
            assertEquals(issueIds[1], secondContinue[readingListId]?.issueId)
            repository.markIssueAsRead(issueIds[1])
            val thirdContinue =
                withTimeout(5000L.milliseconds) {
                    viewModel.continueItems.first { items ->
                        items[readingListId]?.issueId == issueIds[2]
                    }
                }
            assertEquals(issueIds[2], thirdContinue[readingListId]?.issueId)
            repository.markIssueAsRead(issueIds[2])
            val completedContinue =
                withTimeout(5000L.milliseconds) {
                    viewModel.continueItems.first { items ->
                        !items.containsKey(readingListId)
                    }
                }
            assertEquals(false, completedContinue.containsKey(readingListId))
        }
}