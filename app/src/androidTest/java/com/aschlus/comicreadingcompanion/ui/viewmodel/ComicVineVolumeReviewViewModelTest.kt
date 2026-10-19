package com.aschlus.comicreadingcompanion.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.preferences.ComicVineApiKeyStore
import com.aschlus.comicreadingcompanion.data.remote.ComicVineApiClient
import com.aschlus.comicreadingcompanion.data.remote.ComicVineHttpTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class ComicVineVolumeReviewViewModelTest {

    private lateinit var apiKeyStore: ComicVineApiKeyStore
    private lateinit var transport: FakeTransport
    private lateinit var viewModel: ComicVineVolumeReviewViewModel

    @Before
    fun setUp() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        apiKeyStore = ComicVineApiKeyStore(context)

        runBlocking {
            apiKeyStore.clear()
        }

        transport = FakeTransport()

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
    fun load_withoutApiKeyReportsSettingsGuidance() = runBlocking {
        viewModel.load(123L)

        val state = withTimeout(5000L.milliseconds) {
            viewModel.uiState.first { !it.isLoading }
        }

        assertNull(state.volume)
        assertTrue(state.issues.isEmpty())
        assertEquals(
            "Configure your Comic Vine API key in " +
                    "Settings before loading series details.",
            state.error
        )
        assertTrue(transport.requestedUrls.isEmpty())
    }

    @Test
    fun load_fetchesVolumeAndAllIssuePages() = runBlocking {
        apiKeyStore.save("test-api-key")

        transport.responses = listOf(
            """
                {
                    "error": "OK",
                    "status_code": 1,
                    "results": {
                        "id": 123,
                        "name": "Amazing Spider-Man",
                        "start_year": "1963",
                        "count_of_issues": 3,
                        "publisher": {
                            "name": "Marvel"
                        },
                        "site_detail_url": "https://example.com/volume/123",
                        "description": "Series description",
                        "image": {
                            "small_url": "https://example.com/series.jpg"
                        }
                    }
                }
            """.trimIndent(),
            """
                {
                    "error": "OK",
                    "status_code": 1,
                    "number_of_total_results": 3,
                    "results": [
                        {
                            "id": 1001,
                            "issue_number": "1",
                            "name": "Origin",
                            "cover_date": "1963-03-01",
                            "store_date": "1963-01-01",
                            "site_detail_url": "https://example.com/issue/1001",
                            "image": {
                                "small_url": "https://example.com/issue1.jpg"
                            }
                        },
                        {
                            "id": 1002,
                            "issue_number": "2",
                            "name": "The Return",
                            "cover_date": "1963-04-01",
                            "store_date": "1963-02-01",
                            "site_detail_url": "https://example.com/issue/1002",
                            "image": {
                                "small_url": "https://example.com/issue2.jpg"
                            }
                        }
                    ]
                }
            """.trimIndent(),
            """
                {
                    "error": "OK",
                    "status_code": 1,
                    "number_of_total_results": 3,
                    "results": [
                        {
                            "id": 1003,
                            "issue_number": "3",
                            "name": "Another Adventure",
                            "cover_date": "1963-05-01",
                            "site_detail_url": "https://example.com/issue/1003"
                        }
                    ]
                }
            """.trimIndent()
        )

        viewModel.load(123L)

        val state = withTimeout(5000L.milliseconds) {
            viewModel.uiState.first {
                !it.isLoading && it.volume != null
            }
        }

        assertNull(state.error)
        assertNotNull(state.volume)
        assertEquals(123L, state.volume!!.id)
        assertEquals("Amazing Spider-Man", state.volume!!.name)
        assertEquals("Marvel", state.volume!!.publisherName)
        assertEquals(3, state.volume!!.countOfIssues)

        assertEquals(3, state.totalIssueRecords)
        assertEquals(3, state.issues.size)
        assertEquals(
            listOf("1", "2", "3"),
            state.issues.map { it.issueNumber }
        )
        assertEquals(
            listOf(1001L, 1002L, 1003L),
            state.issues.map { it.id }
        )

        assertEquals(3, transport.requestedUrls.size)
        assertTrue(
            transport.requestedUrls[0].contains("/volume/4050-123/")
        )
        assertTrue(
            transport.requestedUrls[1].contains("filter=volume%3A123")
        )
        assertTrue(
            transport.requestedUrls[2].contains("&offset=2")
        )
    }

    @Test
    fun load_retainsPartialIssuesIfLaterPageFails() = runBlocking {
        apiKeyStore.save("test-api-key")

        transport.responses = listOf(
            """
                {
                    "error": "OK",
                    "status_code": 1,
                    "results": {
                        "id": 123,
                        "name": "Test Series",
                        "count_of_issues": 3
                    }
                }
            """.trimIndent(),
            """
                {
                    "error": "OK",
                    "status_code": 1,
                    "number_of_total_results": 3,
                    "results": [
                        {
                            "id": 1001,
                            "issue_number": "1",
                            "name": "Issue One"
                        },
                        {
                            "id": 1002,
                            "issue_number": "2",
                            "name": "Issue Two"
                        }
                    ]
                }
            """.trimIndent()
        )
        transport.failAtRequest = 3

        viewModel.load(123L)

        val state = withTimeout(5000L.milliseconds) {
            viewModel.uiState.first {
                !it.isLoading && it.error != null
            }
        }

        assertNotNull(state.volume)
        assertEquals(2, state.issues.size)
        assertEquals(
            listOf(1001L, 1002L),
            state.issues.map { it.id }
        )
        assertEquals(
            "Could not reach Comic Vine. " +
                    "Check your connection and try again.",
            state.error
        )
    }

    @Test
    fun load_invalidVolumeIdDoesNotRequestData() {
        viewModel.load(0L)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(
            "The selected series has an invalid source ID.",
            viewModel.uiState.value.error
        )
        assertTrue(transport.requestedUrls.isEmpty())
    }

    private class FakeTransport : ComicVineHttpTransport {

        var responses: List<String> = emptyList()
        var failAtRequest: Int? = null
        val requestedUrls = mutableListOf<String>()

        override suspend fun get(url: String): String {
            requestedUrls.add(url)

            if (requestedUrls.size == failAtRequest) {
                throw IOException("Simulated network failure")
            }

            return responses.getOrNull(requestedUrls.size - 1)
                ?: error("No fake response configured for request ${requestedUrls.size}")
        }
    }
}