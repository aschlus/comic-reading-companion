package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComicComponentsTest {

    @get:Rule
    val composeRule =
        createComposeRule()

    @Test
    fun comicSectionBanner_displaysUppercaseText() {
        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                ComicSectionBanner(
                    text = "Continue Reading"
                )
            }
        }

        composeRule
            .onNodeWithText(
                "CONTINUE READING"
            )
            .assertIsDisplayed()
    }

    @Test
    fun comicActionPanel_invokesCallback() {
        var clicked = false

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                ComicActionPanel(
                    title = "Browse Comics",
                    icon =
                        Icons.Default.Search,
                    onClick = {
                        clicked = true
                    }
                )
            }
        }

        composeRule
            .onNodeWithText(
                "BROWSE COMICS"
            )
            .performClick()

        composeRule.runOnIdle {
            assertTrue(clicked)
        }
    }

    @Test
    fun comicFormDialog_cancelDismissesWithoutConfirming() {
        var confirmed = false
        var dismissed = false

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                ComicFormDialog(
                    title = "Add Section",
                    primaryLabel = "Title",
                    primaryValue = "",
                    onPrimaryValueChange = {},
                    secondaryLabel = "Description",
                    secondaryValue = "",
                    onSecondaryValueChange = {},
                    confirmText = "Add",
                    confirmEnabled = false,
                    onConfirm = {
                        confirmed = true
                    },
                    onDismiss = {
                        dismissed = true
                    }
                )
            }
        }

        composeRule
            .onNodeWithText("Cancel")
            .performClick()

        composeRule.runOnIdle {
            assertTrue(dismissed)
            assertFalse(confirmed)
        }
    }

    @Test
    fun comicEmptyState_displaysMessage() {
        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                ComicEmptyState(
                    message = "Nothing here yet"
                )
            }
        }

        composeRule
            .onNodeWithText(
                "Nothing here yet"
            )
            .assertIsDisplayed()
    }
}