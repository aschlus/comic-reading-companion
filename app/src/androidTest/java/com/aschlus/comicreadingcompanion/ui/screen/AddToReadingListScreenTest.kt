package com.aschlus.comicreadingcompanion.ui.screen

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import com.aschlus.comicreadingcompanion.ui.viewmodel.CreateReadingListViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.PendingReadingListIssue
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText

@RunWith(AndroidJUnit4::class)
class AddToReadingListScreenTest {

    @get:Rule
    val composeRule =
        createComposeRule()

    private lateinit var database:
            ComicDatabase

    private lateinit var comicDao:
            ComicDao

    private lateinit var repository:
            ComicRepository

    private lateinit var viewModel:
            CreateReadingListViewModel

    @Before
    fun setUp() {
        val context =
            ApplicationProvider
                .getApplicationContext<Context>()

        database =
            Room.inMemoryDatabaseBuilder(
                context,
                ComicDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()

        comicDao =
            database.comicDao()

        repository =
            ComicRepository(
                comicDao = comicDao,
                database = database
            )

        viewModel =
            CreateReadingListViewModel(
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
    fun addToReadingListScreen_filtersBetweenSeriesAndIssues() =
        runBlocking {
            insertSearchFixture()

            viewModel.beginAddToListSession()

            setScreenContent()

            composeRule
                .onNodeWithText(
                    "Search issues or series"
                )
                .performTextInput(
                    "Test"
                )

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                viewModel
                    .addToListSeriesResults
                    .value
                    .isNotEmpty() &&
                        viewModel
                            .addToListIssueResults
                            .value
                            .isNotEmpty()
            }

            composeRule
                .onNode(
                    hasText("SERIES") and
                            hasClickAction()
                )
                .assertIsDisplayed()

            composeRule
                .onNode(
                    hasText("ISSUES") and
                            hasClickAction()
                )
                .assertIsDisplayed()

            composeRule
                .onNodeWithText(
                    "Test Series"
                )
                .assertIsDisplayed()

            composeRule
                .onNodeWithText(
                    "Test Series #1"
                )
                .assertIsDisplayed()

            composeRule
                .onNode(
                    hasText("ISSUES") and
                            hasClickAction()
                )
                .performClick()

            composeRule
                .onNodeWithText(
                    "Test Series"
                )
                .assertDoesNotExist()

            composeRule
                .onNodeWithText(
                    "Test Series #1"
                )
                .assertIsDisplayed()

            composeRule
                .onNode(
                    hasText("SERIES") and
                            hasClickAction()
                )
                .performClick()

            composeRule
                .onNodeWithText(
                    "Test Series"
                )
                .assertIsDisplayed()

            composeRule
                .onNodeWithText(
                    "Test Series #1"
                )
                .assertDoesNotExist()
        }

    @Test
    fun addToReadingListScreen_selectingIssueUpdatesSelectedCount() =
        runBlocking {
            val fixture =
                insertSearchFixture()

            viewModel.beginAddToListSession()

            setScreenContent()

            composeRule
                .onNodeWithText(
                    "Search issues or series"
                )
                .performTextInput(
                    "Test"
                )

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                viewModel
                    .addToListIssueResults
                    .value
                    .any {
                        it.issueId ==
                                fixture.issueId
                    }
            }

            composeRule
                .onNodeWithText(
                    "Test Series #1"
                )
                .performClick()

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                viewModel
                    .addToListDraftIssues
                    .value
                    .any {
                        it.issueId ==
                                fixture.issueId
                    }
            }

            composeRule
                .onNodeWithText(
                    "1 issue selected"
                )
                .assertIsDisplayed()

            assertEquals(
                listOf(
                    fixture.issueId
                ),
                viewModel
                    .addToListDraftIssues
                    .value
                    .map {
                        it.issueId
                    }
            )
        }

    @Test
    fun addToReadingListScreen_backDiscardsDraftSelection() =
        runBlocking {
            val fixture =
                insertSearchFixture()

            val existingIssue =
                PendingReadingListIssue(
                    issueId = 999L,
                    seriesId = 998L,
                    seriesTitle =
                        "Existing Series",
                    issueNumber = "1",
                    issueTitle = null,
                    publicationDate = null,
                    coverUrl = null
                )

            viewModel.addPendingIssue(
                existingIssue
            )

            viewModel.beginAddToListSession()

            var backClicked =
                false

            setScreenContent(
                onBackClick = {
                    viewModel
                        .cancelAddToListSession()

                    backClicked =
                        true
                }
            )

            composeRule
                .onNodeWithText(
                    "Search issues or series"
                )
                .performTextInput(
                    "Test"
                )

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                viewModel
                    .addToListIssueResults
                    .value
                    .any {
                        it.issueId ==
                                fixture.issueId
                    }
            }

            composeRule
                .onNodeWithText(
                    "Test Series #1"
                )
                .performClick()

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                viewModel
                    .addToListDraftIssues
                    .value
                    .size == 2
            }

            composeRule
                .onNodeWithText(
                    "BACK"
                )
                .performClick()

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                backClicked
            }

            assertEquals(
                listOf(999L),
                viewModel
                    .pendingIssues
                    .value
                    .map {
                        it.issueId
                    }
            )

            assertTrue(
                viewModel
                    .addToListDraftIssues
                    .value
                    .isEmpty()
            )
        }

    @Test
    fun addToReadingListScreen_doneAppliesDraftSelection() =
        runBlocking {
            val fixture =
                insertSearchFixture()

            viewModel.beginAddToListSession()

            var doneClicked =
                false

            setScreenContent(
                onDoneClick = {
                    viewModel
                        .applyAddToListSession()

                    doneClicked =
                        true
                }
            )

            composeRule
                .onNodeWithText(
                    "Search issues or series"
                )
                .performTextInput(
                    "Test"
                )

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                viewModel
                    .addToListIssueResults
                    .value
                    .any {
                        it.issueId ==
                                fixture.issueId
                    }
            }

            composeRule
                .onNodeWithText(
                    "Test Series #1"
                )
                .performClick()

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                viewModel
                    .addToListDraftIssues
                    .value
                    .any {
                        it.issueId ==
                                fixture.issueId
                    }
            }

            composeRule
                .onNodeWithText(
                    "DONE"
                )
                .performClick()

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                doneClicked
            }

            assertEquals(
                listOf(
                    fixture.issueId
                ),
                viewModel
                    .pendingIssues
                    .value
                    .map {
                        it.issueId
                    }
            )

            assertTrue(
                viewModel
                    .addToListDraftIssues
                    .value
                    .isEmpty()
            )
        }

    private fun setScreenContent(
        onBackClick: () -> Unit = {},
        onDoneClick: () -> Unit = {}
    ) {
        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                AddToReadingListScreen(
                    viewModel = viewModel,
                    onBackClick =
                        onBackClick,
                    onDoneClick =
                        onDoneClick
                )
            }
        }
    }

    private suspend fun insertSearchFixture():
            SearchFixture {

        val publisherId =
            comicDao.insertPublisher(
                Publisher(
                    name =
                        "Test Publisher"
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

        val issueId =
            comicDao.insertIssue(
                Issue(
                    seriesId =
                        seriesId,
                    universeId = null,
                    issueNumber = "1",
                    title =
                        "Test Issue",
                    publicationDate =
                        "2026-01-01",
                    coverUrl = null,
                    description = null,
                    issueType =
                        IssueType.REGULAR
                )
            )

        return SearchFixture(
            seriesId = seriesId,
            issueId = issueId
        )
    }

    private data class SearchFixture(
        val seriesId: Long,
        val issueId: Long
    )
}