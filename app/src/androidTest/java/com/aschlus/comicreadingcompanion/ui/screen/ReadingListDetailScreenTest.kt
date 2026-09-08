package com.aschlus.comicreadingcompanion.ui.screen

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithContentDescription
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
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSection
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import com.aschlus.comicreadingcompanion.ui.viewmodel.ReadingListDetailViewModel
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
class ReadingListDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: ReadingListDetailViewModel

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

        viewModel = ReadingListDetailViewModel(repository = repository)
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
    fun readingListDetailScreen_displaysReadingListAndIssue() {
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
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Spider-Man Volume Two",
                        description = "Complete test reading order",
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
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    ReadingListDetailScreen(
                        readingListId = readingListId,
                        startPosition = -1,
                        viewModel = viewModel,
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man #30")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Spider-Man Volume Two").assertIsDisplayed()
            composeRule.onNodeWithText("Complete test reading order").assertIsDisplayed()
            composeRule.onNodeWithText("0 of 1 read • 0% complete").assertIsDisplayed()
            composeRule.onNodeWithText("Amazing Spider-Man #30").assertIsDisplayed()
            composeRule.onNodeWithText("1 issues").assertIsDisplayed()
            composeRule.onNodeWithText("Jump to first unread").assertIsDisplayed()
        }
    }

    @Test
    fun readingListDetailScreen_backButtonInvokesCallback() {
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Back Button Test List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            var backClicked = false

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    ReadingListDetailScreen(
                        readingListId = readingListId,
                        startPosition = -1,
                        viewModel = viewModel,
                        onIssueClick = {},
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
    fun readingListDetailScreen_issueClickInvokesCallback() {
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
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Issue Navigation Test",
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
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            var clickedIssueId: Long? = null

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    ReadingListDetailScreen(
                        readingListId = readingListId,
                        startPosition = -1,
                        viewModel = viewModel,
                        onIssueClick = { id ->
                            clickedIssueId = id
                        },
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man #30")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Amazing Spider-Man #30").performClick()
            composeRule.runOnIdle {
                assertEquals(issueId, clickedIssueId)
            }
        }
    }

    @Test
    fun readingListDetailScreen_checkboxMarksIssueAsRead() {
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
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Checkbox Test",
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
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    ReadingListDetailScreen(
                        readingListId = readingListId,
                        startPosition = -1,
                        viewModel = viewModel,
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man #30")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNode(isToggleable()).assertIsOff()
            composeRule.onNode(isToggleable()).performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("1 of 1 read • 100% complete")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNode(isToggleable()).assertIsOn()
            composeRule.onNodeWithText("1 of 1 read • 100% complete").assertIsDisplayed()
        }
    }

    @Test
    fun readingListDetailScreen_sectionCollapsesAndExpands() {
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
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Section Test",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val sectionId =
                comicDao.insertReadingListSection(
                    ReadingListSection(
                        readingListId = readingListId,
                        title = "Coming Home Arc",
                        description = "A test section",
                        position = 1
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = sectionId,
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    ReadingListDetailScreen(
                        readingListId = readingListId,
                        startPosition = -1,
                        viewModel = viewModel,
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man #30")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule.onNodeWithText("Coming Home Arc").assertIsDisplayed()
            composeRule.onNodeWithText("Amazing Spider-Man #30").assertIsDisplayed()
            composeRule.onNodeWithText("Coming Home Arc").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Amazing Spider-Man #30").assertDoesNotExist()
            composeRule.onNodeWithText("Coming Home Arc").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Amazing Spider-Man #30").assertIsDisplayed()
        }
    }

    @Test
    fun readingListDetailScreen_searchFiltersIssues() {
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
                        volume = 2,
                        startYear = 1999,
                        endYear = 2003
                    )
                )

            val daredevilSeriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Daredevil",
                        volume = 2,
                        startYear = 1998,
                        endYear = 2009
                    )
                )

            val spiderManIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = spiderManSeriesId,
                        universeId = null,
                        issueNumber = "30",
                        title = "Coming Home",
                        publicationDate = "2001-06",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val daredevilIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = daredevilSeriesId,
                        universeId = null,
                        issueNumber = "16",
                        title = "Underboss",
                        publicationDate = "2001-05",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Search Test",
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
                    issueId = spiderManIssueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = null,
                    issueId = daredevilIssueId,
                    position = 2,
                    required = true,
                    notes = null
                )
            )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    ReadingListDetailScreen(
                        readingListId = readingListId,
                        startPosition = -1,
                        viewModel = viewModel,
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man #30")
                    .fetchSemanticsNodes()
                    .isNotEmpty() &&
                        composeRule.onAllNodesWithText("Daredevil #16")
                            .fetchSemanticsNodes()
                            .isNotEmpty()
            }
            composeRule.onNode(hasSetTextAction()).performTextInput("Daredevil")
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("1 of 2 issues")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Daredevil #16").assertIsDisplayed()
            composeRule.onNodeWithText("Amazing Spider-Man #30").assertDoesNotExist()
            composeRule.onNodeWithText("1 of 2 issues").assertIsDisplayed()
        }
    }

    @Test
    fun readingListDetailScreen_readFilterShowsOnlyReadIssues() {
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
                        title = "Status Filter Test",
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

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    ReadingListDetailScreen(
                        readingListId = readingListId,
                        startPosition = -1,
                        viewModel = viewModel,
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man #1")
                    .fetchSemanticsNodes()
                    .isNotEmpty() &&
                        composeRule.onAllNodesWithText("Amazing Spider-Man #2")
                            .fetchSemanticsNodes()
                            .isNotEmpty()
            }
            composeRule.onNodeWithText("Filters").performClick()
            composeRule.onNodeWithText("Filter issues").assertIsDisplayed()
            composeRule.onNodeWithText("Read").performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Show 1 issues")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Show 1 issues").performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Filters (1)")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Amazing Spider-Man #1").assertIsDisplayed()
            composeRule.onNodeWithText("Amazing Spider-Man #2").assertDoesNotExist()
            composeRule.onNodeWithText("1 of 2 issues").assertIsDisplayed()
            composeRule.onNodeWithText("Filters (1)").assertIsDisplayed()
        }
    }

    @Test
    fun readingListDetailScreen_clearAllFiltersRestoresIssues() {
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
                        issueNumber = "10",
                        title = "Required Issue",
                        publicationDate = "1999-10",
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
                        issueNumber = "11",
                        title = "Optional Issue",
                        publicationDate = "1999-11",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Clear Filters Test",
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
                    required = false,
                    notes = null
                )
            )

            repository.markIssueAsRead(firstIssueId)

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    ReadingListDetailScreen(
                        readingListId = readingListId,
                        startPosition = -1,
                        viewModel = viewModel,
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man #10")
                    .fetchSemanticsNodes()
                    .isNotEmpty() &&
                        composeRule.onAllNodesWithText("Amazing Spider-Man #11")
                            .fetchSemanticsNodes()
                            .isNotEmpty()
            }
            composeRule.onNodeWithText("Filters").performClick()
            composeRule.onNodeWithText("Required").performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Show 1 issues")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Show 1 issues").performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Filters (1)")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Amazing Spider-Man #10").assertIsDisplayed()
            composeRule.onNodeWithText("Amazing Spider-Man #11").assertDoesNotExist()
            composeRule.onNodeWithText("Filters (1)").performClick()
            composeRule.onNodeWithText("Clear all filters").performClick()
            composeRule.onNodeWithText("Done").performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("2 issues")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Amazing Spider-Man #10").assertIsDisplayed()
            composeRule.onNodeWithText("Amazing Spider-Man #11").assertIsDisplayed()
            composeRule.onNodeWithText("Filters").assertIsDisplayed()
            composeRule.onNodeWithText("2 issues").assertIsDisplayed()
        }
    }

    @Test
    fun readingListDetailScreen_markAllAsReadUpdatesProgress() {
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
                        title = "Mark All Test",
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

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    ReadingListDetailScreen(
                        readingListId = readingListId,
                        startPosition = -1,
                        viewModel = viewModel,
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("0 of 2 read • 0% complete")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithContentDescription("Reading list options").performClick()
            composeRule.onNodeWithText("Mark all as read").performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("2 of 2 read • 100% complete")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("2 of 2 read • 100% complete").assertIsDisplayed()
            composeRule.onNodeWithText("Jump to first unread").assertDoesNotExist()
        }
    }

    @Test
    fun readingListDetailScreen_resetProgressClearsReadingProgress() {
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
                        title = "Reset Progress Test",
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
            repository.markIssueAsRead(secondIssueId)

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    ReadingListDetailScreen(
                        readingListId = readingListId,
                        startPosition = -1,
                        viewModel = viewModel,
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("2 of 2 read • 100% complete")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithContentDescription("Reading list options").performClick()
            composeRule.onNodeWithText("Reset reading progress").performClick()
            composeRule.onNodeWithText("Reset reading progress?").assertIsDisplayed()
            composeRule.onNodeWithText("Reset").performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("0 of 2 read • 0% complete")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("0 of 2 read • 0% complete").assertIsDisplayed()
            composeRule.onNodeWithText("Jump to first unread").assertIsDisplayed()
        }
    }

    @Test
    fun readingListDetailScreen_markAsReadingUpdatesIssue() {
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
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Mark Reading Test",
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
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    ReadingListDetailScreen(
                        readingListId = readingListId,
                        startPosition = -1,
                        viewModel = viewModel,
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Amazing Spider-Man #30")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithContentDescription("More options").performClick()
            composeRule.onNodeWithText("Mark as reading").performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Currently reading")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Currently reading").assertIsDisplayed()
            composeRule.onNodeWithText("0 of 1 read • 0% complete").assertIsDisplayed()
            composeRule.onNode(isToggleable()).assertIsOff()
        }
    }

    @Test
    fun readingListDetailScreen_markAllBeforeAsReadUpdatesEarlierIssues() {
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
                        title = "Mark Before Test",
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

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    ReadingListDetailScreen(
                        readingListId = readingListId,
                        startPosition = -1,
                        viewModel = viewModel,
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithContentDescription("More options")
                    .fetchSemanticsNodes()
                    .size == 2
            }
            composeRule.onAllNodesWithContentDescription("More options")[1].performClick()
            composeRule.onNodeWithText("Mark all before as read").performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("1 of 2 read • 50% complete")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onAllNodes(isToggleable())[0].assertIsOn()
            composeRule.onAllNodes(isToggleable())[1].assertIsOff()
            composeRule.onNodeWithText("1 of 2 read • 50% complete").assertIsDisplayed()
        }
    }

    @Test
    fun readingListDetailScreen_optionalFilterShowsOnlyOptionalIssues() {
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

            val requiredIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "30",
                        title = "Required Story",
                        publicationDate = "2001-06",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val optionalIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "31",
                        title = "Optional Story",
                        publicationDate = "2001-07",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Spider-Man Reading Order",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1L,
                        updatedAt = 1L
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = null,
                    issueId = requiredIssueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = null,
                    issueId = optionalIssueId,
                    position = 2,
                    required = false,
                    notes = null
                )
            )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    ReadingListDetailScreen(
                        readingListId = readingListId,
                        startPosition = -1,
                        viewModel = viewModel,
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Optional Story")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Filters").performClick()
            composeRule.onNodeWithText("Optional").performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Optional Story")
                    .fetchSemanticsNodes()
                    .isNotEmpty() &&
                        composeRule.onAllNodesWithText("Required Story")
                            .fetchSemanticsNodes()
                            .isEmpty()
            }
            composeRule.onNodeWithText("Filters (1)").assertIsDisplayed()
            composeRule.onNodeWithText("1 of 2 issues").assertIsDisplayed()
        }
    }

    @Test
    fun readingListDetailScreen_seriesFilterShowsOnlySelectedSeries() {
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
                        volume = 2,
                        startYear = 1999,
                        endYear = 2003
                    )
                )

            val daredevilSeriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Daredevil",
                        volume = 2,
                        startYear = 1998,
                        endYear = 2009
                    )
                )

            val spiderManIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = spiderManSeriesId,
                        universeId = null,
                        issueNumber = "30",
                        title = "Spider-Man Story",
                        publicationDate = "2001-06",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val daredevilIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = daredevilSeriesId,
                        universeId = null,
                        issueNumber = "16",
                        title = "Daredevil Story",
                        publicationDate = "2001-05",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Marvel Reading Order",
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
                    issueId = spiderManIssueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = null,
                    issueId = daredevilIssueId,
                    position = 2,
                    required = true,
                    notes = null
                )
            )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    ReadingListDetailScreen(
                        readingListId = readingListId,
                        startPosition = -1,
                        viewModel = viewModel,
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Spider-Man Story")
                    .fetchSemanticsNodes()
                    .isNotEmpty() &&
                        composeRule.onAllNodesWithText("Daredevil Story")
                            .fetchSemanticsNodes()
                            .isNotEmpty()
            }
            composeRule.onNodeWithText("Filters").performClick()
            composeRule.onNodeWithText("All series").performClick()
            composeRule.onNodeWithText("Amazing Spider-Man (Vol. 2)").performClick()
            composeRule.onNodeWithText("Show 1 issues").performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Spider-Man Story")
                    .fetchSemanticsNodes()
                    .isNotEmpty() &&
                        composeRule.onAllNodesWithText("Daredevil Story")
                            .fetchSemanticsNodes()
                            .isEmpty()
            }
            composeRule.onNodeWithText("Filters (1)").assertIsDisplayed()
            composeRule.onNodeWithText("1 of 2 issues").assertIsDisplayed()
        }
    }

    @Test
    fun readingListDetailScreen_noSearchMatchesDisplaysEmptyMessage() {
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
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Spider-Man Reading Order",
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
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    ReadingListDetailScreen(
                        readingListId = readingListId,
                        startPosition = -1,
                        viewModel = viewModel,
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Coming Home")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNode(hasSetTextAction()).performTextInput("Venom")
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule
                    .onAllNodesWithText("No issues match \"Venom\" with the current filters.")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("No issues match \"Venom\" with the current filters.")
                .assertIsDisplayed()
            composeRule.onNodeWithText("0 of 1 issues").assertIsDisplayed()
            composeRule.onNodeWithText("Coming Home").assertDoesNotExist()
        }
    }

    @Test
    fun readingListDetailScreen_resetProgressCancelKeepsProgress() {
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
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Spider-Man Reading Order",
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
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            repository.markIssueAsRead(issueId)

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    ReadingListDetailScreen(
                        readingListId = readingListId,
                        startPosition = -1,
                        viewModel = viewModel,
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("1 of 1 read • 100% complete")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithContentDescription("Reading list options").performClick()
            composeRule.onNodeWithText("Reset reading progress").performClick()
            composeRule.onNodeWithText("Reset reading progress?").assertIsDisplayed()
            composeRule.onNodeWithText("Cancel").performClick()
            composeRule.onNodeWithText("Reset reading progress?").assertDoesNotExist()
            composeRule.onNodeWithText("1 of 1 read • 100% complete").assertIsDisplayed()
        }
    }

    @Test
    fun readingListDetailScreen_startPositionExpandsTargetSection() {
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
                        issueNumber = "30",
                        title = "First Story",
                        publicationDate = "2001-06",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val targetIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "31",
                        title = "Target Story",
                        publicationDate = "2001-07",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Spider-Man Reading Order",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val sectionId =
                comicDao.insertReadingListSection(
                    ReadingListSection(
                        readingListId = readingListId,
                        title = "Main Arc",
                        description = null,
                        position = 1
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = sectionId,
                    issueId = firstIssueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = sectionId,
                    issueId = targetIssueId,
                    position = 2,
                    required = true,
                    notes = null
                )
            )

            val startPosition = mutableStateOf(-1)

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    ReadingListDetailScreen(
                        readingListId = readingListId,
                        startPosition = startPosition.value,
                        viewModel = viewModel,
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Main Arc")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Main Arc").performClick()
            composeRule.onNodeWithContentDescription("Expand section").assertIsDisplayed()
            composeRule.runOnIdle { startPosition.value = 2 }
            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Target Story")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Target Story").assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Collapse section").assertIsDisplayed()
        }
    }

    @Test
    fun readingListDetailScreen_jumpToFirstUnreadKeepsTargetVisible() {
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
                        issueNumber = "30",
                        title = "First Story",
                        publicationDate = "2001-06",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val targetIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "31",
                        title = "Target Story",
                        publicationDate = "2001-07",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Spider-Man Reading Order",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val sectionId =
                comicDao.insertReadingListSection(
                    ReadingListSection(
                        readingListId = readingListId,
                        title = "Main Arc",
                        description = null,
                        position = 1
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = sectionId,
                    issueId = firstIssueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = sectionId,
                    issueId = targetIssueId,
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
                    ReadingListDetailScreen(
                        readingListId = readingListId,
                        startPosition = -1,
                        viewModel = viewModel,
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule.onAllNodesWithText("Main Arc")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("Main Arc").performClick()
            composeRule.onNodeWithContentDescription("Expand section").assertIsDisplayed()
            composeRule.onNodeWithText("Jump to first unread").assertIsDisplayed().performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("Target Story")
                .fetchSemanticsNodes()
                .isNotEmpty()
            }
            composeRule.onNodeWithText("Target Story").assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Collapse section").assertIsDisplayed()
        }
    }
}