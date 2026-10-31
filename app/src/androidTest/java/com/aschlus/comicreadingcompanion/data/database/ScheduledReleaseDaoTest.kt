
package com.aschlus.comicreadingcompanion.data.database

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.PullListSeriesEntity
import com.aschlus.comicreadingcompanion.data.database.entities.ScheduledReleaseEntity
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.SeriesExternalId
import kotlinx.coroutines.flow.first
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
class ScheduledReleaseDaoTest {

    private lateinit var database: ComicDatabase
    private lateinit var dao: ComicDao

    @Before
    fun setUp() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            ComicDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        dao = database.comicDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertScheduledReleases_updatesExistingProviderIssue() =
        runBlocking {
            val original = release(
                externalIssueId = "101",
                displayTitleSnapshot = "Original title",
                lastSyncedAt = 1000L
            )

            dao.upsertScheduledReleases(
                listOf(original)
            )

            val updated = original.copy(
                displayTitleSnapshot = "Updated title",
                focDate = "2026-10-30",
                lastSyncedAt = 2000L
            )

            dao.upsertScheduledReleases(
                listOf(updated)
            )

            val stored = dao.getScheduledRelease(
                providerId = "METRON",
                externalIssueId = "101"
            )

            assertNotNull(stored)
            assertEquals(
                "Updated title",
                stored?.displayTitleSnapshot
            )
            assertEquals("2026-10-30", stored?.focDate)
            assertEquals(2000L, stored?.lastSyncedAt)

            assertEquals(
                1,
                dao.getScheduledReleaseCount("METRON")
            )
        }

    @Test
    fun getScheduledReleasesInRange_filtersAndOrdersResults() =
        runBlocking {
            dao.upsertScheduledReleases(
                listOf(
                    release(
                        externalIssueId = "102",
                        seriesTitleSnapshot = "Spider-Man",
                        issueNumberSnapshot = "2",
                        storeReleaseDate = "2026-11-12"
                    ),
                    release(
                        externalIssueId = "101",
                        seriesTitleSnapshot = "Amazing Spider-Man",
                        issueNumberSnapshot = "1",
                        storeReleaseDate = "2026-11-05"
                    ),
                    release(
                        externalIssueId = "103",
                        seriesTitleSnapshot = "Batman",
                        issueNumberSnapshot = "1",
                        storeReleaseDate = "2026-12-01"
                    ),
                    release(
                        externalIssueId = "104",
                        seriesTitleSnapshot = "Earlier Release",
                        issueNumberSnapshot = "1",
                        storeReleaseDate = "2026-10-31"
                    ),
                    release(
                        providerId = "OTHER",
                        externalIssueId = "101",
                        seriesTitleSnapshot = "Different Provider",
                        storeReleaseDate = "2026-11-07"
                    )
                )
            )

            val results = dao.getScheduledReleasesInRange(
                providerId = "METRON",
                fromDate = "2026-11-05",
                toDate = "2026-11-12"
            )

            assertEquals(
                listOf("101", "102"),
                results.map { it.externalIssueId }
            )

            assertEquals(
                listOf(
                    "2026-11-05",
                    "2026-11-12"
                ),
                results.map { it.storeReleaseDate }
            )
        }

    @Test
    fun scheduledReleaseIdentity_isScopedToProvider() =
        runBlocking {
            dao.upsertScheduledReleases(
                listOf(
                    release(
                        providerId = "METRON",
                        externalIssueId = "123",
                        seriesTitleSnapshot = "Metron Series"
                    ),
                    release(
                        providerId = "OTHER",
                        externalIssueId = "123",
                        seriesTitleSnapshot = "Other Series"
                    )
                )
            )

            val metronRelease = dao.getScheduledRelease(
                providerId = "METRON",
                externalIssueId = "123"
            )

            val otherRelease = dao.getScheduledRelease(
                providerId = "OTHER",
                externalIssueId = "123"
            )

            assertEquals(
                "Metron Series",
                metronRelease?.seriesTitleSnapshot
            )

            assertEquals(
                "Other Series",
                otherRelease?.seriesTitleSnapshot
            )

            assertEquals(
                1,
                dao.getScheduledReleaseCount("METRON")
            )

            assertEquals(
                1,
                dao.getScheduledReleaseCount("OTHER")
            )

            assertNull(
                dao.getScheduledRelease(
                    providerId = "UNKNOWN",
                    externalIssueId = "123"
                )
            )
        }

    @Test
    fun getScheduledReleasesForExternalSeries_returnsOnlyMatchingSeries() =
        runBlocking {
            dao.upsertScheduledReleases(
                listOf(
                    release(
                        externalIssueId = "101",
                        externalSeriesId = "500",
                        issueNumberSnapshot = "1"
                    ),
                    release(
                        externalIssueId = "102",
                        externalSeriesId = "500",
                        issueNumberSnapshot = "2"
                    ),
                    release(
                        externalIssueId = "103",
                        externalSeriesId = "600",
                        seriesTitleSnapshot = "Another Series"
                    ),
                    release(
                        providerId = "OTHER",
                        externalIssueId = "104",
                        externalSeriesId = "500",
                        seriesTitleSnapshot = "Other Provider Series"
                    )
                )
            )

            val results =
                dao.getScheduledReleasesForExternalSeries(
                    providerId = "METRON",
                    externalSeriesId = "500"
                )

            assertEquals(
                listOf("101", "102"),
                results.map { it.externalIssueId }
            )

            assertEquals(
                2,
                results.size
            )
        }

