
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PullListRepositoryTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository

    private var publisherId = -1L

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

        runBlocking {
            publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name = "Marvel Comics"
                    )
                )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addSeriesToPullList_persistsSeriesAndMetadata() =
        runBlocking {

            val seriesId =
                createSeries()

            comicDao.insertIssue(
                createIssue(
                    seriesId = seriesId,
                    issueNumber = "1"
                )
            )

            comicDao.insertIssue(
                createIssue(
                    seriesId = seriesId,
                    issueNumber = "2"
                )
            )

            assertFalse(
                repository
                    .isSeriesOnPullList(seriesId)
                    .first()
            )

            assertTrue(
                repository.addSeriesToPullList(seriesId)
            )

            assertTrue(
                repository
                    .isSeriesOnPullList(seriesId)
                    .first()
            )

            val results =
                repository
                    .getPullListSeries()
                    .first()

            assertEquals(1, results.size)

            val result = results.single()

            assertEquals(
                seriesId,
                result.seriesId
            )

            assertEquals(
                "Amazing Spider-Man",
                result.title
            )

            assertEquals(
                2,
                result.volume
            )

            assertEquals(
                1999,
                result.startYear
            )

            assertEquals(
                2003,
                result.endYear
            )

            assertEquals(
                "Marvel Comics",
                result.publisherName
            )

            assertEquals(
                2,
                result.totalCount
            )
        }

    @Test
    fun addSeriesToPullList_ignoresDuplicateEntries() =
        runBlocking {

            val seriesId =
                createSeries()

            assertTrue(
                repository.addSeriesToPullList(seriesId)
            )

            assertTrue(
                repository.addSeriesToPullList(seriesId)
            )

            val results =
                repository
                    .getPullListSeries()
                    .first()

            assertEquals(1, results.size)

            assertEquals(
                seriesId,
                results.single().seriesId
            )
        }

    @Test
    fun removeSeriesFromPullList_removesTrackedSeries() =
        runBlocking {

            val seriesId =
                createSeries()

            repository.addSeriesToPullList(seriesId)

            assertTrue(
                repository
                    .isSeriesOnPullList(seriesId)
                    .first()
            )

            repository.removeSeriesFromPullList(seriesId)

            assertFalse(
                repository
                    .isSeriesOnPullList(seriesId)
                    .first()
            )

            assertTrue(
                repository
                    .getPullListSeries()
                    .first()
                    .isEmpty()
            )
        }

    @Test
    fun addSeriesToPullList_returnsFalseForUnknownSeries() =
        runBlocking {

            assertFalse(
                repository.addSeriesToPullList(
                    seriesId = 9999L
                )
            )

            assertTrue(
                repository
                    .getPullListSeries()
                    .first()
                    .isEmpty()
            )
        }

    private suspend fun createSeries(): Long {
        return comicDao.insertSeries(
            Series(
                publisherId = publisherId,
                title = "Amazing Spider-Man",
                volume = 2,
                startYear = 1999,
                endYear = 2003
            )
        )
    }

    private fun createIssue(
        seriesId: Long,
        issueNumber: String
    ) = Issue(
        seriesId = seriesId,
        universeId = null,
        issueNumber = issueNumber,
        title = "Test Issue $issueNumber",
        publicationDate = "2001-06",
        coverUrl = null,
        description = null,
        issueType = IssueType.REGULAR
    )
}
