
package com.aschlus.comicreadingcompanion.data.repository

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.remote.ReleaseDataProvider
import com.aschlus.comicreadingcompanion.data.remote.ReleaseProviderResult
import com.aschlus.comicreadingcompanion.data.remote.ReleaseSeriesData
import com.aschlus.comicreadingcompanion.data.remote.ScheduledReleaseData
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ScheduledReleaseChangedDateSyncTest {

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
    fun syncScheduledReleases_updatesTrackedReleaseWhenDateMovesOutsideRange() =
        runBlocking {

            val original = createRelease(
                externalIssueId = "300",
                storeReleaseDate = "2026-10-21"
            )

            val delayed = original.copy(
                storeReleaseDate = "2026-11-11",
                focDate = "2026-10-27",
                sourceModifiedAt = "2026-10-25T12:00:00Z"
            )

            val provider =
                FakeProvider(
                    scheduledReleaseResults = mutableListOf(
                        ReleaseProviderResult.Success(
                            listOf(original)
                        ),
                        ReleaseProviderResult.Success(
                            listOf(delayed)
                        )
                    )
                )

            val firstSummary = sync(provider)

            assertEquals(1, firstSummary.fetchedCount)

            val firstSavedRelease =
                comicDao.getScheduledRelease(
                    providerId = "METRON",
                    externalIssueId = "300"
                )

            assertNotNull(firstSavedRelease)

            assertEquals(
                "2026-10-21",
                firstSavedRelease?.storeReleaseDate
            )

            // The same issue is now outside the original October
            // sync window. It must still be updated because it was
            // already tracked locally.
            val secondSummary = sync(provider)

            assertEquals(1, secondSummary.fetchedCount)

            val updatedRelease =
                comicDao.getScheduledRelease(
                    providerId = "METRON",
                    externalIssueId = "300"
                )

            assertNotNull(updatedRelease)

            assertEquals(
                "2026-11-11",
                updatedRelease?.storeReleaseDate
            )

            assertEquals(
                "2026-10-27",
                updatedRelease?.focDate
            )

            assertEquals(
                "2026-10-25T12:00:00Z",
                updatedRelease?.sourceModifiedAt
            )

            assertEquals(
                listOf(
                    null,
                    Instant.ofEpochMilli(123456L).minusSeconds(1)
                ),
                provider.modifiedAfterRequests
            )
        }

    @Test
    fun syncScheduledReleases_ignoresUntrackedChangesOutsideRange() =
        runBlocking {

            val original = createRelease(
                externalIssueId = "300",
                storeReleaseDate = "2026-10-21"
            )

            val newInRange = createRelease(
                externalIssueId = "301",
                storeReleaseDate = "2026-10-28"
            )

            val unrelatedOutsideRange = createRelease(
                externalIssueId = "999",
                storeReleaseDate = "2026-12-02"
            )

            val provider =
                FakeProvider(
                    scheduledReleaseResults = mutableListOf(
                        ReleaseProviderResult.Success(
                            listOf(original)
                        ),
                        ReleaseProviderResult.Success(
                            listOf(
                                unrelatedOutsideRange,
                                newInRange
                            )
                        )
                    )
                )

            val firstSummary = sync(provider)

            assertEquals(1, firstSummary.fetchedCount)

            val secondSummary = sync(provider)

            // Only issue 301 is new and within the requested range.
            assertEquals(1, secondSummary.fetchedCount)

            assertNotNull(
                comicDao.getScheduledRelease(
                    providerId = "METRON",
                    externalIssueId = "300"
                )
            )

            assertNotNull(
                comicDao.getScheduledRelease(
                    providerId = "METRON",
                    externalIssueId = "301"
                )
            )

            assertNull(
                comicDao.getScheduledRelease(
                    providerId = "METRON",
                    externalIssueId = "999"
                )
            )

            assertEquals(
                2,
                comicDao.getScheduledReleaseCount(
                    providerId = "METRON"
                )
            )
        }

    private suspend fun sync(
        provider: FakeProvider
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

    private fun createRelease(
        externalIssueId: String,
        storeReleaseDate: String
    ) = ScheduledReleaseData(
        providerId = "METRON",
        externalIssueId = externalIssueId,
        externalSeriesId = "45",
        seriesTitle = "Amazing Spider-Man",
        seriesVolume = 2,
        seriesStartYear = 1999,
        publisherName = "Marvel",
        issueNumber = "30",
        displayTitle = "Coming Home",
        coverDate = "2026-10",
        storeReleaseDate = storeReleaseDate,
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

    private class FakeProvider(
        private val scheduledReleaseResults:
        MutableList<ReleaseProviderResult<List<ScheduledReleaseData>>>
    ) : ReleaseDataProvider {

        override val providerId: String = "METRON"

        val modifiedAfterRequests =
            mutableListOf<Instant?>()

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
            return nextResult()
        }

        override suspend fun getScheduledReleases(
            accessToken: String,
            fromDate: LocalDate,
            toDate: LocalDate,
            modifiedAfter: Instant?
        ): ReleaseProviderResult<List<ScheduledReleaseData>> {

            modifiedAfterRequests += modifiedAfter

            return nextResult()
        }

        override suspend fun getReleaseDetails(
            accessToken: String,
            externalIssueId: String
        ): ReleaseProviderResult<ScheduledReleaseData> {
            return ReleaseProviderResult.ServiceError(
                "This test does not use issue-detail lookup."
            )
        }

        override suspend fun getSeriesDetails(
            accessToken: String,
            externalSeriesId: String
        ): ReleaseProviderResult<ReleaseSeriesData> {
            return ReleaseProviderResult.ServiceError(
                "This test does not use series-detail lookup."
            )
        }

        private fun nextResult():
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
