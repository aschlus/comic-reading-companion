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

            val publishers =
                withTimeout(5000L.milliseconds) {
                    viewModel.publishers.first { it.size == 2 }
                }
            assertEquals(2, publishers.size)
            assertEquals(
                setOf(marvelId, dcId),
                publishers.map { it.id }.toSet()
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
}