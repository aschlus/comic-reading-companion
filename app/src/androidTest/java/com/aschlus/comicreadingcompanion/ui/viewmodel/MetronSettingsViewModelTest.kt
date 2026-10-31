package com.aschlus.comicreadingcompanion.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.preferences.MetronApiTokenStore
import com.aschlus.comicreadingcompanion.data.remote.MetronApiClient
import com.aschlus.comicreadingcompanion.data.remote.MetronHttpResponse
import com.aschlus.comicreadingcompanion.data.remote.MetronHttpTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class MetronSettingsViewModelTest {

    private lateinit var tokenStore: MetronApiTokenStore
    private lateinit var viewModel: MetronSettingsViewModel

    @Before
    fun setUp() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        tokenStore = MetronApiTokenStore(context)

        runBlocking {
            tokenStore.clear()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            viewModel.viewModelScope
                .coroutineContext[Job]
                ?.cancelAndJoin()

            tokenStore.clear()
        }
    }

    @Test
    fun saveToken_validatesAndStoresToken() = runBlocking {
        viewModel = createViewModel(statusCode = 200)

        awaitState { !it.isLoading }

        viewModel.saveToken(" test-metron-token ")

        val state = awaitState {
            !it.isSaving && it.hasSavedToken
        }

        assertTrue(state.hasSavedToken)
        assertEquals(
            "Metron API token validated and saved securely.",
            state.message
        )
        assertEquals("test-metron-token", tokenStore.get())
    }

    @Test
    fun saveToken_rejectedTokenIsNotStored() = runBlocking {
        viewModel = createViewModel(statusCode = 401)

        awaitState { !it.isLoading }

        viewModel.saveToken("invalid-token")

        val state = awaitState {
            !it.isSaving && it.error != null
        }

        assertFalse(state.hasSavedToken)
        assertEquals(
            "Metron rejected this API token. Check it and try again.",
            state.error
        )
        assertEquals(null, tokenStore.get())
    }

    private fun createViewModel(
        statusCode: Int
    ): MetronSettingsViewModel {
        val transport = MetronHttpTransport { _, _ ->
            MetronHttpResponse(
                statusCode = statusCode,
                body = if (statusCode in 200..299) {
                    """
                    {
                        "count": 0,
                        "next": null,
                        "previous": null,
                        "results": []
                    }
                    """.trimIndent()
                } else {
                    """{"detail":"Authentication failed."}"""
                }
            )
        }

        return MetronSettingsViewModel(
            tokenStore = tokenStore,
            apiClient = MetronApiClient(transport)
        )
    }

    private suspend fun awaitState(
        predicate: (MetronSettingsUiState) -> Boolean
    ): MetronSettingsUiState =
        withTimeout(5_000L.milliseconds) {
            viewModel.uiState.first(predicate)
        }

    @Test
    fun previewUpcomingReleases_completesAndReportsDateRange() = runBlocking {
        viewModel = createViewModel(statusCode = 200)

        awaitState { !it.isLoading }

        viewModel.saveToken("test-metron-token")

        awaitState {
            !it.isSaving && it.hasSavedToken
        }

        viewModel.previewUpcomingReleases()

        val state = awaitState {
            !it.isCheckingReleases &&
                    it.releasePreviewCount != null
        }

        assertEquals(0, state.releasePreviewCount)
        assertTrue(state.releasePreviewRange != null)
        assertTrue(state.releasePreviewError == null)
    }
}