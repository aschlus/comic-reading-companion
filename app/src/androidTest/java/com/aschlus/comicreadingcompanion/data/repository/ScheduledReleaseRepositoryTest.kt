package com.aschlus.comicreadingcompanion.data.repository

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.entities.ExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.PullListSeriesEntity
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
class ScheduledReleaseRepositoryTest {

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
    fun syncScheduledReleases_persistsProviderSnapshots() =
        runBlocking {

            val seriesId =
                createLocalSeries(
                    title = "Amazing Spider-Man",
                    volume = 2,
                    startYear = 1999
                )

            trackSeries(
                seriesId = seriesId,
                externalSeriesId = "metron-series-10"
            )

            val release =
                createRelease(
                    externalIssueId = "metron-100",
                    externalSeriesId = "metron-series-10",
                    seriesTitle = "Amazing Spider-Man",
                    issueNumber = "1"
                )

            val repository =
                createRepository(
                    result = ReleaseProviderResult.Success(
                        listOf(release)
                    )
                )

            val result =
                repository.syncScheduledReleases(
                    accessToken = "test-token",
                    fromDate = LocalDate.parse("2026-10-01"),
                    toDate = LocalDate.parse("2026-10-31")
                )

            assertTrue(result is ReleaseProviderResult.Success)

            val summary =
                (result as ReleaseProviderResult.Success).value

            assertEquals(1, summary.fetchedCount)
            assertEquals(0, summary.linkedCount)
            assertEquals(0, summary.alreadyLinkedCount)
            assertEquals(1, summary.unmatchedCount)
            assertEquals(0, summary.ambiguousMatchCount)
            assertEquals(0, summary.conflictingExternalIdCount)

            val savedRelease =
                comicDao.getScheduledRelease(
                    providerId = "METRON",
                    externalIssueId = "metron-100"
                )

            assertNotNull(savedRelease)

            assertEquals(
                "metron-series-10",
                savedRelease?.externalSeriesId
            )

            assertEquals(
                "Amazing Spider-Man",
                savedRelease?.seriesTitleSnapshot
            )

            assertEquals(
                2,
                savedRelease?.seriesVolumeSnapshot
            )

            assertEquals(
                1999,
                savedRelease?.seriesStartYearSnapshot
            )

            assertEquals(
                "Marvel",
                savedRelease?.publisherNameSnapshot
            )

            assertEquals(
                "1",
                savedRelease?.issueNumberSnapshot
            )

            assertEquals(
                "Coming Home",
                savedRelease?.displayTitleSnapshot
            )

            assertEquals(
                "2026-10-21",
                savedRelease?.storeReleaseDate
            )

            assertEquals(
                "2026-10-06",
                savedRelease?.focDate
            )

            assertEquals(
                "4.99",
                savedRelease?.price
            )

            assertEquals(
                "USD",
                savedRelease?.priceCurrency
            )

            assertEquals(
                "METRON-SKU-100",
                savedRelease?.distributorSku
            )

            assertEquals(
                "1234567890123",
                savedRelease?.upc
            )

            assertEquals(
                123456L,
                savedRelease?.lastSyncedAt
            )
        }

    @Test
    fun syncScheduledReleases_linksUniqueIssueNumberToMappedSeries() =
        runBlocking {

            val seriesId =
                createLocalSeries(
                    title = "Amazing Spider-Man",
                    volume = 2,
                    startYear = 1999
                )

            val issueId =
                createLocalIssue(
                    seriesId = seriesId,
                    issueNumber = "30",
                    title = "Coming Home"
                )

            trackSeries(
                seriesId = seriesId,
                externalSeriesId = "metron-series-10"
            )

            val repository =
                createRepository(
                    result = ReleaseProviderResult.Success(
                        listOf(
                            createRelease(
                                externalIssueId = "metron-issue-30",
                                externalSeriesId = "metron-series-10",
                                issueNumber = "30"
                            )
                        )
                    )
                )

            val result =
                repository.syncScheduledReleases(
                    accessToken = "test-token",
                    fromDate = LocalDate.parse("2026-10-01"),
                    toDate = LocalDate.parse("2026-10-31")
                )

            assertTrue(result is ReleaseProviderResult.Success)

            val summary =
                (result as ReleaseProviderResult.Success).value

            assertEquals(1, summary.linkedCount)
            assertEquals(0, summary.unmatchedCount)
            assertEquals(0, summary.ambiguousMatchCount)
            assertEquals(0, summary.conflictingExternalIdCount)

            val externalId =
                comicDao.getExternalId(
                    source = "METRON",
                    externalId = "metron-issue-30"
                )

            assertNotNull(externalId)
            assertEquals(issueId, externalId?.issueId)

            val issueExternalIds =
                comicDao.getExternalIdsForIssue(issueId)

            assertEquals(1, issueExternalIds.size)
            assertEquals("METRON", issueExternalIds.single().source)
        }

