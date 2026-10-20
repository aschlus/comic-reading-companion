package com.aschlus.comicreadingcompanion.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import com.aschlus.comicreadingcompanion.data.preferences.ComicVineApiKeyStore
import com.aschlus.comicreadingcompanion.data.remote.ComicVineApiClient
import com.aschlus.comicreadingcompanion.data.remote.ComicVineHttpTransport
import com.aschlus.comicreadingcompanion.data.remote.ComicVineVolumeSearchHit
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import androidx.room3.Room
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.importer.ComicCatalogImporter
import com.aschlus.comicreadingcompanion.data.remote.ComicVineIssueHit

@RunWith(AndroidJUnit4::class)
class ComicVineVolumeReviewViewModelTest {

    private lateinit var apiKeyStore: ComicVineApiKeyStore
    private lateinit var transport: FakeTransport
    private lateinit var viewModel: ComicVineVolumeReviewViewModel
    private var database: ComicDatabase? = null

    @Before
    fun setUp() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        apiKeyStore = ComicVineApiKeyStore(context)

        runBlocking {
            apiKeyStore.clear()
        }

        transport = FakeTransport()

        viewModel = ComicVineVolumeReviewViewModel(
            apiKeyStore = apiKeyStore,
            apiClient = ComicVineApiClient(
                transport = transport
            )
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

        database?.close()
    }

    @Test
    fun load_withoutApiKeyReportsSettingsGuidance() = runBlocking {
        viewModel.load(123L)

        val state = withTimeout(5000L.milliseconds) {
            viewModel.uiState.first { !it.isLoading }
        }

        assertNull(state.volume)
        assertTrue(state.issues.isEmpty())
        assertEquals(
            "Configure your Comic Vine API key in " +
                    "Settings before loading series details.",
            state.error
        )
        assertTrue(transport.requestedUrls.isEmpty())
    }

    @Test
    fun load_fetchesVolumeAndAllIssuePages() = runBlocking {
        apiKeyStore.save("test-api-key")

        transport.responses = listOf(
            """
                {
                    "error": "OK",
                    "status_code": 1,
                    "number_of_total_results": 3,
                    "results": [
                        {
                            "id": 1001,
                            "issue_number": "1",
                            "name": "Origin",
                            "cover_date": "1963-05-01",
                            "store_date": "1963-01-01",
                            "site_detail_url": "https://example.com/issue/1001",
                            "image": {
                                "small_url": "https://example.com/issue1.jpg"
                            }
                        },
                        {
                            "id": 1002,
                            "issue_number": "2",
                            "name": "The Return",
                            "cover_date": "1963-03-01",
                            "store_date": "1963-02-01",
                            "site_detail_url": "https://example.com/issue/1002",
                            "image": {
                                "small_url": "https://example.com/issue2.jpg"
                            }
                        }
                    ]
                }
            """.trimIndent(),
            """
                {
                    "error": "OK",
                    "status_code": 1,
                    "number_of_total_results": 3,
                    "results": [
                        {
                            "id": 1003,
                            "issue_number": "3",
                            "name": "Another Adventure",
                            "cover_date": "1963-04-01",
                            "site_detail_url": "https://example.com/issue/1003"
                        }
                    ]
                }
            """.trimIndent()
        )

        viewModel.load(
            volumeId = 123L,
            selectedVolume = ComicVineVolumeSearchHit(
                id = 123L,
                name = "Amazing Spider-Man",
                startYear = 1963,
                countOfIssues = 3,
                publisherName = "Marvel",
                siteDetailUrl = "https://example.com/volume/123",
                imageUrl = "https://example.com/series.jpg"
            )
        )

        val state = withTimeout(5000L.milliseconds) {
            viewModel.uiState.first {
                !it.isLoading && it.volume != null
            }
        }

        assertNull(state.error)
        assertNotNull(state.volume)
        assertEquals(123L, state.volume!!.id)
        assertEquals("Amazing Spider-Man", state.volume.name)
        assertEquals("Marvel", state.volume.publisherName)
        assertEquals(3, state.volume.countOfIssues)

        assertEquals(3, state.totalIssueRecords)
        assertEquals(3, state.issues.size)
        assertEquals(
            listOf("2", "3", "1"),
            state.issues.map { it.issueNumber }
        )

        assertEquals(
            listOf(1002L, 1003L, 1001L),
            state.issues.map { it.id }
        )

        assertEquals(2, transport.requestedUrls.size)

        assertTrue(
            transport.requestedUrls[0].contains("/issues/")
        )
        assertTrue(
            transport.requestedUrls[0].contains("filter=volume%3A123")
        )
        assertTrue(
            transport.requestedUrls[1].contains("&offset=2")
        )
    }

