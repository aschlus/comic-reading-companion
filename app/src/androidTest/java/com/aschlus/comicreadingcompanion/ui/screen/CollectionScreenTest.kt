
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
import com.aschlus.comicreadingcompanion.data.database.entities.IssueOwnership
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import com.aschlus.comicreadingcompanion.ui.viewmodel.CollectionViewModel
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
class CollectionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: CollectionViewModel

    @Before
    fun setUp() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

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

        viewModel =
            CollectionViewModel(
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
    fun collectionScreen_displaysEmptyState() {
        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                CollectionScreen(
                    viewModel = viewModel,
                    onIssueClick = {}
                )
            }
        }

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithText("COLLECTION")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText("COLLECTION")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("OWNED ISSUES")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "Your collection is empty. " +
                        "Open an issue and mark it as owned " +
                        "to add it here."
            )
            .assertIsDisplayed()
    }

    @Test
    fun collectionScreen_clickingOwnedIssueInvokesCallback() {
        val issueId =
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

                val createdIssueId =
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

                comicDao.markIssueOwned(
                    IssueOwnership(
                        issueId = createdIssueId
                    )
                )

                createdIssueId
            }

        var clickedIssueId: Long? = null

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                CollectionScreen(
                    viewModel = viewModel,
                    onIssueClick = { selectedIssueId ->
                        clickedIssueId = selectedIssueId
                    }
                )
            }
        }

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithText("Amazing Spider-Man")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText("Amazing Spider-Man")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("#30 · Coming Home")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(
                issueId,
                clickedIssueId
            )
        }
    }
}
