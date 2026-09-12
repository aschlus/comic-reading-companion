package com.aschlus.comicreadingcompanion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class AppNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appNavigation_homeToBrowse() {
        composeRule
            .onNodeWithText("Comic Reading Companion")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("BROWSE COMICS")
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithText("Publishers")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText("Browse Comics")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Publishers")
            .assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("Back")
            .assertDoesNotExist()
    }

    @Test
    fun appNavigation_browseToPublisher() {
        composeRule.onNodeWithText("BROWSE COMICS").performClick()
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
        composeRule
            .onNodeWithText("BROWSE COMICS")
            .performClick()
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
        composeRule
            .onNodeWithText("BROWSE COMICS")
            .performClick()
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
        val application =
            composeRule.activity.application
                    as ComicReadingCompanionApplication

        val repository =
            application.container.comicRepository

        val homeUiPreferences =
            application.container.homeUiPreferences

        var allIssueIds =
            emptyList<Long>()

        var expectedNextUpText = ""

        var expectedCoverDescription = ""

        runBlocking {
            homeUiPreferences
                .clearRecentlyOpenedReadingLists()

            val readingList =
                repository
                    .getReadingLists()
                    .first { readingLists ->
                        readingLists.any {
                            it.title ==
                                    "Spider-Man Volume 2"
                        }
                    }
                    .first {
                        it.title ==
                                "Spider-Man Volume 2"
                    }

            val issues =
                repository
                    .getReadingListIssues(
                        readingList.id
                    )
                    .first {
                        it.size >= 21
                    }

            allIssueIds =
                issues.map {
                    it.issueId
                }

            repository.markIssuesAsUnread(
                allIssueIds
            )

            repository.markIssuesAsRead(
                issues
                    .take(20)
                    .map {
                        it.issueId
                    }
            )

            val continueIssue =
                issues[20]

            expectedNextUpText =
                "Next up: " +
                        "${continueIssue.seriesTitle} " +
                        "#${continueIssue.issueNumber}"

            expectedCoverDescription =
                "${continueIssue.seriesTitle} " +
                        "#${continueIssue.issueNumber} cover"
        }

        try {
            composeRule.waitUntil(
                timeoutMillis = 10000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        expectedNextUpText
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithText(
                    expectedNextUpText
                )
                .assertIsDisplayed()

            composeRule
                .onNodeWithText(
                    "Spider-Man Volume 2"
                )
                .performClick()

            composeRule.waitUntil(
                timeoutMillis = 10000L
            ) {
                composeRule
                    .onAllNodesWithContentDescription(
                        expectedCoverDescription
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithContentDescription(
                    expectedCoverDescription
                )
                .assertIsDisplayed()
        } finally {
            runBlocking {
                repository.markIssuesAsUnread(
                    allIssueIds
                )

                homeUiPreferences
                    .clearRecentlyOpenedReadingLists()
            }
        }
    }

    @Test
    fun appNavigation_backNavigationMainPaths() {
        // Home -> Browse
        composeRule
            .onNodeWithText("BROWSE COMICS")
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 10000L
        ) {
            composeRule
                .onAllNodesWithText(
                    "Marvel Comics"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Browse -> Publisher
        composeRule
            .onNodeWithText(
                "Marvel Comics"
            )
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 10000L
        ) {
            composeRule
                .onAllNodesWithText(
                    "Amazing Spider-Man"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Publisher -> Series
        composeRule
            .onNodeWithText(
                "Amazing Spider-Man"
            )
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithText("#1")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Series -> Issue
        composeRule
            .onNodeWithText("#1")
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithText(
                    "Amazing Spider-Man #1"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Issue -> Series
        composeRule
            .onNodeWithContentDescription("Back")
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithText("Issues")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText(
                "Volume 2 • 1998-2003"
            )
            .assertIsDisplayed()

        // Series -> Publisher
        composeRule
            .onNodeWithContentDescription("Back")
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithText(
                    "Marvel Comics"
                )
                .fetchSemanticsNodes()
                .size >= 2
        }

        // Publisher -> Browse
        composeRule
            .onNodeWithContentDescription("Back")
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithText("Publishers")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText("Browse Comics")
            .assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("Back")
            .assertDoesNotExist()

        // Browse -> Home via top-level navigation
        composeRule
            .onNodeWithContentDescription(
                "Home tab"
            )
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithText(
                    "Comic Reading Companion"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText(
                "BROWSE COMICS"
            )
            .assertIsDisplayed()
    }

    @Test
    fun appNavigation_homeToCreateReadingList() {
        composeRule.onNodeWithText("CREATE READING LIST").performClick()
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
                composeRule.onNodeWithText("CREATE READING LIST").performClick()
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
    fun appNavigation_createReadingListBackReturned() {
        composeRule
            .onNodeWithText(
                "CREATE READING LIST"
            )
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithText("Title")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithContentDescription("Back")
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithText(
                    "Comic Reading Companion"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText(
                "Comic Reading Companion"
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "BROWSE COMICS"
            )
            .assertIsDisplayed()
    }

    @Test
    fun appNavigation_deleteReadingListReturnsLibraryAndRemovesList() {
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

        try {
            composeRule
                .onNodeWithContentDescription(
                    "Library tab"
                )
                .performClick()

            composeRule.waitUntil(
                timeoutMillis = 10000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        "READING LISTS"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNode(
                    hasScrollAction()
                )
                .performScrollToNode(
                    hasText(
                        readingListTitle
                    )
                )

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
                    .onAllNodesWithContentDescription(
                        "Library tab"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithContentDescription(
                    "Library tab"
                )
                .assertIsDisplayed()

            runBlocking {
                assertNull(
                    repository.getReadingListById(
                        readingListId
                    )
                )
            }
        } finally {
            runBlocking {
                val remainingList =
                    repository.getReadingListById(
                        readingListId
                    )

                if (remainingList != null) {
                    repository.deleteReadingList(
                        remainingList
                    )
                }
            }
        }
    }

    @Test
    fun appNavigation_openingReadingListRecordsRecentlyOpened() {
        val application =
            composeRule.activity.application
                    as ComicReadingCompanionApplication

        val repository =
            application.container.comicRepository

        val homeUiPreferences =
            application.container.homeUiPreferences

        var readingListId = -1L

        runBlocking {
            homeUiPreferences
                .clearRecentlyOpenedReadingLists()

            readingListId =
                repository
                    .getReadingLists()
                    .first { readingLists ->
                        readingLists.any {
                            it.title ==
                                    "Spider-Man Volume 2"
                        }
                    }
                    .first {
                        it.title ==
                                "Spider-Man Volume 2"
                    }
                    .id
        }

        try {
            composeRule
                .onNodeWithContentDescription(
                    "Library tab"
                )
                .performClick()

            composeRule.waitUntil(
                timeoutMillis = 10000L
            ) {
                composeRule
                    .onAllNodesWithText(
                        "READING LISTS"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNode(
                    hasScrollAction()
                )
                .performScrollToNode(
                    hasText(
                        "Spider-Man Volume 2"
                    )
                )

            composeRule
                .onNodeWithText(
                    "Spider-Man Volume 2"
                )
                .performClick()

            composeRule.waitUntil(
                timeoutMillis = 10000L
            ) {
                composeRule
                    .onAllNodesWithContentDescription(
                        "Back"
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            runBlocking {
                val recentIds =
                    kotlinx.coroutines
                        .withTimeout(
                            5000L.milliseconds
                        ) {
                            homeUiPreferences
                                .recentlyOpenedReadingListIds
                                .first { ids ->
                                    ids.firstOrNull() ==
                                            readingListId
                                }
                        }

                org.junit.Assert.assertEquals(
                    readingListId,
                    recentIds.first()
                )
            }
        } finally {
            runBlocking {
                homeUiPreferences
                    .clearRecentlyOpenedReadingLists()
            }
        }
    }

    @Test
    fun appNavigation_bottomNavigationMovesBetweenTopLevelScreens() {
        composeRule
            .onNodeWithContentDescription(
                "Browse tab"
            )
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 10000L
        ) {
            composeRule
                .onAllNodesWithText(
                    "Publishers"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText(
                "Publishers"
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription(
                "Back"
            )
            .assertDoesNotExist()

        composeRule
            .onNodeWithContentDescription(
                "Library tab"
            )
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithText(
                    "READING LISTS"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText(
                "READING LISTS"
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription(
                "Home tab"
            )
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithText(
                    "Comic Reading Companion"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText(
                "Comic Reading Companion"
            )
            .assertIsDisplayed()
    }

    @Test
    fun appNavigation_bottomNavigationHidesOnDetailAndReturnsOnBack() {
        composeRule
            .onNodeWithContentDescription(
                "Library tab"
            )
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 10000L
        ) {
            composeRule
                .onAllNodesWithText(
                    "Spider-Man Volume 2"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText(
                "Spider-Man Volume 2"
            )
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 10000L
        ) {
            composeRule
                .onAllNodesWithContentDescription(
                    "Back"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithContentDescription(
                "Home tab"
            )
            .assertDoesNotExist()

        composeRule
            .onNodeWithContentDescription(
                "Browse tab"
            )
            .assertDoesNotExist()

        composeRule
            .onNodeWithContentDescription(
                "Library tab"
            )
            .assertDoesNotExist()

        composeRule
            .onNodeWithContentDescription(
                "Back"
            )
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeRule
                .onAllNodesWithContentDescription(
                    "Library tab"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithContentDescription(
                "Library tab"
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "READING LISTS"
            )
            .assertIsDisplayed()
    }
}