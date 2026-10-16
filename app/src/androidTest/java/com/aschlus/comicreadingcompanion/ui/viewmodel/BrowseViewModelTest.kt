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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class BrowseViewModelTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: BrowseViewModel

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

        viewModel = BrowseViewModel(repository = repository)
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
    fun publishers_exposesPublishers() =
        runBlocking {
            val marvelId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )
            val dcId =
                comicDao.insertPublisher(
                    Publisher(name = "DC")
                )

            comicDao.insertSeries(
                Series(
                    publisherId = marvelId,
                    title = "Marvel Series One",
                    volume = 1,
                    startYear = 2000,
                    endYear = null
                )
            )

            comicDao.insertSeries(
                Series(
                    publisherId = marvelId,
                    title = "Marvel Series Two",
                    volume = 1,
                    startYear = 2001,
                    endYear = null
                )
            )

            comicDao.insertSeries(
                Series(
                    publisherId = dcId,
                    title = "DC Series",
                    volume = 1,
                    startYear = 2002,
                    endYear = null
                )
            )

            val publishers =
                withTimeout(5000L.milliseconds) {
                    viewModel.publishers.first { publishers ->
                        publishers
                            .firstOrNull {
                                it.publisherId ==
                                        marvelId
                            }
                            ?.seriesCount == 2 &&
                                publishers
                                    .firstOrNull {
                                        it.publisherId ==
                                                dcId
                                    }
                                    ?.seriesCount == 1
                    }
                }
            assertEquals(
                2,
                publishers.first {
                    it.publisherId == marvelId
                }.seriesCount
            )
            assertEquals(
                1,
                publishers.first {
                    it.publisherId == dcId
                }.seriesCount
            )
        }

    @Test
    fun updateSearchQuery_returnsMatchingSeriesAndIssues() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val spiderManSeriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Amazing Spider-Man",
                        volume = 1,
                        startYear = 1963,
                        endYear = 1998
                    )
                )

            val fantasticFourSeriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Fantastic Four",
                        volume = 1,
                        startYear = 1961,
                        endYear = 1996
                    )
                )

            val spiderManIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = spiderManSeriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Spider-Man!",
                        publicationDate = "1963-03",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

                comicDao.insertIssue(
                    Issue(
                        seriesId = fantasticFourSeriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "The Fantastic Four!",
                        publicationDate = "1961-11",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            viewModel.updateSearchQuery("Spider")

            val seriesResults =
                withTimeout(5000L.milliseconds) {
                    viewModel.seriesResults.first {
                        it.size == 1 && it.first().seriesId == spiderManSeriesId
                    }
                }

            val issueResults =
                withTimeout(5000L.milliseconds) {
                    viewModel.issueResults.first {
                        it.size == 1 && it.first().issueId == spiderManIssueId
                    }
                }

            assertEquals("Amazing Spider-Man", seriesResults.first().title)
            assertEquals(spiderManIssueId, issueResults.first().issueId)
            assertEquals("Amazing Spider-Man", issueResults.first().seriesTitle)
        }

    @Test
    fun updateSearchQuery_blankQueryClearsResults() =
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
                        volume = 1,
                        startYear = 1963,
                        endYear = 1998
                    )
                )

            comicDao.insertIssue(
                Issue(
                    seriesId = seriesId,
                    universeId = null,
                    issueNumber = "1",
                    title = "Spider-Man!",
                    publicationDate = "1963-03",
                    coverUrl = null,
                    description = null,
                    issueType = IssueType.REGULAR
                )
            )

            viewModel.updateSearchQuery("Spider")

            withTimeout(5000L.milliseconds) {
                viewModel.seriesResults.first { it.isNotEmpty() }
            }
            withTimeout(5000L.milliseconds) {
                viewModel.issueResults.first { it.isNotEmpty() }
            }

            viewModel.updateSearchQuery("   ")

            val clearedSeriesResults =
                withTimeout(5000L.milliseconds) {
                    viewModel.seriesResults.first { it.isEmpty() }
                }

            val clearedIssueResults =
                withTimeout(5000L.milliseconds) {
                    viewModel.issueResults.first { it.isEmpty() }
                }

            assertEquals(0, clearedSeriesResults.size)
            assertEquals(0, clearedIssueResults.size)
        }

    @Test
    fun updateSearchQuery_trimsWhitespaceBeforeSearching() =
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
                        volume = 1,
                        startYear = 1963,
                        endYear = 1998
                    )
                )

            val issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Spider-Man!",
                        publicationDate = "1963-03",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            viewModel.updateSearchQuery("   Spider   ")

            val seriesResults =
                withTimeout(5000L.milliseconds) {
                    viewModel.seriesResults.first {
                        it.size == 1 && it.first().seriesId == seriesId
                    }
                }

            val issueResults =
                withTimeout(5000L.milliseconds) {
                    viewModel.issueResults.first {
                        it.size == 1 && it.first().issueId == issueId
                    }
                }

            assertEquals(seriesId, seriesResults.first().seriesId)
            assertEquals(issueId, issueResults.first().issueId)
            assertEquals("   Spider   ", viewModel.searchQuery.value)
        }


    @Test
    fun updatePublisherFilter_filtersBlankQueryResultsAndClearsFilter() =
        runBlocking {
            val marvelPublisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val dcPublisherId =
                comicDao.insertPublisher(
                    Publisher(name = "DC")
                )

            val marvelSeriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = marvelPublisherId,
                        title = "Amazing Spider-Man",
                        volume = 1,
                        startYear = 1963,
                        endYear = null
                    )
                )

            val dcSeriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = dcPublisherId,
                        title = "Batman",
                        volume = 1,
                        startYear = 1940,
                        endYear = null
                    )
                )

            val marvelIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = marvelSeriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Spider-Man!",
                        publicationDate = "1963-03",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val dcIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = dcSeriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Batman!",
                        publicationDate = "1940-04",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            // A publisher filter must work with a blank query.
            viewModel.updatePublisherFilter(marvelPublisherId)

            val marvelSeriesResults =
                withTimeout(5000L.milliseconds) {
                    viewModel.seriesResults.first {
                        it.size == 1 &&
                                it.first().seriesId == marvelSeriesId
                    }
                }

            val marvelIssueResults =
                withTimeout(5000L.milliseconds) {
                    viewModel.issueResults.first {
                        it.size == 1 &&
                                it.first().issueId == marvelIssueId
                    }
                }

            assertEquals(
                listOf(marvelSeriesId),
                marvelSeriesResults.map { it.seriesId }
            )
            assertEquals(
                listOf(marvelIssueId),
                marvelIssueResults.map { it.issueId }
            )
            assertEquals(
                marvelPublisherId,
                viewModel.selectedPublisherId.value
            )

            // Changing publishers must update both result lists.
            viewModel.updatePublisherFilter(dcPublisherId)

            val dcSeriesResults =
                withTimeout(5000L.milliseconds) {
                    viewModel.seriesResults.first {
                        it.size == 1 &&
                                it.first().seriesId == dcSeriesId
                    }
                }

            val dcIssueResults =
                withTimeout(5000L.milliseconds) {
                    viewModel.issueResults.first {
                        it.size == 1 &&
                                it.first().issueId == dcIssueId
                    }
                }

            assertEquals(
                listOf(dcSeriesId),
                dcSeriesResults.map { it.seriesId }
            )
            assertEquals(
                listOf(dcIssueId),
                dcIssueResults.map { it.issueId }
            )

            // Clearing the publisher filter with a blank query
            // should clear both result lists.
            viewModel.updatePublisherFilter(null)

            val clearedSeriesResults =
                withTimeout(5000L.milliseconds) {
                    viewModel.seriesResults.first { it.isEmpty() }
                }

            val clearedIssueResults =
                withTimeout(5000L.milliseconds) {
                    viewModel.issueResults.first { it.isEmpty() }
                }

            assertEquals(0, clearedSeriesResults.size)
            assertEquals(0, clearedIssueResults.size)
            assertEquals(null, viewModel.selectedPublisherId.value)
        }
}