    @Test
    fun syncScheduledReleases_doesNotLinkAmbiguousIssueNumbers() =
        runBlocking {

            val seriesId =
                createLocalSeries(
                    title = "Amazing Spider-Man",
                    volume = 2,
                    startYear = 1999
                )

            createLocalIssue(
                seriesId = seriesId,
                issueNumber = "1",
                title = "First printing"
            )

            createLocalIssue(
                seriesId = seriesId,
                issueNumber = "1",
                title = "Variant edition"
            )

            trackSeries(
                seriesId = seriesId,
                externalSeriesId = "metron-series-10"
            )

            val repository =
                createRepository(
                    result = ReleaseProviderResult.Success(
                        listOf(
                            createRelease(
                                externalIssueId = "metron-issue-1",
                                externalSeriesId = "metron-series-10",
                                issueNumber = "1"
                            )
                        )
                    )
                )

            val result =
                repository.syncScheduledReleases(
                    accessToken = "test-token",
                    fromDate = LocalDate.parse("2026-10-01"),
                    toDate = LocalDate.parse("2026-10-31")
                )

            assertTrue(result is ReleaseProviderResult.Success)

            val summary =
                (result as ReleaseProviderResult.Success).value

            assertEquals(0, summary.linkedCount)
            assertEquals(0, summary.unmatchedCount)
            assertEquals(1, summary.ambiguousMatchCount)

            assertNull(
                comicDao.getExternalId(
                    source = "METRON",
                    externalId = "metron-issue-1"
                )
            )
        }

    @Test
    fun syncScheduledReleases_doesNotOverwriteConflictingExternalId() =
        runBlocking {

            val mappedSeriesId =
                createLocalSeries(
                    title = "Amazing Spider-Man",
                    volume = 2,
                    startYear = 1999
                )

            createLocalIssue(
                seriesId = mappedSeriesId,
                issueNumber = "30",
                title = "Coming Home"
            )

            val otherSeriesId =
                createLocalSeries(
                    title = "Spectacular Spider-Man",
                    volume = 1,
                    startYear = 1976
                )

            val otherIssueId =
                createLocalIssue(
                    seriesId = otherSeriesId,
                    issueNumber = "99",
                    title = "Already linked elsewhere"
                )

            trackSeries(
                seriesId = mappedSeriesId,
                externalSeriesId = "metron-series-10"
            )

            comicDao.insertExternalId(
                ExternalId(
                    issueId = otherIssueId,
                    source = "METRON",
                    externalId = "metron-issue-30",
                    url = "https://metron.cloud/issue/30/"
                )
            )

            val repository =
                createRepository(
                    result = ReleaseProviderResult.Success(
                        listOf(
                            createRelease(
                                externalIssueId = "metron-issue-30",
                                externalSeriesId = "metron-series-10",
                                issueNumber = "30"
                            )
                        )
                    )
                )

            val result =
                repository.syncScheduledReleases(
                    accessToken = "test-token",
                    fromDate = LocalDate.parse("2026-10-01"),
                    toDate = LocalDate.parse("2026-10-31")
                )

            assertTrue(result is ReleaseProviderResult.Success)

            val summary =
                (result as ReleaseProviderResult.Success).value

            assertEquals(0, summary.linkedCount)
            assertEquals(0, summary.unmatchedCount)
            assertEquals(1, summary.conflictingExternalIdCount)

            val externalId =
                comicDao.getExternalId(
                    source = "METRON",
                    externalId = "metron-issue-30"
                )

            assertNotNull(externalId)
            assertEquals(otherIssueId, externalId?.issueId)
        }

