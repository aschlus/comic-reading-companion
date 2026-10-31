
package com.aschlus.comicreadingcompanion.ui.screen

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.preferences.ComicVineApiKeyStore
import com.aschlus.comicreadingcompanion.data.preferences.MetronApiTokenStore
import com.aschlus.comicreadingcompanion.data.remote.ComicVineApiClient
import com.aschlus.comicreadingcompanion.data.remote.ComicVineHttpTransport
import com.aschlus.comicreadingcompanion.data.remote.MetronApiClient
import com.aschlus.comicreadingcompanion.data.remote.MetronHttpResponse
import com.aschlus.comicreadingcompanion.data.remote.MetronHttpTransport
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineSettingsViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.MetronSettingsViewModel
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
class MetronAttributionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var apiKeyStore: ComicVineApiKeyStore
    private lateinit var tokenStore: MetronApiTokenStore

    private lateinit var comicVineViewModel:
        ComicVineSettingsViewModel

    private lateinit var metronViewModel:
        MetronSettingsViewModel

    @Before
    fun setUp() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        apiKeyStore = ComicVineApiKeyStore(context)
        tokenStore = MetronApiTokenStore(context)

        runBlocking {
            apiKeyStore.clear()
            tokenStore.clear()
        }

        comicVineViewModel =
            ComicVineSettingsViewModel(
                apiKeyStore = apiKeyStore,
                apiClient = validComicVineApiClient()
            )

        metronViewModel =
            MetronSettingsViewModel(
                tokenStore = tokenStore,
                apiClient = MetronApiClient(
                    transport = MetronHttpTransport { _, _ ->
                        MetronHttpResponse(
                            statusCode = 200,
                            body = """
                                {
                                    "count": 0,
                                    "next": null,
                                    "previous": null,
                                    "results": []
                                }
                            """.trimIndent()
                        )
                    }
                )
            )
    }

    @After
    fun tearDown() {
        runBlocking {
            comicVineViewModel.viewModelScope
                .coroutineContext[Job]
                ?.cancelAndJoin()

            metronViewModel.viewModelScope
                .coroutineContext[Job]
                ?.cancelAndJoin()

            apiKeyStore.clear()
            tokenStore.clear()
        }
    }

    @Test
    fun settingsScreen_displaysMetronAttributionAndOpensSourceAndLicense() {

        val openedUris = mutableListOf<String>()

        val testUriHandler =
            object : UriHandler {
                override fun openUri(uri: String) {
                    openedUris += uri
                }
            }

        composeRule.setContent {
            CompositionLocalProvider(
                LocalUriHandler provides testUriHandler
            ) {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    SettingsScreen(
                        viewModel = comicVineViewModel,
                        metronViewModel = metronViewModel,
                        onBackClick = {}
                    )
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000L) {
            !comicVineViewModel.uiState.value.isLoading &&
                !metronViewModel.uiState.value.isLoading
        }

        composeRule
            .onNodeWithText("DATA ATTRIBUTION")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "Comic release data from Metron, licensed under " +
                    "Creative Commons Attribution-ShareAlike 4.0 International " +
                    "(CC BY-SA 4.0)."
            )
            .performScrollTo()
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "Changes: Metron release fields are normalized into the " +
                    "app's local data model. Store-date and FOC-date updates " +
                    "reported by Metron are retained in release-change history."
            )
            .performScrollTo()
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "Metron cover images are not imported; their rights are " +
                    "separate from the CC BY-SA 4.0 catalog-data license."
            )
            .performScrollTo()
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("METRON SOURCE")
            .performScrollTo()
            .performClick()

        composeRule
            .onNodeWithText("CC BY-SA 4.0 LICENSE")
            .performScrollTo()
            .performClick()

        assertEquals(
            listOf(
                "https://metron.cloud/",
                "https://creativecommons.org/licenses/by-sa/4.0/"
            ),
            openedUris
        )
    }

    private fun validComicVineApiClient(): ComicVineApiClient {
        return ComicVineApiClient(
            transport = ComicVineHttpTransport { _ ->
                """
                {
                    "error": "OK",
                    "status_code": 1,
                    "results": []
                }
                """.trimIndent()
            }
        )
    }
}
