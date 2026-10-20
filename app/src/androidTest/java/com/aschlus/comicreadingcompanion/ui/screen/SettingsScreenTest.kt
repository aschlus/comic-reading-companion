package com.aschlus.comicreadingcompanion.ui.screen

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.preferences.ComicVineApiKeyStore
import com.aschlus.comicreadingcompanion.data.remote.ComicVineApiClient
import com.aschlus.comicreadingcompanion.data.remote.ComicVineHttpTransport
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineSettingsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var apiKeyStore: ComicVineApiKeyStore
    private lateinit var viewModel: ComicVineSettingsViewModel

    @Before
    fun setUp() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        apiKeyStore = ComicVineApiKeyStore(context)

        runBlocking {
            apiKeyStore.clear()
        }

        viewModel = ComicVineSettingsViewModel(
            apiKeyStore = apiKeyStore,
            apiClient = validApiClient()
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

    private fun validApiClient(): ComicVineApiClient =
        ComicVineApiClient(
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

    private fun launchScreen() {
        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = {}
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000L) {
            !viewModel.uiState.value.isLoading
        }
    }

    @Test
    fun settingsScreen_showsUnconfiguredState() {
        launchScreen()

        composeRule
            .onNodeWithText("SETTINGS")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("API KEY")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("NO API KEY CONFIGURED")
            .assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("API key")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("REMOVE API KEY")
            .assertDoesNotExist()
    }

    @Test
    fun settingsScreen_clearsValidationErrorWhenKeyIsEntered() {
        launchScreen()

        composeRule
            .onNodeWithText("SAVE API KEY")
            .performClick()

        composeRule
            .onNodeWithText("Enter a Comic Vine API Key.")
            .assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("API key")
            .performTextInput("test-comic-vine-key")

        composeRule
            .onNodeWithText("Enter a Comic Vine API Key.")
            .assertDoesNotExist()

        composeRule
            .onNodeWithContentDescription("Show API key")
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_savesKeyAndRemovesItAfterConfirmation() {
        launchScreen()

        composeRule
            .onNodeWithContentDescription("API key")
            .performTextInput("test-comic-vine-key")

        composeRule
            .onNodeWithText("SAVE API KEY")
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5000L) {
            viewModel.uiState.value.hasSavedApiKey &&
                    !viewModel.uiState.value.isSaving
        }

        composeRule
            .onNodeWithText("API KEY CONFIGURED")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "Comic Vine API key validated and saved securely."
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("REMOVE API KEY")
            .performClick()

        composeRule
            .onNodeWithText("Remove API key?")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Cancel")
            .performClick()

        composeRule
            .onNodeWithText("API KEY CONFIGURED")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("REMOVE API KEY")
            .performClick()

        composeRule
            .onNodeWithText("Remove key")
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5000L) {
            !viewModel.uiState.value.isSaving &&
                    !viewModel.uiState.value.hasSavedApiKey
        }

        composeRule
            .onNodeWithText("NO API KEY CONFIGURED")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Comic Vine API key removed.")
            .assertIsDisplayed()

        runBlocking {
            assertNull(apiKeyStore.get())
        }
    }

    @Test
    fun settingsScreen_exportBackupButtonInvokesCallback() {
        var exportRequested = false

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = {},
                    onExportBackupClick = {
                        exportRequested = true
                    }
                )
            }
        }

        composeRule
            .onNodeWithText("DATA BACKUP")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("EXPORT FULL BACKUP")
            .assertIsDisplayed()
            .performClick()

        assertTrue(exportRequested)
    }

    @Test
    fun settingsScreen_validateBackupButtonInvokesCallback() {
        var validationRequested = false

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = {},
                    onValidateBackupClick = {
                        validationRequested = true
                    }
                )
            }
        }

        composeRule
            .onNodeWithText("VALIDATE BACKUP FILE")
            .assertIsDisplayed()
            .performClick()

        assertTrue(validationRequested)
    }

    @Test
    fun settingsScreen_restoreBackupButtonInvokesCallback() {
        var restoreRequested = false

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = {},
                    onRestoreBackupClick = {
                        restoreRequested = true
                    }
                )
            }
        }

        composeRule
            .onNodeWithText("RESTORE FROM BACKUP")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertTrue(restoreRequested)
    }
}