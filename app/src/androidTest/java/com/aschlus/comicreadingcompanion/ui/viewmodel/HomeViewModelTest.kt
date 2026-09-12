package com.aschlus.comicreadingcompanion.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListItem
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
import com.aschlus.comicreadingcompanion.data.preferences.HomeUiPreferences

@RunWith(AndroidJUnit4::class)
class HomeViewModelTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: HomeViewModel
    private lateinit var homeUiPreferences: HomeUiPreferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        homeUiPreferences = HomeUiPreferences(context)

        runBlocking {
            homeUiPreferences.clearRecentlyOpenedReadingLists()
        }

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

        viewModel = HomeViewModel(
            repository = repository,
            homeUiPreferences = homeUiPreferences
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
    fun readingLists_exposesReadingLists() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val firstListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "First Reading List",
                        description = "First",
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val secondListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Second Reading List",
                        description = "Second",
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 2000L,
                        updatedAt = 2000L
                    )
                )

            val readingLists =
                withTimeout(5000L.milliseconds) {
                    viewModel.readingLists.first { it.size == 2 }
                }

            assertEquals(2, readingLists.size)
            assertEquals(secondListId, readingLists[0].id)
            assertEquals("Second Reading List", readingLists[0].title)
            assertEquals(firstListId, readingLists[1].id)
            assertEquals("First Reading List", readingLists[1].title)
        }

    @Test
    fun readingListSummaries_exposesProgressCounts() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Home Summary Test Series",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val firstIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "First",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val secondIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "2",
                        title = "Second",
                        publicationDate = "2000-02",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Home Summary Test List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            listOf(firstIssueId, secondIssueId).forEachIndexed { index, issueId ->
                comicDao.insertReadingListItem(
                    ReadingListItem(
                        readingListId = readingListId,
                        sectionId = null,
                        issueId = issueId,
                        position = index + 1,
                        required = true,
                        notes = null
                    )
                )
            }

            repository.markIssueAsRead(firstIssueId)

            val summary =
                withTimeout(5000L.milliseconds) {
                    viewModel.readingListSummaries.first { summaries ->
                        summaries.any {
                            it.readingListId == readingListId &&
                                it.totalCount == 2 &&
                                it.readCount == 1
                        }
                    }
                }.first { it.readingListId == readingListId }


            assertEquals(2, summary.totalCount)
            assertEquals(1, summary.readCount)
        }

    @Test
    fun continueItems_selectsFirstUnreadIssueForEachList() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Home Continue Test Series",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueIds =
                (1..4).map { number ->
                    comicDao.insertIssue(
                        Issue(
                            seriesId = seriesId,
                            universeId = null,
                            issueNumber = number.toString(),
                            title = "Issue $number",
                            publicationDate = "2000-0$number",
                            coverUrl = null,
                            description = null,
                            issueType = IssueType.REGULAR
                        )
                    )
                }

            val firstListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "First Continue List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val secondListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Second Continue List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 2000L,
                        updatedAt = 2000L
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = firstListId,
                    sectionId = null,
                    issueId = issueIds[0],
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = firstListId,
                    sectionId = null,
                    issueId = issueIds[1],
                    position = 2,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = secondListId,
                    sectionId = null,
                    issueId = issueIds[2],
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = secondListId,
                    sectionId = null,
                    issueId = issueIds[3],
                    position = 2,
                    required = true,
                    notes = null
                )
            )

            repository.markIssueAsRead(issueIds[0])
            repository.markIssueAsReading(issueIds[2])

            val continueItems =
                withTimeout(5000L.milliseconds) {
                    viewModel.continueItems.first { items ->
                        items.size == 2 &&
                            items[firstListId]?.issueId == issueIds[1] &&
                            items[secondListId]?.issueId == issueIds[2]

                    }
                }
            assertEquals(issueIds[1], continueItems[firstListId]?.issueId)
            assertEquals(2, continueItems[firstListId]?.position)
            assertEquals(issueIds[2], continueItems[secondListId]?.issueId)
            assertEquals(1, continueItems[secondListId]?.position)
        }

    @Test
    fun continueItems_reactsToProgressChanges() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Reactive Continue Test Series",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueIds =
                (1..3).map { number ->
                    comicDao.insertIssue(
                        Issue(
                            seriesId = seriesId,
                            universeId = null,
                            issueNumber = number.toString(),
                            title = "Issue $number",
                            publicationDate = "2000-0$number",
                            coverUrl = null,
                            description = null,
                            issueType = IssueType.REGULAR
                        )
                    )
                }

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Reactive Continue List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            issueIds.forEachIndexed { index, issueId ->
                comicDao.insertReadingListItem(
                    ReadingListItem(
                        readingListId = readingListId,
                        sectionId = null,
                        issueId = issueId,
                        position = index + 1,
                        required = true,
                        notes = null
                    )
                )
            }

            val initialContinue =
                withTimeout(5000L.milliseconds) {
                    viewModel.continueItems.first { items ->
                        items[readingListId]?.issueId == issueIds[0]
                    }
                }
            assertEquals(issueIds[0], initialContinue[readingListId]?.issueId)
            repository.markIssueAsRead(issueIds[0])
            val secondContinue =
                withTimeout(5000L.milliseconds) {
                    viewModel.continueItems.first { items ->
                        items[readingListId]?.issueId == issueIds[1]
                    }
                }
            assertEquals(issueIds[1], secondContinue[readingListId]?.issueId)
            repository.markIssueAsRead(issueIds[1])
            val thirdContinue =
                withTimeout(5000L.milliseconds) {
                    viewModel.continueItems.first { items ->
                        items[readingListId]?.issueId == issueIds[2]
                    }
                }
            assertEquals(issueIds[2], thirdContinue[readingListId]?.issueId)
            repository.markIssueAsRead(issueIds[2])
            val completedContinue =
                withTimeout(5000L.milliseconds) {
                    viewModel.continueItems.first { items ->
                        !items.containsKey(readingListId)
                    }
                }
            assertEquals(false, completedContinue.containsKey(readingListId))
        }

    @Test
    fun visibleReadingLists_filtersBySearchQuery() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name = "Marvel"
                    )
                )

            comicDao.insertReadingList(
                ReadingList(
                    title =
                        "Ultimate Marvel",
                    description =
                        "Complete Earth-1610 reading order",
                    publisherId =
                        publisherId,
                    universeId = null,
                    createdAt = 1000L,
                    updatedAt = 1000L
                )
            )

            comicDao.insertReadingList(
                ReadingList(
                    title =
                        "Spider-Man Volume 2 Era",
                    description =
                        "Earth-616 Spider-Man chronology",
                    publisherId =
                        publisherId,
                    universeId = null,
                    createdAt = 2000L,
                    updatedAt = 2000L
                )
            )

            comicDao.insertReadingList(
                ReadingList(
                    title =
                        "Avengers Test List",
                    description =
                        "A separate test chronology",
                    publisherId =
                        publisherId,
                    universeId = null,
                    createdAt = 3000L,
                    updatedAt = 3000L
                )
            )

            withTimeout(
                5000L.milliseconds
            ) {
                viewModel
                    .visibleReadingLists
                    .first {
                        it.size == 3
                    }
            }

            viewModel.updateSearchQuery(
                "spider"
            )

            val titleMatches =
                withTimeout(
                    5000L.milliseconds
                ) {
                    viewModel
                        .visibleReadingLists
                        .first {
                            it.size == 1
                        }
                }

            assertEquals(
                "Spider-Man Volume 2 Era",
                titleMatches.single().title
            )

            viewModel.updateSearchQuery(
                "1610"
            )

            val descriptionMatches =
                withTimeout(
                    5000L.milliseconds
                ) {
                    viewModel
                        .visibleReadingLists
                        .first {
                            it.size == 1 &&
                                    it.single().title ==
                                    "Ultimate Marvel"
                        }
                }

            assertEquals(
                "Ultimate Marvel",
                descriptionMatches.single().title
            )

            viewModel.updateSearchQuery(
                "ULTIMATE"
            )

            val caseInsensitiveMatches =
                withTimeout(
                    5000L.milliseconds
                ) {
                    viewModel
                        .visibleReadingLists
                        .first {
                            it.size == 1 &&
                                    it.single().title ==
                                    "Ultimate Marvel"
                        }
                }

            assertEquals(
                "Ultimate Marvel",
                caseInsensitiveMatches
                    .single()
                    .title
            )

            viewModel.clearSearchQuery()

            val clearedResults =
                withTimeout(
                    5000L.milliseconds
                ) {
                    viewModel
                        .visibleReadingLists
                        .first {
                            it.size == 3
                        }
                }

            assertEquals(
                3,
                clearedResults.size
            )
            assertEquals(
                "",
                viewModel.searchQuery.value
            )
        }

    @Test
    fun visibleReadingLists_appliesSelectedSort() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name = "Marvel"
                    )
                )

            comicDao.insertReadingList(
                ReadingList(
                    title = "Beta Reading List",
                    description = null,
                    publisherId = publisherId,
                    universeId = null,
                    createdAt = 1000L,
                    updatedAt = 2000L
                )
            )

            comicDao.insertReadingList(
                ReadingList(
                    title = "Alpha Reading List",
                    description = null,
                    publisherId = publisherId,
                    universeId = null,
                    createdAt = 2000L,
                    updatedAt = 3000L
                )
            )

            comicDao.insertReadingList(
                ReadingList(
                    title = "Gamma Reading List",
                    description = null,
                    publisherId = publisherId,
                    universeId = null,
                    createdAt = 3000L,
                    updatedAt = 1000L
                )
            )

            val recentlyUpdated =
                withTimeout(
                    5000L.milliseconds
                ) {
                    viewModel
                        .visibleReadingLists
                        .first {
                            it.size == 3
                        }
                }

            assertEquals(
                listOf(
                    "Alpha Reading List",
                    "Beta Reading List",
                    "Gamma Reading List"
                ),
                recentlyUpdated.map {
                    it.title
                }
            )

            viewModel.updateSort(
                HomeReadingListSort
                    .TITLE_ASCENDING
            )

            val ascending =
                withTimeout(
                    5000L.milliseconds
                ) {
                    viewModel
                        .visibleReadingLists
                        .first {
                            it.size == 3 &&
                                    it.first().title ==
                                    "Alpha Reading List" &&
                                    it.last().title ==
                                    "Gamma Reading List"
                        }
                }

            assertEquals(
                listOf(
                    "Alpha Reading List",
                    "Beta Reading List",
                    "Gamma Reading List"
                ),
                ascending.map {
                    it.title
                }
            )

            viewModel.updateSort(
                HomeReadingListSort
                    .TITLE_DESCENDING
            )

            val descending =
                withTimeout(
                    5000L.milliseconds
                ) {
                    viewModel
                        .visibleReadingLists
                        .first {
                            it.size == 3 &&
                                    it.first().title ==
                                    "Gamma Reading List"
                        }
                }

            assertEquals(
                listOf(
                    "Gamma Reading List",
                    "Beta Reading List",
                    "Alpha Reading List"
                ),
                descending.map {
                    it.title
                }
            )

            assertEquals(
                HomeReadingListSort
                    .TITLE_DESCENDING,
                viewModel.sort.value
            )
        }

    @Test
    fun recentlyOpenedReadingLists_preservesRecentOrderAndIgnoresMissingLists() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name = "Marvel"
                    )
                )

            val firstListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "First Recent List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val secondListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Second Recent List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 2000L,
                        updatedAt = 2000L
                    )
                )

            withTimeout(
                5000L.milliseconds
            ) {
                viewModel.readingLists.first {
                    it.size == 2
                }
            }

            viewModel.recordReadingListOpened(
                firstListId
            )

            viewModel.recordReadingListOpened(
                999999L
            )

            viewModel.recordReadingListOpened(
                secondListId
            )

            val recentLists =
                withTimeout(
                    5000L.milliseconds
                ) {
                    viewModel
                        .recentlyOpenedReadingLists
                        .first { lists ->
                            lists.size == 2 &&
                                    lists[0].id ==
                                    secondListId &&
                                    lists[1].id ==
                                    firstListId
                        }
                }

            assertEquals(
                listOf(
                    secondListId,
                    firstListId
                ),
                recentLists.map {
                    it.id
                }
            )
        }

    @Test
    fun continueReadingLists_includesOnlyPartiallyReadLists() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name = "Marvel"
                    )
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Test Series",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            suspend fun insertIssue(
                number: String
            ): Long {
                return comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = number,
                        title = "Issue $number",
                        publicationDate = null,
                        coverUrl = null,
                        description = null,
                        issueType =
                            IssueType.REGULAR
                    )
                )
            }

            suspend fun insertList(
                title: String,
                issueIds: List<Long>,
                updatedAt: Long
            ): Long {
                val readingListId =
                    comicDao.insertReadingList(
                        ReadingList(
                            title = title,
                            description = null,
                            publisherId =
                                publisherId,
                            universeId = null,
                            createdAt = updatedAt,
                            updatedAt = updatedAt
                        )
                    )

                issueIds.forEachIndexed {
                        index,
                        issueId ->

                    comicDao.insertReadingListItem(
                        ReadingListItem(
                            readingListId =
                                readingListId,
                            sectionId = null,
                            issueId = issueId,
                            position = index + 1,
                            required = true,
                            notes = null
                        )
                    )
                }

                return readingListId
            }

            val firstIssueId =
                insertIssue("1")

            val secondIssueId =
                insertIssue("2")

            val thirdIssueId =
                insertIssue("3")

            val fourthIssueId =
                insertIssue("4")

            val inProgressListId =
                insertList(
                    title = "In Progress",
                    issueIds =
                        listOf(
                            firstIssueId,
                            secondIssueId
                        ),
                    updatedAt = 3000L
                )

            insertList(
                title = "Not Started",
                issueIds =
                    listOf(
                        thirdIssueId
                    ),
                updatedAt = 2000L
            )

            insertList(
                title = "Completed",
                issueIds =
                    listOf(
                        fourthIssueId
                    ),
                updatedAt = 1000L
            )

            repository.markIssueAsRead(
                firstIssueId
            )

            repository.markIssueAsRead(
                fourthIssueId
            )

            val continueLists =
                withTimeout(
                    5000L.milliseconds
                ) {
                    viewModel
                        .continueReadingLists
                        .first { lists ->
                            lists.size == 1
                        }
                }

            assertEquals(
                listOf(inProgressListId),
                continueLists.map {
                    it.id
                }
            )
        }
}