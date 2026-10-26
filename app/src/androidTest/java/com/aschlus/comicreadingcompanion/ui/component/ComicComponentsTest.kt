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
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import com.aschlus.comicreadingcompanion.data.database.models.IssueSearchResult
import junit.framework.TestCase.assertEquals

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

    @Test
    fun iconOnlyHeaderActions_exposeButtonRole() {
        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                Column {
                    ComicHomeHeader(
                        onSettingsClick = {}
                    )

                    ComicCreateReadingListHeader(
                        onBackClick = {}
                    )

                    ComicIssueDetailHeader(
                        onBackClick = {}
                    )

                    ComicReadingListDetailHeaderButton(
                        icon = Icons.Default.Search,
                        contentDescription = "Search reading list",
                        onClick = {}
                    )
                }
            }
        }

        val buttonRole =
            SemanticsMatcher.expectValue(
                SemanticsProperties.Role,
                Role.Button
            )

        composeRule
            .onNodeWithContentDescription("Settings")
            .assert(buttonRole)

        composeRule
            .onAllNodesWithContentDescription("Back")
            .assertAll(buttonRole)

        composeRule
            .onNodeWithContentDescription("Search reading list")
            .assert(buttonRole)
    }

    @Test
    fun prominentActionSurfaces_exposeButtonRole() {
        val buttonRole =
            SemanticsMatcher.expectValue(
                SemanticsProperties.Role,
                Role.Button
            )

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                Column {
                    ComicActionPanel(
                        title = "Browse Comics",
                        icon = Icons.Default.Search,
                        onClick = {}
                    )

                    ComicLibraryActionButton(
                        text = "Import List",
                        icon = Icons.Default.Search,
                        backgroundColor =
                            androidx.compose.ui.graphics.Color(0xFF72D2F7),
                        onClick = {}
                    )

                    ComicHomeQuickAccessTile(
                        text = "Quick Browse",
                        icon = Icons.Default.Search,
                        backgroundColor =
                            androidx.compose.ui.graphics.Color(0xFFFFD43B),
                        onClick = {}
                    )

                    ComicAddIssuesTrigger(
                        onClick = {}
                    )
                }
            }
        }

        composeRule
            .onNodeWithText("BROWSE COMICS")
            .assert(buttonRole)

        composeRule
            .onNodeWithText("Import List")
            .assert(buttonRole)

        composeRule
            .onNodeWithText("Quick Browse")
            .assert(buttonRole)

        composeRule
            .onNodeWithText("Search issues or series")
            .assert(buttonRole)
    }

    @Test
    fun libraryReadingListCard_exposesButtonRoleAndInvokesCallback() {
        var clicked = false

        val buttonRole =
            SemanticsMatcher.expectValue(
                SemanticsProperties.Role,
                Role.Button
            )

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                ComicLibraryReadingListCard(
                    title = "Test Reading List",
                    description = "Accessibility test",
                    readCount = 1,
                    totalCount = 5,
                    progress = 0.2f,
                    continueText = null,
                    artworkSeed = 1L,
                    onClick = {
                        clicked = true
                    }
                )
            }
        }

        composeRule
            .onNodeWithText("Test Reading List")
            .assert(buttonRole)
            .performClick()

        composeRule.runOnIdle {
            assertTrue(clicked)
        }
    }

    @Test
    fun comicBrowseResultRow_exposesButtonRoleAndInvokesCallback() {
        var clicked = false

        val buttonRole =
            SemanticsMatcher.expectValue(
                SemanticsProperties.Role,
                Role.Button
            )

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                ComicBrowseResultRow(
                    title = "Test Series",
                    subtitle = "Volume 1 • 2020",
                    supportingText = "1 of 5 read",
                    statusText = null,
                    onClick = {
                        clicked = true
                    }
                )
            }
        }

        composeRule
            .onNodeWithText("Test Series")
            .assert(buttonRole)
            .performClick()

        composeRule.runOnIdle {
            assertTrue(clicked)
        }
    }

    @Test
    fun comicFormDropdownField_exposesButtonRoleAndRequestsExpansion() {
        var requestedExpanded: Boolean? = null

        val buttonRole =
            SemanticsMatcher.expectValue(
                SemanticsProperties.Role,
                Role.Button
            )

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                ComicFormDropdownField(
                    text = "Select publisher",
                    expanded = false,
                    onExpandedChange = {
                        requestedExpanded = it
                    },
                    menuContent = {}
                )
            }
        }

        composeRule
            .onNodeWithText("Select publisher")
            .assert(buttonRole)
            .performClick()

        composeRule.runOnIdle {
            assertEquals(true, requestedExpanded)
        }
    }

    @Test
    fun readingListSelectionHeader_actionsExposeButtonRoles() {
        var readClicked = false
        var unreadClicked = false

        val buttonRole =
            SemanticsMatcher.expectValue(
                SemanticsProperties.Role,
                Role.Button
            )

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                ComicReadingListSelectionHeader(
                    selectedCount = 2,
                    onCancelClick = {},
                    onMarkReadClick = {
                        readClicked = true
                    },
                    onMarkUnreadClick = {
                        unreadClicked = true
                    }
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(
                "Mark selected issues as read"
            )
            .assert(buttonRole)
            .performClick()

        composeRule
            .onNodeWithContentDescription(
                "Mark selected issues as unread"
            )
            .assert(buttonRole)
            .performClick()

        composeRule.runOnIdle {
            assertTrue(readClicked)
            assertTrue(unreadClicked)
        }
    }

    @Test
    fun addToListIssueResult_exposesCheckboxRoleAndTogglesSelection() {
        val selected = mutableStateOf(false)

        val checkboxRole =
            SemanticsMatcher.expectValue(
                SemanticsProperties.Role,
                Role.Checkbox
            )

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                ComicAddToListIssueResult(
                    result = IssueSearchResult(
                        issueId = 1L,
                        seriesId = 2L,
                        seriesTitle = "Test Series",
                        seriesVolume = 1,
                        issueNumber = "1",
                        issueTitle = null,
                        publicationDate = null,
                        publisherName = "Test Publisher",
                        readingStatus = null
                    ),
                    selected = selected.value,
                    onClick = {
                        selected.value = !selected.value
                    }
                )
            }
        }

        composeRule
            .onNodeWithText("Test Series #1")
            .assert(checkboxRole)
            .performClick()

        composeRule.runOnIdle {
            assertTrue(selected.value)
        }
    }

    @Test
    fun comicIssueStatusSelector_exposesButtonRoleAndInvokesCallback() {
        var readingClicked = false

        val buttonRole = SemanticsMatcher.expectValue(
            SemanticsProperties.Role,
            Role.Button
        )

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                ComicIssueStatusSelector(
                    readingStatus = null,
                    onUnreadClick = {},
                    onReadingClick = {
                        readingClicked = true
                    },
                    onReadClick = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Reading reading status")
            .assert(buttonRole)
            .performClick()

        composeRule.runOnIdle {
            assertTrue(readingClicked)
        }
    }
}