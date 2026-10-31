
package com.aschlus.comicreadingcompanion.data.repository

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.ExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.SeriesExternalId
import com.aschlus.comicreadingcompanion.data.remote.ReleaseDataProvider
import com.aschlus.comicreadingcompanion.data.remote.ReleaseProviderResult
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
class ScheduledReleaseComicVineReconciliationTest {

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
    fun applyComicVineSeriesRefresh_linksPreviouslySyncedMetronRelease() =
        runBlocking {
            val publisherId = createPublisher()

            val seriesId =
                createSeries(
                    publisherId = publisherId,
                    title = "Amazing Spider-Man",
                    volume = 2,
                    startYear = 1999
                )

            addMetronSeriesLink(
                seriesId = seriesId
            )

            val syncSummary =
                syncScheduledRelease()

            assertEquals(1, syncSummary.unmatchedCount)

            assertNull(
                comicDao.getExternalId(
                    source = "METRON",
                    externalId = "metron-issue-30"
                )
            )

            applyComicVineRefresh(
                seriesId = seriesId
            )

            val issues =
                comicDao.getIssuesForSeries(seriesId)

            assertEquals(1, issues.size)

            val importedIssue = issues.single()

            assertEquals(
                "30",
                importedIssue.issueNumber
            )

            val metronExternalId =
                comicDao.getExternalId(
                    source = "METRON",
                    externalId = "metron-issue-30"
                )

            assertNotNull(metronExternalId)

            assertEquals(
                importedIssue.id,
                metronExternalId?.issueId
            )

            assertEquals(
                "https://metron.cloud/issue/300/",
                metronExternalId?.url
            )

            val externalIds =
                comicDao.getExternalIdsForIssue(
                    importedIssue.id
                )

            assertEquals(2, externalIds.size)

            assertTrue(
                externalIds.any {
                    it.source == "COMIC_VINE" &&
                        it.externalId == "200030"
                }
            )

            assertTrue(
                externalIds.any {
                    it.source == "METRON" &&
                        it.externalId == "metron-issue-30"
                }
            )

            val savedRelease =
                comicDao.getScheduledRelease(
                    providerId = "METRON",
                    externalIssueId = "metron-issue-30"
                )

            assertNotNull(savedRelease)
        }

    @Test
    fun applyComicVineSeriesRefresh_doesNotLinkAmbiguousIssueNumber() =
        runBlocking {
            val publisherId = createPublisher()

            val seriesId =
                createSeries(
                    publisherId = publisherId,
                    title = "Amazing Spider-Man",
                    volume = 2,
                    startYear = 1999
                )

            createIssue(
                seriesId = seriesId,
                issueNumber = "30",
                title = "First printing"
            )

            createIssue(
                seriesId = seriesId,
                issueNumber = "30",
                title = "Variant edition"
            )

            addMetronSeriesLink(
                seriesId = seriesId
            )

            syncScheduledRelease()

            applyComicVineRefresh(
                seriesId = seriesId
            )

            val issues =
                comicDao.getIssuesForSeries(seriesId)

            assertEquals(2, issues.size)

            assertNull(
                comicDao.getExternalId(
                    source = "METRON",
                    externalId = "metron-issue-30"
                )
            )
        }

