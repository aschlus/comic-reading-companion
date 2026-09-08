package com.aschlus.comicreadingcompanion.ui.screen

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.viewModelScope
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import com.aschlus.comicreadingcompanion.ui.viewmodel.CreateReadingListViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateReadingListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: CreateReadingListViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(context, ComicDatabase::class.java)
                .allowMainThreadQueries()
                .build()

        comicDao = database.comicDao()

        repository =
            ComicRepository(
                comicDao = comicDao,
                database = database
            )

        viewModel = CreateReadingListViewModel(repository = repository)
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
    fun createReadingListScreen_createButtonRequiresTitleAndPublisher() {
        runBlocking {
            comicDao.insertPublisher(Publisher(name = "Marvel"))

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    CreateReadingListScreen(
                        viewModel = viewModel,
                        onBackClick = {},
                        onReadingListCreated = {}
                    )
                }
            }
            composeRule.onNode(hasText("Create Reading List") and hasClickAction()).assertIsNotEnabled()
            composeRule.onNodeWithText("Title").performTextInput("My Spider-Man List")
            composeRule.onNode(hasText("Create Reading List") and hasClickAction()).assertIsNotEnabled()
            composeRule.onNodeWithText("Select publisher").performClick()
            composeRule.onNodeWithText("Marvel").performClick()
            composeRule.onNode(hasText("Create Reading List") and hasClickAction()).assertIsEnabled()
        }
    }

    @Test
    fun createReadingListScreen_createsListAndReturnsCreatedId() {
        runBlocking {
            val publisherId = comicDao.insertPublisher(Publisher(name = "Marvel"))
            val universeId =
                comicDao.insertUniverse(
                    Universe(
                        publisherId = publisherId,
                        name = "Earth-616",
                        designation = "Earth-616",
                        description = null
                    )
                )

            var createdId: Long? = null

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    CreateReadingListScreen(
                        viewModel = viewModel,
                        onBackClick = {},
                        onReadingListCreated = {
                            createdId = it
                        }
                    )
                }
            }
            composeRule.onNodeWithText("Title").performTextInput("My Earth-616 List")
            composeRule.onNodeWithText("Description (optional)")
                .performTextInput("Custom Marvel order")
            composeRule.onNodeWithText("Select publisher").performClick()
            composeRule.onNodeWithText("Marvel").performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {
                viewModel.universes.value.any { it.id == universeId}
            }
            composeRule.onNodeWithText("No specific continuity").performClick()
            composeRule.onNodeWithText("Earth-616").performClick()
            composeRule.onNode(hasText("Create Reading List")
                    and hasClickAction()).performClick()
            composeRule.waitUntil(timeoutMillis = 5000L) {createdId != null}
            val readingList =
                comicDao.getReadingListById(createdId!!)
            assertNotNull(readingList)
        }
    }
}