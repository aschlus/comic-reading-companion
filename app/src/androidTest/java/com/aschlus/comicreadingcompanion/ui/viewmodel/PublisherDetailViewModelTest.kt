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
class PublisherDetailViewModelTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: PublisherDetailViewModel

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

        viewModel = PublisherDetailViewModel(repository = repository)
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
    fun loadPublisher_loadsPublisherAndSeries() =
        runBlocking {
            val marvelId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val dcId =
                comicDao.insertPublisher(
                    Publisher(name = "DC")
                )

            val spiderManSeriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = marvelId,
                        title = "Amazing Spider-Man",
                        volume = 2,
                        startYear = 1999,
                        endYear = 2003
                    )
                )

            comicDao.insertSeries(
                Series(
                    publisherId = dcId,
                    title = "Batman",
                    volume = 1,
                    startYear = 1940,
                    endYear = 2011
                )
            )

            viewModel.loadPublisher(marvelId)

            val loadedPublisher =
                withTimeout(5000L.milliseconds) {
                    viewModel.publisher.first { it?.id == marvelId}
                }

            val loadedSeries =
                withTimeout(5000L.milliseconds) {
                    viewModel.series.first { series ->
                        series.size == 1 &&
                            series.first().seriesId == spiderManSeriesId
                    }
                }

            assertNotNull(loadedPublisher)
            assertEquals("Marvel", loadedPublisher?.name)
            assertEquals(1, loadedSeries.size)
            assertEquals(spiderManSeriesId, loadedSeries.first().seriesId)
            assertEquals("Amazing Spider-Man", loadedSeries.first().title)
            assertEquals(2, loadedSeries.first().volume)
        }

    @Test
    fun series_reactsToReadingProgressChanges() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Progress Test Series",
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
                        title = "First Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            comicDao.insertIssue(
                Issue(
                    seriesId = seriesId,
                    universeId = null,
                    issueNumber = "2",
                    title = "Second Issue",
                    publicationDate = "2000-01",
                    coverUrl = null,
                    description = null,
                    issueType = IssueType.REGULAR
                )
            )

            viewModel.loadPublisher(publisherId)

            val initialSeries =
                withTimeout(5000L.milliseconds) {
                    viewModel.series.first { series ->
                        series.size == 1 &&
                                series.first().totalCount == 2 &&
                                series.first().readCount == 0
                    }
                }.first()

            assertEquals(2, initialSeries.totalCount)
            assertEquals(0, initialSeries.readCount)

            repository.markIssueAsRead(firstIssueId)

            val updatedSeries =
                withTimeout(5000L.milliseconds) {
                    viewModel.series.first { series ->
                        series.size == 1 &&
                            series.first().totalCount == 2 &&
                            series.first().readCount == 1
                    }
                }.first()

            assertEquals(2, updatedSeries.totalCount)
            assertEquals(1, updatedSeries.readCount)
        }
}