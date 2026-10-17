package com.aschlus.comicreadingcompanion.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.IssueOwnership
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingProgress
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class CollectionViewModelTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: CollectionViewModel

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

        viewModel =
            CollectionViewModel(
                repository = repository
            )
    }

    @After
    fun tearDown() {
        runBlocking {
            viewModel
                .viewModelScope
                .coroutineContext[Job]
                ?.cancelAndJoin()
        }

        database.close()
    }

    @Test
    fun stats_countsOwnedIssuesSeriesPublishersAndReadingStatuses() =
        runBlocking {
            val marvelId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val dcId =
                comicDao.insertPublisher(
                    Publisher(name = "DC")
                )

            val spiderManSeriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = marvelId,
                        title = "Amazing Spider-Man",
                        volume = 1,
                        startYear = 1963,
                        endYear = null
                    )
                )

            val batmanSeriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = dcId,
                        title = "Batman",
                        volume = 1,
                        startYear = 1940,
                        endYear = null
                    )
                )

            val avengersSeriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = marvelId,
                        title = "Avengers",
                        volume = 1,
                        startYear = 1963,
                        endYear = null
                    )
                )

            fun createIssue(
                seriesId: Long,
                issueNumber: String
            ) = Issue(
                seriesId = seriesId,
                universeId = null,
                issueNumber = issueNumber,
                title = null,
                publicationDate = null,
                coverUrl = null,
                description = null,
                issueType = IssueType.REGULAR
            )

            val readIssueId =
                comicDao.insertIssue(
                    createIssue(spiderManSeriesId, "1")
                )

            val readingIssueId =
                comicDao.insertIssue(
                    createIssue(spiderManSeriesId, "2")
                )

            val unreadIssueId =
                comicDao.insertIssue(
                    createIssue(batmanSeriesId, "1")
                )

            val explicitUnreadIssueId =
                comicDao.insertIssue(
                    createIssue(avengersSeriesId, "1")
                )

            // This issue must not contribute to collection totals.
            comicDao.insertIssue(
                createIssue(batmanSeriesId, "2")
            )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = readIssueId,
                    status = ReadingStatus.READ,
                    startedAt = 1000L,
                    completedAt = 2000L,
                    notes = null
                )
            )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = readingIssueId,
                    status = ReadingStatus.READING,
                    startedAt = 3000L,
                    completedAt = null,
                    notes = null
                )
            )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = explicitUnreadIssueId,
                    status = ReadingStatus.UNREAD,
                    startedAt = null,
                    completedAt = null,
                    notes = null
                )
            )

            listOf(
                readIssueId,
                readingIssueId,
                unreadIssueId,
                explicitUnreadIssueId
            ).forEach { issueId ->
                comicDao.markIssueOwned(
                    IssueOwnership(issueId = issueId)
                )
            }

            val stats =
                withTimeout(5000L.milliseconds) {
                    viewModel.stats.first {
                        it.totalIssues == 4
                    }
                }

            assertEquals(4, stats.totalIssues)
            assertEquals(3, stats.seriesCount)
            assertEquals(2, stats.publisherCount)
            assertEquals(1, stats.readCount)
            assertEquals(1, stats.readingCount)
            assertEquals(2, stats.unreadCount)

            assertEquals(
                stats.totalIssues,
                stats.readCount +
                        stats.readingCount +
                        stats.unreadCount
            )
        }

    @Test
    fun updateSearchQuery_matchesSeriesTitleIssueNumberIssueTitleAndPublisher() =
        runBlocking {
            val marvelId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel Comics")
                )

            val dcId =
                comicDao.insertPublisher(
                    Publisher(name = "DC Comics")
                )

            val spiderManSeriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = marvelId,
                        title = "Amazing Spider-Man",
                        volume = 1,
                        startYear = 1963,
                        endYear = null
                    )
                )

            val batmanSeriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = dcId,
                        title = "Batman",
                        volume = 1,
                        startYear = 1940,
                        endYear = null
                    )
                )

            val spiderManIssueOneId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = spiderManSeriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Power and Responsibility",
                        publicationDate = "1963-03",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val spiderManIssueFifteenId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = spiderManSeriesId,
                        universeId = null,
                        issueNumber = "15",
                        title = "The Final Chapter",
                        publicationDate = "1964-08",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val batmanIssueOneId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = batmanSeriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "The Dark Knight",
                        publicationDate = "1940-04",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            listOf(
                spiderManIssueOneId,
                spiderManIssueFifteenId,
                batmanIssueOneId
            ).forEach { issueId ->
                comicDao.markIssueOwned(
                    IssueOwnership(issueId = issueId)
                )
            }

            withTimeout(5000L.milliseconds) {
                viewModel.visibleIssues.first {
                    it.size == 3
                }
            }

            // Search by series title.
            viewModel.updateSearchQuery("Amazing Spider-Man")

            val seriesMatches =
                withTimeout(5000L.milliseconds) {
                    viewModel.visibleIssues.first {
                        it.size == 2 &&
                                it.all { issue ->
                                    issue.seriesTitle == "Amazing Spider-Man"
                                }
                    }
                }

            assertEquals(
                setOf(
                    spiderManIssueOneId,
                    spiderManIssueFifteenId
                ),
                seriesMatches.map { it.issueId }.toSet()
            )

            // Search by issue number.
            viewModel.updateSearchQuery("15")

            val issueNumberMatches =
                withTimeout(5000L.milliseconds) {
                    viewModel.visibleIssues.first {
                        it.size == 1 &&
                                it.first().issueId == spiderManIssueFifteenId
                    }
                }

            assertEquals(
                spiderManIssueFifteenId,
                issueNumberMatches.single().issueId
            )

            // Search by issue title.
            viewModel.updateSearchQuery("Dark Knight")

            val issueTitleMatches =
                withTimeout(5000L.milliseconds) {
                    viewModel.visibleIssues.first {
                        it.size == 1 &&
                                it.first().issueId == batmanIssueOneId
                    }
                }

            assertEquals(
                batmanIssueOneId,
                issueTitleMatches.single().issueId
            )

            // Search by publisher.
            viewModel.updateSearchQuery("Marvel Comics")

            val publisherMatches =
                withTimeout(5000L.milliseconds) {
                    viewModel.visibleIssues.first {
                        it.size == 2 &&
                                it.all { issue ->
                                    issue.publisherName == "Marvel Comics"
                                }
                    }
                }

            assertEquals(
                setOf(
                    spiderManIssueOneId,
                    spiderManIssueFifteenId
                ),
                publisherMatches.map { it.issueId }.toSet()
            )

            // Filtering must not change the collection-wide statistics.
            assertEquals(
                3,
                viewModel.stats.value.totalIssues
            )
        }

    @Test
    fun updateSort_ordersOwnedIssuesByTitleAndPublicationDate() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val spiderManSeriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Amazing Spider-Man",
                        volume = 1,
                        startYear = 1963,
                        endYear = null
                    )
                )

            val batmanSeriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Batman",
                        volume = 1,
                        startYear = 1940,
                        endYear = null
                    )
                )

            fun createIssue(
                seriesId: Long,
                issueNumber: String,
                publicationDate: String
            ) = Issue(
                seriesId = seriesId,
                universeId = null,
                issueNumber = issueNumber,
                title = null,
                publicationDate = publicationDate,
                coverUrl = null,
                description = null,
                issueType = IssueType.REGULAR
            )

            val spiderManOneId =
                comicDao.insertIssue(
                    createIssue(
                        seriesId = spiderManSeriesId,
                        issueNumber = "1",
                        publicationDate = "1963-03"
                    )
                )

            val spiderManFifteenId =
                comicDao.insertIssue(
                    createIssue(
                        seriesId = spiderManSeriesId,
                        issueNumber = "15",
                        publicationDate = "1964-08"
                    )
                )

            val batmanOneId =
                comicDao.insertIssue(
                    createIssue(
                        seriesId = batmanSeriesId,
                        issueNumber = "1",
                        publicationDate = "1940-04"
                    )
                )

            listOf(
                spiderManOneId,
                spiderManFifteenId,
                batmanOneId
            ).forEach { issueId ->
                comicDao.markIssueOwned(
                    IssueOwnership(issueId = issueId)
                )
            }

            withTimeout(5000L.milliseconds) {
                viewModel.visibleIssues.first {
                    it.size == 3
                }
            }

            // Newest publication date first.
            viewModel.updateSort(
                CollectionSort.PUBLICATION_NEWEST
            )

            val newestFirst =
                withTimeout(5000L.milliseconds) {
                    viewModel.visibleIssues.first {
                        it.map { issue -> issue.issueId } ==
                                listOf(
                                    spiderManFifteenId,
                                    spiderManOneId,
                                    batmanOneId
                                )
                    }
                }

            assertEquals(
                listOf(
                    spiderManFifteenId,
                    spiderManOneId,
                    batmanOneId
                ),
                newestFirst.map { it.issueId }
            )

            // Oldest publication date first.
            viewModel.updateSort(
                CollectionSort.PUBLICATION_OLDEST
            )

            val oldestFirst =
                withTimeout(5000L.milliseconds) {
                    viewModel.visibleIssues.first {
                        it.map { issue -> issue.issueId } ==
                                listOf(
                                    batmanOneId,
                                    spiderManOneId,
                                    spiderManFifteenId
                                )
                    }
                }

            assertEquals(
                listOf(
                    batmanOneId,
                    spiderManOneId,
                    spiderManFifteenId
                ),
                oldestFirst.map { it.issueId }
            )

            // Series title A-Z, then publication date ascending.
            viewModel.updateSort(
                CollectionSort.TITLE_ASCENDING
            )

            val titleAscending =
                withTimeout(5000L.milliseconds) {
                    viewModel.visibleIssues.first {
                        it.map { issue -> issue.issueId } ==
                                listOf(
                                    spiderManOneId,
                                    spiderManFifteenId,
                                    batmanOneId
                                )
                    }
                }

            assertEquals(
                listOf(
                    spiderManOneId,
                    spiderManFifteenId,
                    batmanOneId
                ),
                titleAscending.map { it.issueId }
            )

            // Sorting must not change the collection-wide totals.
            assertEquals(
                3,
                viewModel.stats.value.totalIssues
            )
        }

    @Test
    fun ownershipAndReadingStatusChanges_updateCollectionAndStats() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Amazing Spider-Man",
                        volume = 1,
                        startYear = 1963,
                        endYear = null
                    )
                )

            val issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Power and Responsibility",
                        publicationDate = "1963-03",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            // An unowned issue must not appear in Collection.
            assertEquals(
                0,
                withTimeout(5000L.milliseconds) {
                    viewModel.visibleIssues.first {
                        it.isEmpty()
                    }.size
                }
            )
            assertEquals(0, viewModel.stats.value.totalIssues)

            // Mark the issue as owned.
            comicDao.markIssueOwned(
                IssueOwnership(issueId = issueId)
            )

            withTimeout(5000L.milliseconds) {
                viewModel.stats.first {
                    it.totalIssues == 1 &&
                            it.unreadCount == 1
                }
            }

            assertEquals(
                issueId,
                viewModel.visibleIssues.value.single().issueId
            )

            // Update reading progress for the already-owned issue.
            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = issueId,
                    status = ReadingStatus.READ,
                    startedAt = 1000L,
                    completedAt = 2000L,
                    notes = null
                )
            )

            withTimeout(5000L.milliseconds) {
                viewModel.stats.first {
                    it.totalIssues == 1 &&
                            it.readCount == 1 &&
                            it.unreadCount == 0
                }
            }

            // Removing ownership should remove the issue and reset totals.
            comicDao.markIssueUnowned(issueId)

            withTimeout(5000L.milliseconds) {
                viewModel.stats.first {
                    it.totalIssues == 0 &&
                            it.seriesCount == 0 &&
                            it.publisherCount == 0 &&
                            it.readCount == 0 &&
                            it.readingCount == 0 &&
                            it.unreadCount == 0
                }
            }

            assertEquals(
                emptyList<Any>(),
                viewModel.visibleIssues.value
            )
        }
}