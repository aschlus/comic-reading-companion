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
                    onCreateReadingListClick = {},
                    onReadingListClick = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText("Comic Reading Companion").assertIsDisplayed()
        composeRule.onNodeWithText("BROWSE COMICS").assertIsDisplayed()
        composeRule.onNodeWithText("CREATE READING LIST").assertIsDisplayed()
    }

    @Test
    fun homeScreen_browseButtonInvokesCallback() {
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
                    onCreateReadingListClick = {},
                    onReadingListClick = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText("BROWSE COMICS").performClick()
        composeRule.runOnIdle { assert(browseClicked) }
    }

    @Test
    fun homeScreen_createReadingListButtonInvokesCallback() {
        var createClicked = false

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                HomeScreen(
                    viewModel = viewModel,
                    onBrowseClick = {},
                    onCreateReadingListClick = {
                        createClicked = true
                    },
                    onReadingListClick = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText("CREATE READING LIST").performClick()
        composeRule.runOnIdle {
            assert(createClicked)
        }
    }

    @Test
    fun homeScreen_displaysRecentlyOpenedReadingListsInRecentOrder() {
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name = "Marvel"
                    )
                )

            val firstListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title =
                            "Older Recent List",
                        description = null,
                        publisherId =
                            publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val secondListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title =
                            "Newest Recent List",
                        description = null,
                        publisherId =
                            publisherId,
                        universeId = null,
                        createdAt = 2000L,
                        updatedAt = 2000L
                    )
                )

            withTimeout(
                5000L.milliseconds
            ) {
                viewModel
                    .readingLists
                    .first {
                        it.size == 2
                    }
            }

            viewModel.recordReadingListOpened(
                firstListId
            )

            viewModel.recordReadingListOpened(
                secondListId
            )

            withTimeout(
                5000L.milliseconds
            ) {
                viewModel
                    .recentlyOpenedReadingLists
                    .first { lists ->
                        lists.size == 2 &&
                                lists[0].id ==
                                secondListId &&
                                lists[1].id ==
                                firstListId
                    }
            }

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    HomeScreen(
                        viewModel = viewModel,
                        onBrowseClick = {},
                        onCreateReadingListClick = {},
                        onReadingListClick = { _, _ -> }
                    )
                }
            }

            composeRule
                .onNodeWithText(
                    "RECENTLY OPENED"
                )
                .assertIsDisplayed()

            composeRule
                .onAllNodesWithText(
                    "Newest Recent List"
                )[0]
                .assertIsDisplayed()

            composeRule
                .onAllNodesWithText(
                    "Older Recent List"
                )[0]
                .assertIsDisplayed()
        }
    }

    @Test
    fun homeScreen_recentlyOpenedInvokesCallbackWithContinuePosition() {
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
                        issueNumber = "1",
                        title = "First Issue",
                        publicationDate = "1999-01",
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
                        title = "Second Issue",
                        publicationDate = "1999-02",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title =
                            "Recent Continue List",
                        description = null,
                        publisherId = publisherId,
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

            viewModel.recordReadingListOpened(
                readingListId
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
                        onCreateReadingListClick = {},
                        onReadingListClick = { id,
                                               position ->

                            clickedReadingListId =
                                id

                            clickedPosition =
                                position
                        }
                    )
                }
            }

            composeRule.waitUntil(
                timeoutMillis = 10000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        "Recent Continue List"
                    )
                    .fetchSemanticsNodes()
                    .size == 2
            }

            // Current Home ordering is:
            //
            // 0 -> Continue Reading card
            // 1 -> Recently Opened card
            //
            // This test specifically verifies the
            // Recently Opened card callback.
            composeRule
                .onAllNodesWithText(
                    "Recent Continue List"
                )[1]
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
                        onCreateReadingListClick = {},
                        onReadingListClick = { _, _ -> }
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
                        onCreateReadingListClick = {},
                        onReadingListClick = {
                                id,
                                position ->

                            clickedReadingListId =
                                id

                            clickedPosition =
                                position
                        }
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

            // First copy is the card in
            // Continue Reading.
            composeRule
                .onAllNodesWithText(
                    "Continue Reading Test"
                )[0]
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
}