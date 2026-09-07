package com.aschlus.comicreadingcompanion.ui.viewmodel

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class IssueDetailViewModelTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: IssueDetailViewModel

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

        viewModel = IssueDetailViewModel(repository = repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadIssue_loadsIssueDetail() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Amazing Spider-Man",
                        volume = 2,
                        startYear = 1999,
                        endYear = 2003
                    )
                )

            val issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "30",
                        title = "Coming Home",
                        publicationDate = "2001-06",
                        coverUrl = "https://example.com/cover.jpg",
                        description = "Test description",
                        issueType = IssueType.REGULAR
                    )
                )

            viewModel.loadIssue(issueId)

            val loadedIssue =
                withTimeout(5000L.milliseconds) {
                    viewModel.issue.first { it?.issueId == issueId}
                }

            assertNotNull(loadedIssue)
            assertEquals(issueId, loadedIssue?.issueId)
            assertEquals("Amazing Spider-Man", loadedIssue?.seriesTitle)
            assertEquals(2, loadedIssue?.seriesVolume)
            assertEquals("30", loadedIssue?.issueNumber)
            assertEquals("Coming Home", loadedIssue?.issueTitle)
            assertEquals("Marvel", loadedIssue?.publisherName)
            assertEquals(null, loadedIssue?.readingStatus)
        }

    @Test
    fun markAsReading_updatesIssueStatus() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Reading Status Test Series",
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
                        title = "Test Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            viewModel.loadIssue(issueId)

            withTimeout(5000L.milliseconds) {
                viewModel.issue.first { it?.issueId == issueId}
            }

            viewModel.markAsReading()

            val updatedIssue =
                withTimeout(5000L.milliseconds) {
                    viewModel.issue.first { it?.readingStatus == ReadingStatus.READING}
                }

            assertEquals(ReadingStatus.READING, updatedIssue?.readingStatus)
        }

    @Test
    fun markAsRead_updatesIssueStatus() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Read Status Test Series",
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
                        title = "Test Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            viewModel.loadIssue(issueId)

            withTimeout(5000L.milliseconds) {
                viewModel.issue.first { it?.issueId == issueId}
            }

            viewModel.markAsRead()

            val updatedIssue =
                withTimeout(5000L.milliseconds) {
                    viewModel.issue.first { it?.readingStatus == ReadingStatus.READ}
                }

            assertEquals(ReadingStatus.READ, updatedIssue?.readingStatus)
        }

    @Test
    fun markAsUnread_updatesIssueStatus() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Unread Status Test Series",
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
                        title = "Test Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            repository.markIssueAsRead(issueId)

            viewModel.loadIssue(issueId)

            withTimeout(5000L.milliseconds) {
                viewModel.issue.first { it?.readingStatus == ReadingStatus.READ}
            }

            viewModel.markAsUnread()

            val updatedIssue =
                withTimeout(5000L.milliseconds) {
                    viewModel.issue.first { it?.readingStatus == null}
                }

            assertEquals(null, updatedIssue?.readingStatus)
        }
}