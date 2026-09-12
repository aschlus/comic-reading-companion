package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
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
}