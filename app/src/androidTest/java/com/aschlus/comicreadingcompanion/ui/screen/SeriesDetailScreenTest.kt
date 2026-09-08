package com.aschlus.comicreadingcompanion.ui.screen

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import com.aschlus.comicreadingcompanion.ui.viewmodel.SeriesDetailViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeriesDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

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
    fun seriesDetailScreen_displaysSeriesAndIssues() {
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
                        coverUrl = null,
                        description = "Spider-Man faces a dangerous new enemy.",
                        issueType = IssueType.REGULAR
                    )
                )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    SeriesDetailScreen(
                        seriesId = seriesId,
                        viewModel = viewModel,
                        onPublisherClick = {},
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("#30")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Volume 2 • 1999-2003").assertIsDisplayed()
            composeRule.onNodeWithText("Marvel").assertIsDisplayed()
            composeRule.onNodeWithText("0 of 1 read • 0% complete").assertIsDisplayed()
            composeRule.onNodeWithText("Issues").assertIsDisplayed()
            composeRule.onNodeWithText("#30").assertIsDisplayed()
            composeRule.onNodeWithText("Coming Home").assertIsDisplayed()
            composeRule.onNodeWithText("Jun 2001 • Regular").assertIsDisplayed()
            composeRule.onNodeWithText("Unread").assertIsDisplayed()
        }
    }

    @Test
    fun seriesDetailScreen_backButtonInvokesCallback() {
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

            var backClicked = false

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    SeriesDetailScreen(
                        seriesId = seriesId,
                        viewModel = viewModel,
                        onPublisherClick = {},
                        onIssueClick = {},
                        onBackClick = {
                            backClicked = true
                        }
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Marvel")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithContentDescription("Back").performClick()
            composeRule.runOnIdle {
                assertEquals(true, backClicked)
            }
        }
    }

    @Test
    fun seriesDetailScreen_publisherClickInvokesCallback() {
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

            var clickedPublisherId: Long? = null

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    SeriesDetailScreen(
                        seriesId = seriesId,
                        viewModel = viewModel,
                        onPublisherClick = { id ->
                            clickedPublisherId = id
                        },
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Marvel")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Marvel").performClick()
            composeRule.runOnIdle {
                assertEquals(publisherId, clickedPublisherId)
            }
        }
    }

    @Test
    fun seriesDetailScreen_issueClickInvokesCallback() {
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
                        coverUrl = null,
                        description = "Spider-Man faces a dangerous new enemy.",
                        issueType = IssueType.REGULAR
                    )
                )

            var clickedIssueId: Long? = null

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    SeriesDetailScreen(
                        seriesId = seriesId,
                        viewModel = viewModel,
                        onPublisherClick = {},
                        onIssueClick = { id ->
                            clickedIssueId = id
                        },
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("#30")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("#30").performClick()
            composeRule.runOnIdle {
                assertEquals(issueId, clickedIssueId)
            }
        }
    }

    @Test
    fun seriesDetailScreen_progressUpdatesWhenIssuesStatusChanges() {
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
                        coverUrl = null,
                        description = "Spider-Man faces a dangerous new enemy.",
                        issueType = IssueType.REGULAR
                    )
                )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    SeriesDetailScreen(
                        seriesId = seriesId,
                        viewModel = viewModel,
                        onPublisherClick = {},
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("0 of 1 read • 0% complete")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Unread").assertIsDisplayed()
            repository.markIssueAsRead(issueId)
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("1 of 1 read • 100% complete")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Read").assertIsDisplayed()
        }
    }
}