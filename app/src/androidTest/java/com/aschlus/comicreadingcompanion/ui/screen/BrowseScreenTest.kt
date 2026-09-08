package com.aschlus.comicreadingcompanion.ui.screen

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
import com.aschlus.comicreadingcompanion.ui.viewmodel.BrowseViewModel
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
class BrowseScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

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
    fun browseScreen_displaysPublishers() {
        runBlocking {
            comicDao.insertPublisher(
                Publisher(name = "Marvel")
            )
            comicDao.insertPublisher(
                Publisher(name = "DC")
            )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    BrowseScreen(
                        viewModel = viewModel,
                        onPublisherClick = {},
                        onSeriesClick = {},
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Marvel")
                    .fetchSemanticsNodes()
                    .isNotEmpty() &&
                        composeRule.onAllNodesWithText("DC")
                            .fetchSemanticsNodes()
                            .isNotEmpty()
            }
            composeRule.onNodeWithText("Browse Comics").assertIsDisplayed()
            composeRule.onNodeWithText("Search series and issues").assertIsDisplayed()
            composeRule.onNodeWithText("Publishers").assertIsDisplayed()
            composeRule.onNodeWithText("Marvel").assertIsDisplayed()
            composeRule.onNodeWithText("DC").assertIsDisplayed()
        }
    }

    @Test
    fun browseScreen_publisherClickInvokesCallback() {
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            var clickedPublisherId: Long? = null

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    BrowseScreen(
                        viewModel = viewModel,
                        onPublisherClick = { id ->
                            clickedPublisherId = id
                        },
                        onSeriesClick = {},
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
    fun browseScreen_searchDisplaysSeriesAndIssueResults() {
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
                        startYear = 1962,
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

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    BrowseScreen(
                        viewModel = viewModel,
                        onPublisherClick = {},
                        onSeriesClick = {},
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.onNode(hasSetTextAction()).performTextInput("Spider")
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man")
                    .fetchSemanticsNodes()
                    .isNotEmpty() &&
                        composeRule.onAllNodesWithText("Amazing Spider-Man #1")
                            .fetchSemanticsNodes()
                            .isNotEmpty()
            }
            composeRule.onNodeWithText("Series").assertIsDisplayed()
            composeRule.onNodeWithText("Amazing Spider-Man").assertIsDisplayed()
            composeRule.onNodeWithText("Issues").assertIsDisplayed()
            composeRule.onNodeWithText("Amazing Spider-Man #1").assertIsDisplayed()
            composeRule.onNodeWithText("Spider-Man!").assertIsDisplayed()
        }
    }

    @Test
    fun browseScreen_seriesResultInvokesCallback() {
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
                        startYear = 1962,
                        endYear = 1998
                    )
                )

            var clickedSeriesId: Long? = null

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    BrowseScreen(
                        viewModel = viewModel,
                        onPublisherClick = {},
                        onSeriesClick = { id ->
                            clickedSeriesId = id
                        },
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.onNode(hasSetTextAction()).performTextInput("Amazing")
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Amazing Spider-Man").performClick()
            composeRule.runOnIdle {
                assertEquals(seriesId, clickedSeriesId)
            }
        }
    }

    @Test
    fun browseScreen_clearingSearchRestoresBrowseContent() {
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
                        startYear = 1962,
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

            var clickedIssueId: Long? = null

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    BrowseScreen(
                        viewModel = viewModel,
                        onPublisherClick = {},
                        onSeriesClick = {},
                        onIssueClick = { id ->
                            clickedIssueId = id
                        },
                        onBackClick = {}
                    )
                }
            }

            composeRule.onNode(hasSetTextAction()).performTextInput("Spider")
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man #1")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Amazing Spider-Man #1").performClick()
            composeRule.runOnIdle {
                assertEquals(seriesId, clickedIssueId)
            }
        }
    }

    @Test
    fun browseScreen_issueResultInvokesCallback() {
        runBlocking {
            comicDao.insertPublisher(
                Publisher(name = "Marvel")
            )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId =
                            comicDao.insertPublisher(
                                Publisher(name = "DC")
                            ),
                        title = "Batman",
                        volume = 1,
                        startYear = 1940,
                        endYear = 2011
                    )
                )

            comicDao.insertIssue(
                Issue(
                    seriesId = seriesId,
                    universeId = null,
                    issueNumber = "1",
                    title = "The Legend of the Batman",
                    publicationDate = "1940-04",
                    coverUrl = null,
                    description = null,
                    issueType = IssueType.REGULAR
                )
            )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    BrowseScreen(
                        viewModel = viewModel,
                        onPublisherClick = {},
                        onSeriesClick = {},
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
            composeRule.onNode(hasSetTextAction()).performTextInput("Batman")
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Batman")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Publishers").assertDoesNotExist()
            composeRule.onNodeWithContentDescription("Clear search").performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Publishers")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Publishers").assertIsDisplayed()
            composeRule.onNodeWithText("Marvel").assertIsDisplayed()
            composeRule.onNodeWithText("DC").assertIsDisplayed()
            composeRule.onNodeWithText("Batman").assertDoesNotExist()
        }
    }
}