package com.aschlus.comicreadingcompanion.ui.screen

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
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
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
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
                        onIssueClick = {}
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
            composeRule.onNodeWithText("BROWSE").assertIsDisplayed()
            composeRule.onNodeWithText("Search comics, series, or issues").assertIsDisplayed()
            composeRule.onNodeWithText("PUBLISHERS").assertIsDisplayed()
            composeRule.onNodeWithText("Marvel").assertIsDisplayed()
            composeRule.onNodeWithText("DC").assertIsDisplayed()
            composeRule
                .onNodeWithContentDescription("Marvel publisher artwork").assertIsDisplayed()
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
                        onIssueClick = {}
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
                        onIssueClick = {}
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
            composeRule.onAllNodesWithText("SERIES").assertCountEquals(2)
            composeRule.onNodeWithText("Amazing Spider-Man").assertIsDisplayed()
            composeRule.onAllNodesWithText("ISSUES").assertCountEquals(2)
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
                        onIssueClick = {}
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
                        }
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
                        onIssueClick = {}
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
            composeRule.onNodeWithText("PUBLISHERS").assertDoesNotExist()
            composeRule.onNodeWithContentDescription("Clear search").performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("PUBLISHERS")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("PUBLISHERS").assertIsDisplayed()
            composeRule.onNodeWithText("Marvel").assertIsDisplayed()
            composeRule.onNodeWithText("DC").assertIsDisplayed()
            composeRule.onNodeWithText("Batman").assertDoesNotExist()
        }
    }

    @Test
    fun browseScreen_searchFiltersOnlyAppearWithQuery() {
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name = "Test Publisher"
                    )
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId =
                            publisherId,
                        title =
                            "Test Series",
                        volume = 1,
                        startYear = 2026,
                        endYear = null
                    )
                )

            comicDao.insertIssue(
                Issue(
                    seriesId =
                        seriesId,
                    universeId =
                        null,
                    issueNumber =
                        "1",
                    title =
                        "Test Issue",
                    publicationDate =
                        "2026-01",
                    coverUrl =
                        null,
                    description =
                        null,
                    issueType =
                        IssueType.REGULAR
                )
            )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    BrowseScreen(
                        viewModel =
                            viewModel,
                        onPublisherClick = {},
                        onSeriesClick = {},
                        onIssueClick = {}
                    )
                }
            }

            composeRule
                .onNodeWithText(
                    "ALL"
                )
                .assertDoesNotExist()

            composeRule
                .onNode(
                    hasSetTextAction()
                )
                .performTextInput(
                    "Test"
                )

            composeRule
                .onNodeWithText(
                    "ALL"
                )
                .assertIsDisplayed()

            composeRule
                .onNodeWithContentDescription(
                    "Clear search"
                )
                .performClick()

            composeRule
                .onNodeWithText(
                    "ALL"
                )
                .assertDoesNotExist()

            composeRule
                .onNodeWithText(
                    "PUBLISHERS"
                )
                .assertIsDisplayed()
        }
    }

    @Test
    fun browseScreen_searchFindsIssueByDisplayedLabel() {
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name = "Test Publisher"
                    )
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Slingers",
                        volume = 1,
                        startYear = 1998,
                        endYear = 1999
                    )
                )

            comicDao.insertIssue(
                Issue(
                    seriesId = seriesId,
                    universeId = null,
                    issueNumber = "0",
                    title = "Test Issue",
                    publicationDate = "1998-11",
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
                        onIssueClick = {}
                    )
                }
            }

            composeRule
                .onNode(hasSetTextAction())
                .performTextInput(
                    "Slingers #0"
                )

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        "Test Issue"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithText(
                    "Test Issue"
                )
                .assertIsDisplayed()
        }
    }

    @Test
    fun browseScreen_filterSheetPublisherFilterWorksWithBlankAndTextQueriesAndCanClearAll() {
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

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    BrowseScreen(
                        viewModel = viewModel,
                        onPublisherClick = {},
                        onSeriesClick = {},
                        onIssueClick = {}
                    )
                }
            }

            // Open the filter sheet and verify its three selectors.
            composeRule.onNodeWithText("Filters")
                .performClick()

            composeRule.onNodeWithText("FILTER BROWSE")
                .assertIsDisplayed()

            composeRule.onNodeWithText("PUBLISHER")
                .assertIsDisplayed()

            composeRule.onNodeWithText("UNIVERSE / CONTINUITY")
                .assertIsDisplayed()

            composeRule.onNodeWithText("PUBLICATION YEAR")
                .assertIsDisplayed()

            composeRule.onNodeWithText("All publishers")
                .assertIsDisplayed()

            composeRule.onNodeWithText("All universes")
                .assertIsDisplayed()

            composeRule.onNodeWithText("All years")
                .assertIsDisplayed()

            // Select Marvel while the search query is blank.
            composeRule.onNodeWithText("All publishers")
                .performClick()

            composeRule.onAllNodesWithText("Marvel")
                .onLast()
                .performClick()

            composeRule.onNodeWithText("Clear all filters")
                .performScrollTo()
                .assertIsDisplayed()

            composeRule.onNodeWithText(
                "Show",
                substring = true
            )
                .performScrollTo()
                .assertIsDisplayed()
                .performClick()

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule.onNodeWithText("Amazing Spider-Man")
                .assertIsDisplayed()

            composeRule.onNodeWithText("Batman")
                .assertDoesNotExist()

            composeRule.onNodeWithText("Filters (1)")
                .assertIsDisplayed()

            // The publisher filter must also combine with text search.
            composeRule.onNode(hasSetTextAction())
                .performTextInput("Spider")

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man #1")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule.onNodeWithText("Amazing Spider-Man #1")
                .assertIsDisplayed()

            composeRule.onNodeWithText("Batman")
                .assertDoesNotExist()

            // Clear the search query without losing the publisher filter.
            composeRule.onNodeWithContentDescription("Clear search")
                .performClick()

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule.onNodeWithText("Amazing Spider-Man")
                .assertIsDisplayed()

            composeRule.onNodeWithText("Batman")
                .assertDoesNotExist()

            composeRule.onNodeWithText("Filters (1)")
                .performClick()

            composeRule.onNodeWithText("Clear all filters")
                .performClick()

            composeRule.onNodeWithText("Done")
                .performClick()

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("PUBLISHERS")
                    .fetchSemanticsNodes()
                    .isNotEmpty() &&
                        composeRule.onAllNodesWithText("DC")
                            .fetchSemanticsNodes()
                            .isNotEmpty()
            }

            composeRule.onNodeWithText("PUBLISHERS")
                .assertIsDisplayed()

            composeRule.onNodeWithText("Marvel")
                .assertIsDisplayed()

            composeRule.onNodeWithText("DC")
                .assertIsDisplayed()

            composeRule.onNodeWithText("Filters (1)")
                .assertDoesNotExist()

            composeRule.onNodeWithText("ALL")
                .assertDoesNotExist()
        }
    }

    @Test
    fun browseScreen_universeFilterWorksWithBlankAndTextQueries() =
        runBlocking {
            val marvelPublisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val dcPublisherId =
                comicDao.insertPublisher(
                    Publisher(name = "DC")
                )

            val earth616Id =
                comicDao.insertUniverse(
                    Universe(
                        publisherId = marvelPublisherId,
                        name = "Marvel Universe",
                        designation = "Earth-616",
                        description = null
                    )
                )

            val earth1610Id =
                comicDao.insertUniverse(
                    Universe(
                        publisherId = marvelPublisherId,
                        name = "Ultimate Universe",
                        designation = "Earth-1610",
                        description = null
                    )
                )

            val earth0Id =
                comicDao.insertUniverse(
                    Universe(
                        publisherId = dcPublisherId,
                        name = "DC Universe",
                        designation = "Earth-0",
                        description = null
                    )
                )

            val amazingSpiderManId =
                comicDao.insertSeries(
                    Series(
                        publisherId = marvelPublisherId,
                        title = "Amazing Spider-Man",
                        volume = 1,
                        startYear = 1963,
                        endYear = null
                    )
                )

            val ultimateSpiderManId =
                comicDao.insertSeries(
                    Series(
                        publisherId = marvelPublisherId,
                        title = "Ultimate Spider-Man",
                        volume = 1,
                        startYear = 2000,
                        endYear = null
                    )
                )

            val batmanId =
                comicDao.insertSeries(
                    Series(
                        publisherId = dcPublisherId,
                        title = "Batman",
                        volume = 1,
                        startYear = 1940,
                        endYear = null
                    )
                )

            comicDao.insertIssue(
                Issue(
                    seriesId = amazingSpiderManId,
                    universeId = earth616Id,
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
                    seriesId = ultimateSpiderManId,
                    universeId = earth1610Id,
                    issueNumber = "1",
                    title = "Powerless",
                    publicationDate = "2000-10",
                    coverUrl = null,
                    description = null,
                    issueType = IssueType.REGULAR
                )
            )

            comicDao.insertIssue(
                Issue(
                    seriesId = batmanId,
                    universeId = earth0Id,
                    issueNumber = "1",
                    title = "Batman",
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
                        onIssueClick = {}
                    )
                }
            }

            // Select Earth-616 with a blank search query.
            composeRule.onNodeWithText("Filters")
                .performClick()

            composeRule.onNodeWithText("All universes")
                .performClick()

            composeRule.onNodeWithText(
                "Earth-616 · Marvel Universe · Marvel"
            ).performClick()

            composeRule.onNodeWithText("Clear all filters")
                .performScrollTo()
                .assertIsDisplayed()

            composeRule.onNodeWithText(
                "Show",
                substring = true
            ).performScrollTo()
                .performClick()

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule.onNodeWithText("Amazing Spider-Man")
                .assertIsDisplayed()

            composeRule.onNodeWithText("Ultimate Spider-Man")
                .assertDoesNotExist()

            composeRule.onNodeWithText("Batman")
                .assertDoesNotExist()

            // The selected universe must also constrain text searches.
            composeRule.onNode(hasSetTextAction())
                .performTextInput("Spider")

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man #1")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule.onNodeWithText("Amazing Spider-Man #1")
                .assertIsDisplayed()

            composeRule.onNodeWithText("Ultimate Spider-Man #1")
                .assertDoesNotExist()

            composeRule.onNodeWithText("Filters (1)")
                .assertIsDisplayed()

            // Clearing the query must preserve the universe filter.
            composeRule.onNodeWithContentDescription("Clear search")
                .performClick()

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule.onNodeWithText("Amazing Spider-Man")
                .assertIsDisplayed()

            composeRule.onNodeWithText("Ultimate Spider-Man")
                .assertDoesNotExist()

            composeRule.onNodeWithText("Batman")
                .assertDoesNotExist()
        }

    @Test
    fun browseScreen_publicationYearFilterWorksWithBlankAndTextQueries() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val amazingSpiderManId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Amazing Spider-Man",
                        volume = 1,
                        startYear = 1963,
                        endYear = null
                    )
                )

            val fantasticFourId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Fantastic Four",
                        volume = 1,
                        startYear = 1961,
                        endYear = null
                    )
                )

            comicDao.insertIssue(
                Issue(
                    seriesId = amazingSpiderManId,
                    universeId = null,
                    issueNumber = "1",
                    title = "Spider-Man",
                    publicationDate = "1963-03",
                    coverUrl = null,
                    description = null,
                    issueType = IssueType.REGULAR
                )
            )

            comicDao.insertIssue(
                Issue(
                    seriesId = fantasticFourId,
                    universeId = null,
                    issueNumber = "1",
                    title = "Fantastic Four",
                    publicationDate = "1964-11",
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
                        onIssueClick = {}
                    )
                }
            }

            // Open filters and select 1963.
            composeRule.onNodeWithText("Filters")
                .performClick()

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("All years")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule.onNodeWithText("All years")
                .performClick()

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("1963")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule.onNodeWithText("1963")
                .performClick()

            composeRule.onNodeWithText("Clear all filters")
                .performScrollTo()
                .assertIsDisplayed()

            composeRule.onNodeWithText(
                "Show",
                substring = true
            ).performScrollTo()
                .performClick()

            // The year filter should work with a blank query.
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule.onNodeWithText("Amazing Spider-Man")
                .assertIsDisplayed()

            composeRule.onNodeWithText("Fantastic Four")
                .assertDoesNotExist()

            composeRule.onNodeWithText("Filters (1)")
                .assertIsDisplayed()

            // The selected year should also constrain text searches.
            composeRule.onNode(hasSetTextAction())
                .performTextInput("Spider")

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man #1")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule.onNodeWithText("Amazing Spider-Man #1")
                .assertIsDisplayed()

            composeRule.onNodeWithText("Fantastic Four")
                .assertDoesNotExist()

            // Clearing search must preserve the year filter.
            composeRule.onNodeWithContentDescription("Clear search")
                .performClick()

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule.onNodeWithText("Amazing Spider-Man")
                .assertIsDisplayed()

            composeRule.onNodeWithText("Fantastic Four")
                .assertDoesNotExist()

            composeRule.onNodeWithText("Filters (1)")
                .assertIsDisplayed()

            Unit
        }
}