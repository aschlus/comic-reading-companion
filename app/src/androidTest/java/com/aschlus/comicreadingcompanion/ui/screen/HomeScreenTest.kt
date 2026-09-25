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
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListItem
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.preferences.HomeUiPreferences
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.test.onNodeWithContentDescription
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingProgress
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: HomeViewModel
    private lateinit var homeUiPreferences: HomeUiPreferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        homeUiPreferences = HomeUiPreferences(context)

        runBlocking {
            homeUiPreferences.clearRecentlyOpenedReadingLists()
        }

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

        viewModel = HomeViewModel(
            repository = repository,
            homeUiPreferences = homeUiPreferences
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
    fun homeScreen_emptyStateDisplaysCoreContent() {
        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                HomeScreen(
                    viewModel = viewModel,
                    onBrowseClick = {},
                    onLibraryClick = {},
                    onCreateReadingListClick = {},
                    onReadingListClick = { _, _ -> },
                    onIssueClick = {},
                    onReadingHistoryClick = {}
                )
            }
        }

        composeRule
            .onNodeWithText("HOME")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "CONTINUE READING"
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "Ready for your next story? " +
                        "Start a reading list to " +
                        "continue it here."
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "QUICK ACCESS"
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "RECENTLY READ"
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "No recent reads yet. " +
                        "Finish an issue and " +
                        "we’ll keep track of it here."
            )
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_discoverComicsInvokesBrowseCallback() {
        var browseClicked = false

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                HomeScreen(
                    viewModel = viewModel,
                    onBrowseClick = {
                        browseClicked = true
                    },
                    onLibraryClick = {},
                    onCreateReadingListClick = {},
                    onReadingListClick = { _, _ -> },
                    onIssueClick = {},
                    onReadingHistoryClick = {}
                )
            }
        }

        composeRule
            .onNodeWithText(
                "DISCOVER\nCOMICS"
            )
            .performClick()

        composeRule.runOnIdle {
            assert(browseClicked)
        }
    }

    @Test
    fun homeScreen_newListInvokesCreateCallback() {
        var createClicked = false

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                HomeScreen(
                    viewModel = viewModel,
                    onBrowseClick = {},
                    onLibraryClick = {},
                    onCreateReadingListClick = {
                        createClicked = true
                    },
                    onReadingListClick = { _, _ -> },
                    onIssueClick = {},
                    onReadingHistoryClick = {}
                )
            }
        }

        composeRule
            .onNodeWithText(
                "NEW\nLIST"
            )
            .performClick()

        composeRule.runOnIdle {
            assert(createClicked)
        }
    }

    @Test
    fun homeScreen_continueReadingDisplaysOnlyInProgressLists() {
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name = "Marvel"
                    )
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Test Series",
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
                        title = "First",
                        publicationDate = null,
                        coverUrl = null,
                        description = null,
                        issueType =
                            IssueType.REGULAR
                    )
                )

            val secondIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "2",
                        title = "Second",
                        publicationDate = null,
                        coverUrl = null,
                        description = null,
                        issueType =
                            IssueType.REGULAR
                    )
                )

            val untouchedIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "3",
                        title = "Third",
                        publicationDate = null,
                        coverUrl = null,
                        description = null,
                        issueType =
                            IssueType.REGULAR
                    )
                )

            val inProgressListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title =
                            "In Progress List",
                        description = null,
                        publisherId =
                            publisherId,
                        universeId = null,
                        createdAt = 2000L,
                        updatedAt = 2000L
                    )
                )

            val untouchedListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title =
                            "Untouched List",
                        description = null,
                        publisherId =
                            publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId =
                        inProgressListId,
                    sectionId = null,
                    issueId = firstIssueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId =
                        inProgressListId,
                    sectionId = null,
                    issueId = secondIssueId,
                    position = 2,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId =
                        untouchedListId,
                    sectionId = null,
                    issueId = untouchedIssueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            repository.markIssueAsRead(
                firstIssueId
            )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    HomeScreen(
                        viewModel = viewModel,
                        onBrowseClick = {},
                        onLibraryClick = {},
                        onCreateReadingListClick = {},
                        onReadingListClick = { _, _ -> },
                        onIssueClick = {},
                        onReadingHistoryClick = {}
                    )
                }
            }

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        "CONTINUE READING"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithText(
                    "CONTINUE READING"
                )
                .assertIsDisplayed()

            // In-progress lists belong on Home.
            assertEquals(
                1,
                composeRule
                    .onAllNodesWithText(
                        "In Progress List"
                    )
                    .fetchSemanticsNodes()
                    .size
            )

