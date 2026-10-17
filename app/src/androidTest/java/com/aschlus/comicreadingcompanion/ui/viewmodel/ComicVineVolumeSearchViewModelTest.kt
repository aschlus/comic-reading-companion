package com.aschlus.comicreadingcompanion.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.preferences.ComicVineApiKeyStore
import com.aschlus.comicreadingcompanion.data.remote.ComicVineApiClient
import com.aschlus.comicreadingcompanion.data.remote.ComicVineHttpTransport
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class ComicVineVolumeSearchViewModelTest {

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

        transport = FakeTransport()

        viewModel = ComicVineVolumeSearchViewModel(
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
    fun initialState_reportsNoConfiguredApiKey() = runBlocking {
        val state = withTimeout(5000L.milliseconds) {
            viewModel.uiState.first { !it.isLoadingApiKey }
        }

        assertFalse(state.hasApiKey)
        assertFalse(state.isSearching)
        assertTrue(state.results.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun search_withBlankQuery_reportsErrorWithoutRequest() =
        runBlocking {
            withTimeout(5000L.milliseconds) {
                viewModel.uiState.first { !it.isLoadingApiKey }
            }

            viewModel.search()

            assertEquals(
                "Enter a series title to search.",
                viewModel.uiState.value.error
            )
            assertTrue(transport.requestedUrls.isEmpty())
        }

    @Test
    fun search_withoutConfiguredKey_reportsSettingsGuidance() =
        runBlocking {
            withTimeout(5000L.milliseconds) {
                viewModel.uiState.first { !it.isLoadingApiKey }
            }

            viewModel.updateQuery("Amazing Spider-Man")
            viewModel.search()

            val state = withTimeout(5000L.milliseconds) {
                viewModel.uiState.first {
                    !it.isSearching && it.error != null
                }
            }

            assertFalse(state.hasApiKey)
            assertEquals(
                "Configure your Comic Vine API key in " +
                        "Settings before searching.",
                state.error
            )
            assertTrue(transport.requestedUrls.isEmpty())
        }

    @Test
    fun search_success_exposesVolumeResults() = runBlocking {
        apiKeyStore.save("test-api-key")

        transport.responseBody = """
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
        """.trimIndent()

        withTimeout(5000L.milliseconds) {
            viewModel.uiState.first { !it.isLoadingApiKey }
        }

        viewModel.updateQuery("Amazing Spider-Man")
        viewModel.search()

        val state = withTimeout(5000L.milliseconds) {
            viewModel.uiState.first {
                !it.isSearching && it.results.isNotEmpty()
            }
        }

        assertTrue(state.hasApiKey)
        assertNull(state.error)
        assertEquals(1, state.totalResults)
        assertEquals(1, state.results.size)
        assertEquals(123L, state.results.single().id)
        assertEquals(
            "Amazing Spider-Man",
            state.results.single().name
        )
        assertEquals(
            "Marvel",
            state.results.single().publisherName
        )
        assertEquals(
            "https://example.com/cover.jpg",
            state.results.single().imageUrl
        )
        assertTrue(
            transport.requestedUrls.single()
                .contains("query=Amazing+Spider-Man")
        )
    }

    @Test
    fun search_invalidKey_reportsErrorWithoutClearingSavedKey() =
        runBlocking {
            apiKeyStore.save("invalid-api-key")

            transport.responseBody = """
                {
                    "error": "Invalid API Key",
                    "status_code": 100,
                    "results": []
                }
            """.trimIndent()

            withTimeout(5000L.milliseconds) {
                viewModel.uiState.first { !it.isLoadingApiKey }
            }

            viewModel.updateQuery("Batman")
            viewModel.search()

            val state = withTimeout(5000L.milliseconds) {
                viewModel.uiState.first {
                    !it.isSearching && it.error != null
                }
            }

            assertTrue(state.hasApiKey)
            assertTrue(state.results.isEmpty())
            assertEquals(
                "Comic Vine rejected the saved API key. " +
                        "Open Settings to replace it.",
                state.error
            )
            assertEquals("invalid-api-key", apiKeyStore.get())
        }

    @Test
    fun search_networkFailure_reportsError() = runBlocking {
        apiKeyStore.save("test-api-key")
        transport.failure =
            IOException("Simulated network failure")

        withTimeout(5000L.milliseconds) {
            viewModel.uiState.first { !it.isLoadingApiKey }
        }

        viewModel.updateQuery("Batman")
        viewModel.search()

        val state = withTimeout(5000L.milliseconds) {
            viewModel.uiState.first {
                !it.isSearching && it.error != null
            }
        }

        assertTrue(state.results.isEmpty())
        assertEquals(
            "Could not reach Comic Vine. " +
                    "Check your connection and try again.",
            state.error
        )
    }

    @Test
    fun loadMore_appendsResultsAndAdvancesOffset() = runBlocking {
        apiKeyStore.save("test-api-key")

        transport.responseBodies = listOf(
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

        withTimeout(5000L.milliseconds) {
            viewModel.uiState.first { !it.isLoadingApiKey }
        }

        viewModel.updateQuery("Spider-Man")
        viewModel.search()

        withTimeout(5000L.milliseconds) {
            viewModel.uiState.first {
                !it.isSearching && it.results.size == 1
            }
        }

        assertTrue(viewModel.uiState.value.hasMoreResults)
        assertEquals(1, viewModel.uiState.value.nextOffset)

        viewModel.loadMore()

        val state = withTimeout(5000L.milliseconds) {
            viewModel.uiState.first {
                !it.isSearching && it.results.size == 2
            }
        }

        assertEquals(
            listOf(101L, 202L),
            state.results.map { it.id }
        )
        assertEquals(2, state.totalResults)
        assertEquals(2, state.nextOffset)
        assertFalse(state.hasMoreResults)
        assertEquals(2, transport.requestedUrls.size)
        assertTrue(
            transport.requestedUrls[0].contains("&offset=0")
        )
        assertTrue(
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
        var failure: IOException? = null
        val requestedUrls = mutableListOf<String>()

        private var responseIndex = 0

        override suspend fun get(url: String): String {
            requestedUrls.add(url)

            failure?.let { throw it }

            val queuedResponses = responseBodies

            if (queuedResponses != null) {
                return queuedResponses[responseIndex++]
            }

            return responseBody
        }
    }
}