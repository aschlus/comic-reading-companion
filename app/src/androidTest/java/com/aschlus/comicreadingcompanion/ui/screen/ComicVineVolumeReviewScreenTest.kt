package com.aschlus.comicreadingcompanion.ui.screen

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToLog
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.preferences.ComicVineApiKeyStore
import com.aschlus.comicreadingcompanion.data.remote.ComicVineApiClient
import com.aschlus.comicreadingcompanion.data.remote.ComicVineHttpTransport
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineVolumeReviewViewModel
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
class ComicVineVolumeReviewScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var apiKeyStore: ComicVineApiKeyStore
    private lateinit var viewModel: ComicVineVolumeReviewViewModel
    private lateinit var transport: FakeTransport

    @Before
    fun setUp() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        apiKeyStore = ComicVineApiKeyStore(context)

        runBlocking {
            apiKeyStore.clear()
            apiKeyStore.save("test-api-key")
        }

        transport = FakeTransport(
            responses = listOf(
                """
                    {
                        "error": "OK",
                        "status_code": 1,
                        "results": {
                            "id": 123,
                            "name": "Amazing Spider-Man",
                            "start_year": "1963",
                            "count_of_issues": 1,
                            "publisher": {
                                "name": "Marvel"
                            },
                            "site_detail_url":
                                "https://example.com/volume/123",
                            "description": "Series description"
                        }
                    }
                """.trimIndent(),
                """
                    {
                        "error": "OK",
                        "status_code": 1,
                        "number_of_total_results": 1,
                        "results": [
                            {
                                "id": 1001,
                                "issue_number": "1",
                                "name": "Origin",
                                "cover_date": "1963-03-01",
                                "site_detail_url":
                                    "https://example.com/issue/1001"
                            }
                        ]
                    }
                """.trimIndent()
            )
        )

        viewModel = ComicVineVolumeReviewViewModel(
            apiKeyStore = apiKeyStore,
            apiClient = ComicVineApiClient(
                transport = transport
            )
        )
    }

    @After
    fun tearDown() {
        runBlocking {
            viewModel.viewModelScope
                .coroutineContext[Job]
                ?.cancelAndJoin()

            apiKeyStore.clear()
        }
    }

    @Test
    fun reviewScreen_displaysSeriesAndRetrievedIssues() {
        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                ComicVineVolumeReviewScreen(
                    viewModel = viewModel,
                    onBackClick = {}
                )
            }
        }

        viewModel.load(123L)

        composeRule.waitUntil(timeoutMillis = 5000L) {
            !viewModel.uiState.value.isLoading &&
                    viewModel.uiState.value.volume != null
        }

        composeRule
            .onNodeWithText("SERIES REVIEW")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("SERIES DETAILS")
            .assertIsDisplayed()

        assertEquals(
            "Amazing Spider-Man",
            viewModel.uiState.value.volume?.name
        )

        composeRule
            .onNodeWithText("Amazing Spider-Man")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Series description")
            .assertDoesNotExist()

        composeRule
            .onNodeWithText("Amazing Spider-Man")
            .performTextReplacement("Edited Series Title")

        assertEquals(
            "Edited Series Title",
            viewModel.uiState.value.editedTitle
        )

        composeRule.onNodeWithText("PUBLISHER").assertIsDisplayed()
        composeRule.onNodeWithText("Marvel").assertIsDisplayed()

        composeRule.onNodeWithText("VOLUME NUMBER").assertExists()
        composeRule.onNodeWithText("START YEAR").assertExists()
        composeRule.onNodeWithText("END YEAR").assertExists()
        composeRule.onNodeWithText("No universe").assertExists()
        composeRule.onNodeWithText("IMPORT TO CATALOG").assertExists()

        composeRule
            .onNodeWithText("ISSUES")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Loaded 1 of 1 source issue records")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("#1")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Origin")
            .assertIsDisplayed()

        assertEquals(2, transport.requestedUrls.size)
    }

    private class FakeTransport(
        private val responses: List<String>
    ) : ComicVineHttpTransport {

        val requestedUrls = mutableListOf<String>()

        override suspend fun get(url: String): String {
            requestedUrls.add(url)

            return checkNotNull(
                responses.getOrNull(requestedUrls.lastIndex)
            ) {
                "No fake response configured for request ${requestedUrls.size}"
            }
        }
    }
}