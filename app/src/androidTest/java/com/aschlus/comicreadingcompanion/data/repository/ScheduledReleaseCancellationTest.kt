
package com.aschlus.comicreadingcompanion.data.repository

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduledReleaseCancellationTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao:
        com.aschlus.comicreadingcompanion.data.database.ComicDao

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
    fun setScheduledReleaseManuallyCanceled_recordsAndReversesStatus() =
        runBlocking {

            val provider =
                FakeProvider(
                    responses = mutableListOf(
                        ReleaseProviderResult.Success(
                            listOf(createRelease())
                        )
                    )
                )

            val repository = createRepository(provider)

            sync(repository)

            val original =
                comicDao.getScheduledRelease(
                    providerId = "METRON",
                    externalIssueId = "300"
                )

            assertNotNull(original)
            assertNull(original?.manuallyCanceledAt)

            // The user explicitly confirms cancellation.
            nowMillis = 2000L

            assertTrue(
                repository.setScheduledReleaseManuallyCanceled(
                    providerId = "METRON",
                    externalIssueId = "300",
                    isCanceled = true
                )
            )

            val canceled =
                comicDao.getScheduledRelease(
                    providerId = "METRON",
                    externalIssueId = "300"
                )

            assertEquals(
                2000L,
                canceled?.manuallyCanceledAt
            )

            val firstHistory =
                comicDao.getScheduledReleaseChangesForIssue(
                    providerId = "METRON",
                    externalIssueId = "300"
                )

            assertEquals(1, firstHistory.size)
            assertNull(firstHistory.single().previousManuallyCanceledAt)
            assertEquals(
                2000L,
                firstHistory.single().newManuallyCanceledAt
            )

            // Repeating cancellation is idempotent.
            nowMillis = 2500L

            assertTrue(
                repository.setScheduledReleaseManuallyCanceled(
                    providerId = "METRON",
                    externalIssueId = "300",
                    isCanceled = true
                )
            )

            assertEquals(
                1,
                comicDao.getScheduledReleaseChangesForIssue(
                    providerId = "METRON",
                    externalIssueId = "300"
                ).size
            )

            assertEquals(
                2000L,
                comicDao.getScheduledRelease(
                    providerId = "METRON",
                    externalIssueId = "300"
                )?.manuallyCanceledAt
            )

            // The user can reinstate the release.
            nowMillis = 3000L

            assertTrue(
                repository.setScheduledReleaseManuallyCanceled(
                    providerId = "METRON",
                    externalIssueId = "300",
                    isCanceled = false
                )
            )

            val reinstated =
                comicDao.getScheduledRelease(
                    providerId = "METRON",
                    externalIssueId = "300"
                )

            assertNotNull(reinstated)
            assertNull(reinstated?.manuallyCanceledAt)

            val history =
                comicDao.getScheduledReleaseChangesForIssue(
                    providerId = "METRON",
                    externalIssueId = "300"
                )

            assertEquals(2, history.size)

            // History is returned newest first.
            assertEquals(
                2000L,
                history[0].previousManuallyCanceledAt
            )
            assertNull(history[0].newManuallyCanceledAt)

            assertNull(history[1].previousManuallyCanceledAt)
            assertEquals(
                2000L,
                history[1].newManuallyCanceledAt
            )
        }

    @Test
    fun syncScheduledReleases_preservesManualCancellation() =
        runBlocking {

            val original = createRelease()

            val updated =
                original.copy(
                    displayTitle = "Updated release title",
                    sourceModifiedAt = "2026-10-25T12:00:00Z"
                )

            val provider =
                FakeProvider(
                    responses = mutableListOf(
                        ReleaseProviderResult.Success(
                            listOf(original)
                        ),
                        ReleaseProviderResult.Success(
                            listOf(updated)
                        )
                    )
                )

            val repository = createRepository(provider)

            sync(repository)

            nowMillis = 2000L

            assertTrue(
                repository.setScheduledReleaseManuallyCanceled(
                    providerId = "METRON",
                    externalIssueId = "300",
                    isCanceled = true
                )
            )

            nowMillis = 3000L

            sync(repository)

            val saved =
                comicDao.getScheduledRelease(
                    providerId = "METRON",
                    externalIssueId = "300"
                )

            assertNotNull(saved)

            assertEquals(
                "Updated release title",
                saved?.displayTitleSnapshot
            )

            assertEquals(
                2000L,
                saved?.manuallyCanceledAt
            )

            // A provider refresh does not create a fake cancellation
            // event or remove the explicit manual decision.
            val history =
                comicDao.getScheduledReleaseChangesForIssue(
                    providerId = "METRON",
                    externalIssueId = "300"
                )

            assertEquals(1, history.size)
            assertEquals(
                2000L,
                history.single().newManuallyCanceledAt
            )
        }

    @Test
    fun setScheduledReleaseManuallyCanceled_returnsFalseForUnknownRelease() =
        runBlocking {

            val repository =
                createRepository(
                    FakeProvider(
                        responses = mutableListOf()
                    )
                )

            assertFalse(
                repository.setScheduledReleaseManuallyCanceled(
                    providerId = "METRON",
                    externalIssueId = "missing",
                    isCanceled = true
                )
            )

            assertTrue(
                comicDao.getScheduledReleaseChangesForIssue(
                    providerId = "METRON",
                    externalIssueId = "missing"
                ).isEmpty()
            )
        }

    private fun createRepository(
        provider: FakeProvider
    ) = ScheduledReleaseRepository(
        comicDao = comicDao,
        database = database,
        releaseDataProvider = provider,
        currentTimeMillis = { nowMillis }
    )

    private suspend fun sync(
        repository: ScheduledReleaseRepository
    ) {
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

    private fun createRelease() =
        ScheduledReleaseData(
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
