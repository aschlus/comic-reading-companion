
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
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduledReleaseChangeHistoryTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao

    private var nowMillis = 1000L

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
    fun syncScheduledReleases_recordsStoreDateAndFocChanges() =
        runBlocking {

            val original =
                createRelease(
                    storeReleaseDate = "2026-10-21",
                    focDate = "2026-10-06"
                )

            val delayed =
                original.copy(
                    storeReleaseDate = "2026-11-11",
                    focDate = "2026-10-27",
                    sourceModifiedAt = "2026-10-25T12:00:00Z"
                )

            val provider =
                FakeProvider(
                    responses = mutableListOf(
                        ReleaseProviderResult.Success(
                            listOf(original)
                        ),
                        ReleaseProviderResult.Success(
                            listOf(delayed)
                        )
                    )
                )

            nowMillis = 1000L
            sync(provider)

            nowMillis = 2000L
            sync(provider)

            val changes =
                comicDao.getScheduledReleaseChangesForIssue(
                    providerId = "METRON",
                    externalIssueId = "300"
                )

            assertEquals(1, changes.size)

            val change = changes.single()

            assertEquals("METRON", change.providerId)
            assertEquals("300", change.externalIssueId)
            assertEquals("45", change.externalSeriesId)

            assertEquals(
                "Amazing Spider-Man",
                change.seriesTitleSnapshot
            )

            assertEquals("30", change.issueNumberSnapshot)

            assertEquals(
                "2026-10-21",
                change.previousStoreReleaseDate
            )

            assertEquals(
                "2026-11-11",
                change.newStoreReleaseDate
            )

            assertEquals(
                "2026-10-06",
                change.previousFocDate
            )

            assertEquals(
                "2026-10-27",
                change.newFocDate
            )

            assertEquals(
                "2026-10-25T12:00:00Z",
                change.sourceModifiedAt
            )

            assertEquals(2000L, change.detectedAt)

            // The current release must also reflect the new dates.
            val currentRelease =
                comicDao.getScheduledRelease(
                    providerId = "METRON",
                    externalIssueId = "300"
                )

            assertNotNull(currentRelease)

            assertEquals(
                "2026-11-11",
                currentRelease?.storeReleaseDate
            )

            assertEquals(
                "2026-10-27",
                currentRelease?.focDate
            )
        }

    @Test
    fun syncScheduledReleases_doesNotRecordUnchangedDates() =
        runBlocking {

            val release =
                createRelease(
                    storeReleaseDate = "2026-10-21",
                    focDate = "2026-10-06"
                )

            val provider =
                FakeProvider(
                    responses = mutableListOf(
                        ReleaseProviderResult.Success(
                            listOf(release)
                        ),
                        ReleaseProviderResult.Success(
                            listOf(release)
                        )
                    )
                )

            sync(provider)
            sync(provider)

            val changes =
                comicDao.getScheduledReleaseChangesForIssue(
                    providerId = "METRON",
                    externalIssueId = "300"
                )

            assertTrue(changes.isEmpty())
        }

    @Test
    fun syncScheduledReleases_recordsRemovedStoreDateWithoutCallingItCanceled() =
        runBlocking {

            val original =
                createRelease(
                    storeReleaseDate = "2026-10-21",
                    focDate = "2026-10-06"
                )

            val dateWithdrawn =
                original.copy(
                    storeReleaseDate = null,
                    sourceModifiedAt = "2026-10-25T12:00:00Z"
                )

            val provider =
                FakeProvider(
                    responses = mutableListOf(
                        ReleaseProviderResult.Success(
                            listOf(original)
                        ),
                        ReleaseProviderResult.Success(
                            listOf(dateWithdrawn)
                        )
                    )
                )

            sync(provider)
            sync(provider)

            val changes =
                comicDao.getScheduledReleaseChangesForIssue(
                    providerId = "METRON",
                    externalIssueId = "300"
                )

            assertEquals(1, changes.size)

            assertEquals(
                "2026-10-21",
                changes.single().previousStoreReleaseDate
            )

            assertNull(
                changes.single().newStoreReleaseDate
            )

            val currentRelease =
                comicDao.getScheduledRelease(
                    providerId = "METRON",
                    externalIssueId = "300"
                )

            assertNotNull(currentRelease)
            assertNull(currentRelease?.storeReleaseDate)

            // A missing store date is recorded as a date change,
            // not interpreted as an issue cancellation.
        }

    private suspend fun sync(
        provider: FakeProvider
    ) {
        val repository =
            ScheduledReleaseRepository(
                comicDao = comicDao,
                database = database,
                releaseDataProvider = provider,
                currentTimeMillis = { nowMillis }
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
    }

    private fun createRelease(
        storeReleaseDate: String?,
        focDate: String?
    ) = ScheduledReleaseData(
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
        storeReleaseDate = storeReleaseDate,
        focDate = focDate,
        price = "4.99",
        priceCurrency = "USD",
        distributorSku = "METRON-SKU-300",
        upc = null,
        comicVineId = null,
        gcdId = null,
        sourceUrl = "https://metron.cloud/issue/300/",
        sourceModifiedAt = "2026-10-20T08:00:00Z"
    )

    private class FakeProvider(
        private val responses:
        MutableList<ReleaseProviderResult<List<ScheduledReleaseData>>>
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
            return nextResponse()
        }

        override suspend fun getScheduledReleases(
            accessToken: String,
            fromDate: LocalDate,
            toDate: LocalDate,
            modifiedAfter: Instant?
        ): ReleaseProviderResult<List<ScheduledReleaseData>> {
            return nextResponse()
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

        private fun nextResponse():
            ReleaseProviderResult<List<ScheduledReleaseData>> {
            return if (responses.isNotEmpty()) {
                responses.removeAt(0)
            } else {
                ReleaseProviderResult.ServiceError(
                    "No response was configured."
                )
            }
        }
    }
}