    @Test
    fun load_retainsPartialIssuesIfLaterPageFails() = runBlocking {
        apiKeyStore.save("test-api-key")

        transport.responses = listOf(
            """
                {
                    "error": "OK",
                    "status_code": 1,
                    "results": {
                        "id": 123,
                        "name": "Test Series",
                        "count_of_issues": 3
                    }
                }
            """.trimIndent(),
            """
                {
                    "error": "OK",
                    "status_code": 1,
                    "number_of_total_results": 3,
                    "results": [
                        {
                            "id": 1001,
                            "issue_number": "1",
                            "name": "Issue One"
                        },
                        {
                            "id": 1002,
                            "issue_number": "2",
                            "name": "Issue Two"
                        }
                    ]
                }
            """.trimIndent()
        )
        transport.failAtRequest = 3

        viewModel.load(123L)

        val state = withTimeout(5000L.milliseconds) {
            viewModel.uiState.first {
                !it.isLoading && it.error != null
            }
        }

        assertNotNull(state.volume)
        assertEquals(2, state.issues.size)
        assertEquals(
            listOf(1001L, 1002L),
            state.issues.map { it.id }
        )
        assertEquals(
            "Could not reach Comic Vine. " +
                    "Check your connection and try again.",
            state.error
        )
    }

    @Test
    fun load_invalidVolumeIdDoesNotRequestData() {
        viewModel.load(0L)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(
            "The selected series has an invalid source ID.",
            viewModel.uiState.value.error
        )
        assertTrue(transport.requestedUrls.isEmpty())
    }

    @Test
    fun availableUniversesArePublishedToUiState() = runBlocking {
        val universe = Universe(
            id = 1L,
            publisherId = 1L,
            name = "Earth-616",
            designation = "Earth-616",
            description = null
        )

        viewModel = ComicVineVolumeReviewViewModel(
            apiKeyStore = apiKeyStore,
            apiClient = ComicVineApiClient(
                transport = transport
            ),
            universeFlow = flowOf(listOf(universe))
        )

        val state = withTimeout(5000L.milliseconds) {
            viewModel.uiState.first {
                it.availableUniverses == listOf(universe)
            }
        }

        assertEquals(
            listOf(universe),
            state.availableUniverses
        )
    }

    @Test
    fun selectUniverse_updatesSelectedUniverseId() = runBlocking {
        val universe = Universe(
            id = 1L,
            publisherId = 1L,
            name = "Earth-616",
            designation = "Earth-616",
            description = null
        )

        viewModel = ComicVineVolumeReviewViewModel(
            apiKeyStore = apiKeyStore,
            apiClient = ComicVineApiClient(
                transport = transport
            ),
            universeFlow = flowOf(listOf(universe))
        )

        withTimeout(5000L.milliseconds) {
            viewModel.uiState.first {
                it.availableUniverses == listOf(universe)
            }
        }

        viewModel.selectUniverse(universe.id)

        assertEquals(
            universe.id,
            viewModel.uiState.value.selectedUniverseId
        )
    }

    @Test
    fun createUniverse_createsPublisherAndUniverseAndSelectsIt() =
        runBlocking {
            val context =
                ApplicationProvider.getApplicationContext<Context>()

            val testDatabase =
                Room.inMemoryDatabaseBuilder(
                    context,
                    ComicDatabase::class.java
                )
                    .allowMainThreadQueries()
                    .build()

            database = testDatabase

            val repository =
                ComicRepository(
                    comicDao = testDatabase.comicDao(),
                    database = testDatabase
                )

            apiKeyStore.save("test-api-key")

            transport.responses = listOf(
                """
                {
                    "error": "OK",
                    "status_code": 1,
                    "number_of_total_results": 0,
                    "results": []
                }
            """.trimIndent()
            )

            viewModel = ComicVineVolumeReviewViewModel(
                apiKeyStore = apiKeyStore,
                apiClient = ComicVineApiClient(
                    transport = transport
                ),
                universeFlow = repository.getAllUniverses(),
                comicRepository = repository
            )

            viewModel.load(
                volumeId = 123L,
                selectedVolume = ComicVineVolumeSearchHit(
                    id = 123L,
                    name = "Amazing Spider-Man",
                    startYear = 1963,
                    countOfIssues = 0,
                    publisherName = "Marvel",
                    siteDetailUrl = "https://example.com/volume/123",
                    imageUrl = null
                )
            )

            withTimeout(5000L.milliseconds) {
                viewModel.uiState.first {
                    !it.isLoading && it.volume != null
                }
            }

            viewModel.createUniverse(
                name = "Marvel Universe",
                designation = "Earth-616"
            )

            val creationState =
                withTimeout(5000L.milliseconds) {
                    viewModel.uiState.first {
                        !it.isCreatingUniverse &&
                                (
                                        it.selectedUniverseId != null ||
                                                it.universeCreationError != null
                                        )
                    }
                }

            assertNull(creationState.universeCreationError)
            assertNotNull(creationState.selectedUniverseId)

            val publishers = repository.getPublishers()

            assertEquals(
                listOf("Marvel"),
                publishers.map { it.name }
            )

            val publisher = publishers.single()
            val universes =
                repository.getUniverseForPublisher(publisher.id)

            assertEquals(1, universes.size)

            val universe = universes.single()

            assertEquals(publisher.id, universe.publisherId)
            assertEquals("Marvel Universe", universe.name)
            assertEquals("Earth-616", universe.designation)
            assertEquals(
                universe.id,
                creationState.selectedUniverseId
            )

            assertEquals(
                listOf(universe),
                creationState.availableUniverses
            )
        }

