package com.aschlus.comicreadingcompanion.ui.screen

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingProgress
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.preferences.HomeUiPreferences
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
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
class ReadingHistoryScreenTest {

    @get:Rule
    val composeRule =
        createComposeRule()

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: HomeViewModel
    private lateinit var homeUiPreferences:
            HomeUiPreferences

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

        homeUiPreferences =
            HomeUiPreferences(context)

        viewModel =
            HomeViewModel(
                repository = repository,
                homeUiPreferences =
                    homeUiPreferences
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
    fun readingHistoryScreen_emptyStateDisplays() {
        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                ReadingHistoryScreen(
                    viewModel = viewModel,
                    onIssueClick = {},
                    onBackClick = {}
                )
            }
        }

        composeRule
            .onNodeWithText(
                "READING HISTORY"
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription(
                "Back"
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "No reading history yet. " +
                        "Finish an issue and " +
                        "we’ll keep track of it here."
            )
            .assertIsDisplayed()
    }

    @Test
    fun readingHistoryScreen_groupsIssuesByReadAge() {
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name = "History Publisher"
                    )
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId =
                            publisherId,
                        title =
                            "History Series",
                        volume = 1,
                        startYear = 2026,
                        endYear = null
                    )
                )

            val now =
                System.currentTimeMillis()

            insertCompletedIssue(
                seriesId = seriesId,
                issueNumber = "1",
                title = "Today Issue",
                completedAt =
                    now - 1_000L
            )

            insertCompletedIssue(
                seriesId = seriesId,
                issueNumber = "2",
                title = "Yesterday Issue",
                completedAt =
                    now -
                            TimeUnit.DAYS
                                .toMillis(1) -
                            1_000L
            )

            insertCompletedIssue(
                seriesId = seriesId,
                issueNumber = "3",
                title = "Week Issue",
                completedAt =
                    now -
                            TimeUnit.DAYS
                                .toMillis(7) -
                            1_000L
            )

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    ReadingHistoryScreen(
                        viewModel = viewModel,
                        onIssueClick = {},
                        onBackClick = {}
                    )
                }
            }

            composeRule.waitUntil(
                timeoutMillis = 5000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        "History Series #1"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithText("TODAY")
                .assertIsDisplayed()

            composeRule
                .onNodeWithText("YESTERDAY")
                .assertIsDisplayed()

            composeRule
                .onNodeWithText("ONE WEEK AGO")
                .assertIsDisplayed()

            composeRule
                .onNodeWithText(
                    "History Series #1"
                )
                .assertIsDisplayed()

            composeRule
                .onNodeWithText(
                    "History Series #2"
                )
                .assertIsDisplayed()

            composeRule
                .onNodeWithText(
                    "History Series #3"
                )
                .assertIsDisplayed()

            val formatter =
                SimpleDateFormat(
                    "MMM d, yyyy",
                    Locale.getDefault()
                )

            composeRule
                .onNodeWithText(
                    "Finished ${
                        formatter.format(
                            Date(now - 1_000L)
                        )
                    }"
                )
                .assertIsDisplayed()
        }
    }

    @Test
    fun readingHistoryScreen_issueClickInvokesCallback() {
        var clickedIssueId:
                Long? = null

        val issueId =
            runBlocking {
                val publisherId =
                    comicDao.insertPublisher(
                        Publisher(
                            name =
                                "Clickable Publisher"
                        )
                    )

                val seriesId =
                    comicDao.insertSeries(
                        Series(
                            publisherId =
                                publisherId,
                            title =
                                "Clickable History",
                            volume = 1,
                            startYear = 2026,
                            endYear = null
                        )
                    )

                insertCompletedIssue(
                    seriesId = seriesId,
                    issueNumber = "7",
                    title = "Clickable Issue",
                    completedAt =
                        System.currentTimeMillis()
                )
            }

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                ReadingHistoryScreen(
                    viewModel = viewModel,
                    onIssueClick = {
                        clickedIssueId = it
                    },
                    onBackClick = {}
                )
            }
        }

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithText(
                    "Clickable History #7"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText(
                "Clickable History #7"
            )
            .performClick()

        composeRule.runOnIdle {
            assertEquals(
                issueId,
                clickedIssueId
            )
        }
    }

    @Test
    fun readingHistoryScreen_backInvokesCallback() {
        var backClicked = false

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                ReadingHistoryScreen(
                    viewModel = viewModel,
                    onIssueClick = {},
                    onBackClick = {
                        backClicked = true
                    }
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(
                "Back"
            )
            .performClick()

        composeRule.runOnIdle {
            assertEquals(
                true,
                backClicked
            )
        }
    }

    private suspend fun insertCompletedIssue(
        seriesId: Long,
        issueNumber: String,
        title: String?,
        completedAt: Long
    ): Long {
        val issueId =
            comicDao.insertIssue(
                Issue(
                    seriesId = seriesId,
                    universeId = null,
                    issueNumber = issueNumber,
                    title = title,
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
                startedAt = completedAt,
                completedAt = completedAt,
                notes = null
            )
        )

        return issueId
    }
}