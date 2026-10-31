
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
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.SeriesExternalId
import com.aschlus.comicreadingcompanion.data.remote.ReleaseDataProvider
import com.aschlus.comicreadingcompanion.data.remote.ReleaseProviderResult
import com.aschlus.comicreadingcompanion.data.remote.ReleaseSeriesData
import com.aschlus.comicreadingcompanion.data.remote.ScheduledReleaseData
import java.time.Instant
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
class ScheduledReleaseIncrementalSyncTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao

    private var nowMillis = 123456L

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
    fun syncScheduledReleases_reconcilesStoredReleaseWhenIncrementalResponseIsEmpty() =
        runBlocking {

            val provider =
                FakeIncrementalReleaseDataProvider(
                    scheduledReleaseResults = mutableListOf(
                        ReleaseProviderResult.Success(
                            listOf(
                                createRelease()
                            )
                        ),
                        ReleaseProviderResult.Success(
                            emptyList()
                        )
                    ),
                    seriesDetailsResults = mutableListOf(
                        ReleaseProviderResult.Success(
                            createSeriesDetails()
                        )
                    )
                )

            // The first sync saves the release and caches its series
            // metadata, but there is no local catalog match yet.
            val firstSummary = sync(provider)

            assertEquals(1, firstSummary.fetchedCount)
            assertEquals(1, firstSummary.unmatchedCount)

            assertNotNull(
                comicDao.getScheduledRelease(
                    providerId = "METRON",
                    externalIssueId = "300"
                )
            )

            assertNotNull(
                comicDao.getReleaseSeriesMetadata(
                    providerId = "METRON",
                    externalSeriesId = "45"
                )
            )

            assertNull(provider.modifiedAfterRequests.first())

            // Import the matching Comic Vine series and issue after
            // the initial release-data sync.
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

            // The API returns no changed releases. The repository
            // must still reconcile the previously saved release.
            val secondSummary = sync(provider)

            assertEquals(0, secondSummary.fetchedCount)
            assertEquals(1, secondSummary.linkedCount)
            assertEquals(0, secondSummary.unmatchedCount)

            assertEquals(
                Instant.ofEpochMilli(123456L).minusSeconds(1),
                provider.modifiedAfterRequests[1]
            )

            // The successful metadata response should be reused.
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
            assertEquals(seriesId, metronSeriesLink?.seriesId)

            val metronIssueLink =
                comicDao.getExternalId(
                    source = "METRON",
                    externalId = "300"
                )

            assertNotNull(metronIssueLink)
            assertEquals(issueId, metronIssueLink?.issueId)
        }

    @Test
    fun syncScheduledReleases_retriesFailedMetadataLookupWhenIncrementalResponseIsEmpty() =
        runBlocking {

            val provider =
                FakeIncrementalReleaseDataProvider(
                    scheduledReleaseResults = mutableListOf(
                        ReleaseProviderResult.Success(
                            listOf(
                                createRelease()
                            )
                        ),
                        ReleaseProviderResult.Success(
                            emptyList()
                        )
                    ),
                    seriesDetailsResults = mutableListOf(
                        ReleaseProviderResult.NetworkError,
                        ReleaseProviderResult.Success(
                            createSeriesDetails()
                        )
                    )
                )

            // The first metadata request fails. The release must
            // still be saved, and the failed lookup must not be cached.
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

            // Import the corresponding catalog data before retrying.
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

            // No issue changes are returned by the provider. The
            // repository must retry the previously failed lookup.
            val secondSummary = sync(provider)

            assertEquals(0, secondSummary.fetchedCount)
            assertEquals(1, secondSummary.linkedCount)
            assertEquals(0, secondSummary.unmatchedCount)

            assertEquals(
                listOf("45", "45"),
                provider.seriesDetailsRequests
            )

            assertEquals(
                Instant.ofEpochMilli(123456L).minusSeconds(1),
                provider.modifiedAfterRequests[1]
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

    @Test
    fun syncScheduledReleases_doesNotAdvanceCheckpointWhenProviderFails() =
        runBlocking {

            val provider =
                FakeIncrementalReleaseDataProvider(
                    scheduledReleaseResults = mutableListOf(
                        ReleaseProviderResult.Success(
                            listOf(
                                createRelease()
                            )
                        ),
                        ReleaseProviderResult.RateLimited(
                            retryAfterSeconds = 60L
                        )
                    ),
                    seriesDetailsResults = mutableListOf(
                        ReleaseProviderResult.NetworkError
                    )
                )

            nowMillis = 100000L

            val firstSummary = sync(provider)

            assertEquals(1, firstSummary.fetchedCount)

            val firstSyncState =
                comicDao.getScheduledReleaseSyncState(
                    providerId = "METRON",
                    fromDate = "2026-10-01",
                    toDate = "2026-10-31"
                )

            assertNotNull(firstSyncState)
            assertEquals(
                100000L,
                firstSyncState?.lastSuccessfulSyncAt
            )

            // A later failed attempt must not replace the last
            // successful checkpoint.
            nowMillis = 200000L

            val failedResult =
                syncResult(provider)

            assertTrue(
                failedResult is ReleaseProviderResult.RateLimited
            )

            assertEquals(
                Instant.ofEpochMilli(100000L).minusSeconds(1),
                provider.modifiedAfterRequests[1]
            )

            val stateAfterFailure =
                comicDao.getScheduledReleaseSyncState(
                    providerId = "METRON",
                    fromDate = "2026-10-01",
                    toDate = "2026-10-31"
                )

            assertNotNull(stateAfterFailure)
            assertEquals(
                100000L,
                stateAfterFailure?.lastSuccessfulSyncAt
            )
        }

    private suspend fun sync(
        provider: FakeIncrementalReleaseDataProvider
    ): ScheduledReleaseSyncSummary {
        val result = syncResult(provider)

        assertTrue(
            result is ReleaseProviderResult.Success
        )

        return (result as ReleaseProviderResult.Success).value
    }

    private suspend fun syncResult(
        provider: FakeIncrementalReleaseDataProvider
    ): ReleaseProviderResult<ScheduledReleaseSyncSummary> {

        val repository =
            ScheduledReleaseRepository(
                comicDao = comicDao,
                database = database,
                releaseDataProvider = provider,
                currentTimeMillis = { nowMillis }
            )

        return repository.syncScheduledReleases(
            accessToken = "test-token",
            fromDate = LocalDate.parse("2026-10-01"),
            toDate = LocalDate.parse("2026-10-31")
        )
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

    private fun createRelease(): ScheduledReleaseData {
        return ScheduledReleaseData(
            providerId = "METRON",
            externalIssueId = "300",
            externalSeriesId = "45",
            seriesTitle = "Amazing Spider-Man",
            seriesVolume = 2,
            seriesStartYear = 1999,
            publisherName = "Marvel",
            issueNumber = "30",
            displayTitle = "Coming Home",
            coverDate = "2026-10",
            storeReleaseDate = "2026-10-21",
            focDate = "2026-10-06",
            price = "4.99",
            priceCurrency = "USD",
            distributorSku = "METRON-SKU-300",
            upc = null,
            comicVineId = null,
            gcdId = null,
            sourceUrl = "https://metron.cloud/issue/300/",
            sourceModifiedAt = "2026-10-20T08:00:00Z"
        )
    }

    private fun createSeriesDetails(): ReleaseSeriesData {
        return ReleaseSeriesData(
            providerId = "METRON",
            externalSeriesId = "45",
            comicVineId = "18693",
            gcdId = "621",
            sourceUrl = "https://metron.cloud/series/45/",
            sourceModifiedAt = "2026-10-20T08:00:00Z"
        )
    }

    private class FakeIncrementalReleaseDataProvider(
        private val scheduledReleaseResults:
        MutableList<ReleaseProviderResult<List<ScheduledReleaseData>>>,
        private val seriesDetailsResults:
        MutableList<ReleaseProviderResult<ReleaseSeriesData>>
    ) : ReleaseDataProvider {

        override val providerId: String = "METRON"

        val modifiedAfterRequests =
            mutableListOf<Instant?>()

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
            return nextScheduledReleaseResult()
        }

        override suspend fun getScheduledReleases(
            accessToken: String,
            fromDate: LocalDate,
            toDate: LocalDate,
            modifiedAfter: Instant?
        ): ReleaseProviderResult<List<ScheduledReleaseData>> {

            modifiedAfterRequests += modifiedAfter

            return nextScheduledReleaseResult()
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

            return if (seriesDetailsResults.isNotEmpty()) {
                seriesDetailsResults.removeAt(0)
            } else {
                ReleaseProviderResult.ServiceError(
                    "No series-detail response was configured."
                )
            }
        }

        private fun nextScheduledReleaseResult():
            ReleaseProviderResult<List<ScheduledReleaseData>> {
            return if (scheduledReleaseResults.isNotEmpty()) {
                scheduledReleaseResults.removeAt(0)
            } else {
                ReleaseProviderResult.ServiceError(
                    "No scheduled-release response was configured."
                )
            }
        }
    }
}
