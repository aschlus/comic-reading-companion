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
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
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
class SeriesDetailViewModelTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: SeriesDetailViewModel

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

        viewModel = SeriesDetailViewModel(repository = repository)
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
    fun loadIssue_loadsSeriesDetailAndIssues() =
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

            val firstIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "30",
                        title = "Coming Home",
                        publicationDate = "2001-06",
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
                        issueNumber = "31",
                        title = "The Conversation",
                        publicationDate = "2001-07",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            viewModel.loadSeries(seriesId)

            val loadedSeries =
                withTimeout(5000L.milliseconds) {
                    viewModel.series.first { it?.seriesId == seriesId}
                }

            val loadedIssues =
                withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { it.size == 2 }
                }

            assertNotNull(loadedSeries)
            assertEquals("Amazing Spider-Man", loadedSeries?.title)
            assertEquals(2, loadedSeries?.volume)
            assertEquals("Marvel", loadedSeries?.publisherName)
            assertEquals(2, loadedIssues.size)
            assertEquals(firstIssueId, loadedIssues[0].issueId)
            assertEquals(secondIssueId, loadedIssues[1].issueId)
        }

    @Test
    fun issues_reactToReadingProgressChange() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Reactive Series",
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
                        title = "Reactive Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            viewModel.loadSeries(issueId)

            val initialIssue =
                withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { it.size == 1}
                }.first()

            assertEquals(null, initialIssue.readingStatus)

            repository.markIssueAsRead(issueId)

            val updatedIssue =
                withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { issues ->
                        issues.size == 1 &&
                            issues.first().readingStatus == ReadingStatus.READ
                    }
                }.first()

            assertEquals(ReadingStatus.READ, updatedIssue.readingStatus)
        }
}