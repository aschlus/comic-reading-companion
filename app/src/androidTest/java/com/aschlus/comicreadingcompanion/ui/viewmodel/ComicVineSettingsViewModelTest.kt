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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class ComicVineSettingsViewModelTest {

    private lateinit var apiKeyStore: ComicVineApiKeyStore
    private lateinit var transport: FakeTransport
    private lateinit var viewModel: ComicVineSettingsViewModel

    @Before
    fun setUp() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        apiKeyStore = ComicVineApiKeyStore(context)

        runBlocking {
            apiKeyStore.clear()
        }

        transport = FakeTransport()

        viewModel = ComicVineSettingsViewModel(
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
    fun initialState_reportsNoConfiguredKey() = runBlocking {
        val state = withTimeout(5000L.milliseconds) {
            viewModel.uiState.first { !it.isLoading }
        }

        assertFalse(state.hasSavedApiKey)
        assertFalse(state.isSaving)
        assertNull(state.message)
        assertNull(state.error)
    }

    @Test
    fun saveApiKey_andReplaceKey_updatesConfiguredStatus() =
        runBlocking {
            withTimeout(5000L.milliseconds) {
                viewModel.uiState.first { !it.isLoading }
            }

            viewModel.saveApiKey("first-test-key")

            val savedState = withTimeout(5000L.milliseconds) {
                viewModel.uiState.first {
                    it.hasSavedApiKey &&
                            !it.isSaving &&
                            it.message ==
                            "Comic Vine API key validated and saved securely."
                }
            }

            assertTrue(savedState.hasSavedApiKey)
            assertEquals("first-test-key", apiKeyStore.get())

            viewModel.saveApiKey("replacement-test-key")

            val replacedState = withTimeout(5000L.milliseconds) {
                viewModel.uiState.first {
                    it.hasSavedApiKey &&
                            !it.isSaving &&
                            it.message ==
                            "Comic Vine API key validated and saved securely."
                }
            }

            assertTrue(replacedState.hasSavedApiKey)
            assertEquals(
                "replacement-test-key",
                apiKeyStore.get()
            )
        }

    @Test
    fun clearApiKey_removesKeyAndUpdatesStatus() = runBlocking {
        withTimeout(5000L.milliseconds) {
            viewModel.uiState.first { !it.isLoading }
        }

        viewModel.saveApiKey("test-key-to-remove")

        withTimeout(5000L.milliseconds) {
            viewModel.uiState.first {
                it.hasSavedApiKey &&
                        !it.isSaving &&
                        it.message ==
                        "Comic Vine API key validated and saved securely."
            }
        }

        viewModel.clearApiKey()

        val clearedState = withTimeout(5000L.milliseconds) {
            viewModel.uiState.first {
                !it.isSaving &&
                        !it.hasSavedApiKey &&
                        it.message == "Comic Vine API key removed."
            }
        }

        assertFalse(clearedState.hasSavedApiKey)
        assertNull(apiKeyStore.get())
    }

    @Test
    fun saveApiKey_withBlankInput_reportsValidationError() =
        runBlocking {
            withTimeout(5000L.milliseconds) {
                viewModel.uiState.first { !it.isLoading }
            }

            viewModel.saveApiKey("   ")

            val state = viewModel.uiState.value

            assertFalse(state.isSaving)
            assertFalse(state.hasSavedApiKey)
            assertNotNull(state.error)
            assertEquals(
                "Enter a Comic Vine API Key.",
                state.error
            )
            assertNull(apiKeyStore.get())
        }

    @Test
    fun clearFeedback_removesExistingValidationError() =
        runBlocking {
            withTimeout(5000L.milliseconds) {
                viewModel.uiState.first { !it.isLoading }
            }

            viewModel.saveApiKey("   ")

            assertEquals(
                "Enter a Comic Vine API Key.",
                viewModel.uiState.value.error
            )

            viewModel.clearFeedback()

            assertNull(viewModel.uiState.value.error)
            assertNull(viewModel.uiState.value.message)
        }

    @Test
    fun saveApiKey_withInvalidReplacement_preservesPreviouslySavedKey() =
        runBlocking {
            withTimeout(5000L.milliseconds) {
                viewModel.uiState.first { !it.isLoading }
            }

            viewModel.saveApiKey("known-good-key")

            withTimeout(5000L.milliseconds) {
                viewModel.uiState.first {
                    it.hasSavedApiKey &&
                            !it.isSaving &&
                            it.message ==
                            "Comic Vine API key validated and saved securely."
                }
            }

            transport.responseBody = """
                {
                    "error": "Invalid API Key",
                    "status_code": 100,
                    "results": []
                }
            """.trimIndent()

            viewModel.saveApiKey("invalid-replacement-key")

            val rejectedState = withTimeout(5000L.milliseconds) {
                viewModel.uiState.first {
                    !it.isSaving &&
                            it.error ==
                            "Comic Vine rejected this API key. " +
                            "Check it and try again."
                }
            }

            assertTrue(rejectedState.hasSavedApiKey)
            assertEquals(
                "known-good-key",
                apiKeyStore.get()
            )
        }

    @Test
    fun saveApiKey_withNetworkFailure_doesNotSaveNewKey() =
        runBlocking {
            withTimeout(5000L.milliseconds) {
                viewModel.uiState.first { !it.isLoading }
            }

            transport.failure =
                IOException("Simulated network failure")

            viewModel.saveApiKey("key-not-to-be-saved")

            val failedState = withTimeout(5000L.milliseconds) {
                viewModel.uiState.first {
                    !it.isSaving &&
                            it.error ==
                            "Could not reach Comic Vine. " +
                            "Check your connection and try again."
                }
            }

            assertFalse(failedState.hasSavedApiKey)
            assertNull(apiKeyStore.get())
        }

    private class FakeTransport : ComicVineHttpTransport {

        var responseBody: String = """
            {
                "error": "OK",
                "status_code": 1,
                "results": []
            }
        """.trimIndent()

        var failure: IOException? = null

        override suspend fun get(url: String): String {
            failure?.let { throw it }
            return responseBody
        }
    }
}