    @Test
    fun syncScheduledReleases_recognizesExistingLinkAndUpdatesUrl() =
        runBlocking {

            val seriesId =
                createLocalSeries(
                    title = "Amazing Spider-Man",
                    volume = 2,
                    startYear = 1999
                )

            val issueId =
                createLocalIssue(
                    seriesId = seriesId,
                    issueNumber = "30",
                    title = "Coming Home"
                )

            trackSeries(
                seriesId = seriesId,
                externalSeriesId = "metron-series-10"
            )

            comicDao.insertExternalId(
                ExternalId(
                    issueId = issueId,
                    source = "METRON",
                    externalId = "metron-issue-30",
                    url = "https://metron.cloud/old/"
                )
            )

            val repository =
                createRepository(
                    result = ReleaseProviderResult.Success(
                        listOf(
                            createRelease(
                                externalIssueId = "metron-issue-30",
                                externalSeriesId = "metron-series-10",
                                issueNumber = "30",
                                sourceUrl =
                                    "https://metron.cloud/issue/30/"
                            )
                        )
                    )
                )

            val result =
                repository.syncScheduledReleases(
                    accessToken = "test-token",
                    fromDate = LocalDate.parse("2026-10-01"),
                    toDate = LocalDate.parse("2026-10-31")
                )

            assertTrue(result is ReleaseProviderResult.Success)

            val summary =
                (result as ReleaseProviderResult.Success).value

            assertEquals(0, summary.linkedCount)
            assertEquals(1, summary.alreadyLinkedCount)

            val externalIds =
                comicDao.getExternalIdsForIssue(issueId)

            assertEquals(1, externalIds.size)

            assertEquals(
                "https://metron.cloud/issue/30/",
                externalIds.single().url
            )
        }

    @Test
    fun syncScheduledReleases_doesNotWriteWhenProviderFails() =
        runBlocking {

            val seriesId =
                createLocalSeries(
                    title = "Amazing Spider-Man",
                    volume = 2,
                    startYear = 1999
                )

            trackSeries(
                seriesId = seriesId,
                externalSeriesId = "metron-series-10"
            )

            val repository =
                createRepository(
                    result = ReleaseProviderResult.RateLimited(
                        retryAfterSeconds = 60L
                    )
                )

            val result =
                repository.syncScheduledReleases(
                    accessToken = "test-token",
                    fromDate = LocalDate.parse("2026-10-01"),
                    toDate = LocalDate.parse("2026-10-31")
                )

            assertTrue(result is ReleaseProviderResult.RateLimited)

            assertEquals(
                0,
                comicDao.getScheduledReleaseCount(
                    providerId = "METRON"
                )
            )
        }

    @Test
    fun syncScheduledReleases_resolvesAndCachesMetronSeriesByComicVineId() =
        runBlocking {

            val seriesId =
                createLocalSeries(
                    title = "Amazing Spider-Man",
                    volume = 2,
                    startYear = 1999
                )

            val issueId =
                createLocalIssue(
                    seriesId = seriesId,
                    issueNumber = "30",
                    title = "Coming Home"
                )

            comicDao.insertSeriesExternalId(
                SeriesExternalId(
                    seriesId = seriesId,
                    source = "COMIC_VINE",
                    externalId = "18693",
                    url = null
                )
            )

            comicDao.insertPullListSeries(
                PullListSeriesEntity(
                    seriesId = seriesId,
                    addedAt = 100L
                )
            )

            val requestedMetronSeriesIds =
                mutableListOf<String>()

            val repository =
                createRepository(
                    result = ReleaseProviderResult.Success(
                        listOf(
                            createRelease(
                                externalIssueId = "metron-issue-123",
                                externalSeriesId = "45",
                                issueNumber = "30"
                            )
                        )
                    ),
                    comicVineLookupResult =
                        ReleaseProviderResult.Success(
                            listOf(
                                ReleaseSeriesData(
                                    providerId = "METRON",
                                    externalSeriesId = "45",
                                    comicVineId = "18693",
                                    gcdId = "621",
                                    sourceUrl =
                                        "https://metron.cloud/series/45/",
                                    sourceModifiedAt =
                                        "2026-10-30T12:00:00Z",
                                    seriesTitle =
                                        "Amazing Spider-Man (1999)",
                                    seriesVolume = 2,
                                    seriesStartYear = 1999,
                                    publisherName = "Marvel Comics"
                                )
                            )
                        ),
                    requestedSeriesIds = requestedMetronSeriesIds
                )

            val result =
                repository.syncScheduledReleases(
                    accessToken = "test-token",
                    fromDate = LocalDate.parse("2026-10-01"),
                    toDate = LocalDate.parse("2026-10-31")
                )

            assertTrue(result is ReleaseProviderResult.Success)

            val summary =
                (result as ReleaseProviderResult.Success).value

            assertEquals(1, summary.fetchedCount)
            assertEquals(1, summary.linkedCount)
            assertEquals(0, summary.alreadyLinkedCount)
            assertEquals(0, summary.unmatchedCount)

            assertEquals(
                listOf("45"),
                requestedMetronSeriesIds
            )

            val metronLink =
                comicDao.getSeriesExternalId(
                    source = "METRON",
                    externalId = "45"
                )

            assertNotNull(metronLink)

            assertEquals(
                seriesId,
                metronLink?.seriesId
            )

            assertEquals(
                issueId,
                comicDao.getExternalId(
                    source = "METRON",
                    externalId = "metron-issue-123"
                )?.issueId
            )

            val metadata =
                comicDao.getReleaseSeriesMetadata(
                    providerId = "METRON",
                    externalSeriesId = "45"
                )

            assertNotNull(metadata)

            assertEquals(
                "18693",
                metadata?.comicVineId
            )

            assertEquals(
                "621",
                metadata?.gcdId
            )
        }

