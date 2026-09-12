package com.aschlus.comicreadingcompanion.ui.screen

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListItem
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.ImportReadingListState
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: HomeViewModel

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

        viewModel = HomeViewModel(repository = repository)
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
                    onImportReadingListClick = {},
                    onReadingListClick = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText("Comic Reading Companion").assertIsDisplayed()
        composeRule.onNodeWithText("Browse Comics").assertIsDisplayed()
        composeRule.onNodeWithText("Create Reading List").assertIsDisplayed()
        composeRule.onNodeWithText("Import Reading List").assertIsDisplayed()
        composeRule.onNodeWithText("My Reading Lists").assertIsDisplayed()
        composeRule.onNodeWithText("No reading lists yet").assertIsDisplayed()
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
                    onImportReadingListClick = {},
                    onReadingListClick = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText("Browse Comics").performClick()
        composeRule.runOnIdle { assert(browseClicked) }
    }

    @Test
    fun homeScreen_readingListCardDisplaysProgressAndContinue() {
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
                        title = "Spider-Man Reading Order",
                        description = "A test reading list",
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

            repository.markIssueAsRead(firstIssueId)

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    HomeScreen(
                        viewModel = viewModel,
                        onBrowseClick = {},
                        onCreateReadingListClick = {},
                        onImportReadingListClick = {},
                        onReadingListClick = { _, _ -> }
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Spider-Man Reading Order")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Spider-Man Reading Order").assertIsDisplayed()
            composeRule.onNodeWithText("A test reading list").assertIsDisplayed()
            composeRule.onNodeWithText("1 of 2 read • 50% complete").assertIsDisplayed()
            composeRule.onNodeWithText("Continue: Amazing Spider-Man #2").assertIsDisplayed()
        }
    }

    @Test
    fun homeScreen_readingListCardInvokesCallbackWithContinuePosition() {
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
                        title = "Clickable Reading List",
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

            repository.markIssueAsRead(firstIssueId)

            var clickedReadingListId: Long? = null
            var clickedPosition: Int? = null

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    HomeScreen(
                        viewModel = viewModel,
                        onBrowseClick = {},
                        onCreateReadingListClick = {},
                        onImportReadingListClick = {},
                        onReadingListClick = { id, position ->
                            clickedReadingListId = id
                            clickedPosition = position
                        }
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Clickable Reading List")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Clickable Reading List").performClick()
            composeRule.runOnIdle {
                assertEquals(readingListId, clickedReadingListId)
                assertEquals(2, clickedPosition)
            }
        }
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
                    onImportReadingListClick = {},
                    onReadingListClick = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText("Create Reading List").performClick()
        composeRule.runOnIdle {
            assert(createClicked)
        }
    }

    @Test
    fun homeScreen_importReadingListButtonInvokesCallback() {
        var importClicked = false

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                HomeScreen(
                    viewModel = viewModel,
                    onBrowseClick = {},
                    onCreateReadingListClick = {},
                    onImportReadingListClick = {
                        importClicked = true
                    },
                    onReadingListClick = { _, _ -> }
                )
            }
        }

        composeRule
            .onNodeWithText("Import Reading List")
            .performClick()

        composeRule.runOnIdle {
            assert(importClicked)
        }
    }

    @Test
    fun homeScreen_importingStateDisablesImportButton() {
        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                HomeScreen(
                    viewModel = viewModel,
                    onBrowseClick = {},
                    onCreateReadingListClick = {},
                    onImportReadingListClick = {},
                    importState =
                        ImportReadingListState.Importing,
                    onReadingListClick = { _, _ -> }
                )
            }
        }

        composeRule
            .onNodeWithText(
                "Importing Reading List…"
            )
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun homeScreen_importSuccessDisplaysMessage() {
        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                HomeScreen(
                    viewModel = viewModel,
                    onBrowseClick = {},
                    onCreateReadingListClick = {},
                    onImportReadingListClick = {},
                    importState =
                        ImportReadingListState.Success(
                            title = "Test Import"
                        ),
                    onReadingListClick = { _, _ -> }
                )
            }
        }

        composeRule
            .onNodeWithText(
                "Imported \"Test Import\""
            )
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_importErrorDisplaysMessage() {
        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                HomeScreen(
                    viewModel = viewModel,
                    onBrowseClick = {},
                    onCreateReadingListClick = {},
                    onImportReadingListClick = {},
                    importState =
                        ImportReadingListState.Error(
                            message = "Import failed"
                        ),
                    onReadingListClick = { _, _ -> }
                )
            }
        }

        composeRule
            .onNodeWithText("Import failed")
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_searchFiltersReadingListsAndCanBeCleared() {
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name = "Marvel"
                    )
                )

            comicDao.insertReadingList(
                ReadingList(
                    title =
                        "Ultimate Marvel",
                    description =
                        "Complete Earth-1610 reading order",
                    publisherId =
                        publisherId,
                    universeId = null,
                    createdAt = 1000L,
                    updatedAt = 1000L
                )
            )

            comicDao.insertReadingList(
                ReadingList(
                    title =
                        "Spider-Man Volume 2 Era",
                    description =
                        "Earth-616 Spider-Man chronology",
                    publisherId =
                        publisherId,
                    universeId = null,
                    createdAt = 2000L,
                    updatedAt = 2000L
                )
            )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    HomeScreen(
                        viewModel = viewModel,
                        onBrowseClick = {},
                        onCreateReadingListClick = {},
                        onImportReadingListClick = {},
                        onReadingListClick = { _, _ -> }
                    )
                }
            }

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        "Ultimate Marvel"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty() &&
                        composeRule
                            .onAllNodesWithText(
                                "Spider-Man Volume 2 Era"
                            )
                            .fetchSemanticsNodes()
                            .isNotEmpty()
            }

            composeRule
                .onNodeWithText(
                    "Search reading lists"
                )
                .performTextInput(
                    "ultimate"
                )

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        "Spider-Man Volume 2 Era"
                    )
                    .fetchSemanticsNodes()
                    .isEmpty()
            }

            composeRule
                .onNodeWithText(
                    "Ultimate Marvel"
                )
                .assertIsDisplayed()

            composeRule
                .onNodeWithText(
                    "Spider-Man Volume 2 Era"
                )
                .assertDoesNotExist()

            composeRule
                .onNodeWithContentDescription(
                    "Clear reading list search"
                )
                .assertIsDisplayed()
                .performClick()

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        "Spider-Man Volume 2 Era"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithText(
                    "Ultimate Marvel"
                )
                .assertIsDisplayed()

            composeRule
                .onNodeWithText(
                    "Spider-Man Volume 2 Era"
                )
                .assertIsDisplayed()
        }
    }

    @Test
    fun homeScreen_sortControlReordersFilteredReadingLists() {
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name = "Marvel"
                    )
                )

            comicDao.insertReadingList(
                ReadingList(
                    title =
                        "Gamma Marvel List",
                    description = null,
                    publisherId =
                        publisherId,
                    universeId = null,
                    createdAt = 1000L,
                    updatedAt = 1000L
                )
            )

            comicDao.insertReadingList(
                ReadingList(
                    title =
                        "Alpha Marvel List",
                    description = null,
                    publisherId =
                        publisherId,
                    universeId = null,
                    createdAt = 2000L,
                    updatedAt = 3000L
                )
            )

            comicDao.insertReadingList(
                ReadingList(
                    title =
                        "Beta DC List",
                    description = null,
                    publisherId =
                        publisherId,
                    universeId = null,
                    createdAt = 3000L,
                    updatedAt = 2000L
                )
            )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    HomeScreen(
                        viewModel = viewModel,
                        onBrowseClick = {},
                        onCreateReadingListClick = {},
                        onImportReadingListClick = {},
                        onReadingListClick = { _, _ -> }
                    )
                }
            }

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        "Alpha Marvel List"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithText(
                    "Search reading lists"
                )
                .performTextInput(
                    "marvel"
                )

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        "Beta DC List"
                    )
                    .fetchSemanticsNodes()
                    .isEmpty()
            }

            composeRule
                .onNodeWithText(
                    "Recently updated"
                )
                .performClick()

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        "Title A-Z"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithText(
                    "Title A-Z"
                )
                .performClick()

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                viewModel
                    .visibleReadingLists
                    .value
                    .map {
                        it.title
                    } ==
                        listOf(
                            "Alpha Marvel List",
                            "Gamma Marvel List"
                        )
            }

            assertEquals(
                listOf(
                    "Alpha Marvel List",
                    "Gamma Marvel List"
                ),
                viewModel
                    .visibleReadingLists
                    .value
                    .map {
                        it.title
                    }
            )

            composeRule
                .onNodeWithText(
                    "Title A-Z"
                )
                .performClick()

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        "Title Z-A"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithText(
                    "Title Z-A"
                )
                .performClick()

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                viewModel
                    .visibleReadingLists
                    .value
                    .map {
                        it.title
                    } ==
                        listOf(
                            "Gamma Marvel List",
                            "Alpha Marvel List"
                        )
            }

            assertEquals(
                listOf(
                    "Gamma Marvel List",
                    "Alpha Marvel List"
                ),
                viewModel
                    .visibleReadingLists
                    .value
                    .map {
                        it.title
                    }
            )
        }
    }
}