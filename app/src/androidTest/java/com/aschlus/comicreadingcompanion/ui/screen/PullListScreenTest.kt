
package com.aschlus.comicreadingcompanion.ui.screen

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.viewModelScope
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ScheduledReleaseEntity
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.SeriesExternalId
import com.aschlus.comicreadingcompanion.data.preferences.MetronApiTokenStore
import com.aschlus.comicreadingcompanion.data.remote.MetronApiClient
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import com.aschlus.comicreadingcompanion.data.repository.ScheduledReleaseRepository
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import com.aschlus.comicreadingcompanion.ui.viewmodel.PullListViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PullListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var scheduledReleaseRepository: ScheduledReleaseRepository
    private lateinit var metronApiTokenStore: MetronApiTokenStore
    private lateinit var viewModel: PullListViewModel

    @Before
    fun setUp() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        database =
            Room.inMemoryDatabaseBuilder(
                context,
                ComicDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()

        comicDao = database.comicDao()

        repository =
            ComicRepository(
                comicDao = comicDao,
                database = database
            )

        scheduledReleaseRepository =
            ScheduledReleaseRepository(
                comicDao = comicDao,
                database = database,
                releaseDataProvider = MetronApiClient()
            )

        metronApiTokenStore =
            MetronApiTokenStore(context)

        viewModel =
            PullListViewModel(
                repository = repository,
                scheduledReleaseRepository = scheduledReleaseRepository,
                metronApiTokenStore = metronApiTokenStore,
                currentDate = {
                    LocalDate.parse("2026-10-31")
                }
            )
    }

    @After
    fun tearDown() {
        runBlocking {
            viewModel
                .viewModelScope
                .coroutineContext[Job]
                ?.cancelAndJoin()
        }

        database.close()
    }

    @Test
    fun pullListScreen_displaysEmptyStatesAndActions() {
        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                PullListScreen(
                    viewModel = viewModel,
                    onBrowseClick = {},
                    onSeriesClick = {},
                    onMetronSettingsClick = {}
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule
                .onAllNodesWithText(
                    "Your personal pull list is empty. " +
                        "Add ongoing series to start following " +
                        "their upcoming issues."
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
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
            .onNodeWithText("REFRESH RELEASES")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("METRON SETTINGS")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("UPCOMING STORE RELEASES")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "No upcoming store releases are cached " +
                    "for your tracked series. Refresh to check Metron."
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("FINAL ORDER CUTOFFS")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "No upcoming FOC deadlines are cached " +
                    "for your tracked series."
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
                    viewModel = viewModel,
                    onBrowseClick = {
                        clicked = true
                    },
                    onSeriesClick = {},
                    onMetronSettingsClick = {}
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

    @Test
    fun pullListScreen_metronSettingsActionInvokesCallback() {
        var clicked = false

        composeRule.setContent {
            ComicReadingCompanionTheme(
                dynamicColor = false
            ) {
                PullListScreen(
                    viewModel = viewModel,
                    onBrowseClick = {},
                    onSeriesClick = {},
                    onMetronSettingsClick = {
                        clicked = true
                    }
                )
            }
        }

        composeRule
            .onNodeWithText("METRON SETTINGS")
            .performClick()

        composeRule.runOnIdle {
            assertTrue(clicked)
        }
    }

    @Test
    fun pullListScreen_displaysTrackedSeriesAndScheduledReleases() =
        runBlocking {

            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel Comics")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Amazing Spider-Man",
                        volume = 2,
                        startYear = 1999,
                        endYear = 2003
                    )
                )

            comicDao.insertIssue(
                Issue(
                    seriesId = seriesId,
                    universeId = null,
                    issueNumber = "1",
                    title = "Test Issue 1",
                    publicationDate = "1999-01",
                    coverUrl = null,
                    description = null,
                    issueType = IssueType.REGULAR
                )
            )

            comicDao.insertIssue(
                Issue(
                    seriesId = seriesId,
                    universeId = null,
                    issueNumber = "2",
                    title = "Test Issue 2",
                    publicationDate = "1999-02",
                    coverUrl = null,
                    description = null,
                    issueType = IssueType.REGULAR
                )
            )

            repository.addSeriesToPullList(seriesId)

            comicDao.insertSeriesExternalId(
                SeriesExternalId(
                    seriesId = seriesId,
                    source = "METRON",
                    externalId = "500",
                    url = null
                )
            )

            comicDao.upsertScheduledReleases(
                listOf(
                    ScheduledReleaseEntity(
                        providerId = "METRON",
                        externalIssueId = "201",
                        externalSeriesId = "500",
                        seriesTitleSnapshot = "Amazing Spider-Man",
                        seriesVolumeSnapshot = 2,
                        seriesStartYearSnapshot = 1999,
                        publisherNameSnapshot = "Marvel Comics",
                        issueNumberSnapshot = "1",
                        displayTitleSnapshot = "A New Beginning",
                        coverDate = "2026-11-01",
                        storeReleaseDate = "2026-11-05",
                        focDate = "2026-11-03",
                        price = "4.99",
                        priceCurrency = "USD",
                        distributorSku = null,
                        upc = null,
                        comicVineId = null,
                        gcdId = null,
                        sourceUrl = null,
                        sourceModifiedAt = null,
                        lastSyncedAt = 1000L
                    )
                )
            )

            var clickedSeriesId: Long? = null

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    PullListScreen(
                        viewModel = viewModel,
                        onBrowseClick = {},
                        onSeriesClick = { id ->
                            clickedSeriesId = id
                        },
                        onMetronSettingsClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule
                    .onAllNodesWithText("Amazing Spider-Man")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithText("Amazing Spider-Man")
                .assertIsDisplayed()

            composeRule
                .onAllNodesWithText("Marvel Comics")
                .onFirst()
                .assertIsDisplayed()

            composeRule
                .onNodeWithText("Volume 2 • 1999-2003")
                .assertIsDisplayed()

            composeRule
                .onNodeWithText("2 ISSUES")
                .assertIsDisplayed()

            composeRule
                .onNodeWithText("UPCOMING STORE RELEASES")
                .assertIsDisplayed()

            composeRule.waitUntil(timeoutMillis = 5000L) {
                viewModel.storeReleaseGroups.value.any {
                    it.date == LocalDate.parse("2026-11-05")
                } &&
                viewModel.focDeadlineGroups.value.any {
                    it.date == LocalDate.parse("2026-11-03")
                }
            }
            composeRule
                .onNodeWithText("Nov 5, 2026")
                .performScrollTo()
                .assertIsDisplayed()

            composeRule
                .onAllNodesWithText("Amazing Spider-Man #1")
                .onFirst()
                .assertIsDisplayed()

            composeRule
                .onAllNodesWithText("A New Beginning")
                .onFirst()
                .assertIsDisplayed()

            composeRule
                .onNodeWithText("FOC Nov 3, 2026")
                .performScrollTo()
                .assertIsDisplayed()

            composeRule
                .onNodeWithText("FINAL ORDER CUTOFFS")
                .performScrollTo()
                .assertIsDisplayed()

            composeRule
                .onNodeWithText("Nov 3, 2026")
                .performScrollTo()
                .assertIsDisplayed()

            composeRule
                .onNodeWithText("STORE DATE Nov 5, 2026")
                .performScrollTo()
                .assertIsDisplayed()

            composeRule
                .onNodeWithText("Amazing Spider-Man")
                .performClick()

            composeRule.runOnIdle {
                assertEquals(seriesId, clickedSeriesId)
            }
        }

    @Test
    fun pullListScreen_updatesWhenTrackedSeriesIsRemoved() {
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel Comics")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Amazing Spider-Man",
                        volume = 2,
                        startYear = 1999,
                        endYear = 2003
                    )
                )

            repository.addSeriesToPullList(seriesId)

            composeRule.setContent {
                ComicReadingCompanionTheme(
                    dynamicColor = false
                ) {
                    PullListScreen(
                        viewModel = viewModel,
                        onBrowseClick = {},
                        onSeriesClick = {},
                        onMetronSettingsClick = {}
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule
                    .onAllNodesWithText("Amazing Spider-Man")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            repository.removeSeriesFromPullList(seriesId)

            composeRule.waitUntil(timeoutMillis = 5000L) {
                composeRule
                    .onAllNodesWithText(
                        "Your personal pull list is empty. " +
                            "Add ongoing series to start following " +
                            "their upcoming issues."
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onNodeWithText(
                    "Your personal pull list is empty. " +
                        "Add ongoing series to start following " +
                        "their upcoming issues."
                )
                .assertIsDisplayed()
        }
    }
}