    @Test
    fun syncScheduledReleases_resolvesSeriesByNameWhenComicVineIdIsMissing() =
        runBlocking {

            val seriesId =
                createLocalSeries(
                    title = "Amazing Spider-Man",
                    volume = 2,
                    startYear = 1999
                )

            val issueId =
                createLocalIssue(
                    seriesId = seriesId,
                    issueNumber = "30",
                    title = "Coming Home"
                )

            comicDao.insertPullListSeries(
                PullListSeriesEntity(
                    seriesId = seriesId,
                    addedAt = 100L
                )
            )

            val requestedMetronSeriesIds =
                mutableListOf<String>()

            val repository =
                createRepository(
                    result = ReleaseProviderResult.Success(
                        listOf(
                            createRelease(
                                externalIssueId = "metron-issue-456",
                                externalSeriesId = "45",
                                issueNumber = "30"
                            )
                        )
                    ),
                    seriesNameLookupResult =
                        ReleaseProviderResult.Success(
                            listOf(
                                ReleaseSeriesData(
                                    providerId = "METRON",
                                    externalSeriesId = "45",
                                    comicVineId = null,
                                    gcdId = "621",
                                    sourceUrl =
                                        "https://metron.cloud/series/45/",
                                    sourceModifiedAt = null,
                                    seriesTitle =
                                        "Amazing Spider-Man (1999)",
                                    seriesVolume = 2,
                                    seriesStartYear = 1999,
                                    publisherName = "Marvel Comics"
                                )
                            )
                        ),
                    requestedSeriesIds = requestedMetronSeriesIds
                )

            val result =
                repository.syncScheduledReleases(
                    accessToken = "test-token",
                    fromDate = LocalDate.parse("2026-10-01"),
                    toDate = LocalDate.parse("2026-10-31")
                )

            assertTrue(result is ReleaseProviderResult.Success)

            val summary =
                (result as ReleaseProviderResult.Success).value

            assertEquals(1, summary.linkedCount)

            assertEquals(
                listOf("45"),
                requestedMetronSeriesIds
            )

            val metronLink =
                comicDao.getSeriesExternalId(
                    source = "METRON",
                    externalId = "45"
                )

            assertNotNull(metronLink)

            assertEquals(
                seriesId,
                metronLink?.seriesId
            )

            assertEquals(
                issueId,
                comicDao.getExternalId(
                    source = "METRON",
                    externalId = "metron-issue-456"
                )?.issueId
            )
        }

    private suspend fun trackSeries(
        seriesId: Long,
        externalSeriesId: String
    ) {
        comicDao.insertPullListSeries(
            PullListSeriesEntity(
                seriesId = seriesId,
                addedAt = 100L
            )
        )

        comicDao.insertSeriesExternalId(
            SeriesExternalId(
                seriesId = seriesId,
                source = "METRON",
                externalId = externalSeriesId,
                url = null
            )
        )
    }

