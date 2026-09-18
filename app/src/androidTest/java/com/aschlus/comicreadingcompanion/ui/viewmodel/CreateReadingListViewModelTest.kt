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
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSource
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListStyle
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import com.aschlus.comicreadingcompanion.data.database.models.IssueSearchResult
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds
import com.aschlus.comicreadingcompanion.data.database.models.SeriesSearchResult

@RunWith(AndroidJUnit4::class)
class CreateReadingListViewModelTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: CreateReadingListViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

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

        viewModel = CreateReadingListViewModel(repository = repository)
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
    fun publishers_exposesPublishers() =
        runBlocking {
            val marvelId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )
            val dcId =
                comicDao.insertPublisher(
                    Publisher(name = "DC")
                )

            val publishers =
                withTimeout(5000L.milliseconds) {
                    viewModel.publishers.first { it.size == 2 }
                }

            assertEquals(
                setOf(marvelId, dcId),
                publishers.map { it.id }.toSet()
            )
        }

    @Test
    fun createReadingList_createsUserReadingListFromForm() =
        runBlocking {
            val publisherId = comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )
            viewModel.updateTitle("  My Spider-Man List  ")
            viewModel.updateDescription("  My custom order  ")
            viewModel.selectPublisher(publisherId)
            viewModel.createReadingList()
            val readingListId =
                withTimeout(5000L.milliseconds) {
                    viewModel.createdReadingListId.first { it != null }
                }
            assertNotNull(readingListId)
            val readingList =
                comicDao.getReadingListById(readingListId!!)
            assertNotNull(readingList)
            assertEquals("My Spider-Man List", readingList?.title)
            assertEquals("My custom order", readingList?.description)
            assertEquals(publisherId, readingList?.publisherId)
            assertNull(readingList?.universeId)
            assertEquals(ReadingListStyle.GREEN, readingList?.style)
            assertEquals(ReadingListSource.USER, readingList?.source)
        }

    @Test
    fun selectPublisher_loadsUniversesForSelectedPublisher() =
        runBlocking {
            val marvelId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val dcId =
                comicDao.insertPublisher(
                    Publisher(name = "DC")
                )

            val earth616Id =
                comicDao.insertUniverse(
                    Universe(
                        publisherId = marvelId,
                        name = "Earth-616",
                        designation = "Earth-616",
                        description = null
                    )
                )

            val ultimateId =
                comicDao.insertUniverse(
                    Universe(
                        publisherId = marvelId,
                        name = "Ultimate Universe",
                        designation = "Earth-1610",
                        description = null
                    )
                )

            comicDao.insertUniverse(
                Universe(
                    publisherId = dcId,
                    name = "Prime Earth",
                    designation = "Earth-0",
                    description = null
                )
            )

            viewModel.selectPublisher(marvelId)
            val universes =
                withTimeout(5000L.milliseconds) {
                    viewModel.universes.first { it.size == 2 }
                }
            assertEquals(
                setOf(earth616Id, ultimateId),
                universes.map { it.id }.toSet()
            )
            assertEquals(marvelId, viewModel.selectedPublisherId.value)
        }

    @Test
    fun createReadingList_usesSelectedUniverse() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val universeId =
                comicDao.insertUniverse(
                    Universe(
                        publisherId = publisherId,
                        name = "Earth-616",
                        designation = "Earth-616",
                        description = null
                    )
                )

            viewModel.updateTitle("Earth-616 Reading List")
            viewModel.selectPublisher(publisherId)

            withTimeout(5000L.milliseconds) {
                viewModel.universes.first { it.any { universe ->
                    universe.id == universeId
                } }
            }

            viewModel.selectUniverse(universeId)

            assertEquals(universeId, viewModel.selectedUniverseId.value)
            viewModel.createReadingList()

            val readingListId =
                withTimeout(5000L.milliseconds) {
                    viewModel.createdReadingListId.first { it != null }
                }

            val readingList =
                comicDao.getReadingListById(readingListId!!)

            assertNotNull(readingList)
            assertEquals(publisherId, readingList?.publisherId)
            assertEquals(universeId, readingList?.universeId)
            assertEquals(ReadingListSource.USER, readingList?.source)
        }

    @Test
    fun canCreateReadingList_requiresTitleAndPublisher() =
        runBlocking {
            assertEquals(false, viewModel.canCreateReadingList.value)
            viewModel.updateTitle("My Reading List")
            assertEquals(false, viewModel.canCreateReadingList.first { !it })
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )
            viewModel.selectPublisher(publisherId)
            val enabled =
                withTimeout(5000L.milliseconds) {
                    viewModel.canCreateReadingList.first { it }
                }
            assertEquals(true, enabled)
            viewModel.updateTitle("   ")
            val disabledAgain =
                withTimeout(5000L.milliseconds) {
                    viewModel.canCreateReadingList.first { !it }
                }
            assertEquals(false, disabledAgain)
        }

    @Test
    fun selectPublisher_clearsSelectedUniverseAndLoadsNewUniverse() =
        runBlocking {
            val marvelId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val dcId =
                comicDao.insertPublisher(
                    Publisher(name = "DC")
                )

            val earth616Id =
                comicDao.insertUniverse(
                    Universe(
                        publisherId = marvelId,
                        name = "Earth-616",
                        designation = "Earth-616",
                        description = null
                    )
                )

            val primeEarthId =
                comicDao.insertUniverse(
                    Universe(
                        publisherId = dcId,
                        name = "Prime Earth",
                        designation = "Earth-0",
                        description = null
                    )
                )

            viewModel.selectPublisher(marvelId)
            withTimeout(5000L.milliseconds) {
                viewModel.universes.first { it.any { universe ->
                    universe.id == earth616Id
                } }
            }
            viewModel.selectUniverse(earth616Id)
            assertEquals(earth616Id, viewModel.selectedUniverseId.value)

            viewModel.selectPublisher(dcId)
            assertNull(viewModel.selectedUniverseId.value)
            val dcUniverses =
                withTimeout(5000L.milliseconds) {
                    viewModel.universes.first { it.size == 1 && it.first().id == primeEarthId }
                }
            assertEquals(1, dcUniverses.size)
            assertEquals(primeEarthId, dcUniverses.first().id)
            assertEquals(dcId, viewModel.selectedPublisherId.value)
        }

    @Test
    fun createReadingList_usesSelectedStyle() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Test Publisher")
                )

            viewModel.updateTitle("Styled Reading List")
            viewModel.selectPublisher(publisherId)
            viewModel.selectStyle(ReadingListStyle.PURPLE)

            assertEquals(ReadingListStyle.PURPLE, viewModel.selectedStyle.value)

            viewModel.createReadingList()

            val readingListId = withTimeout(5000L.milliseconds) {
                viewModel
                    .createdReadingListId
                    .first { it != null }
            }

            val readingList =
                comicDao.getReadingListById(readingListId!!)

            assertNotNull(readingList)
            assertEquals(ReadingListStyle.PURPLE, readingList?.style)
        }

    @Test
    fun addPendingIssue_addsIssueOnce() =
        runBlocking {
            val pendingIssue =
                PendingReadingListIssue(
                    issueId = 10L,
                    seriesId = 20L,
                    seriesTitle = "Test Series",
                    issueNumber = "1",
                    issueTitle = "First Issue",
                    publicationDate = "2026-01-01",
                    coverUrl = null
                )

            viewModel.addPendingIssue(pendingIssue)
            viewModel.addPendingIssue(pendingIssue)

            assertEquals(1, viewModel.pendingIssues.value.size)
            assertEquals(
                10L,
                viewModel.pendingIssues.value.single().issueId
            )
        }

    @Test
    fun removePendingIssue_removesSelectedIssue() =
        runBlocking {
            viewModel.addPendingIssue(
                PendingReadingListIssue(
                    issueId = 10L,
                    seriesId = 20L,
                    seriesTitle = "Test Series",
                    issueNumber = "1",
                    issueTitle = null,
                    publicationDate = null,
                    coverUrl = null
                )
            )

            viewModel.addPendingIssue(
                PendingReadingListIssue(
                    issueId = 11L,
                    seriesId = 20L,
                    seriesTitle = "Test Series",
                    issueNumber = "2",
                    issueTitle = null,
                    publicationDate = null,
                    coverUrl = null
                )
            )

            viewModel.removePendingIssue(10L)

            assertEquals(
                listOf(11L),
                viewModel.pendingIssues.value.map { it.issueId }
            )
        }

    @Test
    fun addPendingSeries_addsAllSeriesIssues() =
        runBlocking {
            val publisherId = comicDao.insertPublisher(
                    Publisher(name = "Test Publisher")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Test Series",
                        volume = 1,
                        startYear = 2026,
                        endYear = null
                    )
                )

            val firstIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "First Issue",
                        publicationDate = "2026-01-01",
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
                        title = "Second Issue",
                        publicationDate = "2026-02-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            viewModel.addPendingSeries(
                seriesId = seriesId,
                seriesTitle = "Test Series"
            )

            val pendingIssues = withTimeout(5000L.milliseconds) {
                    viewModel.pendingIssues.first {
                        it.size == 2
                    }
                }

            assertEquals(
                setOf(firstIssueId, secondIssueId),
                pendingIssues.map { it.issueId }.toSet()
            )

            assertEquals(
                setOf("Test Series"),
                pendingIssues.map { it.seriesTitle }.toSet()
            )
        }

    @Test
    fun addPendingSeries_doesNotDuplicateAlreadySelectedIssue() =
        runBlocking {
            val publisherId = comicDao.insertPublisher(
                    Publisher(name = "Test Publisher")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Test Series",
                        volume = 1,
                        startYear = 2026,
                        endYear = null
                    )
                )

            val firstIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "First Issue",
                        publicationDate = null,
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
                        title = "Second Issue",
                        publicationDate = null,
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            viewModel.addPendingIssue(
                PendingReadingListIssue(
                    issueId = firstIssueId,
                    seriesId = seriesId,
                    seriesTitle = "Test Series",
                    issueNumber = "1",
                    issueTitle = "First Issue",
                    publicationDate = null,
                    coverUrl = null
                )
            )

            viewModel.addPendingSeries(
                seriesId = seriesId,
                seriesTitle = "Test Series"
            )

            val pendingIssues = withTimeout(5000L.milliseconds) {
                    viewModel.pendingIssues.first {
                        it.size == 2
                    }
                }

            assertEquals(
                listOf(firstIssueId, secondIssueId),
                pendingIssues.map { it.issueId }
            )
        }

    @Test
    fun createReadingList_createsPendingIssuesInOrder() =
        runBlocking {
            val publisherId = comicDao.insertPublisher(
                    Publisher(name = "Test Publisher")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId =
                            publisherId,
                        title = "Test Series",
                        volume = 1,
                        startYear = 2026,
                        endYear = null
                    )
                )

            val firstIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "First",
                        publicationDate = null,
                        coverUrl = null,
                        description = null,
                        issueType =
                            IssueType.REGULAR
                    )
                )

            val secondIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "2",
                        title = "Second",
                        publicationDate = null,
                        coverUrl = null,
                        description = null,
                        issueType =
                            IssueType.REGULAR
                    )
                )

            viewModel.updateTitle("New Reading List")
            viewModel.selectPublisher(publisherId)
            viewModel.addPendingIssue(
                PendingReadingListIssue(
                    issueId = secondIssueId,
                    seriesId = seriesId,
                    seriesTitle =
                        "Test Series",
                    issueNumber = "2",
                    issueTitle = "Second",
                    publicationDate = null,
                    coverUrl = null
                )
            )
            viewModel.addPendingIssue(
                PendingReadingListIssue(
                    issueId = firstIssueId,
                    seriesId = seriesId,
                    seriesTitle =
                        "Test Series",
                    issueNumber = "1",
                    issueTitle = "First",
                    publicationDate = null,
                    coverUrl = null
                )
            )
            viewModel.createReadingList()

            val readingListId = withTimeout(5000L.milliseconds) {
                    viewModel
                        .createdReadingListId
                        .first {
                            it != null
                        }
                }!!

            val items = comicDao.getItemsForReadingList(readingListId)

            assertEquals(
                listOf(secondIssueId, firstIssueId),
                items.map { it.issueId }
            )
            assertEquals(
                listOf(1, 2),
                items.map { it.position }
            )
        }

    @Test
    fun updateAddToListQuery_returnsSeriesAndIssueResults() =
        runBlocking {
            val publisherId = comicDao.insertPublisher(
                Publisher(name = "Test Publisher")
            )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Test Series",
                        volume = 1,
                        startYear = 2026,
                        endYear = null
                    )
                )

            val issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Test Issue",
                        publicationDate = "2026-01-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            viewModel.updateAddToListQuery("Test")

            val seriesResults = withTimeout(5000L.milliseconds) {
                    viewModel
                        .addToListSeriesResults
                        .first {
                            it.isNotEmpty()
                        }
                }

            val issueResults = withTimeout(5000L.milliseconds) {
                    viewModel
                        .addToListIssueResults
                        .first {
                            it.isNotEmpty()
                        }
                }

            assertTrue(seriesResults.any { it.seriesId == seriesId })
            assertTrue(issueResults.any { it.issueId == issueId })
        }

    @Test
    fun clearAddToListSearch_resetsQueryResultsAndFilter() =
        runBlocking {
            val publisherId = comicDao.insertPublisher(
                Publisher(name = "Test Publisher")
            )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Test Series",
                        volume = 1,
                        startYear = 2026,
                        endYear = null
                    )
                )

            comicDao.insertIssue(
                Issue(
                    seriesId = seriesId,
                    universeId = null,
                    issueNumber = "1",
                    title = "Test Issue",
                    publicationDate = null,
                    coverUrl = null,
                    description = null,
                    issueType = IssueType.REGULAR
                )
            )

            viewModel.selectAddToListFilter(AddToListFilter.ISSUES)
            viewModel.updateAddToListQuery("Test")

            withTimeout(5000L.milliseconds) {
                viewModel
                    .addToListIssueResults
                    .first {
                        it.isNotEmpty()
                    }
            }

            viewModel.clearAddToListSearch()

            assertEquals("", viewModel.addToListQuery.value)
            assertEquals(AddToListFilter.ALL, viewModel.addToListFilter.value)
            assertTrue(viewModel.addToListSeriesResults.value.isEmpty())
            assertTrue(viewModel.addToListIssueResults.value.isEmpty())
        }

    @Test
    fun cancelAddToListSession_discardsDraftChanges() =
        runBlocking {
            val originalIssue =
                PendingReadingListIssue(
                    issueId = 10L,
                    seriesId = 20L,
                    seriesTitle = "Original Series",
                    issueNumber = "1",
                    issueTitle = null,
                    publicationDate = null,
                    coverUrl = null
                )

            viewModel.addPendingIssue(originalIssue)

            viewModel.beginAddToListSession()

            viewModel.toggleAddToListIssue(
                IssueSearchResult(
                    issueId = 11L,
                    seriesId = 21L,
                    seriesTitle = "New Series",
                    seriesVolume = 1,
                    issueNumber = "2",
                    issueTitle = "New Issue",
                    publicationDate = null,
                    publisherName = "Test Publisher",
                    readingStatus = null
                )
            )

            assertEquals(
                setOf(10L, 11L),
                viewModel.addToListDraftIssues.value.map { it.issueId }.toSet()
            )

            viewModel.cancelAddToListSession()

            assertEquals(
                listOf(10L),
                viewModel.pendingIssues.value.map { it.issueId }
            )
            assertTrue(viewModel.addToListDraftIssues.value.isEmpty())
        }

    @Test
    fun applyAddToListSession_commitsDraftChanges() =
        runBlocking {
            viewModel.addPendingIssue(
                PendingReadingListIssue(
                    issueId = 10L,
                    seriesId = 20L,
                    seriesTitle = "Original Series",
                    issueNumber = "1",
                    issueTitle = null,
                    publicationDate = null,
                    coverUrl = null
                )
            )

            viewModel.beginAddToListSession()

            viewModel.toggleAddToListIssue(
                IssueSearchResult(
                    issueId = 11L,
                    seriesId = 21L,
                    seriesTitle = "New Series",
                    seriesVolume = 1,
                    issueNumber = "2",
                    issueTitle = "New Issue",
                    publicationDate = null,
                    publisherName = "Test Publisher",
                    readingStatus = null
                )
            )

            viewModel.applyAddToListSession()

            assertEquals(
                listOf(10L, 11L),
                viewModel.pendingIssues.value.map { it.issueId }
            )
            assertTrue(viewModel.addToListDraftIssues.value.isEmpty())
        }

    @Test
    fun toggleAddToListSeries_addsAllSeriesIssuesWithoutDuplicates() =
        runBlocking {
            val publisherId = comicDao.insertPublisher(
                Publisher(name = "Test Publisher")
            )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Test Series",
                        volume = 1,
                        startYear = 2026,
                        endYear = null
                    )
                )

            val firstIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "First Issue",
                        publicationDate = null,
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
                        title = "Second Issue",
                        publicationDate = null,
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            viewModel.beginAddToListSession()

            viewModel.toggleAddToListIssue(
                IssueSearchResult(
                    issueId = firstIssueId,
                    seriesId = seriesId,
                    seriesTitle = "Test Series",
                    seriesVolume = 1,
                    issueNumber = "1",
                    issueTitle = "First Issue",
                    publicationDate = null,
                    publisherName = "Test Publisher",
                    readingStatus = null
                )
            )

            viewModel.toggleAddToListSeries(
                SeriesSearchResult(
                    seriesId = seriesId,
                    title = "Test Series",
                    volume = 1,
                    startYear = 2026,
                    endYear = null,
                    publisherName = "Test Publisher",
                    totalCount = 2,
                    readCount = 0
                )
            )

            val draftIssues = withTimeout(5000L.milliseconds) {
                    viewModel
                        .addToListDraftIssues
                        .first {
                            it.size == 2
                        }
                }

            assertEquals(
                listOf(firstIssueId, secondIssueId),
                draftIssues.map { it.issueId }
            )
        }

    @Test
    fun toggleAddToListSeries_removesSeriesWhenFullySelected() =
        runBlocking {
            val publisherId = comicDao.insertPublisher(
                Publisher(name = "Test Publisher")
            )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Test Series",
                        volume = 1,
                        startYear = 2026,
                        endYear = null
                    )
                )

            comicDao.insertIssue(
                Issue(
                    seriesId = seriesId,
                    universeId = null,
                    issueNumber = "1",
                    title = null,
                    publicationDate = null,
                    coverUrl = null,
                    description = null,
                    issueType = IssueType.REGULAR
                )
            )

            comicDao.insertIssue(
                Issue(
                    seriesId = seriesId,
                    universeId = null,
                    issueNumber = "2",
                    title = null,
                    publicationDate = null,
                    coverUrl = null,
                    description = null,
                    issueType = IssueType.REGULAR
                )
            )

            val result =
                SeriesSearchResult(
                    seriesId = seriesId,
                    title = "Test Series",
                    volume = 1,
                    startYear = 2026,
                    endYear = null,
                    publisherName = "Test Publisher",
                    totalCount = 2,
                    readCount = 0
                )

            viewModel.beginAddToListSession()
            viewModel.toggleAddToListSeries(result)

            withTimeout(5000L.milliseconds) {
                viewModel
                    .addToListDraftIssues
                    .first {
                        it.size == 2
                    }
            }

            assertTrue(viewModel.isAddToListSeriesSelected(seriesId = seriesId, totalCount = 2))

            viewModel.toggleAddToListSeries(result)

            withTimeout(5000L.milliseconds) {
                viewModel
                    .addToListDraftIssues
                    .first {
                        it.isEmpty()
                    }
            }

            assertTrue(viewModel.addToListDraftIssues.value.isEmpty())
        }

    @Test
    fun movePendingIssue_reordersIssues() =
        runBlocking {
            viewModel.addPendingIssue(
                PendingReadingListIssue(
                    issueId = 10L,
                    seriesId = 20L,
                    seriesTitle = "Test Series",
                    issueNumber = "1",
                    issueTitle = null,
                    publicationDate = null,
                    coverUrl = null
                )
            )

            viewModel.addPendingIssue(
                PendingReadingListIssue(
                    issueId = 11L,
                    seriesId = 20L,
                    seriesTitle = "Test Series",
                    issueNumber = "2",
                    issueTitle = null,
                    publicationDate = null,
                    coverUrl = null
                )
            )

            viewModel.addPendingIssue(
                PendingReadingListIssue(
                    issueId = 12L,
                    seriesId = 20L,
                    seriesTitle = "Test Series",
                    issueNumber = "3",
                    issueTitle = null,
                    publicationDate = null,
                    coverUrl = null
                )
            )

            viewModel.movePendingIssue(
                issueId = 12L,
                offset = -1
            )

            assertEquals(
                listOf(10L, 12L, 11L),
                viewModel.pendingIssues.value.map { it.issueId }
            )

            viewModel.movePendingIssue(issueId = 10L, offset = 1)

            assertEquals(
                listOf(12L, 10L, 11L),
                viewModel.pendingIssues.value.map { it.issueId }
            )
        }

    @Test
    fun movePendingIssue_ignoresOutOfBoundsMoves() =
        runBlocking {
            viewModel.addPendingIssue(
                PendingReadingListIssue(
                    issueId = 10L,
                    seriesId = 20L,
                    seriesTitle = "Test Series",
                    issueNumber = "1",
                    issueTitle = null,
                    publicationDate = null,
                    coverUrl = null
                )
            )

            viewModel.addPendingIssue(
                PendingReadingListIssue(
                    issueId = 11L,
                    seriesId = 20L,
                    seriesTitle = "Test Series",
                    issueNumber = "2",
                    issueTitle = null,
                    publicationDate = null,
                    coverUrl = null
                )
            )

            viewModel.movePendingIssue(issueId = 10L, offset = -1)
            viewModel.movePendingIssue(issueId = 11L, offset = 1)

            assertEquals(
                listOf(10L, 11L),
                viewModel.pendingIssues.value.map { it.issueId }
            )
        }

    @Test
    fun createReadingList_withMultipleContinuitiesStoresNoSingleUniverse() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name = "Multiverse Publisher"
                    )
                )

            val firstUniverseId =
                comicDao.insertUniverse(
                    Universe(
                        publisherId =
                            publisherId,
                        name =
                            "First Universe",
                        designation =
                            "Earth-1",
                        description =
                            null
                    )
                )

            val secondUniverseId =
                comicDao.insertUniverse(
                    Universe(
                        publisherId =
                            publisherId,
                        name =
                            "Second Universe",
                        designation =
                            "Earth-2",
                        description =
                            null
                    )
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId =
                            publisherId,
                        title =
                            "Multiverse Create Series",
                        volume = 1,
                        startYear = 2026,
                        endYear = null
                    )
                )

            val firstIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId =
                            seriesId,
                        universeId =
                            firstUniverseId,
                        issueNumber =
                            "1",
                        title =
                            "First Universe Issue",
                        publicationDate =
                            "2026-01",
                        coverUrl =
                            null,
                        description =
                            null,
                        issueType =
                            IssueType.REGULAR
                    )
                )

            val secondIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId =
                            seriesId,
                        universeId =
                            secondUniverseId,
                        issueNumber =
                            "2",
                        title =
                            "Second Universe Issue",
                        publicationDate =
                            "2026-02",
                        coverUrl =
                            null,
                        description =
                            null,
                        issueType =
                            IssueType.REGULAR
                    )
                )

            viewModel.updateTitle(
                "Multiverse Reading List"
            )

            viewModel.selectPublisher(
                publisherId
            )

            withTimeout(
                5000L.milliseconds
            ) {
                viewModel.universes.first {
                    it.size == 2
                }
            }

            viewModel.selectUniverse(
                firstUniverseId
            )

            viewModel.addPendingIssue(
                PendingReadingListIssue(
                    issueId =
                        firstIssueId,
                    seriesId =
                        seriesId,
                    seriesTitle =
                        "Multiverse Create Series",
                    issueNumber =
                        "1",
                    issueTitle =
                        "First Universe Issue",
                    publicationDate =
                        "2026-01",
                    coverUrl =
                        null
                )
            )

            viewModel.addPendingIssue(
                PendingReadingListIssue(
                    issueId =
                        secondIssueId,
                    seriesId =
                        seriesId,
                    seriesTitle =
                        "Multiverse Create Series",
                    issueNumber =
                        "2",
                    issueTitle =
                        "Second Universe Issue",
                    publicationDate =
                        "2026-02",
                    coverUrl =
                        null
                )
            )

            viewModel.createReadingList()

            val readingListId =
                withTimeout(
                    5000L.milliseconds
                ) {
                    viewModel
                        .createdReadingListId
                        .first {
                            it != null
                        }
                }!!

            val readingList =
                comicDao.getReadingListById(
                    readingListId
                )!!

            assertNull(
                readingList.universeId
            )

            assertEquals(
                listOf(
                    firstIssueId,
                    secondIssueId
                ),
                comicDao
                    .getItemsForReadingList(
                        readingListId
                    )
                    .map { item ->
                        item.issueId
                    }
            )
        }
}