    @Test
    fun importReviewedSeries_importsSeriesIssuesAndComicVineIds() =
        runBlocking {
            val context =
                ApplicationProvider.getApplicationContext<Context>()

            val testDatabase =
                Room.inMemoryDatabaseBuilder(
                    context,
                    ComicDatabase::class.java
                )
                    .allowMainThreadQueries()
                    .build()

            database = testDatabase

            val repository =
                ComicRepository(
                    comicDao = testDatabase.comicDao(),
                    database = testDatabase
                )

            val publisherId =
                repository.addPublisher(
                    Publisher(name = "Marvel Comics")
                )

            val universe =
                Universe(
                    publisherId = publisherId,
                    name = "Marvel Universe",
                    designation = "Earth-616",
                    description = null
                )

            val universeId = repository.addUniverse(universe)

            apiKeyStore.save("test-api-key")

            transport.responses = listOf(
                """
                {
                    "error": "OK",
                    "status_code": 1,
                    "number_of_total_results": 2,
                    "results": [
                        {
                            "id": 1001,
                            "issue_number": "1",
                            "name": "Origin",
                            "cover_date": "1963-03-01",
                            "store_date": "1963-01-01",
                            "site_detail_url": "https://example.com/issue/1001",
                            "image": {
                                "small_url": "https://example.com/issue1.jpg"
                            }
                        },
                        {
                            "id": 1002,
                            "issue_number": "2",
                            "name": "The Return Annual",
                            "cover_date": "1963-04-01",
                            "site_detail_url": "https://example.com/issue/1002",
                            "image": {
                                "small_url": "https://example.com/issue2.jpg"
                            }
                        }
                    ]
                }
            """.trimIndent()
            )

            viewModel = ComicVineVolumeReviewViewModel(
                apiKeyStore = apiKeyStore,
                apiClient = ComicVineApiClient(
                    transport = transport
                ),
                comicRepository = repository,
                universeFlow = repository.getAllUniverses(),
                comicCatalogImporter = ComicCatalogImporter(
                    comicDao = testDatabase.comicDao(),
                    database = testDatabase
                )
            )

            withTimeout(5000L.milliseconds) {
                viewModel.uiState.first {
                    it.availableUniverses.any { available ->
                        available.id == universeId
                    }
                }
            }

            viewModel.load(
                volumeId = 123L,
                selectedVolume = ComicVineVolumeSearchHit(
                    id = 123L,
                    name = "Amazing Spider-Man",
                    startYear = 1963,
                    countOfIssues = 2,
                    publisherName = "Marvel",
                    siteDetailUrl = "https://example.com/volume/123",
                    imageUrl = "https://example.com/series.jpg"
                )
            )

            withTimeout(5000L.milliseconds) {
                viewModel.uiState.first {
                    !it.isLoading &&
                            it.volume != null &&
                            it.issues.size == 2
                }
            }

            viewModel.updateEditedTitle("The Amazing Spider-Man")
            viewModel.updateEditedPublisher("Marvel Comics")
            viewModel.updateEditedVolume("2")
            viewModel.updateEditedStartYear("1999")
            viewModel.updateEditedEndYear("2003")

            viewModel.selectUniverse(universeId)
            viewModel.importReviewedSeries()

            val completedState =
                withTimeout(5000L.milliseconds) {
                    viewModel.uiState.first {
                        !it.isImporting &&
                                (
                                        it.importSuccessMessage != null ||
                                                it.importError != null
                                        )
                    }
                }

            assertNull(completedState.importError)
            assertNotNull(completedState.importSuccessMessage)
            assertTrue(
                completedState.importSuccessMessage!!.contains("2 issue(s)")
            )

            val seriesExternalId =
                testDatabase.comicDao().getSeriesExternalId(
                    source = "COMIC_VINE",
                    externalId = "123"
                )

            assertNotNull(seriesExternalId)

            val importedSeries =
                testDatabase.comicDao().getSeriesById(
                    seriesExternalId!!.seriesId
                )

            assertNotNull(importedSeries)
            assertEquals("The Amazing Spider-Man", importedSeries?.title)
            assertEquals(publisherId, importedSeries?.publisherId)

            val importedIssues =
                testDatabase.comicDao().getIssuesForSeries(
                    importedSeries!!.id
                )

            assertEquals(2, importedIssues.size)

            val issueOneExternalId =
                testDatabase.comicDao().getExternalId(
                    source = "COMIC_VINE",
                    externalId = "1001"
                )

            val issueTwoExternalId =
                testDatabase.comicDao().getExternalId(
                    source = "COMIC_VINE",
                    externalId = "1002"
                )

            assertNotNull(issueOneExternalId)

            val importedIssueOne =
                testDatabase.comicDao().getIssueById(
                    issueOneExternalId!!.issueId
                )

            val importedIssueTwo =
                testDatabase.comicDao().getIssueById(
                    issueTwoExternalId!!.issueId
                )

            assertNotNull(importedIssueOne)
            assertEquals(2, importedSeries.volume)
            assertEquals(1999, importedSeries.startYear)
            assertEquals(2003, importedSeries.endYear)
            assertEquals("Origin", importedIssueOne?.title)
            assertEquals("1963-03", importedIssueOne?.publicationDate)
            assertEquals(
                "https://example.com/issue1.jpg",
                importedIssueOne?.coverUrl
            )
            assertEquals(universeId, importedIssueOne?.universeId)
            assertNull(importedIssueOne?.description)

            assertEquals(IssueType.REGULAR, importedIssueOne?.issueType)
            assertEquals(IssueType.ANNUAL, importedIssueTwo?.issueType)

            viewModel.selectUniverse(null)
            viewModel.importReviewedSeries()

            val noUniverseState =
                withTimeout(5000L.milliseconds) {
                    viewModel.uiState.first {
                        !it.isImporting &&
                                (
                                        it.importSuccessMessage != null ||
                                                it.importError != null
                                        )
                    }
                }

            assertNull(noUniverseState.importError)
            assertNotNull(noUniverseState.importSuccessMessage)

            val seriesExternalIdAfterReimport =
                testDatabase.comicDao().getSeriesExternalId(
                    source = "COMIC_VINE",
                    externalId = "123"
                )

            assertNotNull(seriesExternalIdAfterReimport)
            assertEquals(
                importedSeries.id,
                seriesExternalIdAfterReimport!!.seriesId
            )

            val issuesAfterReimport =
                testDatabase.comicDao().getIssuesForSeries(importedSeries.id)

            assertEquals(2, issuesAfterReimport.size)

            val issueOneAfterReimport =
                testDatabase.comicDao().getIssueById(importedIssueOne!!.id)

            assertNotNull(issueOneAfterReimport)
            assertNull(issueOneAfterReimport?.universeId)
        }

