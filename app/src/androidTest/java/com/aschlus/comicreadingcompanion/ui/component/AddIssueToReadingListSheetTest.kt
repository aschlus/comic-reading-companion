package com.aschlus.comicreadingcompanion.ui.component

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.aschlus.comicreadingcompanion.ui.viewmodel.AddIssueToReadingListViewModel
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
class AddIssueToReadingListSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: AddIssueToReadingListViewModel

    private var issueId: Long = 0
    private var publisherId: Long = 0

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

        runBlocking {
            publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
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

            issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Test Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )
        }

        viewModel =
            AddIssueToReadingListViewModel(
                issueId = issueId,
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
    fun addIssueToReadingListSheet_displaysUserReadingLists() {
        runBlocking {
            repository.createUserReadingList(
                title = "My Spider-Man List",
                description = "Favorite issues",
                publisherId = publisherId,
                universeId = null
            )
        }

        composeRule.setContent {
            AddIssueToReadingListSheet(
                viewModel = viewModel,
                onDismissRequest = {}
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("My Spider-Man List")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Add to Reading List").assertIsDisplayed()
        composeRule.onNodeWithText("My Spider-Man List").assertIsDisplayed()
        composeRule.onNodeWithText("Favorite issues").assertIsDisplayed()
    }

    @Test
    fun addIssueToReadingListSheet_addsIssueAndDismisses() {
        runBlocking {
            val readingListId =
                repository.createUserReadingList(
                    title = "My List",
                    description = null,
                    publisherId = publisherId,
                    universeId = null
                )

            var showSheet by mutableStateOf(true)

            composeRule.setContent {
                if (showSheet) {
                    AddIssueToReadingListSheet(
                        viewModel = viewModel,
                        onDismissRequest = {
                            showSheet = false
                        }
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodesWithText("My List")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithText("My List").performClick()
            composeRule.waitUntil(timeoutMillis = 5_000) {
                !showSheet
            }

            val items = comicDao.getItemsForReadingList(readingListId)
            assertEquals(1, items.size)
            assertEquals(issueId, items.first().issueId)
            composeRule.onNodeWithText("Add to Reading List").assertDoesNotExist()
        }
    }
}