package com.aschlus.comicreadingcompanion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appNavigation_homeToBrowse() {
        composeRule.onNodeWithText("Comic Reading Companion").assertIsDisplayed()
        composeRule.onNodeWithText("Browse Comics").performClick()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("Publishers")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Browse Comics").assertIsDisplayed()
        composeRule.onNodeWithText("Publishers").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun appNavigation_browseToPublisher() {
        composeRule.onNodeWithText("Browse Comics").performClick()
        composeRule.waitUntil(timeoutMillis = 10000L) {
            composeRule.onAllNodesWithText("Marvel Comics")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Marvel Comics").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("Marvel Comics")
                .fetchSemanticsNodes()
                .size >= 2
        }
        composeRule.onAllNodesWithText("Marvel Comics")[0].assertIsDisplayed()
        composeRule.onAllNodesWithText("Marvel Comics")[1].assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun appNavigation_publisherToSeries() {
        composeRule.onNodeWithText("Browse Comics").performClick()
        composeRule.waitUntil(timeoutMillis = 10000L) {
            composeRule.onAllNodesWithText("Marvel Comics")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Marvel Comics").performClick()
        composeRule.waitUntil(timeoutMillis = 10000L) {
            composeRule.onAllNodesWithText("Amazing Spider-Man")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Amazing Spider-Man").performClick()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("Amazing Spider-Man")
                .fetchSemanticsNodes()
                .size >= 2
        }
        composeRule.onNodeWithText("Issues").assertIsDisplayed()
        composeRule.onNodeWithText("Volume 2 • 1998-2003").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun appNavigation_seriesToIssue() {
        composeRule.onNodeWithText("Browse Comics").performClick()
        composeRule.waitUntil(timeoutMillis = 10000L) {
            composeRule.onAllNodesWithText("Marvel Comics")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Marvel Comics").performClick()
        composeRule.waitUntil(timeoutMillis = 10000L) {
            composeRule.onAllNodesWithText("Amazing Spider-Man")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Amazing Spider-Man").performClick()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("#1")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("#1").performClick()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("Amazing Spider-Man #1")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Amazing Spider-Man #1").assertIsDisplayed()
        composeRule.onNodeWithText("#1").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun appNavigation_homeToReadingListWithContinue() {
        composeRule.waitUntil(timeoutMillis = 15000L) {
            composeRule.onAllNodesWithText("Spider-Man Volume 2").fetchSemanticsNodes().isNotEmpty()
        }

        val application = composeRule.activity.application
            as ComicReadingCompanionApplication
        val repository = application.container.comicRepository
        var allIssueIds = emptyList<Long>()
        var expectedContinueText = ""
        var expectedCoverDescription = ""

        runBlocking {
            val readingList =
                repository.getReadingLists().first { readingLists ->
                        readingLists.any { readingList ->
                            readingList.title == "Spider-Man Volume 2"
                        }
                    }.first { readingList ->
                        readingList.title == "Spider-Man Volume 2"
                    }

            val issues = repository.getReadingListIssues(readingList.id)
                    .first { issues -> issues.size >= 21 }

            allIssueIds = issues.map { issue -> issue.issueId }

            repository.markIssuesAsUnread(allIssueIds)

            repository.markIssuesAsRead(issues
                .take(20).map { issue -> issue.issueId }
            )

            val continueIssue = issues[20]

            expectedContinueText =
                "Continue: " +
                        "${continueIssue.seriesTitle} " +
                        "#${continueIssue.issueNumber}"

            expectedCoverDescription =
                "${continueIssue.seriesTitle} " +
                "#${continueIssue.issueNumber} cover"
        }

        try {
            composeRule.waitUntil(timeoutMillis = 10000L) {
                composeRule.onAllNodesWithText(expectedContinueText)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onAllNodesWithText(expectedContinueText)[0].assertIsDisplayed()

            composeRule.onNodeWithText("Spider-Man Volume 2").performClick()

            composeRule.waitUntil(timeoutMillis = 10000L) {
                composeRule.onAllNodesWithContentDescription(expectedCoverDescription)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule.onNodeWithContentDescription(expectedCoverDescription)
                .assertIsDisplayed()

        } finally {
            runBlocking {
                repository.markIssuesAsUnread(allIssueIds)
            }
        }
    }

    @Test
    fun appNavigation_backNavigationMainPaths() {
        //Home -> Browse
        composeRule.onNodeWithText("Browse Comics").performClick()
        composeRule.waitUntil(timeoutMillis = 10000L) {
            composeRule.onAllNodesWithText("Marvel Comics")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        //Browse -> Publisher
        composeRule.onNodeWithText("Marvel Comics").performClick()
        composeRule.waitUntil(timeoutMillis = 10000L) {
            composeRule.onAllNodesWithText("Amazing Spider-Man")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        //Publisher -> Series
        composeRule.onNodeWithText("Amazing Spider-Man").performClick()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("#1")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        //Series -> Issue
        composeRule.onNodeWithText("#1").performClick()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("Amazing Spider-Man #1")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        //Issue -> Series
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("Issues")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Volume 2 • 1998-2003").assertIsDisplayed()

        //Series -> Publisher
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("Marvel Comics")
                .fetchSemanticsNodes()
                .size >= 2
        }

        //Publisher -> Browse
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("Publishers")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Browse Comics").assertIsDisplayed()

        //Browse -> Home
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("Comic Reading Companion")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("My Reading Lists").assertIsDisplayed()
    }

    @Test
    fun appNavigation_homeToCreateReadingList() {
        composeRule.onNodeWithText("Create Reading List").performClick()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("Title")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Title").assertIsDisplayed()
        composeRule.onNodeWithText("Select publisher").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun appNavigation_createReadingListCreatesAndOpensDetail() {
        val readingListTile = "Navigation Test List ${System.currentTimeMillis()}"
        val application = composeRule.activity.application as ComicReadingCompanionApplication
        val repository = application.container.comicRepository

        runBlocking {
            repository
                .getPublishersFlow()
                .first { publishers ->
                    publishers.any { publisher ->
                        publisher.name == "Marvel Comics"
                    }
                }

            try {
                composeRule.onNodeWithText("Create Reading List").performClick()
                composeRule.waitUntil(timeoutMillis = 5000L) {
                    composeRule.onAllNodesWithText("Title")
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule.onNodeWithText("Title").performTextInput(readingListTile)
                composeRule.onNodeWithText("Select publisher").performClick()
                composeRule.waitUntil(timeoutMillis = 5000L) {
                    composeRule.onAllNodesWithText("Marvel Comics")
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule.onNodeWithText("Marvel Comics").performClick()
                composeRule.waitForIdle()
                composeRule.onNode(hasText("Create Reading List") and hasClickAction())
                    .performClick()
                composeRule.waitUntil(timeoutMillis = 5000L) {
                    composeRule.onAllNodesWithText("No issues in this reading list")
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule.onNodeWithText(readingListTile).assertIsDisplayed()
                composeRule.onNodeWithText("0 of 0 read • 0% complete").assertIsDisplayed()
                composeRule.onNodeWithText("No issues in this reading list").assertIsDisplayed()
            } finally {
                runBlocking {
                    val createdReadingList =
                        repository
                            .getReadingLists()
                            .first()
                            .firstOrNull { readingList ->
                                readingList.title == readingListTile
                            }

                    if (createdReadingList != null) {
                        repository.deleteReadingList(createdReadingList)
                    }
                }
            }
        }
    }

    @Test
    fun appNavigate_createReadingListBackReturned() {
        composeRule.onNodeWithText("Create Reading List").performClick()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("Title")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("My Reading Lists")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Comic Reading Companion").assertIsDisplayed()
        composeRule.onNodeWithText("My Reading Lists").assertIsDisplayed()
    }

    @Test
    fun appNavigation_deleteReadingListReturnsHomeAndRemovesList() {
        val application =
            composeRule.activity.application
                    as ComicReadingCompanionApplication

        val repository =
            application.container.comicRepository

        val readingListTitle =
            "Navigation Delete Test " +
                    System.currentTimeMillis()

        var readingListId = 0L

        runBlocking {
            val publisher =
                repository
                    .getPublishersFlow()
                    .first { publishers ->
                        publishers.isNotEmpty()
                    }
                    .first()

            readingListId =
                repository.createUserReadingList(
                    title = readingListTitle,
                    description = null,
                    publisherId = publisher.id,
                    universeId = null
                )
        }

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithText(
                    readingListTitle
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText(
                readingListTitle
            )
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithText(
                    "0 of 0 read • 0% complete"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithContentDescription(
                "Reading list options"
            )
            .performClick()

        composeRule
            .onNodeWithText(
                "Delete reading list"
            )
            .performClick()

        composeRule
            .onNodeWithText(
                "Delete reading list?"
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Delete")
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithText(
                    "My Reading Lists"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText(
                "Comic Reading Companion"
            )
            .assertIsDisplayed()

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithText(
                    readingListTitle
                )
                .fetchSemanticsNodes()
                .isEmpty()
        }

        runBlocking {
            assertNull(
                repository.getReadingListById(
                    readingListId
                )
            )
        }
    }
}