    private fun createRepository(
        result: ReleaseProviderResult<List<ScheduledReleaseData>>,
        comicVineLookupResult:
        ReleaseProviderResult<List<ReleaseSeriesData>> =
            ReleaseProviderResult.Success(emptyList()),
        seriesNameLookupResult:
        ReleaseProviderResult<List<ReleaseSeriesData>> =
            ReleaseProviderResult.Success(emptyList()),
        requestedSeriesIds: MutableList<String> = mutableListOf()
    ): ScheduledReleaseRepository {
        return ScheduledReleaseRepository(
            comicDao = comicDao,
            database = database,
            releaseDataProvider =
                FakeReleaseDataProvider(
                    scheduledReleasesResult = result,
                    comicVineLookupResult = comicVineLookupResult,
                    seriesNameLookupResult = seriesNameLookupResult,
                    requestedSeriesIds = requestedSeriesIds
                ),
            currentTimeMillis = { 123456L }
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
        issueNumber: String,
        title: String
    ): Long {
        return comicDao.insertIssue(
            Issue(
                seriesId = seriesId,
                universeId = null,
                issueNumber = issueNumber,
                title = title,
                publicationDate = null,
                coverUrl = null,
                description = null,
                issueType = IssueType.REGULAR,
                isIssueTypeManuallyOverridden = false
            )
        )
    }

    private fun createRelease(
        externalIssueId: String = "metron-issue-100",
        externalSeriesId: String = "metron-series-10",
        seriesTitle: String = "Amazing Spider-Man",
        issueNumber: String? = "30",
        sourceUrl: String? = "https://metron.cloud/issue/100/",
        seriesVolume: Int? = 2,
        seriesStartYear: Int? = 1999,
        publisherName: String? = "Marvel"
    ): ScheduledReleaseData {
        return ScheduledReleaseData(
            providerId = "METRON",
            externalIssueId = externalIssueId,
            externalSeriesId = externalSeriesId,
            seriesTitle = seriesTitle,
            seriesVolume = seriesVolume,
            seriesStartYear = seriesStartYear,
            publisherName = publisherName,
            issueNumber = issueNumber,
            displayTitle = "Coming Home",
            coverDate = "2026-10",
            storeReleaseDate = "2026-10-21",
            focDate = "2026-10-06",
            price = "4.99",
            priceCurrency = "USD",
            distributorSku = "METRON-SKU-100",
            upc = "1234567890123",
            comicVineId = "123456",
            gcdId = "654321",
            sourceUrl = sourceUrl,
            sourceModifiedAt = "2026-10-01T12:00:00Z"
        )
    }

    private class FakeReleaseDataProvider(
        private val scheduledReleasesResult:
        ReleaseProviderResult<List<ScheduledReleaseData>>,
        private val comicVineLookupResult:
        ReleaseProviderResult<List<ReleaseSeriesData>>,
        private val seriesNameLookupResult:
        ReleaseProviderResult<List<ReleaseSeriesData>>,
        private val requestedSeriesIds: MutableList<String>
    ) : ReleaseDataProvider {

        override val providerId: String = "METRON"

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
            return scheduledReleasesResult
        }

        override suspend fun findSeriesByComicVineId(
            accessToken: String,
            comicVineId: String
        ): ReleaseProviderResult<List<ReleaseSeriesData>> {
            return comicVineLookupResult
        }

        override suspend fun findSeriesByName(
            accessToken: String,
            seriesName: String
        ): ReleaseProviderResult<List<ReleaseSeriesData>> {
            return seriesNameLookupResult
        }

        override suspend fun getScheduledReleasesForSeries(
            accessToken: String,
            externalSeriesId: String,
            fromDate: LocalDate,
            toDate: LocalDate,
            modifiedAfter: Instant?
        ): ReleaseProviderResult<List<ScheduledReleaseData>> {

            requestedSeriesIds += externalSeriesId

            return scheduledReleasesResult
        }

        override suspend fun getReleaseDetails(
            accessToken: String,
            externalIssueId: String
        ): ReleaseProviderResult<ScheduledReleaseData> {
            return ReleaseProviderResult.ServiceError(
                "This test does not use release details."
            )
        }
    }
}