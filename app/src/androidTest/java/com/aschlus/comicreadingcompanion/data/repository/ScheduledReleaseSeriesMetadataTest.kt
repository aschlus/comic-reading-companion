
package com.aschlus.comicreadingcompanion.data.repository

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ReleaseSeriesMetadataEntity
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.SeriesExternalId
import com.aschlus.comicreadingcompanion.data.remote.ReleaseDataProvider
import com.aschlus.comicreadingcompanion.data.remote.ReleaseProviderResult
import com.aschlus.comicreadingcompanion.data.remote.ReleaseSeriesData
import com.aschlus.comicreadingcompanion.data.remote.ScheduledReleaseData
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduledReleaseSeriesMetadataTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao

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
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun syncScheduledReleases_automaticallyMapsSeriesUsingComicVineId() =
        runBlocking {

            val seriesId =
                createLocalSeries(
                    title = "Amazing Spider-Man",
                    volume = 2,
                    startYear = 1999
                )

            comicDao.insertSeriesExternalId(
                SeriesExternalId(
                    seriesId = seriesId,
                    source = "COMIC_VINE",
                    externalId = "18693",
                    url = "https://comicvine.gamespot.com/volume/18693/"
                )
            )

            val issueId =
                createLocalIssue(
                    seriesId = seriesId,
                    issueNumber = "30"
                )

            val provider =
                FakeReleaseDataProvider(
                    scheduledReleases = listOf(
                        createRelease(
                            externalIssueId = "300",
                            externalSeriesId = "45",
                            issueNumber = "30"
                        )
                    ),
                    seriesDetailsResults = mutableMapOf(
                        "45" to ReleaseProviderResult.Success(
                            createSeriesDetails(
                                externalSeriesId = "45",
                                comicVineId = "18693"
                            )
                        )
                    )
                )

            val summary = sync(provider)

            assertEquals(1, summary.fetchedCount)
            assertEquals(1, summary.linkedCount)
            assertEquals(0, summary.alreadyLinkedCount)
            assertEquals(0, summary.unmatchedCount)
            assertEquals(0, summary.ambiguousMatchCount)
            assertEquals(0, summary.conflictingExternalIdCount)

            val metronSeriesLink =
                comicDao.getSeriesExternalId(
                    source = "METRON",
                    externalId = "45"
                )

            assertNotNull(metronSeriesLink)

            assertEquals(
                seriesId,
                metronSeriesLink?.seriesId
            )

            assertEquals(
                "https://metron.cloud/series/45/",
                metronSeriesLink?.url
            )

            val metronIssueLink =
                comicDao.getExternalId(
                    source = "METRON",
                    externalId = "300"
                )

            assertNotNull(metronIssueLink)

            assertEquals(
                issueId,
                metronIssueLink?.issueId
            )

            val cachedMetadata =
                comicDao.getReleaseSeriesMetadata(
                    providerId = "METRON",
                    externalSeriesId = "45"
                )

            assertNotNull(cachedMetadata)

            assertEquals(
                "18693",
                cachedMetadata?.comicVineId
            )

            assertEquals(
                "621",
                cachedMetadata?.gcdId
            )

            assertEquals(
                123456L,
                cachedMetadata?.lastFetchedAt
            )

            assertEquals(
                listOf("45"),
                provider.seriesDetailsRequests
            )
        }

    @Test
    fun syncScheduledReleases_reusesCachedMetadataAfterCatalogIsImported() =
        runBlocking {

            val releases = listOf(
                createRelease(
                    externalIssueId = "300",
                    externalSeriesId = "45",
                    issueNumber = "30"
                ),
                createRelease(
                    externalIssueId = "301",
                    externalSeriesId = "45",
                    issueNumber = "31"
                )
            )

            val provider =
                FakeReleaseDataProvider(
                    scheduledReleases = releases,
                    seriesDetailsResults = mutableMapOf(
                        "45" to ReleaseProviderResult.Success(
                            createSeriesDetails(
                                externalSeriesId = "45",
                                comicVineId = "18693"
                            )
                        )
                    )
                )

            // First sync has no corresponding local series.
            // It should fetch and cache metadata, but leave
            // both scheduled issues unmatched.
            val firstSummary = sync(provider)

            assertEquals(2, firstSummary.fetchedCount)
            assertEquals(0, firstSummary.linkedCount)
            assertEquals(2, firstSummary.unmatchedCount)

            assertEquals(
                listOf("45"),
                provider.seriesDetailsRequests
            )

            val cachedMetadata =
                comicDao.getReleaseSeriesMetadata(
                    providerId = "METRON",
                    externalSeriesId = "45"
                )

            assertNotNull(cachedMetadata)
            assertEquals("18693", cachedMetadata?.comicVineId)

            // Simulate importing the corresponding Comic Vine
            // series and its issues after the first sync.
            val seriesId =
                createLocalSeries(
                    title = "Amazing Spider-Man",
                    volume = 2,
                    startYear = 1999
                )

            comicDao.insertSeriesExternalId(
                SeriesExternalId(
                    seriesId = seriesId,
                    source = "COMIC_VINE",
                    externalId = "18693",
                    url = "https://comicvine.gamespot.com/volume/18693/"
                )
            )

            val firstIssueId =
                createLocalIssue(
                    seriesId = seriesId,
                    issueNumber = "30"
                )

            val secondIssueId =
                createLocalIssue(
                    seriesId = seriesId,
                    issueNumber = "31"
                )

            // Second sync should use the cached metadata.
            // It must not make another Metron series-detail call.
            val secondSummary = sync(provider)

            assertEquals(2, secondSummary.fetchedCount)
            assertEquals(2, secondSummary.linkedCount)
            assertEquals(0, secondSummary.unmatchedCount)

            assertEquals(
                listOf("45"),
                provider.seriesDetailsRequests
            )

            val metronSeriesLink =
                comicDao.getSeriesExternalId(
                    source = "METRON",
                    externalId = "45"
                )

            assertNotNull(metronSeriesLink)

            assertEquals(
                seriesId,
                metronSeriesLink?.seriesId
            )

            assertEquals(
                firstIssueId,
                comicDao.getExternalId(
                    source = "METRON",
                    externalId = "300"
                )?.issueId
            )

            assertEquals(
                secondIssueId,
                comicDao.getExternalId(
                    source = "METRON",
                    externalId = "301"
                )?.issueId
            )
        }

    @Test
    fun syncScheduledReleases_cachesMetadataWhenComicVineIdIsMissing() =
        runBlocking {

            val provider =
                FakeReleaseDataProvider(
                    scheduledReleases = listOf(
                        createRelease(
                            externalIssueId = "300",
                            externalSeriesId = "45",
                            issueNumber = "30"
                        )
                    ),
                    seriesDetailsResults = mutableMapOf(
                        "45" to ReleaseProviderResult.Success(
                            createSeriesDetails(
                                externalSeriesId = "45",
                                comicVineId = null
                            )
                        )
                    )
                )

            val firstSummary = sync(provider)

            assertEquals(1, firstSummary.unmatchedCount)

            val cachedMetadata =
                comicDao.getReleaseSeriesMetadata(
                    providerId = "METRON",
                    externalSeriesId = "45"
                )

            assertNotNull(cachedMetadata)
            assertNull(cachedMetadata?.comicVineId)
            assertEquals("621", cachedMetadata?.gcdId)

            // A second sync should reuse the successful response,
            // including the fact that no Comic Vine ID was present.
            val secondSummary = sync(provider)

            assertEquals(1, secondSummary.unmatchedCount)

            assertEquals(
                listOf("45"),
                provider.seriesDetailsRequests
            )
        }

    @Test
    fun syncScheduledReleases_doesNotOverwriteConflictingSeriesMapping() =
        runBlocking {

            val existingMetronSeriesId =
                createLocalSeries(
                    title = "Amazing Spider-Man",
                    volume = 2,
                    startYear = 1999
                )

            val comicVineSeriesId =
                createLocalSeries(
                    title = "Spectacular Spider-Man",
                    volume = 1,
                    startYear = 1976
                )

            createLocalIssue(
                seriesId = existingMetronSeriesId,
                issueNumber = "30"
            )

            comicDao.insertSeriesExternalId(
                SeriesExternalId(
                    seriesId = existingMetronSeriesId,
                    source = "METRON",
                    externalId = "45",
                    url = "https://metron.cloud/series/45/"
                )
            )

            comicDao.insertSeriesExternalId(
                SeriesExternalId(
                    seriesId = comicVineSeriesId,
                    source = "COMIC_VINE",
                    externalId = "18693",
                    url = "https://comicvine.gamespot.com/volume/18693/"
                )
            )

            // Simulate metadata previously fetched and cached.
            comicDao.upsertReleaseSeriesMetadata(
                listOf(
                    ReleaseSeriesMetadataEntity(
                        providerId = "METRON",
                        externalSeriesId = "45",
                        comicVineId = "18693",
                        gcdId = "621",
                        sourceUrl = "https://metron.cloud/series/45/",
                        sourceModifiedAt = "2026-10-20T08:00:00Z",
                        lastFetchedAt = 1000L
                    )
                )
            )

            val provider =
                FakeReleaseDataProvider(
                    scheduledReleases = listOf(
                        createRelease(
                            externalIssueId = "300",
                            externalSeriesId = "45",
                            issueNumber = "30"
                        )
                    ),
                    seriesDetailsResults = mutableMapOf()
                )

            val summary = sync(provider)

            assertEquals(0, summary.linkedCount)
            assertEquals(1, summary.conflictingExternalIdCount)

            val metronSeriesLink =
                comicDao.getSeriesExternalId(
                    source = "METRON",
                    externalId = "45"
                )

            assertNotNull(metronSeriesLink)

            // The original Metron mapping must remain unchanged.
            assertEquals(
                existingMetronSeriesId,
                metronSeriesLink?.seriesId
            )

            // The issue must not be linked through the conflicting
            // Comic Vine series identifier.
            assertNull(
                comicDao.getExternalId(
                    source = "METRON",
                    externalId = "300"
                )
            )

            assertTrue(
                provider.seriesDetailsRequests.isEmpty()
            )
        }

    @Test
    fun syncScheduledReleases_retriesFailedMetadataLookupWithoutLosingRelease() =
        runBlocking {

            val provider =
                FakeReleaseDataProvider(
                    scheduledReleases = listOf(
                        createRelease(
                            externalIssueId = "300",
                            externalSeriesId = "45",
                            issueNumber = "30"
                        )
                    ),
                    seriesDetailsResults = mutableMapOf(
                        "45" to ReleaseProviderResult.NetworkError
                    )
                )

            // The failed metadata request must not prevent
            // the scheduled release from being saved.
            val firstSummary = sync(provider)

            assertEquals(1, firstSummary.fetchedCount)
            assertEquals(1, firstSummary.unmatchedCount)

            assertNotNull(
                comicDao.getScheduledRelease(
                    providerId = "METRON",
                    externalIssueId = "300"
                )
            )

            assertNull(
                comicDao.getReleaseSeriesMetadata(
                    providerId = "METRON",
                    externalSeriesId = "45"
                )
            )

            // Import the matching local series before retrying.
            val seriesId =
                createLocalSeries(
                    title = "Amazing Spider-Man",
                    volume = 2,
                    startYear = 1999
                )

            comicDao.insertSeriesExternalId(
                SeriesExternalId(
                    seriesId = seriesId,
                    source = "COMIC_VINE",
                    externalId = "18693",
                    url = "https://comicvine.gamespot.com/volume/18693/"
                )
            )

            val issueId =
                createLocalIssue(
                    seriesId = seriesId,
                    issueNumber = "30"
                )

            // The next lookup succeeds.
            provider.seriesDetailsResults["45"] =
                ReleaseProviderResult.Success(
                    createSeriesDetails(
                        externalSeriesId = "45",
                        comicVineId = "18693"
                    )
                )

            val secondSummary = sync(provider)

            assertEquals(1, secondSummary.linkedCount)

            assertEquals(
                listOf("45", "45"),
                provider.seriesDetailsRequests
            )

            assertEquals(
                issueId,
                comicDao.getExternalId(
                    source = "METRON",
                    externalId = "300"
                )?.issueId
            )

            assertNotNull(
                comicDao.getReleaseSeriesMetadata(
                    providerId = "METRON",
                    externalSeriesId = "45"
                )
            )
        }

    private suspend fun sync(
        provider: FakeReleaseDataProvider
    ): ScheduledReleaseSyncSummary {

        val repository =
            ScheduledReleaseRepository(
                comicDao = comicDao,
                database = database,
                releaseDataProvider = provider,
                currentTimeMillis = { 123456L }
            )

        val result =
            repository.syncScheduledReleases(
                accessToken = "test-token",
                fromDate = LocalDate.parse("2026-10-01"),
                toDate = LocalDate.parse("2026-10-31")
            )

        assertTrue(
            result is ReleaseProviderResult.Success
        )

        return (result as ReleaseProviderResult.Success).value
    }

    private suspend fun createLocalSeries(
        title: String,
        volume: Int?,
        startYear: Int?
    ): Long {

        val existingPublisher =
            comicDao.getPublisherByName("Marvel")

        val publisherId =
            existingPublisher?.id
                ?: comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

        return comicDao.insertSeries(
            Series(
                publisherId = publisherId,
                title = title,
                volume = volume,
                startYear = startYear,
                endYear = null
            )
        )
    }

    private suspend fun createLocalIssue(
        seriesId: Long,
        issueNumber: String
    ): Long {
        return comicDao.insertIssue(
            Issue(
                seriesId = seriesId,
                universeId = null,
                issueNumber = issueNumber,
                title = "Test issue $issueNumber",
                publicationDate = null,
                coverUrl = null,
                description = null,
                issueType = IssueType.REGULAR,
                isIssueTypeManuallyOverridden = false
            )
        )
    }

    private fun createRelease(
        externalIssueId: String,
        externalSeriesId: String,
        issueNumber: String
    ): ScheduledReleaseData {
        return ScheduledReleaseData(
            providerId = "METRON",
            externalIssueId = externalIssueId,
            externalSeriesId = externalSeriesId,
            seriesTitle = "Amazing Spider-Man",
            seriesVolume = 2,
            seriesStartYear = 1999,
            publisherName = "Marvel",
            issueNumber = issueNumber,
            displayTitle = "Coming Home",
            coverDate = "2026-10",
            storeReleaseDate = "2026-10-21",
            focDate = "2026-10-06",
            price = "4.99",
            priceCurrency = "USD",
            distributorSku = "METRON-SKU-$externalIssueId",
            upc = null,
            comicVineId = null,
            gcdId = null,
            sourceUrl = "https://metron.cloud/issue/$externalIssueId/",
            sourceModifiedAt = "2026-10-20T08:00:00Z"
        )
    }

    private fun createSeriesDetails(
        externalSeriesId: String,
        comicVineId: String?
    ) = ReleaseSeriesData(
        providerId = "METRON",
        externalSeriesId = externalSeriesId,
        comicVineId = comicVineId,
        gcdId = "621",
        sourceUrl = "https://metron.cloud/series/$externalSeriesId/",
        sourceModifiedAt = "2026-10-20T08:00:00Z"
    )

    private class FakeReleaseDataProvider(
        private val scheduledReleases: List<ScheduledReleaseData>,
        val seriesDetailsResults:
        MutableMap<String, ReleaseProviderResult<ReleaseSeriesData>>
    ) : ReleaseDataProvider {

        override val providerId: String = "METRON"

        val seriesDetailsRequests =
            mutableListOf<String>()

        override suspend fun validateConnection(
            accessToken: String
        ): ReleaseProviderResult<Unit> {
            return ReleaseProviderResult.Success(Unit)
        }

        override suspend fun getScheduledReleases(
            accessToken: String,
            fromDate: LocalDate,
            toDate: LocalDate
        ): ReleaseProviderResult<List<ScheduledReleaseData>> {
            return ReleaseProviderResult.Success(
                scheduledReleases
            )
        }

        override suspend fun getReleaseDetails(
            accessToken: String,
            externalIssueId: String
        ): ReleaseProviderResult<ScheduledReleaseData> {
            return ReleaseProviderResult.ServiceError(
                "This test does not use release details."
            )
        }

        override suspend fun getSeriesDetails(
            accessToken: String,
            externalSeriesId: String
        ): ReleaseProviderResult<ReleaseSeriesData> {

            seriesDetailsRequests += externalSeriesId

            return seriesDetailsResults[externalSeriesId]
                ?: ReleaseProviderResult.ServiceError(
                    "No series-detail response was configured."
                )
        }
    }
}
