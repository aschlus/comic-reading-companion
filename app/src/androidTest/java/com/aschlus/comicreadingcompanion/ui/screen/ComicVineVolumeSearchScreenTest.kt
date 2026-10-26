package com.aschlus.comicreadingcompanion.ui.screen

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.preferences.ComicVineApiKeyStore
import com.aschlus.comicreadingcompanion.data.remote.ComicVineApiClient
import com.aschlus.comicreadingcompanion.data.remote.ComicVineHttpTransport
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineVolumeSearchViewModel
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

@RunWith(AndroidJUnit4::class)
class ComicVineVolumeSearchScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var apiKeyStore: ComicVineApiKeyStore
    private lateinit var transport: FakeTransport
    private lateinit var viewModel: ComicVineVolumeSearchViewModel

    @Before
    fun setUp() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        apiKeyStore = ComicVineApiKeyStore(context)

        runBlocking {
            apiKeyStore.clear()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            if (this@ComicVineVolumeSearchScreenTest::viewModel.isInitialized) {
                viewModel.viewModelScope
                    .coroutineContext[Job]
                    ?.cancelAndJoin()
            }

            apiKeyStore.clear()
        }
    }

    private fun launchScreen(
        apiKey: String? = null,
        responseBody: String? = null,
        responseBodies: List<String>? = null,
        onVolumeSelected: (Long) -> Unit = {}
    ) {
        runBlocking {
            apiKeyStore.clear()

            if (apiKey != null) {
                apiKeyStore.save(apiKey)
            }
        }

        transport = FakeTransport().apply {
            if (responseBody != null) {
                this.responseBody = responseBody
            }

            this.responseBodies = responseBodies
        }

        viewModel = ComicVineVolumeSearchViewModel(
            apiKeyStore = apiKeyStore,
            apiClient = ComicVineApiClient(
                transport = transport
            )
        )

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                ComicVineVolumeSearchScreen(
                    viewModel = viewModel,
                    onBackClick = {},
                    onVolumeClick = onVolumeSelected
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000L) {
            !viewModel.uiState.value.isLoadingApiKey
        }
    }

    @Test
    fun searchScreen_withoutApiKey_showsSettingsGuidance() {
        launchScreen()

        composeRule
            .onNodeWithText("SEARCH")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("SERIES SEARCH")
            .assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("Series search")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("SEARCH VOLUMES")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "Configure your API key " +
                        "in Settings before searching."
            )
            .assertIsDisplayed()

        assertEquals(
            0,
            transport.requestedUrls.size
        )
    }

    @Test
    fun searchScreen_searchesAndDisplaysVolumeResults() {
        var selectedVolumeId: Long? = null

        launchScreen(
            apiKey = "test-api-key",
            responseBody = """
                {
                    "error": "OK",
                    "status_code": 1,
                    "number_of_total_results": 1,
                    "results": [
                        {
                            "id": 123,
                            "name": "Amazing Spider-Man",
                            "start_year": "1963",
                            "count_of_issues": 441,
                            "publisher": {
                                "name": "Marvel"
                            },
                            "site_detail_url":
                                "https://example.com/volume/123",
                            "image": {
                                "small_url":
                                    "https://example.com/cover.jpg"
                            }
                        }
                    ]
                }
            """.trimIndent(),
            onVolumeSelected = { volumeId ->
                selectedVolumeId = volumeId
            }
        )

        composeRule
            .onNodeWithContentDescription("Series search")
            .performTextInput("Amazing Spider-Man")

        composeRule
            .onNodeWithText("SEARCH VOLUMES")
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5000L) {
            !viewModel.uiState.value.isSearching &&
                    viewModel.uiState.value.results.size == 1
        }

        composeRule
            .onAllNodesWithText("Amazing Spider-Man")
            .assertCountEquals(2)

        composeRule
            .onNodeWithText("1963 • Marvel • 441 issues")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Source ID: 123")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("1 of 1 results")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("LOAD MORE RESULTS")
            .assertDoesNotExist()

        assertEquals(
            1,
            transport.requestedUrls.size
        )

        composeRule
            .onNodeWithContentDescription("Series result 123")
            .performClick()

        assertEquals(123L, selectedVolumeId)
    }

    @Test
    fun searchScreen_matchingOptionsAreCollapsedByDefaultAndEditable() {
        launchScreen()

        val buttonRole = SemanticsMatcher.expectValue(
            SemanticsProperties.Role,
            Role.Button
        )

        composeRule
            .onNodeWithContentDescription("Publisher filter")
            .assertDoesNotExist()

        composeRule
            .onNodeWithContentDescription("Start year filter")
            .assertDoesNotExist()

        composeRule
            .onNodeWithContentDescription("Expected issue count filter")
            .assertDoesNotExist()
        val minimumTouchTargetPx = with(composeRule.density) {
            48.dp.toPx()
        }

        val matchingOptionsBounds = composeRule
            .onNodeWithText("ADDITIONAL MATCHING OPTIONS")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(
            "Matching options control must be at least 48dp tall",
            matchingOptionsBounds.height >= minimumTouchTargetPx
        )

        assertTrue(
            "Matching options control must be at least 48dp wide",
            matchingOptionsBounds.width >= minimumTouchTargetPx
        )

        composeRule
            .onNodeWithText("ADDITIONAL MATCHING OPTIONS")
            .assert(buttonRole)
            .performClick()

        composeRule
            .onNodeWithContentDescription("Publisher filter")
            .assertIsDisplayed()
            .performTextInput("Image Comics")

        composeRule
            .onNodeWithContentDescription("Start year filter")
            .performTextInput("2003")

        composeRule
            .onNodeWithContentDescription("Expected issue count filter")
            .performTextInput("145")

        assertEquals(
            "Image Comics",
            viewModel.uiState.value.publisherFilter
        )
        assertEquals(
            "2003",
            viewModel.uiState.value.startYearFilter
        )
        assertEquals(
            "145",
            viewModel.uiState.value.expectedIssueCountFilter
        )

        composeRule
            .onNodeWithText("HIDE MATCHING OPTIONS")
            .assert(buttonRole)
            .performClick()

        composeRule
            .onNodeWithContentDescription("Publisher filter")
            .assertDoesNotExist()

        assertEquals(
            "Image Comics",
            viewModel.uiState.value.publisherFilter
        )
    }

    @Test
    fun searchScreen_loadMoreDisplaysNextPage() {
        launchScreen(
            apiKey = "test-api-key",
            responseBodies = listOf(
                """
                    {
                        "error": "OK",
                        "status_code": 1,
                        "number_of_total_results": 2,
                        "results": [
                            {
                                "id": 101,
                                "name": "Spider-Man",
                                "start_year": "1990",
                                "count_of_issues": 10
                            }
                        ]
                    }
                """.trimIndent(),
                """
                    {
                        "error": "OK",
                        "status_code": 1,
                        "number_of_total_results": 2,
                        "results": [
                            {
                                "id": 202,
                                "name": "Spider-Man Adventures",
                                "start_year": "1994",
                                "count_of_issues": 15
                            }
                        ]
                    }
                """.trimIndent()
            )
        )

        composeRule
            .onNodeWithContentDescription("Series search")
            .performTextInput("Spider-Man")

        composeRule
            .onNodeWithText("SEARCH VOLUMES")
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5000L) {
            !viewModel.uiState.value.isSearching &&
                    viewModel.uiState.value.results.size == 1
        }

        composeRule
            .onAllNodesWithText("Spider-Man")
            .assertCountEquals(2)

        composeRule
            .onNodeWithText("LOAD MORE RESULTS")
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5000L) {
            !viewModel.uiState.value.isSearching &&
                    viewModel.uiState.value.results.size == 2
        }

        composeRule
            .onNodeWithText("Spider-Man Adventures")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("2 of 2 results")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("LOAD MORE RESULTS")
            .assertDoesNotExist()

        assertEquals(
            2,
            transport.requestedUrls.size
        )

        assertEquals(
            true,
            transport.requestedUrls[0].contains("&offset=0")
        )

        assertEquals(
            true,
            transport.requestedUrls[1].contains("&offset=1")
        )
    }

    private class FakeTransport : ComicVineHttpTransport {

        var responseBody: String = """
            {
                "error": "OK",
                "status_code": 1,
                "number_of_total_results": 0,
                "results": []
            }
        """.trimIndent()

        var responseBodies: List<String>? = null
        val requestedUrls = mutableListOf<String>()
        private var responseIndex = 0

        override suspend fun get(url: String): String {
            requestedUrls.add(url)

            val queuedResponses = responseBodies

            if (queuedResponses != null) {
                return queuedResponses[responseIndex++]
            }

            return responseBody
        }
    }
}