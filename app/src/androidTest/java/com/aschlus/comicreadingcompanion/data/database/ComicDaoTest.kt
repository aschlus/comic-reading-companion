package com.aschlus.comicreadingcompanion.data.database

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.entities.ExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListItem
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSection
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingProgress
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.SeriesExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComicDaoTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao

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
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertPublisher_canRetrievePublisherByName() =
        runBlocking {
            comicDao.insertPublisher(
                Publisher(name = "Marvel")
            )

            val publisher =
                comicDao.getPublisherByName("Marvel")

            assertNotNull(publisher)

            assertEquals("Marvel", publisher?.name)
        }

    @Test
    fun getAllPublishersFlow_returnsInsertedPublishers() =
        runBlocking {
            comicDao.insertPublisher(
                Publisher(name = "Marvel")
            )

            comicDao.insertPublisher(
                Publisher(name = "DC")
            )

            val publishers =
                comicDao.getAllPublishersFlow().first()

            assertEquals(2, publishers.size)

            assertEquals(
                listOf("DC", "Marvel"),
                publishers.map { publisher -> publisher.name }
            )
        }

    @Test
    fun getUniverseByDesignation_returnsUniversForPublisher() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            comicDao.insertUniverse(
                Universe(
                    publisherId = publisherId,
                    name = "Marvel Universe",
                    designation = "Earth-616",
                    description = "Primary Marvel continuity"
                )
            )

            val universe =
                comicDao.getUniverseByDesignation(
                    publisherId = publisherId,
                    designation = "Earth-616"
                )

            assertNotNull(universe)
            assertEquals("Marvel Universe", universe?.name)
            assertEquals("Earth-616", universe?.designation)
        }

    @Test
    fun getUniverseByDesignation_scopesDesignationToPublisher() =
        runBlocking {
            val marvelPublisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val dcPublisherId =
                comicDao.insertPublisher(
                    Publisher(name = "DC")
                )

            comicDao.insertUniverse(
                Universe(
                    publisherId = marvelPublisherId,
                    name = "Marvel Test Universe",
                    designation = "Earth-Test",
                    description = null
                )
            )

            comicDao.insertUniverse(
                Universe(
                    publisherId = dcPublisherId,
                    name = "DC Test Universe",
                    designation = "Earth-Test",
                    description = null
                )
            )

            val marvelUniverse =
                comicDao.getUniverseByDesignation(
                    publisherId = marvelPublisherId,
                    designation = "Earth-Test"
                )

            val dcUniverse =
                comicDao.getUniverseByDesignation(
                    publisherId = dcPublisherId,
                    designation = "Earth-Test"
                )

            assertNotNull(marvelUniverse)
            assertNotNull(dcUniverse)
            assertEquals("Marvel Test Universe", marvelUniverse?.name)
            assertEquals("DC Test Universe", dcUniverse?.name)
            assertEquals(marvelPublisherId, marvelUniverse?.publisherId)
            assertEquals(dcPublisherId, dcUniverse?.publisherId)
        }

    @Test
    fun getSeries_returnsMatchingTitleAndVolume() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            comicDao.insertSeries(
                Series(
                    publisherId = publisherId,
                    title = "Amazing Spider-Man",
                    volume = 2,
                    startYear = 1999,
                    endYear = 2003
                )
            )

            val series =
                comicDao.getSeries(
                    publisherId = publisherId,
                    title = "Amazing Spider-Man",
                    volume = 2
                )

            assertNotNull(series)
            assertEquals("Amazing Spider-Man", series?.title)
            assertEquals(2, series?.volume)
        }

    @Test
    fun getSeries_handlesNullVolume() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Independent Publisher")
                )

            comicDao.insertSeries(
                Series(
                    publisherId = publisherId,
                    title = "Untitled Volume Test",
                    volume = null,
                    startYear = null,
                    endYear = null
                )
            )

            val series =
                comicDao.getSeries(
                    publisherId = publisherId,
                    title = "Untitled Volume Test",
                    volume = null
                )

            assertNotNull(series)
            assertEquals("Untitled Volume Test", series?.title)
            assertEquals(null, series?.volume)
        }

    @Test
    fun getIssue_returnsMatchingSeriesAndIssueNumber() =
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
                        volume = 2,
                        startYear = 1999,
                        endYear = 2003
                    )
                )

            comicDao.insertIssue(
                Issue(
                    seriesId = seriesId,
                    universeId = null,
                    issueNumber = "30",
                    title = "Test Issue",
                    publicationDate = "2001-06",
                    coverUrl = null,
                    description = null,
                    issueType = IssueType.REGULAR
                )
            )

            val issue =
                comicDao.getIssue(
                    seriesId = seriesId,
                    issueNumber = "30"
                )

            assertNotNull(issue)
            assertEquals("30", issue?.issueNumber)
            assertEquals("Test Issue", issue?.title)
            assertEquals(seriesId, issue?.seriesId)
        }

    @Test
    fun getIssuesForSeries_returnsIssuesInPublicationOrder() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Spider-Man Test Series",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2001
                    )
                )

            comicDao.insertIssue(
                Issue(
                    seriesId = seriesId,
                    universeId = null,
                    issueNumber = "3",
                    title = "Third",
                    publicationDate = "2000-03",
                    coverUrl = null,
                    description = null,
                    issueType = IssueType.REGULAR
                )
            )

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

            val issues =
                comicDao.getIssuesForSeries(seriesId)

            assertEquals(3, issues.size)
            assertEquals(
                listOf("1", "2", "3"),
                issues.map { issue -> issue.issueNumber }
            )
        }

    @Test
    fun getAllReadingLists_returnsMostRecentlyUpdatedFirst() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            comicDao.insertReadingList(
                ReadingList(
                    title = "Older List",
                    description = null,
                    publisherId = publisherId,
                    universeId = null,
                    createdAt = 1000L,
                    updatedAt = 1000L
                )
            )

            comicDao.insertReadingList(
                ReadingList(
                    title = "Newer List",
                    description = null,
                    publisherId = publisherId,
                    universeId = null,
                    createdAt = 2000L,
                    updatedAt = 3000L
                )
            )

            val readingLists = comicDao.getAllReadingLists().first()

            assertEquals(
                listOf("Newer List", "Older List"),
                readingLists.map { readingList -> readingList.title }
            )
        }

    @Test
    fun getSectionsForReadingList_returnsSectionsInPositionOrder() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Section Test",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            comicDao.insertReadingListSection(
                ReadingListSection(
                    readingListId = readingListId,
                    title = "Third",
                    description = null,
                    position = 3
                )
            )

            comicDao.insertReadingListSection(
                ReadingListSection(
                    readingListId = readingListId,
                    title = "First",
                    description = null,
                    position = 1
                )
            )

            comicDao.insertReadingListSection(
                ReadingListSection(
                    readingListId = readingListId,
                    title = "Second",
                    description = null,
                    position = 2
                )
            )

            val sections =
                comicDao.getSectionsForReadingList(readingListId)

            assertEquals(
                listOf(1, 2, 3),
                sections.map { section -> section.position }
            )
        }

    @Test
    fun getItemsForReadingList_returnsItemsInReadingOrder() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
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

            val issueOneId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Issue One",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val issueTwoId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "2",
                        title = "Issue Two",
                        publicationDate = "2000-02",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Item Order Test",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = null,
                    issueId = issueTwoId,
                    position = 2,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = null,
                    issueId = issueOneId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            val items =
                comicDao.getItemsForReadingList(readingListId)

            assertEquals(
                listOf(1, 2),
                items.map { item -> item.position }
            )

            assertEquals(
                listOf(issueOneId, issueTwoId),
                items.map { item -> item.issueId }
            )
        }

    @Test
    fun getItemsForSection_returnsOnlyItemsInRequestedSection() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Section Item Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueOneId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Issue One",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val issueTwoId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "2",
                        title = "Issue Two",
                        publicationDate = "2000-02",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val issueThreeId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "3",
                        title = "Issue Three",
                        publicationDate = "2000-03",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Section Query Test",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val firstSectionId =
                comicDao.insertReadingListSection(
                    ReadingListSection(
                        readingListId = readingListId,
                        title = "First Section",
                        description = null,
                        position = 1
                    )
                )

            val secondSectionId =
                comicDao.insertReadingListSection(
                    ReadingListSection(
                        readingListId = readingListId,
                        title = "Second Section",
                        description = null,
                        position = 2
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = firstSectionId,
                    issueId = issueTwoId,
                    position = 2,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = secondSectionId,
                    issueId = issueThreeId,
                    position = 3,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = firstSectionId,
                    issueId = issueOneId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            val items =
                comicDao.getItemsForSection(
                    readingListId = readingListId,
                    sectionId = firstSectionId
                )

            assertEquals(2, items.size)
            assertEquals(
                listOf(issueOneId, issueTwoId),
                items.map { item -> item.issueId }
            )
            assertEquals(
                listOf(1, 2),
                items.map { item -> item.position}
            )
        }

    @Test
    fun getUnsectionedItemsForReadingList_returnsOnlyUnsectionedItems() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Unsectioned Item Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueOneId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Issue One",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val issueTwoId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "2",
                        title = "Issue Two",
                        publicationDate = "2000-02",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val issueThreeId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "3",
                        title = "Issue Three",
                        publicationDate = "2000-03",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Unsectioned Query Test",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val sectionId =
                comicDao.insertReadingListSection(
                    ReadingListSection(
                        readingListId = readingListId,
                        title = "Section",
                        description = null,
                        position = 1
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = null,
                    issueId = issueTwoId,
                    position = 2,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = sectionId,
                    issueId = issueThreeId,
                    position = 3,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = null,
                    issueId = issueOneId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            val items =
                comicDao.getUnsectionedItemsForReadingList(
                    readingListId
                )

            assertEquals(2, items.size)
            assertEquals(
                listOf(issueOneId, issueTwoId),
                items.map { item -> item.issueId }
            )
            assertEquals(
                listOf(1, 2),
                items.map { item -> item.position }
            )
        }

    @Test
    fun getReadingListIssues_returnsJoinedMetadataAndReadingStatus() =
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
                        volume = 2,
                        startYear = 1999,
                        endYear = 2003
                    )
                )

            val issueOneId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "30",
                        title = "Coming Home",
                        publicationDate = "2001-06",
                        coverUrl = "https://example.com/30.jpg",
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val issueTwoId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "31",
                        title = "The Conversation",
                        publicationDate = "2001-07",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Joined Query Test",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val sectionId =
                comicDao.insertReadingListSection(
                    ReadingListSection(
                        readingListId = readingListId,
                        title = "Coming Home",
                        description = "First story arc",
                        position = 1
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = sectionId,
                    issueId = issueOneId,
                    position = 1,
                    required = false,
                    notes = "Optional note"
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = null,
                    issueId = issueTwoId,
                    position = 2,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = issueOneId,
                    status = ReadingStatus.READ,
                    startedAt = 2000L,
                    completedAt = 3000L,
                    notes = null
                )
            )

            val issues =
                comicDao.getReadingListIssues(readingListId).first()

            assertEquals(2, issues.size)

            val firstIssue = issues[0]
            assertEquals(1, firstIssue.position)
            assertEquals(issueOneId, firstIssue.issueId)
            assertEquals("Amazing Spider-Man", firstIssue.seriesTitle)
            assertEquals(2, firstIssue.seriesVolume)
            assertEquals("30", firstIssue.issueNumber)
            assertEquals("Coming Home", firstIssue.issueTitle)
            assertEquals("2001-06", firstIssue.publicationDate)
            assertEquals("https://example.com/30.jpg", firstIssue.coverUrl)
            assertEquals(false, firstIssue.required)
            assertEquals("Optional note", firstIssue.notes)
            assertEquals(sectionId, firstIssue.sectionId)
            assertEquals("Coming Home", firstIssue.sectionTitle)
            assertEquals("First story arc", firstIssue.sectionDescription)
            assertEquals(1, firstIssue.sectionPosition)
            assertEquals(ReadingStatus.READ, firstIssue.readingStatus)

            val secondIssue = issues[1]
            assertEquals(2, secondIssue.position)
            assertEquals(issueTwoId, secondIssue.issueId)
            assertEquals(null, secondIssue.sectionId)
            assertEquals(null, secondIssue.sectionTitle)
            assertEquals(null, secondIssue.readingStatus)
        }

    @Test
    fun getReadingListSummaries_calculatesTotalAndReadCounts() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Summary Test Series",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueOneId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Issue One",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val issueTwoId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "2",
                        title = "Issue Two",
                        publicationDate = "2000-02",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val issueThreeId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "3",
                        title = "Issue Three",
                        publicationDate = "2000-03",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Summary Test",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            listOf(issueOneId, issueTwoId, issueThreeId).forEachIndexed { index, issueId ->
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

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = issueOneId,
                    status = ReadingStatus.READ,
                    startedAt = 1000L,
                    completedAt = 2000L,
                    notes = null
                )
            )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = issueTwoId,
                    status = ReadingStatus.READING,
                    startedAt = 3000L,
                    completedAt = null,
                    notes = null
                )
            )

            val summaries =
                comicDao.getReadingListSummaries().first()

            assertEquals(1, summaries.size)

            val summary = summaries.first()
            assertEquals(readingListId, summary.readingListId)
            assertEquals(3, summary.totalCount)
            assertEquals(1, summary.readCount)
        }

    @Test
    fun getUnreadReadingListItems_excludesReadIssuesAndPreservesOrder() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Continue Test Series",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueOneId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Issue One",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val issueTwoId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "2",
                        title = "Issue Two",
                        publicationDate = "2000-02",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val issueThreeId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "3",
                        title = "Issue Three",
                        publicationDate = "2000-03",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Continue Test",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            listOf(issueOneId, issueTwoId, issueThreeId).forEachIndexed { index, issueId ->
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

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = issueOneId,
                    status = ReadingStatus.READ,
                    startedAt = 1000L,
                    completedAt = 2000L,
                    notes = null
                )
            )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = issueTwoId,
                    status = ReadingStatus.READING,
                    startedAt = 3000L,
                    completedAt = null,
                    notes = null
                )
            )

            val continueItems = comicDao.getUnreadReadingListItems().first()
            assertEquals(2, continueItems.size)
            assertEquals(
                listOf(issueTwoId, issueThreeId),
                continueItems.map { item -> item.issueId }
            )
            assertEquals(
                listOf(2, 3),
                continueItems.map { item -> item.position }
            )
            assertEquals("Continue Test Series", continueItems.first().seriesTitle)
            assertEquals("2", continueItems.first().issueNumber)
        }

    @Test
    fun searchSeries_isCaseInsensitive() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val matchingSeriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Amazing Spider-Man",
                        volume = 2,
                        startYear = 1999,
                        endYear = 2003
                    )
                )

            comicDao.insertSeries(
                Series(
                    publisherId = publisherId,
                    title = "Daredevil",
                    volume = 2,
                    startYear = 1998,
                    endYear = 2009
                )
            )
            @Suppress("SpellCheckingInspection")
            val results = comicDao.searchSeries("sPiDeR").first()

            assertEquals(1, results.size)

            val result = results.first()

            assertEquals(matchingSeriesId, result.seriesId)
            assertEquals("Amazing Spider-Man", result.title)
            assertEquals("Marvel", result.publisherName)
        }

    @Test
    fun searchIssues_matchesSeriesTitleIssueTitleAndIssueNumber() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Spectacular Spider-Man",
                        volume = 1,
                        startYear = 1976,
                        endYear = 1998
                    )
                )

            val issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "42",
                        title = "The Final Chapter",
                        publicationDate = "1980-05",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val bySeriesTitle = comicDao.searchIssues("spectacular").first()
            assertEquals(1, bySeriesTitle.size)
            assertEquals(issueId, bySeriesTitle.first().issueId)
            val byIssueTitle = comicDao.searchIssues("final").first()
            assertEquals(1, byIssueTitle.size)
            assertEquals(issueId, byIssueTitle.first().issueId)
            val byIssueNumber = comicDao.searchIssues("42").first()
            assertEquals(1, byIssueNumber.size)
            assertEquals(issueId, byIssueNumber.first().issueId)
            assertEquals("Spectacular Spider-Man", byIssueNumber.first().seriesTitle)
            assertEquals("The Final Chapter", byIssueNumber.first().issueTitle)
            assertEquals("Marvel", byIssueNumber.first().publisherName)
        }

    @Test
    fun getExternalIds_returnsMatchingSourceAndExternalId() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "External ID Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Test Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            @Suppress("SpellCheckingInspection")
            comicDao.insertExternalId(
                ExternalId(
                    issueId = issueId,
                    source = "comicvine",
                    externalId = "4000-12345",
                    url = "https://example.com/comicvine"
                )
            )

            comicDao.insertExternalId(
                ExternalId(
                    issueId = issueId,
                    source = "other_provider",
                    externalId = "ABC-123",
                    url = null
                )
            )

            @Suppress("SpellCheckingInspection")
            val externalId =
                comicDao.getExternalId(
                    source = "comicvine",
                    externalId = "4000-12345"
                )

            assertNotNull(externalId)
            assertEquals(issueId, externalId?.issueId)
            @Suppress("SpellCheckingInspection")
            assertEquals("comicvine", externalId?.source)
            assertEquals("4000-12345", externalId?.externalId)
            assertEquals("https://example.com/comicvine", externalId?.url)
        }

    @Test
    fun upsertReadingProgress_updatesExistingProgressForIssue() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Progress Upsert Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Test Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val progressId =
                comicDao.insertReadingProgress(
                    ReadingProgress(
                        issueId = issueId,
                        status = ReadingStatus.READING,
                        startedAt = 1000L,
                        completedAt = null,
                        notes = "Original note"
                    )
                )

            comicDao.upsertReadingProgress(
                ReadingProgress(
                    id = progressId,
                    issueId = issueId,
                    status = ReadingStatus.READ,
                    startedAt = 1000L,
                    completedAt = 2000L,
                    notes = "Updated note"
                )
            )

            val progress = comicDao.getReadingProgressForIssue(issueId)

            assertNotNull(progress)
            assertEquals(progressId, progress?.id)
            assertEquals(ReadingStatus.READ, progress?.status)
            assertEquals(1000L, progress?.startedAt)
            assertEquals(2000L, progress?.completedAt)
            assertEquals("Updated note", progress?.notes)
        }

    @Test
    fun getReadingProgressForIssues_returnsONlyRequestedIssues() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Batch Progress Retrieval Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueOneId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Issue One",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val issueTwoId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "2",
                        title = "Issue Two",
                        publicationDate = "2000-02",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val issueThreeId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "3",
                        title = "Issue Three",
                        publicationDate = "2000-03",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = issueOneId,
                    status = ReadingStatus.READ,
                    startedAt = 1000L,
                    completedAt = 2000L,
                    notes = null
                )
            )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = issueTwoId,
                    status = ReadingStatus.READING,
                    startedAt = 3000L,
                    completedAt = null,
                    notes = null
                )
            )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = issueThreeId,
                    status = ReadingStatus.READ,
                    startedAt = 4000L,
                    completedAt = 5000L,
                    notes = null
                )
            )

            val progress =
                comicDao.getReadingProgressForIssues(
                    listOf(issueOneId, issueThreeId)
                )

            assertEquals(2, progress.size)
            assertEquals(
                setOf(issueOneId, issueThreeId),
                progress.map{ item -> item.issueId}.toSet()
            )
        }

    @Test
    fun deleteReadingProgressForIssues_removesOnlyRequestedIssues() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Batch Progress Delete Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueOneId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Issue One",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val issueTwoId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "2",
                        title = "Issue Two",
                        publicationDate = "2000-02",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val issueThreeId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "3",
                        title = "Issue Three",
                        publicationDate = "2000-03",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            listOf(issueOneId, issueTwoId, issueThreeId).forEach { issueId ->
                comicDao.insertReadingProgress(
                    ReadingProgress(
                        issueId = issueId,
                        status = ReadingStatus.READ,
                        startedAt = 1000L,
                        completedAt = 2000L,
                        notes = null
                    )
                )
            }

            comicDao.deleteReadingProgressForIssues(
                listOf(issueOneId, issueThreeId)
            )

            val issueOneProgress = comicDao.getReadingProgressForIssue(issueOneId)
            val issueTwoProgress = comicDao.getReadingProgressForIssue(issueTwoId)
            val issueThreeProgress = comicDao.getReadingProgressForIssue(issueThreeId)

            assertEquals(null, issueOneProgress)
            assertNotNull(issueTwoProgress)
            assertEquals(ReadingStatus.READ, issueTwoProgress?.status)
            assertEquals(null, issueThreeProgress)
        }

    @Test
    fun getReadingListItem_returnsItemForRequestedReadingListAndIssue() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Reading List Item Lookup Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Test Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val firstReadingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "First List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val secondReadingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Second List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 2000L,
                        updatedAt = 2000L
                    )
                )

            val firstItemId =
                comicDao.insertReadingListItem(
                    ReadingListItem(
                        readingListId = firstReadingListId,
                        sectionId = null,
                        issueId = issueId,
                        position = 1,
                        required = true,
                        notes = "First list note"
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = secondReadingListId,
                    sectionId = null,
                    issueId = issueId,
                    position = 5,
                    required = true,
                    notes = "Second list note"
                )
            )

            val item =
                comicDao.getReadingListItem(
                    readingListId = firstReadingListId,
                    issueId = issueId
                )

            assertNotNull(item)
            assertEquals(firstItemId, item?.id)
            assertEquals(firstReadingListId, item?.readingListId)
            assertEquals(issueId, item?.issueId)
            assertEquals(1, item?.position)
            assertEquals(true, item?.required)
            assertEquals("First list note", item?.notes)
        }

    @Test
    fun getExternalIdsForIssue_returnsExternalIdsInSourceOrder() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "External ID Order Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Test Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            comicDao.insertExternalId(
                ExternalId(
                    issueId = issueId,
                    source = "marvel",
                    externalId = "MARVEL-1",
                    url = null
                )
            )

            comicDao.insertExternalId(
                ExternalId(
                    issueId = issueId,
                    source = "comicvine",
                    externalId = "CV-1",
                    url = null
                )
            )

            comicDao.insertExternalId(
                ExternalId(
                    issueId = issueId,
                    source = "gcd",
                    externalId = "GCD-1",
                    url = null
                )
            )

            val externalIds = comicDao.getExternalIdsForIssue(issueId)
            assertEquals(3, externalIds.size)
            assertEquals(
                listOf("comicvine", "gcd", "marvel"),
                externalIds.map { externalId -> externalId.source }
            )
            assertEquals(
                setOf(issueId),
                externalIds.map { externalId -> externalId.issueId }.toSet()
            )
        }

    @Test
    fun deleteReadingList_cascadesSectionsAndItemsButPreservesIssueAndProgress() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Reading List Cascade Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Test Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = issueId,
                    status = ReadingStatus.READ,
                    startedAt = 1000L,
                    completedAt = 2000L,
                    notes = null
                )
            )

            val readingList =
                ReadingList(
                    title = "CascadeTest",
                    description = null,
                    publisherId = publisherId,
                    universeId = null,
                    createdAt = 1000L,
                    updatedAt = 1000L
                )

            val readingListId = comicDao.insertReadingList(readingList)

            val sectionId =
                comicDao.insertReadingListSection(
                    ReadingListSection(
                        readingListId = readingListId,
                        title = "Test Section",
                        description = null,
                        position = 1
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = sectionId,
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            comicDao.deleteReadingList(readingList.copy(id = readingListId))
            val deletedReadingList = comicDao.getReadingListById(readingListId)
            val sections = comicDao.getSectionsForReadingList(readingListId)
            val items = comicDao.getItemsForReadingList(readingListId)
            val issue = comicDao.getIssueById(issueId)
            val progress = comicDao.getReadingProgressForIssue(issueId)

            assertEquals(null, deletedReadingList)
            assertEquals(0, sections.size)
            assertEquals(0, items.size)
            assertNotNull(issue)
            assertNotNull(progress)
            assertEquals(ReadingStatus.READ, progress?.status)
        }

    @Test
    fun deleteReadingListSection_setsItemSectionIdToNull() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Section Delete Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Test Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Section Delete Test",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val section =
                ReadingListSection(
                    readingListId = readingListId,
                    title = "Delete Me",
                    description = null,
                    position = 1
                )

            val sectionId = comicDao.insertReadingListSection(section)

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = sectionId,
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            comicDao.deleteReadingListSection(section.copy(id = sectionId))

            val sections = comicDao.getSectionsForReadingList(readingListId)
            val item = comicDao.getReadingListItem(
                readingListId = readingListId,
                issueId = issueId
            )

            assertEquals(0, sections.size)
            assertNotNull(item)
            assertEquals(null, item?.sectionId)
        }

    @Test
    fun getSeriesDetail_returnsSeriesAndPublisherMetadata() =
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
                        volume = 2,
                        startYear = 1999,
                        endYear = 2003
                    )
                )

            val detail =
                comicDao.getSeriesDetail(seriesId).first()

            assertNotNull(detail)
            assertEquals(seriesId, detail?.seriesId)
            assertEquals("Amazing Spider-Man", detail?.title)
            assertEquals(2, detail?.volume)
            assertEquals(1999, detail?.startYear)
            assertEquals(2003, detail?.endYear)
            assertEquals(publisherId, detail?.publisherId)
            assertEquals("Marvel", detail?.publisherName)
        }

    @Test
    fun getSeriesIssues_returnsIssueMetadataAndReadingStatusInOrder() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Series Issue Projection Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueTwoId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "2",
                        title = "Second Issue",
                        publicationDate = "2000-02",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val issueOneId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "First Issue",
                        publicationDate = "2000-01",
                        coverUrl = "https://example.com/1.jpg",
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = issueOneId,
                    status = ReadingStatus.READ,
                    startedAt = 1000L,
                    completedAt = 2000L,
                    notes = null
                )
            )

            val issues = comicDao.getSeriesIssues(seriesId).first()

            assertEquals(2, issues.size)
            val firstIssue = issues[0]
            assertEquals(issueOneId, firstIssue.issueId)
            assertEquals("1", firstIssue.issueNumber)
            assertEquals("First Issue", firstIssue.issueTitle)
            assertEquals("2000-01", firstIssue.publicationDate)
            assertEquals("https://example.com/1.jpg", firstIssue.coverUrl)
            assertEquals(IssueType.REGULAR, firstIssue.issueType)
            assertEquals(ReadingStatus.READ, firstIssue.readingStatus)
            val secondIssue = issues[1]
            assertEquals(issueTwoId, secondIssue.issueId)
            assertEquals("2", secondIssue.issueNumber)
            assertEquals(null, secondIssue.readingStatus)
        }

    @Test
    fun getPublisherSeries_returnsSeriesMetadataAndPrgoressCount() =
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
                        volume = 2,
                        startYear = 1999,
                        endYear = 2003
                    )
                )

            val issueOneId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title = "Issue One",
                        publicationDate = "1999-01",
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
                    title = "Issue Two",
                    publicationDate = "1999-02",
                    coverUrl = null,
                    description = null,
                    issueType = IssueType.REGULAR
                )
            )

            val issueThreeId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = null,
                        issueNumber = "3",
                        title = "Issue Three",
                        publicationDate = "1999-03",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = issueOneId,
                    status = ReadingStatus.READ,
                    startedAt = 1000L,
                    completedAt = 2000L,
                    notes = null
                )
            )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = issueThreeId,
                    status = ReadingStatus.READING,
                    startedAt = 3000L,
                    completedAt = null,
                    notes = null
                )
            )

            val series = comicDao.getPublisherSeries(publisherId).first()

            assertEquals(1, series.size)
            val result = series.first()
            assertEquals(seriesId, result.seriesId)
            assertEquals("Amazing Spider-Man", result.title)
            assertEquals(2, result.volume)
            assertEquals(1999, result.startYear)
            assertEquals(2003, result.endYear)
            assertEquals(3, result.totalCount)
            assertEquals(1, result.readCount)
        }

    @Test
    fun getIssueDetail_returnsJoinedIssueMetadataAndReadingStatus() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val universeId =
                comicDao.insertUniverse(
                    Universe(
                        publisherId = publisherId,
                        name = "Marvel Universe",
                        designation = "Earth-616",
                        description = "Main Marvel continuity"
                    )
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Amazing Spider-Man",
                        volume = 2,
                        startYear = 1999,
                        endYear = 2003
                    )
                )

            val issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = universeId,
                        issueNumber = "30",
                        title = "Coming Home",
                        publicationDate = "2001-06",
                        coverUrl = "https://example.com/30.jpg",
                        description = "Spider-Man faces a dangerous new threat.",
                        issueType = IssueType.REGULAR
                    )
                )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = issueId,
                    status = ReadingStatus.READING,
                    startedAt = 1000L,
                    completedAt = null,
                    notes = null
                )
            )

            val detail = comicDao.getIssueDetail(issueId).first()

            assertEquals(issueId, detail.issueId)
            assertEquals(seriesId, detail.seriesId)
            assertEquals("Amazing Spider-Man", detail.seriesTitle)
            assertEquals(2, detail.seriesVolume)
            assertEquals("30", detail.issueNumber)
            assertEquals("Coming Home", detail.issueTitle)
            assertEquals("2001-06", detail.publicationDate)
            assertEquals("https://example.com/30.jpg", detail.coverUrl)
            assertEquals("Spider-Man faces a dangerous new threat.", detail.description)
            assertEquals(IssueType.REGULAR, detail.issueType)
            assertEquals("Marvel", detail.publisherName)
            assertEquals("Marvel Universe", detail.universeName)
            assertEquals("Earth-616", detail.universeDesignation)
            assertEquals(ReadingStatus.READING, detail.readingStatus)
        }

    @Test
    fun getSeriesExternalIds_returnsMatchingSourceAndExternalId() =
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
                        volume = 2,
                        startYear = 1999,
                        endYear = 2003
                    )
                )

            val externalIdId =
                comicDao.insertSeriesExternalId(
                    SeriesExternalId(
                        seriesId = seriesId,
                        source = "COMIC_VINE",
                        externalId = "2127",
                        url = "https://example.com/series"
                    )
                )

            val externalId =
                comicDao.getSeriesExternalId(
                    source = "COMIC_VINE",
                    externalId = "2127"
                )

            assertNotNull(externalId)
            assertEquals(externalIdId, externalId?.id)
            assertEquals(seriesId, externalId?.seriesId)
            assertEquals("COMIC_VINE", externalId?.source)
            assertEquals("2127", externalId?.externalId)
        }
}