    @Test
    fun getPullListScheduledReleasesInRange_returnsOnlyTrackedUpcomingReleases() =
        runBlocking {
            val publisherId =
                dao.insertPublisher(
                    Publisher(name = "Marvel Comics")
                )

            val trackedSeriesId =
                dao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Amazing Spider-Man",
                        volume = 2,
                        startYear = 1999,
                        endYear = 2003
                    )
                )

            val untrackedSeriesId =
                dao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Spectacular Spider-Man",
                        volume = 1,
                        startYear = 1976,
                        endYear = 1998
                    )
                )

            dao.insertPullListSeries(
                PullListSeriesEntity(
                    seriesId = trackedSeriesId,
                    addedAt = 1000L
                )
            )

            dao.insertSeriesExternalId(
                SeriesExternalId(
                    seriesId = trackedSeriesId,
                    source = "METRON",
                    externalId = "500",
                    url = null
                )
            )

            dao.insertSeriesExternalId(
                SeriesExternalId(
                    seriesId = untrackedSeriesId,
                    source = "METRON",
                    externalId = "600",
                    url = null
                )
            )

            dao.upsertScheduledReleases(
                listOf(
                    release(
                        externalIssueId = "201",
                        externalSeriesId = "500",
                        issueNumberSnapshot = "1",
                        storeReleaseDate = "2026-11-05",
                        focDate = "2026-10-27"
                    ),
                    release(
                        externalIssueId = "202",
                        externalSeriesId = "500",
                        issueNumberSnapshot = "2",
                        storeReleaseDate = "2026-11-20",
                        focDate = "2026-11-04"
                    ),
                    release(
                        externalIssueId = "203",
                        externalSeriesId = "600",
                        storeReleaseDate = "2026-11-06"
                    ),
                    release(
                        externalIssueId = "204",
                        externalSeriesId = "500",
                        storeReleaseDate = "2026-11-07"
                    ).copy(
                        manuallyCanceledAt = 5000L
                    ),
                    release(
                        externalIssueId = "205",
                        externalSeriesId = "500",
                        storeReleaseDate = "2026-11-20",
                        focDate = null
                    )
                )
            )

            val results =
                dao.getPullListScheduledReleasesInRange(
                    providerId = "METRON",
                    fromDate = "2026-10-31",
                    toDate = "2026-11-14"
                ).first()

            assertEquals(
                listOf("201", "202"),
                results.map { it.externalIssueId }
            )

            val storeRelease =
                results.first()

            assertEquals(
                trackedSeriesId,
                storeRelease.seriesId
            )

            assertEquals(
                "Amazing Spider-Man",
                storeRelease.seriesTitle
            )

            assertEquals(
                "Marvel Comics",
                storeRelease.publisherName
            )

            assertEquals(
                "2026-11-05",
                storeRelease.storeReleaseDate
            )

            val focRelease =
                results.last()

            assertEquals(
                "2026-11-04",
                focRelease.focDate
            )

            assertEquals(
                "2026-11-20",
                focRelease.storeReleaseDate
            )

            assertTrue(
                results.none {
                    it.manuallyCanceledAt != null
                }
            )
        }

    private fun release(
        providerId: String = "METRON",
        externalIssueId: String,
        externalSeriesId: String = "500",
        seriesTitleSnapshot: String = "Amazing Spider-Man",
        seriesVolumeSnapshot: Int? = 1,
        seriesStartYearSnapshot: Int? = 2025,
        publisherNameSnapshot: String? = "Marvel Comics",
        issueNumberSnapshot: String? = "1",
        displayTitleSnapshot: String? = "Test issue",
        storeReleaseDate: String? = "2026-11-05",
        focDate: String? = null,
        lastSyncedAt: Long = 1000L
    ) = ScheduledReleaseEntity(
        providerId = providerId,
        externalIssueId = externalIssueId,
        externalSeriesId = externalSeriesId,
        seriesTitleSnapshot = seriesTitleSnapshot,
        seriesVolumeSnapshot = seriesVolumeSnapshot,
        seriesStartYearSnapshot = seriesStartYearSnapshot,
        publisherNameSnapshot = publisherNameSnapshot,
        issueNumberSnapshot = issueNumberSnapshot,
        displayTitleSnapshot = displayTitleSnapshot,
        coverDate = "2026-11-01",
        storeReleaseDate = storeReleaseDate,
        focDate = focDate,
        price = "4.99",
        priceCurrency = "USD",
        distributorSku = null,
        upc = null,
        comicVineId = null,
        gcdId = null,
        sourceUrl = null,
        sourceModifiedAt = null,
        lastSyncedAt = lastSyncedAt
    )
}