// Untouched lists now belong only in Library,
// so they should not appear on Home.
            assertEquals(
                0,
                composeRule
                    .onAllNodesWithText(
                        "Untouched List"
                    )
                    .fetchSemanticsNodes()
                    .size
            )

            composeRule
                .onNodeWithText(
                    "Next up: Test Series #2"
                )
                .assertIsDisplayed()
        }
    }

    @Test
    fun homeScreen_continueReadingInvokesCallbackWithContinuePosition() {
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name = "Marvel"
                    )
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Test Series",
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
                        title = "First",
                        publicationDate = null,
                        coverUrl = null,
                        description = null,
                        issueType =
                            IssueType.REGULAR
                    )
                )

            val secondIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "2",
                        title = "Second",
                        publicationDate = null,
                        coverUrl = null,
                        description = null,
                        issueType =
                            IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title =
                            "Continue Reading Test",
                        description = null,
                        publisherId =
                            publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId =
                        readingListId,
                    sectionId = null,
                    issueId = firstIssueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId =
                        readingListId,
                    sectionId = null,
                    issueId = secondIssueId,
                    position = 2,
                    required = true,
                    notes = null
                )
            )

            repository.markIssueAsRead(
                firstIssueId
            )

            var clickedReadingListId:
                    Long? = null

            var clickedPosition:
                    Int? = null

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    HomeScreen(
                        viewModel = viewModel,
                        onBrowseClick = {},
                        onLibraryClick = {},
                        onCreateReadingListClick = {},
                        onReadingListClick = {
                                id,
                                position ->

                            clickedReadingListId =
                                id

                            clickedPosition =
                                position
                        },
                        onIssueClick = {},
                        onReadingHistoryClick = {}
                    )
                }
            }

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        "Next up: Test Series #2"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithText(
                    "Continue Reading Test"
                )
                .performClick()

            composeRule.runOnIdle {
                assertEquals(
                    readingListId,
                    clickedReadingListId
                )

                assertEquals(
                    2,
                    clickedPosition
                )
            }
        }
    }

    @Test
    fun homeScreen_continueReadingSeeAllInvokesLibraryCallback() {
        var libraryClicked = false

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                HomeScreen(
                    viewModel = viewModel,
                    onBrowseClick = {},
                    onLibraryClick = {
                        libraryClicked = true
                    },
                    onCreateReadingListClick = {},
                    onReadingListClick = { _, _ -> },
                    onIssueClick = {},
                    onReadingHistoryClick = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(
                "CONTINUE READING see all"
            )
            .performClick()

        composeRule.runOnIdle {
            assert(libraryClicked)
        }
    }

    @Test
    fun homeScreen_recentlyReadDisplaysNewestFirst() {
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
                        title = "Recent Test Series",
                        volume = 1,
                        startYear = 2026,
                        endYear = null
                    )
                )

            val oldestIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Oldest",
                        publicationDate = null,
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val middleIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "2",
                        title = "Middle",
                        publicationDate = null,
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val newestIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "3",
                        title = "Newest",
                        publicationDate = null,
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = oldestIssueId,
                    status = ReadingStatus.READ,
                    startedAt = 1000L,
                    completedAt = 1000L,
                    notes = null
                )
            )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = middleIssueId,
                    status = ReadingStatus.READ,
                    startedAt = 2000L,
                    completedAt = 2000L,
                    notes = null
                )
            )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = newestIssueId,
                    status = ReadingStatus.READ,
                    startedAt = 3000L,
                    completedAt = 3000L,
                    notes = null
                )
            )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    HomeScreen(
                        viewModel = viewModel,
                        onBrowseClick = {},
                        onLibraryClick = {},
                        onCreateReadingListClick = {},
                        onReadingListClick = { _, _ -> },
                        onIssueClick = {},
                        onReadingHistoryClick = {}
                    )
                }
            }

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        "Issue #3"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            val newestBounds =
                composeRule
                    .onNodeWithText(
                        "Issue #3"
                    )
                    .fetchSemanticsNode()
                    .boundsInRoot

            val middleBounds =
                composeRule
                    .onNodeWithText(
                        "Issue #2"
                    )
                    .fetchSemanticsNode()
                    .boundsInRoot

            val oldestBounds =
                composeRule
                    .onNodeWithText(
                        "Issue #1"
                    )
                    .fetchSemanticsNode()
                    .boundsInRoot

            assertTrue(
                newestBounds.left <
                        middleBounds.left
            )

            assertTrue(
                middleBounds.left <
                        oldestBounds.left
            )
        }
    }

    @Test
    fun homeScreen_recentlyReadDisplaysAtMostThreeIssues() {
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
                        title = "Limit Test Series",
                        volume = 1,
                        startYear = 2026,
                        endYear = null
                    )
                )

            repeat(4) { index ->
                val issueNumber =
                    index + 1

                val issueId =
                    comicDao.insertIssue(
                        Issue(
                            seriesId = seriesId,
                            universeId = null,
                            issueNumber =
                                issueNumber.toString(),
                            title =
                                "Issue $issueNumber",
                            publicationDate = null,
                            coverUrl = null,
                            description = null,
                            issueType =
                                IssueType.REGULAR
                        )
                    )

                comicDao.insertReadingProgress(
                    ReadingProgress(
                        issueId = issueId,
                        status =
                            ReadingStatus.READ,
                        startedAt =
                            issueNumber * 1000L,
                        completedAt =
                            issueNumber * 1000L,
                        notes = null
                    )
                )
            }

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    HomeScreen(
                        viewModel = viewModel,
                        onBrowseClick = {},
                        onLibraryClick = {},
                        onCreateReadingListClick = {},
                        onReadingListClick = { _, _ -> },
                        onIssueClick = {},
                        onReadingHistoryClick = {}
                    )
                }
            }

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        "Issue #4"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithText(
                    "Issue #4"
                )
                .assertIsDisplayed()

            composeRule
                .onNodeWithText(
                    "Issue #3"
                )
                .assertIsDisplayed()

            composeRule
                .onNodeWithText(
                    "Issue #2"
                )
                .assertIsDisplayed()

            assertFalse(
                composeRule
                    .onAllNodesWithText(
                        "Issue #1"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            )
        }
    }

    @Test
    fun homeScreen_recentlyReadInvokesIssueCallback() {
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
                        title = "Clickable Series",
                        volume = 1,
                        startYear = 2026,
                        endYear = null
                    )
                )

            val issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "7",
                        title = "Clickable Issue",
                        publicationDate = null,
                        coverUrl = null,
                        description = null,
                        issueType =
                            IssueType.REGULAR
                    )
                )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = issueId,
                    status = ReadingStatus.READ,
                    startedAt = 1000L,
                    completedAt = 2000L,
                    notes = null
                )
            )

            var clickedIssueId:
                    Long? = null

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    HomeScreen(
                        viewModel = viewModel,
                        onBrowseClick = {},
                        onLibraryClick = {},
                        onCreateReadingListClick = {},
                        onReadingListClick = { _, _ -> },
                        onIssueClick = {
                            clickedIssueId = it
                        },
                        onReadingHistoryClick = {}
                    )
                }
            }

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        "Issue #7"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithText(
                    "Issue #7"
                )
                .performClick()

            composeRule.runOnIdle {
                assertEquals(
                    issueId,
                    clickedIssueId
                )
            }
        }
    }

    @Test
    fun homeScreen_readingHistoryTileInvokesCallback() {
        var historyClicked = false

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                HomeScreen(
                    viewModel = viewModel,
                    onBrowseClick = {},
                    onLibraryClick = {},
                    onCreateReadingListClick = {},
                    onReadingListClick = {
                            _, _ ->
                    },
                    onIssueClick = {},
                    onReadingHistoryClick = {
                        historyClicked = true
                    }
                )
            }
        }

        composeRule
            .onNodeWithText(
                "READING\nHISTORY"
            )
            .performClick()

        composeRule.runOnIdle {
            assertTrue(historyClicked)
        }
    }

    @Test
    fun homeScreen_recentlyReadSeeAllInvokesHistoryCallback() {
        var historyClicked = false

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                HomeScreen(
                    viewModel = viewModel,
                    onBrowseClick = {},
                    onLibraryClick = {},
                    onCreateReadingListClick = {},
                    onReadingListClick = {
                            _, _ ->
                    },
                    onIssueClick = {},
                    onReadingHistoryClick = {
                        historyClicked = true
                    }
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(
                "RECENTLY READ see all"
            )
            .performClick()

        composeRule.runOnIdle {
            assertTrue(historyClicked)
        }
    }

    @Test
    fun homeScreen_continueReadingIncludesActivelyReadingIssueWithNoCompletedIssues() {
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
                        title = "Active Series",
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
                        title = "First",
                        publicationDate = null,
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
                        issueNumber = "2",
                        title = "Second",
                        publicationDate = null,
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Active Reading List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = null,
                    issueId = firstIssueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = null,
                    issueId = secondIssueId,
                    position = 2,
                    required = true,
                    notes = null
                )
            )

            repository.markIssueAsReading(
                secondIssueId
            )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    HomeScreen(
                        viewModel = viewModel,
                        onBrowseClick = {},
                        onLibraryClick = {},
                        onCreateReadingListClick = {},
                        onReadingListClick = { _, _ -> },
                        onIssueClick = {},
                        onReadingHistoryClick = {}
                    )
                }
            }

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        "Active Reading List"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithText(
                    "Active Reading List"
                )
                .assertIsDisplayed()

            composeRule
                .onNodeWithText(
                    "Next up: Active Series #2"
                )
                .assertIsDisplayed()
        }
    }
}