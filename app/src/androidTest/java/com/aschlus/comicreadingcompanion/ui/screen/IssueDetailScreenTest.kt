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
import com.aschlus.comicreadingcompanion.ui.viewmodel.AddIssueToReadingListViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.IssueDetailViewModel
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
class IssueDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

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
        runBlocking {
            viewModel
                .viewModelScope
                .coroutineContext[Job]
                ?.cancelAndJoin()
        }
        database.close()
    }

    @Test
    fun issueDetailScreen_displaysIssueMetadata() {
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
                    IssueDetailScreen(
                        issueId = issueId,
                        viewModel = viewModel,
                        onSeriesClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man #30")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Amazing Spider-Man #30").assertIsDisplayed()
            composeRule.onNodeWithText("Amazing Spider-Man").assertIsDisplayed()
            composeRule.onNodeWithText("#30").assertIsDisplayed()
            composeRule.onNodeWithText("Coming Home").assertIsDisplayed()
            composeRule.onNodeWithText("Jun 2001 • Regular").assertIsDisplayed()
            composeRule.onNodeWithText("Marvel").assertIsDisplayed()
            composeRule.onNodeWithText("Description").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithText("Spider-Man faces a dangerous new enemy.").performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun issueDetailScreen_backButtonInvokesCallback() {
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

            var backClicked = false

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    IssueDetailScreen(
                        issueId = issueId,
                        viewModel = viewModel,
                        onSeriesClick = {},
                        onBackClick = {
                            backClicked = true
                        }
                    )
                }
            }
            composeRule.onNodeWithContentDescription("Back").performClick()
            composeRule.runOnIdle {
                assertEquals(true, backClicked)
            }
        }
    }

    @Test
    fun issueDetailScreen_seriesClickInvokesCallback() {
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

            var clickedSeriesId: Long? = null

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    IssueDetailScreen(
                        issueId = issueId,
                        viewModel = viewModel,
                        onSeriesClick = { id ->
                            clickedSeriesId = id
                        },
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Amazing Spider-Man").performScrollTo().performClick()
            composeRule.runOnIdle {
                assertEquals(seriesId, clickedSeriesId)
            }
        }
    }

    @Test
    fun issueDetailScreen_markAsReadingUpdatesStatus() {
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
                    IssueDetailScreen(
                        issueId = issueId,
                        viewModel = viewModel,
                        onSeriesClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man #30")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Reading").performScrollTo().performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Currently reading")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Currently reading").performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun issueDetailScreen_markAsUnreadClearsStatus() {
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
                    IssueDetailScreen(
                        issueId = issueId,
                        viewModel = viewModel,
                        onSeriesClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man #30")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Read").performScrollTo().performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Read")
                    .fetchSemanticsNodes()
                    .size >= 2
            }
            composeRule.onAllNodesWithText("Read")[0].performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun issueDetailScreen_markAsReadUpdatesStatus() {
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

            repository.markIssueAsRead(issueId)

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    IssueDetailScreen(
                        issueId = issueId,
                        viewModel = viewModel,
                        onSeriesClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Read")
                    .fetchSemanticsNodes()
                    .size >= 2
            }
            composeRule.onNodeWithText("Unread").performScrollTo().performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Unread")
                    .fetchSemanticsNodes()
                    .size >= 2 &&
                        composeRule.onAllNodesWithText("Read")
                            .fetchSemanticsNodes()
                            .size == 1
            }
            composeRule.onAllNodesWithText("Unread")[0].performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun issueDetailScreen_addToReadingListButtonOpensSheet() {
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
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            repository.createUserReadingList(
                title = "My Spider-Man List",
                description = null,
                publisherId = publisherId,
                universeId = null
            )

            val addViewModel =
            AddIssueToReadingListViewModel(
                issueId = issueId,
                repository = repository
            )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    IssueDetailScreen(
                        issueId = issueId,
                        viewModel = viewModel,
                        addToReadingListViewModel = addViewModel,
                        onSeriesClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Add to Reading List")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Add to Reading List").performScrollTo().performClick()
            composeRule.onNodeWithText("My Spider-Man List").assertIsDisplayed()
            addViewModel
                .viewModelScope
                .coroutineContext[Job]
                ?.cancelAndJoin()
        }
    }

    @Test
    fun issueDetailScreen_addsIssueToSelectedReadingList() {
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
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                repository.createUserReadingList(
                    title = "My List",
                    description = null,
                    publisherId = publisherId,
                    universeId = null
                )

            val addViewModel =
                AddIssueToReadingListViewModel(
                    issueId = issueId,
                    repository = repository
                )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    IssueDetailScreen(
                        issueId = issueId,
                        viewModel = viewModel,
                        addToReadingListViewModel = addViewModel,
                        onSeriesClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.onNodeWithText("Add to Reading List").performScrollTo().performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("My List")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("My List").performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                runBlocking {
                    comicDao.getItemsForReadingList(readingListId).size == 1
                }
            }
            val items = comicDao.getItemsForReadingList(readingListId)
            assertEquals(1, items.size)
            assertEquals(issueId, items.first().issueId)
            addViewModel
                .viewModelScope
                .coroutineContext[Job]
                ?.cancelAndJoin()
        }
    }
}