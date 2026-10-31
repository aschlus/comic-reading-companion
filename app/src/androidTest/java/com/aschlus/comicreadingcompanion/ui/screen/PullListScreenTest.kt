package com.aschlus.comicreadingcompanion.ui.screen

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import com.aschlus.comicreadingcompanion.ui.viewmodel.PullListViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PullListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: PullListViewModel

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

        comicDao = database.comicDao()

        repository =
            ComicRepository(
                comicDao = comicDao,
                database = database
            )

        viewModel =
            PullListViewModel(
                repository = repository
            )
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
    fun pullListScreen_displaysEmptyStatesAndBrowseAction() {
        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                PullListScreen(
                    viewModel = viewModel,
                    onBrowseClick = {},
                    onSeriesClick = {}
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule
                .onAllNodesWithText(
                    "Your personal pull list is empty. " +
                        "Add ongoing series to start following " +
                        "their upcoming issues."
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText("PULL LIST")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("TRACKED SERIES")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "Your personal pull list is empty. " +
                    "Add ongoing series to start following " +
                    "their upcoming issues."
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("BROWSE SERIES")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("UPCOMING RELEASES")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "Upcoming store-release dates and " +
                    "Final Order Cutoff deadlines will " +
                    "appear here for the series you follow."
            )
            .assertIsDisplayed()
    }

    @Test
    fun pullListScreen_browseActionInvokesCallback() {
        var clicked = false

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                PullListScreen(
                    viewModel = viewModel,
                    onBrowseClick = {
                        clicked = true
                    },
                    onSeriesClick = {}
                )
            }
        }

        composeRule
            .onNodeWithText("BROWSE SERIES")
            .performClick()

        composeRule.runOnIdle {
            assertTrue(clicked)
        }
    }

    @Test
    fun pullListScreen_displaysTrackedSeriesAndNavigatesToDetails() =
        runBlocking {

            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel Comics")
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

            comicDao.insertIssue(
                Issue(
                    seriesId = seriesId,
                    universeId = null,
                    issueNumber = "1",
                    title = "Test Issue 1",
                    publicationDate = "1999-01",
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
                    title = "Test Issue 2",
                    publicationDate = "1999-02",
                    coverUrl = null,
                    description = null,
                    issueType = IssueType.REGULAR
                )
            )

            repository.addSeriesToPullList(seriesId)

            var clickedSeriesId: Long? = null

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    PullListScreen(
                        viewModel = viewModel,
                        onBrowseClick = {},
                        onSeriesClick = { id ->
                            clickedSeriesId = id
                        }
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule
                    .onAllNodesWithText("Amazing Spider-Man")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithText("Amazing Spider-Man")
                .assertIsDisplayed()

            composeRule
                .onNodeWithText("Marvel Comics")
                .assertIsDisplayed()

            composeRule
                .onNodeWithText("Volume 2 • 1999-2003")
                .assertIsDisplayed()

            composeRule
                .onNodeWithText("2 ISSUES")
                .assertIsDisplayed()

            composeRule
                .onNodeWithText("Amazing Spider-Man")
                .performClick()

            composeRule.runOnIdle {
                assertEquals(seriesId, clickedSeriesId)
            }
        }

    @Test
    fun pullListScreen_updatesWhenTrackedSeriesIsRemoved() {
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel Comics")
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

            repository.addSeriesToPullList(seriesId)

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    PullListScreen(
                        viewModel = viewModel,
                        onBrowseClick = {},
                        onSeriesClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule
                    .onAllNodesWithText("Amazing Spider-Man")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            repository.removeSeriesFromPullList(seriesId)

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule
                    .onAllNodesWithText(
                        "Your personal pull list is empty. " +
                            "Add ongoing series to start following " +
                            "their upcoming issues."
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithText(
                    "Your personal pull list is empty. " +
                        "Add ongoing series to start following " +
                        "their upcoming issues."
                )
                .assertIsDisplayed()
        }
    }
}