    private class FakeTransport : ComicVineHttpTransport {

        var responses: List<String> = emptyList()
        var failAtRequest: Int? = null
        val requestedUrls = mutableListOf<String>()

        override suspend fun get(url: String): String {
            requestedUrls.add(url)

            if (requestedUrls.size == failAtRequest) {
                throw IOException("Simulated network failure")
            }

            return responses.getOrNull(requestedUrls.size - 1)
                ?: error("No fake response configured for request ${requestedUrls.size}")
        }
    }


    @Test
    fun issueTypeInference_usesExplicitLabelsAndDefaultsToRegular() {
        fun issue(
            id: Long,
            number: String? = "1",
            name: String? = null
        ) = ComicVineIssueHit(
            id = id,
            issueNumber = number,
            name = name,
            coverDate = null,
            storeDate = null,
            siteDetailUrl = null,
            imageUrl = null
        )

        val cases = listOf(
            issue(1L, name = "Giant-Size X-Men") to "GIANT_SIZE",
            issue(2L, name = "Spider-Man Annual") to "ANNUAL",
            issue(3L, name = "Wolverine: One-Shot") to "ONE_SHOT",
            issue(4L, name = "Official Preview") to "PREVIEW",
            issue(5L, name = "Holiday Special") to "SPECIAL",
            issue(6L, name = "Origin") to "REGULAR",
            issue(7L, name = "The Specialized Spider-Man") to "REGULAR",
            issue(8L, number = "Annual 1998") to "ANNUAL"
        )

        cases.forEach { (comicIssue, expectedType) ->
            assertEquals(
                "Unexpected type for issue ${comicIssue.id}",
                expectedType,
                ComicVineIssueTypeInference.infer(comicIssue)
            )
        }
    }

}