    @Test
    fun applyComicVineSeriesRefresh_doesNotOverwriteMetronIdLinkedElsewhere() =
        runBlocking {
            val publisherId = createPublisher()

            val mappedSeriesId =
                createSeries(
                    publisherId = publisherId,
                    title = "Amazing Spider-Man",
                    volume = 2,
                    startYear = 1999
                )

            val otherSeriesId =
                createSeries(
                    publisherId = publisherId,
                    title = "Spectacular Spider-Man",
                    volume = 1,
                    startYear = 1976
                )

            val otherIssueId =
                createIssue(
                    seriesId = otherSeriesId,
                    issueNumber = "99",
                    title = "Already linked elsewhere"
                )

            comicDao.insertExternalId(
                ExternalId(
                    issueId = otherIssueId,
                    source = "METRON",
                    externalId = "metron-issue-30",
                    url = "https://metron.cloud/issue/old/"
                )
            )

            addMetronSeriesLink(
                seriesId = mappedSeriesId
            )

            syncScheduledRelease()

            applyComicVineRefresh(
                seriesId = mappedSeriesId
            )

            val issues =
                comicDao.getIssuesForSeries(mappedSeriesId)

            assertEquals(1, issues.size)

            val externalId =
                comicDao.getExternalId(
                    source = "METRON",
                    externalId = "metron-issue-30"
                )

            assertNotNull(externalId)

            assertEquals(
                otherIssueId,
                externalId?.issueId
            )

            assertEquals(
                "https://metron.cloud/issue/old/",
                externalId?.url
            )

            val importedIssueExternalIds =
                comicDao.getExternalIdsForIssue(
                    issues.single().id
                )

            assertTrue(
                importedIssueExternalIds.none {
                    it.source == "METRON"
                }
            )
        }

    private suspend fun createPublisher(): Long {
        return comicDao.insertPublisher(
            Publisher(name = "Marvel")
        )
    }

    private suspend fun createSeries(
        publisherId: Long,
        title: String,
        volume: Int?,
        startYear: Int?
    ): Long {
        return comicDao.insertSeries(
            Series(
                publisherId = publisherId,
                title = title,
                volume = volume,
                startYear = startYear,
                endYear = null,
                universeId = null
            )
        )
    }

    private suspend fun createIssue(
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

    private suspend fun addMetronSeriesLink(
        seriesId: Long
    ) {
        comicDao.insertSeriesExternalId(
            SeriesExternalId(
                seriesId = seriesId,
                source = "METRON",
                externalId = "metron-series-100",
                url = "https://metron.cloud/series/100/"
            )
        )
    }

    private suspend fun syncScheduledRelease():
        ScheduledReleaseSyncSummary {

        val repository =
            ScheduledReleaseRepository(
                comicDao = comicDao,
                database = database,
                releaseDataProvider = FakeReleaseDataProvider(),
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

    private suspend fun applyComicVineRefresh(
        seriesId: Long
    ) {
        val repository =
            ComicRepository(
                comicDao = comicDao,
                database = database
            )

        repository.applyComicVineSeriesRefresh(
            seriesId = seriesId,
            comicVineVolumeId = 100200L,
            refreshedTitle = "Amazing Spider-Man",
            refreshedStartYear = 1999,
            volumeUrl = "https://comicvine.gamespot.com/volume/100200/",
            sourceIssues = listOf(
                ComicVineRefreshIssue(
                    comicVineIssueId = 200030L,
                    issueNumber = "30",
                    title = "Coming Home",
                    publicationDate = "2001-06-01",
                    coverUrl = null,
                    issueType = IssueType.REGULAR,
                    siteDetailsUrl = "https://comicvine.gamespot.com/issue/200030/"
                )
            )
        )
    }

    private class FakeReleaseDataProvider :
        ReleaseDataProvider {

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
            return ReleaseProviderResult.Success(
                listOf(
                    ScheduledReleaseData(
                        providerId = "METRON",
                        externalIssueId = "metron-issue-30",
                        externalSeriesId = "metron-series-100",
                        seriesTitle = "Amazing Spider-Man",
                        seriesVolume = 2,
                        seriesStartYear = 1999,
                        publisherName = "Marvel",
                        issueNumber = "30",
                        displayTitle = "Coming Home",
                        coverDate = "2001-06",
                        storeReleaseDate = "2001-06-20",
                        focDate = "2001-06-05",
                        price = "2.25",
                        priceCurrency = "USD",
                        distributorSku = "METRON-300",
                        upc = "1234567890123",
                        comicVineId = null,
                        gcdId = null,
                        sourceUrl = "https://metron.cloud/issue/300/",
                        sourceModifiedAt = "2026-10-01T12:00:00Z"
                    )
                )
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
    }
}
