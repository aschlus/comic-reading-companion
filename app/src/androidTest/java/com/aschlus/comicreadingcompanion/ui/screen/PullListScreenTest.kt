
package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PullListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pullListScreen_displaysEmptyStatesAndBrowseAction() {
        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                PullListScreen(
                    onBrowseClick = {}
                )
            }
        }

        composeRule
            .onNodeWithText("PULL LIST")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("TRACKED SERIES")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "Your personal pull list is empty. " +
                    "Add ongoing series to start following " +
                    "their upcoming issues."
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("BROWSE SERIES")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("UPCOMING RELEASES")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "Upcoming store-release dates and " +
                    "Final Order Cutoff deadlines will " +
                    "appear here for the series you follow."
            )
            .assertIsDisplayed()
    }

    @Test
    fun pullListScreen_browseActionInvokesCallback() {
        var clicked = false

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                PullListScreen(
                    onBrowseClick = {
                        clicked = true
                    }
                )
            }
        }

        composeRule
            .onNodeWithText("BROWSE SERIES")
            .performClick()

        composeRule.runOnIdle {
            assertTrue(clicked)
        }
    }
}
