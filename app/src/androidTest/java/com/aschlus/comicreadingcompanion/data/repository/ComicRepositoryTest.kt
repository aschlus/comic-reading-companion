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
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingProgress
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListItem
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSection
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSource
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
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
class ComicRepositoryTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository

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
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun createIssue(
        issueNumber: String = "1"
    ): Long {
        val publisher =
            comicDao.getPublisherByName("Marvel")

        val publisherId =
            publisher?.id
                ?: comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

        val existingSeries =
            comicDao.getSeries(
                publisherId = publisherId,
                title = "Repository Test Series",
                volume = 1
            )

        val seriesId =
            existingSeries?.id
                ?: comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Repository Test Series",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
            )

        return comicDao.insertIssue(
            Issue(
                seriesId = seriesId,
                universeId = null,
                issueNumber = issueNumber,
                title = "Test Issue",
                publicationDate = "2000-01",
                coverUrl = null,
                description = null,
                issueType = IssueType.REGULAR
            )
        )
    }

    @Test
    fun markIssueAsReading_createsReadingProgressWithStartedAt() =
        runBlocking {
            val issueId = createIssue()
            val before = System.currentTimeMillis()
            repository.markIssueAsReading(issueId)
            val after = System.currentTimeMillis()
            val progress = repository.getReadingProgressForIssue(issueId)
            assertNotNull(progress)
            assertEquals(ReadingStatus.READING, progress?.status)
            assertNotNull(progress?.startedAt)
            assertTrue(progress!!.startedAt!! >= before)
            assertTrue(progress.startedAt!! <= after)
            assertNull(progress.completedAt)
        }

    @Test
    fun markIssueAsReading_preservesStartedAtAndClearsCompletedAt() =
        runBlocking {
            val issueId = createIssue()
            val progressId =
                comicDao.insertReadingProgress(
                    ReadingProgress(
                        issueId = issueId,
                        status = ReadingStatus.READ,
                        startedAt = 1000L,
                        completedAt = 2000L,
                        notes = "Keep this note"
                    )
                )

            repository.markIssueAsReading(issueId)
            val progress = repository.getReadingProgressForIssue(issueId)
            assertNotNull(progress)
            assertEquals(progressId, progress?.id)
            assertEquals(ReadingStatus.READING, progress?.status)
            assertEquals(1000L, progress?.startedAt)
            assertNull(progress?.completedAt)
            assertEquals("Keep this note", progress?.notes)
        }

    @Test
    fun markIssueAsRead_createsReadProgressWithTimestamps() =
        runBlocking {
            val issueId = createIssue()
            val before = System.currentTimeMillis()
            repository.markIssueAsRead(issueId)
            val after = System.currentTimeMillis()
            val progress = repository.getReadingProgressForIssue(issueId)
            assertNotNull(progress)
            assertEquals(ReadingStatus.READ, progress?.status)
            assertNotNull(progress?.startedAt)
            assertNotNull(progress?.completedAt)
            assertTrue(progress!!.startedAt!! >= before)
            assertTrue(progress.startedAt!! <= after)
            assertTrue(progress.completedAt!! >= before)
            assertTrue(progress.completedAt!! <= after)
            assertEquals(progress.startedAt, progress.completedAt)
        }

    @Test
    fun markIssueAsRead_preservesStartedAtAndExistingProgressData() =
        runBlocking {
            val issueId = createIssue()
            val progressId =
                comicDao.insertReadingProgress(
                    ReadingProgress(
                        issueId = issueId,
                        status = ReadingStatus.READING,
                        startedAt = 1000L,
                        completedAt = null,
                        notes = "Keep this note"
                    )
                )

            val before = System.currentTimeMillis()
            repository.markIssueAsRead(issueId)
            val after = System.currentTimeMillis()
            val progress = comicDao.getReadingProgressForIssue(issueId)
            assertNotNull(progress)
            assertEquals(progressId, progress?.id)
            assertEquals(ReadingStatus.READ, progress?.status)
            assertEquals(1000L, progress?.startedAt)
            assertNotNull(progress?.completedAt)
            assertTrue(progress!!.completedAt!! >= before)
            assertTrue(progress.completedAt!! <= after)
            assertEquals("Keep this note", progress.notes)
        }

    @Test
    fun markIssuesAsUnread_removesExistingProgress() =
        runBlocking {
            val issueId = createIssue()
            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = issueId,
                    status = ReadingStatus.READ,
                    startedAt = 1000L,
                    completedAt = 2000L,
                    notes = "Existing note"
                )
            )
            val before = repository.getReadingProgressForIssue(issueId)
            assertNotNull(before)
            repository.markIssueAsUnread(issueId)
            val after = repository.getReadingProgressForIssue(issueId)
            assertNull(after)
        }

    @Test
    fun markIssuesAsRead_marksMultipleIssuesAsRead() =
        runBlocking {
            val firstIssueId = createIssue(issueNumber = "1")
            val secondIssueId = createIssue(issueNumber = "2")
            val before = System.currentTimeMillis()
            repository.markIssuesAsRead(listOf(firstIssueId, secondIssueId))
            val after = System.currentTimeMillis()
            val firstProgress = repository.getReadingProgressForIssue(firstIssueId)
            val secondProgress = repository.getReadingProgressForIssue(secondIssueId)

            assertNotNull(firstProgress)
            assertNotNull(secondProgress)
            assertEquals(ReadingStatus.READ, firstProgress?.status)
            assertEquals(ReadingStatus.READ, secondProgress?.status)
            assertNotNull(firstProgress?.startedAt)
            assertNotNull(firstProgress?.completedAt)
            assertNotNull(secondProgress?.startedAt)
            assertNotNull(secondProgress?.completedAt)
            assertTrue(firstProgress!!.startedAt!! >= before)
            assertTrue(firstProgress.startedAt!! <= after)
            assertTrue(secondProgress!!.startedAt!! >= before)
            assertTrue(secondProgress.startedAt!! <= after)
            assertEquals(firstProgress.startedAt, firstProgress.completedAt)
            assertEquals(secondProgress.startedAt, secondProgress.completedAt)
        }

    @Test
    fun markIssuesAsRead_deduplicatesIssueIds() =
        runBlocking {
            val issueId = createIssue()
            repository.markIssuesAsRead(listOf(issueId, issueId, issueId))
            val progress = comicDao.getReadingProgressForIssues(listOf(issueId))
            assertEquals(1, progress.size)
            assertEquals(issueId, progress.first().issueId)
            assertEquals(ReadingStatus.READ, progress.first().status)
        }

    @Test
    fun markIssuesAsRead_preservesExistingStartedAt() =
        runBlocking {
            val firstIssueId = createIssue(issueNumber = "1")
            val secondIssueId = createIssue(issueNumber = "2")
            val firstProgressId =
                comicDao.insertReadingProgress(
                    ReadingProgress(
                        issueId = firstIssueId,
                        status = ReadingStatus.READING,
                        startedAt = 1000L,
                        completedAt = null,
                        notes = "First note"
                    )
                )

            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = secondIssueId,
                    status = ReadingStatus.READING,
                    startedAt = null,
                    completedAt = null,
                    notes = "Second note"
                )
            )
            val before = System.currentTimeMillis()
            repository.markIssuesAsRead(listOf(firstIssueId, secondIssueId))
            val after = System.currentTimeMillis()
            val firstProgress = repository.getReadingProgressForIssue(firstIssueId)
            val secondProgress = repository.getReadingProgressForIssue(secondIssueId)
            assertNotNull(firstProgress)
            assertNotNull(secondProgress)
            assertEquals(firstProgressId, firstProgress?.id)
            assertEquals(ReadingStatus.READ, firstProgress?.status)
            assertEquals(1000L, firstProgress?.startedAt)
            assertNotNull(firstProgress?.completedAt)
            assertEquals("First note", firstProgress?.notes)
            assertEquals(ReadingStatus.READ, secondProgress?.status)
            assertNotNull(secondProgress?.startedAt)
            assertTrue(secondProgress!!.startedAt!! >= before)
            assertTrue(secondProgress.startedAt!! <= after)
            assertNotNull(secondProgress.completedAt)
            assertEquals("Second note", secondProgress.notes)
        }

    @Test
    fun markIssuesAsUnread_removesOnlyRequestedProgress() =
        runBlocking {
            val firstIssueId = createIssue(issueNumber = "1")
            val secondIssueId = createIssue(issueNumber = "2")
            val thirdIssueId = createIssue(issueNumber = "3")
            listOf(firstIssueId, secondIssueId, thirdIssueId).forEach { issueId ->
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

            repository.markIssuesAsUnread(listOf(firstIssueId, thirdIssueId))
            val firstProgress = repository.getReadingProgressForIssue(firstIssueId)
            val secondProgress = repository.getReadingProgressForIssue(secondIssueId)
            val thirdProgress = repository.getReadingProgressForIssue(thirdIssueId)
            assertNull(firstProgress)
            assertNotNull(secondProgress)
            assertEquals(ReadingStatus.READ, secondProgress?.status)
            assertNull(thirdProgress)
        }

    @Test
    fun markIssuesAsUnread_handlesDuplicateIssueIds() =
        runBlocking {
            val issueId = createIssue()
            comicDao.insertReadingProgress(
                ReadingProgress(
                    issueId = issueId,
                    status = ReadingStatus.READ,
                    startedAt = 1000L,
                    completedAt = 2000L,
                    notes = null
                )
            )

            repository.markIssuesAsUnread(listOf(issueId, issueId, issueId))
            val progress = repository.getReadingProgressForIssue(issueId)
            assertNull(progress)
        }

    @Test
    fun markIssuesAsRead_withEmptyList_doesNothing() =
        runBlocking {
            val issueId = createIssue()
            val progressId =
                comicDao.insertReadingProgress(
                    ReadingProgress(
                        issueId = issueId,
                        status = ReadingStatus.READING,
                        startedAt = 1000L,
                        completedAt = null,
                        notes = "Keep this"
                    )
                )
            repository.markIssuesAsRead(emptyList())
            val progress = repository.getReadingProgressForIssue(issueId)
            assertNotNull(progress)
            assertEquals(progressId, progress?.id)
            assertEquals(ReadingStatus.READING, progress?.status)
            assertEquals(1000L, progress?.startedAt)
            assertNull(progress?.completedAt)
            assertEquals("Keep this", progress?.notes)
        }

    @Test
    fun markIssuesAsUnread_withEmptyList_doesNothing() =
        runBlocking {
            val issueId = createIssue()
            val progressId =
                comicDao.insertReadingProgress(
                    ReadingProgress(
                        issueId = issueId,
                        status = ReadingStatus.READ,
                        startedAt = 1000L,
                        completedAt = 2000L,
                        notes = "Keep this"
                    )
                )
            repository.markIssuesAsUnread(emptyList())
            val progress = repository.getReadingProgressForIssue(issueId)
            assertNotNull(progress)
            assertEquals(progressId, progress?.id)
            assertEquals(ReadingStatus.READ, progress?.status)
            assertEquals(1000L, progress?.startedAt)
            assertEquals(2000L, progress?.completedAt)
            assertEquals("Keep this", progress?.notes)
        }

    @Test
    fun createUserReadingList_createsUserOwnedReadingList() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val readingListId =
                repository.createUserReadingList(
                    title = "My Spider-Man List",
                    description = "My custom reading order",
                    publisherId = publisherId,
                    universeId = null
                )

            val readingList =
                comicDao.getReadingListById(readingListId)

            assertNotNull(readingList)
            assertEquals("My Spider-Man List", readingList?.title)
            assertEquals("My custom reading order", readingList?.description)
            assertEquals(publisherId, readingList?.publisherId)
            assertNull(readingList?.universeId)
            assertEquals(ReadingListSource.USER, readingList?.source)
            assertNull(readingList?.sourceKey)
        }

    @Test
    fun createUserReadingList_setsCreationAndUpdateTimestamps() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val before = System.currentTimeMillis()

            val readingListId =
                repository.createUserReadingList(
                    title = "Timestamp Test",
                    description = null,
                    publisherId = publisherId,
                    universeId = null
                )

            val after = System.currentTimeMillis()

            val readingList =
                comicDao.getReadingListById(readingListId)

            assertNotNull(readingList)
            assertTrue(readingList!!.createdAt >= before)
            assertTrue(readingList.createdAt <= after)
            assertEquals(readingList.createdAt, readingList.updatedAt)
        }

    @Test
    fun addIssueToUserReadingList_appendsIssuesAndUpdatesReadingList() =
        runBlocking {
            val firstIssueId = createIssue(issueNumber = "1")
            val secondIssueId = createIssue(issueNumber = "2")
            val publisher = comicDao.getPublisherByName("Marvel")
            assertNotNull(publisher)

            val readingListId =
                repository.createUserReadingList(
                    title = "Custom List",
                    description = null,
                    publisherId = publisher!!.id,
                    universeId = null
                )

            val readingListBefore = comicDao.getReadingListById(readingListId)
            assertNotNull(readingListBefore)

            val firstItemId =
                repository.addIssueToUserReadingList(
                    readingListId = readingListId,
                    issueId = firstIssueId
                )

            val secondItemId =
                repository.addIssueToUserReadingList(
                    readingListId = readingListId,
                    issueId = secondIssueId
                )

            val items = comicDao.getItemsForReadingList(readingListId)

            assertEquals(2, items.size)
            assertEquals(firstItemId, items[0].id)
            assertEquals(firstIssueId, items[0].issueId)
            assertEquals(1, items[0].position)
            assertNull(items[0].sectionId)
            assertTrue(items[0].required)
            assertNull(items[0].notes)
            assertEquals(secondItemId, items[1].id)
            assertEquals(secondIssueId, items[1].issueId)
            assertEquals(2, items[1].position)

            val readingListAfter = comicDao.getReadingListById(readingListId)
            assertNotNull(readingListAfter)
            assertTrue(readingListAfter!!.updatedAt > readingListBefore!!.updatedAt)
        }

    @Test
    fun addIssueToUserReadingList_existingIssueDoesNotCreateDuplicates() =
        runBlocking {
            val issueId = createIssue()
            val publisher = comicDao.getPublisherByName("Marvel")
            assertNotNull(publisher)
            val readingListId =
                repository.createUserReadingList(
                    title = "Duplicate Test",
                    description = null,
                    publisherId = publisher!!.id,
                    universeId = null
                )

            val firstItemId =
                repository.addIssueToUserReadingList(
                    readingListId = readingListId,
                    issueId = issueId
                )

            val readingListAfterFirstAdd = comicDao.getReadingListById(readingListId)
            val secondItemId =
                repository.addIssueToUserReadingList(
                    readingListId = readingListId,
                    issueId = issueId
                )
            val items = comicDao.getItemsForReadingList(readingListId)
            val readingListAfterSecondAdd = comicDao.getReadingListById(readingListId)

            assertEquals(firstItemId, secondItemId)
            assertEquals(1, items.size)
            assertEquals(issueId, items.first().issueId)
            assertEquals(
                readingListAfterFirstAdd?.updatedAt,
                readingListAfterSecondAdd?.updatedAt
            )
        }

    @Test
    fun addIssueToUserReadingList_rejectsBundledReadingList() =
        runBlocking {
            val issueId = createIssue()
            val publisher = comicDao.getPublisherByName("Marvel")
            assertNotNull(publisher)

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Bundled Test",
                        description = null,
                        publisherId = publisher!!.id,
                        universeId = null,
                        source = ReadingListSource.BUNDLED,
                        sourceKey = "bundled-test",
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            var thrownException: IllegalArgumentException? = null

            try {
                repository.addIssueToUserReadingList(
                    readingListId = readingListId,
                    issueId = issueId
                )
            } catch (
                exception: IllegalArgumentException
            ) {
                thrownException = exception
            }

            assertNotNull(thrownException)
            assertEquals(
                "Reading list $readingListId " +
                "is not user-owned",
                thrownException?.message
            )

            val items = comicDao.getItemsForReadingList(readingListId)
            assertTrue(items.isEmpty())
        }

    @Test
    fun addIssueToUserReadingList_rejectsMissingIssue() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val readingListId =
                repository.createUserReadingList(
                    title = "Missing Issue Test",
                    description = null,
                    publisherId = publisherId,
                    universeId = null
                )

            val missingIssueId = Long.MAX_VALUE

            var thrownException: IllegalArgumentException? = null

            try {
                repository.addIssueToUserReadingList(
                    readingListId = readingListId,
                    issueId = missingIssueId
                )
            } catch (
                exception: IllegalArgumentException
            ) {
                thrownException = exception
            }

            assertNotNull(thrownException)
            assertEquals(
                "Issue $missingIssueId does not exist",
                thrownException?.message
            )

            val items = comicDao.getItemsForReadingList(readingListId)
            assertTrue(items.isEmpty())
        }

    @Test
    fun addIssueToUserReadingList_rejectsDifferentPublisher() =
        runBlocking {
            val marvelPublisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val dcPublisherId =
                comicDao.insertPublisher(
                    Publisher(name = "DC")
                )

            val dcSeriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = dcPublisherId,
                        title = "Batman",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val dcIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = dcSeriesId,
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
                repository.createUserReadingList(
                    title = "Marvel List",
                    description = null,
                    publisherId = marvelPublisherId,
                    universeId = null
                )

            var thrownException: IllegalArgumentException? = null

            try {
                repository.addIssueToUserReadingList(
                    readingListId = readingListId,
                    issueId = dcIssueId
                )
            } catch (
                exception: IllegalArgumentException
            ) {
                thrownException = exception
            }

            assertNotNull(
                thrownException
            )

            assertEquals(
                "Issue $dcIssueId belongs to a " +
                        "different publisher",
                thrownException?.message
            )

            assertTrue(comicDao.getItemsForReadingList(readingListId).isEmpty())
        }

    @Test
    fun addIssueToUserReadingList_rejectsDifferentContinuity() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val earth616Id =
                comicDao.insertUniverse(
                    Universe(
                        publisherId = publisherId,
                        name = "Marvel Universe",
                        designation = "Earth-616",
                        description = null
                    )
                )

            val earth1610Id =
                comicDao.insertUniverse(
                    Universe(
                        publisherId = publisherId,
                        name = "Ultimate Universe",
                        designation = "Earth-1610",
                        description = null
                    )
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Ultimate Spider-Man",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId = seriesId,
                        universeId = earth1610Id,
                        issueNumber = "1",
                        title = "Test Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                repository.createUserReadingList(
                    title = "Earth-616 List",
                    description = null,
                    publisherId = publisherId,
                    universeId = earth616Id
                )

            var thrownException:
                    IllegalArgumentException? = null

            try {
                repository.addIssueToUserReadingList(
                    readingListId = readingListId,
                    issueId = issueId
                )
            } catch (
                exception: IllegalArgumentException
            ) {
                thrownException = exception
            }

            assertNotNull(
                thrownException
            )

            assertEquals(
                "Issue $issueId belongs to a " +
                        "different continuity",
                thrownException?.message
            )

            assertTrue(comicDao.getItemsForReadingList(readingListId).isEmpty())
        }

    @Test
    fun removeIssuesFromUserReadingList_removesIssueAndCompactsPositions() =
        runBlocking {
            val firstIssueId = createIssue(issueNumber = "1")
            val secondIssueId = createIssue(issueNumber = "2")
            val thirdIssueId = createIssue(issueNumber = "3")
            val publisher = comicDao.getPublisherByName("Marvel")
            assertNotNull(publisher)
            val readingListId =
                repository.createUserReadingList(
                    title = "Removal Test",
                    description = null,
                    publisherId = publisher!!.id,
                    universeId = null
                )

            repository.addIssueToUserReadingList(
                readingListId = readingListId,
                issueId = firstIssueId
            )

            repository.addIssueToUserReadingList(
                readingListId = readingListId,
                issueId = secondIssueId
            )

            repository.addIssueToUserReadingList(
                readingListId = readingListId,
                issueId = thirdIssueId
            )

            val readingListBefore = comicDao.getReadingListById(readingListId)
            assertNotNull(readingListBefore)

            val wasRemoved =
                repository
                    .removeIssueFromUserReadingList(
                        readingListId = readingListId,
                        issueId = secondIssueId
                    )

            assertTrue(wasRemoved)

            val items = comicDao.getItemsForReadingList(readingListId)

            assertEquals(2, items.size)
            assertEquals(firstIssueId, items[0].issueId)
            assertEquals(1, items[0].position)
            assertEquals(thirdIssueId, items[1].issueId)
            assertEquals(2, items[1].position)
            assertNotNull(comicDao.getIssueById(secondIssueId))

            val readingListAfter = comicDao.getReadingListById(readingListId)

            assertNotNull(readingListAfter)
            assertTrue(readingListAfter!!.updatedAt > readingListBefore!!.updatedAt)
        }

    @Test
    fun removeIssueFromUserReadingList_missingItemDoesNothing() =
        runBlocking {
            val firstIssueId = createIssue(issueNumber = "1")
            val missingIssueId = createIssue(issueNumber = "2")
            val publisher = comicDao.getPublisherByName("Marvel")
            assertNotNull(publisher)

            val readingListId =
                repository.createUserReadingList(
                    title = "No-op Removal Test",
                    description = null,
                    publisherId = publisher!!.id,
                    universeId = null
                )

            repository.addIssueToUserReadingList(
                readingListId = readingListId,
                issueId = firstIssueId
            )

            val readingListBefore = comicDao.getReadingListById(readingListId)
            assertNotNull(readingListBefore)

            val wasRemoved =
                repository
                    .removeIssueFromUserReadingList(
                        readingListId =
                            readingListId,
                        issueId =
                            missingIssueId
                    )

            assertEquals(false, wasRemoved)

            val items = comicDao.getItemsForReadingList(readingListId)

            assertEquals(1, items.size)
            assertEquals(firstIssueId, items.first().issueId)
            assertEquals(1, items.first().position)

            val readingListAfter = comicDao.getReadingListById(readingListId)

            assertEquals(
                readingListBefore?.updatedAt,
                readingListAfter?.updatedAt
            )
        }

    @Test
    fun removeIssueFromUserReadingList_rejectsBundledReadingList() =
        runBlocking {
            val issueId = createIssue()
            val publisher = comicDao.getPublisherByName("Marvel")
            assertNotNull(publisher)

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Bundled Removal Test",
                        description = null,
                        publisherId = publisher!!.id,
                        universeId = null,
                        source = ReadingListSource.BUNDLED,
                        sourceKey = "bundled-removal-test",
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = null,
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            var thrownException: IllegalArgumentException? = null

            try {
                repository
                    .removeIssueFromUserReadingList(
                        readingListId = readingListId,
                        issueId = issueId
                    )
            } catch (
                exception: IllegalArgumentException
            ) {
                thrownException = exception
            }

            assertNotNull(thrownException)
            assertEquals(
                "Reading list $readingListId " +
                        "is not user-owned",
                thrownException?.message
            )

            val items = comicDao.getItemsForReadingList(readingListId)

            assertEquals(1, items.size)

            assertEquals(issueId, items.first().issueId)

            assertEquals(
                1000L,
                comicDao.getReadingListById(readingListId)?.updatedAt
            )
        }

    @Test
    fun removeIssueFromUserReadingList_rejectsMissingReadingList() =
        runBlocking {
            val issueId = createIssue()
            val missingReadingListId = Long.MAX_VALUE
            var thrownException: IllegalArgumentException? = null

            try {
                repository
                    .removeIssueFromUserReadingList(
                        readingListId = missingReadingListId,
                        issueId = issueId
                    )
            } catch (
                exception: IllegalArgumentException
            ) {
                thrownException = exception
            }

            assertNotNull(thrownException)
            assertEquals(
                "Reading list " +
                        "$missingReadingListId does not exist",
                thrownException?.message
            )
            assertNotNull(comicDao.getIssueById(issueId))
        }

    @Test
    fun reorderUserReadingListItems_reordersItemsWithContiguousPositions() =
        runBlocking {
            val firstIssueId = createIssue("1")
            val secondIssueId = createIssue("2")
            val thirdIssueId = createIssue("3")

            val publisher = comicDao.getPublisherByName("Marvel")!!

            val readingListId = repository.createUserReadingList(
                    title = "Reorder Test",
                    description = null,
                    publisherId = publisher.id,
                    universeId = null
                )

            val firstItemId = repository.addIssueToUserReadingList(
                    readingListId,
                    firstIssueId
                )

            val secondItemId = repository.addIssueToUserReadingList(
                    readingListId,
                    secondIssueId
                )

            val thirdItemId = repository.addIssueToUserReadingList(
                    readingListId,
                    thirdIssueId
                )

            repository.reorderUserReadingListItems(
                readingListId = readingListId,
                orderedItemIds = listOf(thirdItemId, firstItemId, secondItemId)
            )

            val items = comicDao.getItemsForReadingList(readingListId)

            assertEquals(
                listOf(thirdItemId, firstItemId, secondItemId),
                items.map { it.id }
            )

            assertEquals(
                listOf(1, 2, 3),
                items.map { it.position }
            )
        }

    @Test
    fun reorderUserReadingListItems_rejectsMovingItemsBetweenSections() =
        runBlocking {
            val firstIssueId = createIssue("1")
            val secondIssueId = createIssue("2")
            val thirdIssueId = createIssue("3")

            val publisher = comicDao.getPublisherByName("Marvel")!!

            val readingListId = repository.createUserReadingList(
                    title = "Section Reorder Test",
                    description = null,
                    publisherId = publisher.id,
                    universeId = null
                )

            val firstSectionId = repository.addReadingListSection(
                    ReadingListSection(
                        readingListId = readingListId,
                        title = "Section One",
                        description = null,
                        position = 1
                    )
                )

            val secondSectionId = repository.addReadingListSection(
                    ReadingListSection(
                        readingListId = readingListId,
                        title = "Section Two",
                        description = null,
                        position = 2
                    )
                )

            val firstItemId = repository.addReadingListItem(
                    ReadingListItem(
                        readingListId = readingListId,
                        sectionId = firstSectionId,
                        issueId = firstIssueId,
                        position = 1,
                        required = true,
                        notes = null
                    )
                )

            val secondItemId = repository.addReadingListItem(
                    ReadingListItem(
                        readingListId = readingListId,
                        sectionId = firstSectionId,
                        issueId = secondIssueId,
                        position = 2,
                        required = true,
                        notes = null
                    )
                )

            val thirdItemId = repository.addReadingListItem(
                    ReadingListItem(
                        readingListId = readingListId,
                        sectionId = secondSectionId,
                        issueId = thirdIssueId,
                        position = 3,
                        required = true,
                        notes = null
                    )
                )

            var exception: IllegalArgumentException? = null

            try {
                repository.reorderUserReadingListItems(
                    readingListId = readingListId,
                    orderedItemIds = listOf(firstItemId, thirdItemId, secondItemId)
                )
            } catch (caught: IllegalArgumentException) {
                exception = caught
            }

            assertNotNull(exception)

            assertEquals(
                "Items cannot move between sections while reordering",
                exception?.message
            )

            val items = comicDao.getItemsForReadingList(readingListId)

            assertEquals(
                listOf(firstItemId, secondItemId, thirdItemId),
                items.map { it.id }
            )
        }

    @Test
    fun reorderUserReadingListItems_rejectsMissingItemIds() =
        runBlocking {
            val firstIssueId = createIssue("1")
            val secondIssueId = createIssue("2")

            val publisher = comicDao.getPublisherByName("Marvel")!!

            val readingListId = repository.createUserReadingList(
                    title = "Missing Item Test",
                    description = null,
                    publisherId = publisher.id,
                    universeId = null
                )

            val firstItemId = repository.addIssueToUserReadingList(
                    readingListId,
                    firstIssueId
                )

            repository.addIssueToUserReadingList(readingListId, secondIssueId)

            var exception: IllegalArgumentException? = null

            try {
                repository.reorderUserReadingListItems(
                    readingListId = readingListId,
                    orderedItemIds = listOf(
                        firstItemId
                    )
                )
            } catch (caught: IllegalArgumentException) {
                exception = caught
            }

            assertNotNull(exception)

            assertEquals(
                "Reordered item count does not match reading list",
                exception?.message
            )
        }

    @Test
    fun reorderUserReadingListItems_rejectsDuplicateItemIds() =
        runBlocking {
            val firstIssueId = createIssue("1")
            val secondIssueId = createIssue("2")

            val publisher = comicDao.getPublisherByName("Marvel")!!

            val readingListId = repository.createUserReadingList(
                    title = "Duplicate Item Test",
                    description = null,
                    publisherId = publisher.id,
                    universeId = null
                )

            val firstItemId = repository.addIssueToUserReadingList(
                    readingListId,
                    firstIssueId
                )

            repository.addIssueToUserReadingList(readingListId, secondIssueId)

            var exception: IllegalArgumentException? = null

            try {
                repository.reorderUserReadingListItems(
                    readingListId = readingListId,
                    orderedItemIds = listOf(firstItemId, firstItemId)
                )
            } catch (caught: IllegalArgumentException) {
                exception = caught
            }

            assertNotNull(exception)

            assertEquals(
                "Reordered item IDs contain duplicates",
                exception?.message
            )
        }
    @Test
    fun reorderUserReadingListItems_rejectsNonUserOwnedList() =
        runBlocking {
            val issueId = createIssue("1")
            val publisher = comicDao.getPublisherByName("Marvel")!!

            val readingListId = comicDao.insertReadingList(
                    ReadingList(
                        title = "Bundled List",
                        description = null,
                        publisherId = publisher.id,
                        universeId = null,
                        source = ReadingListSource.BUNDLED,
                        sourceKey = "bundled-test",
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val itemId = repository.addReadingListItem(
                    ReadingListItem(
                        readingListId = readingListId,
                        sectionId = null,
                        issueId = issueId,
                        position = 1,
                        required = true,
                        notes = null
                    )
                )

            var exception: IllegalArgumentException? = null

            try {
                repository.reorderUserReadingListItems(
                    readingListId = readingListId,
                    orderedItemIds = listOf(itemId)
                )
            } catch (caught: IllegalArgumentException) {
                exception = caught
            }

            assertNotNull(exception)

            assertEquals(
                "Reading list $readingListId is not user-owned",
                exception?.message
            )
        }

    @Test
    fun reorderUserReadingListItems_updatesReadingListTimestamp() =
        runBlocking {
            val firstIssueId = createIssue("1")
            val secondIssueId = createIssue("2")

            val publisher = comicDao.getPublisherByName("Marvel")!!

            val readingListId = repository.createUserReadingList(
                    title = "Timestamp Reorder Test",
                    description = null,
                    publisherId = publisher.id,
                    universeId = null
                )

            val firstItemId = repository.addIssueToUserReadingList(
                    readingListId,
                    firstIssueId
                )

            val secondItemId = repository.addIssueToUserReadingList(
                    readingListId,
                    secondIssueId
                )

            val before = comicDao.getReadingListById(readingListId)!!

            repository.reorderUserReadingListItems(
                readingListId = readingListId,
                orderedItemIds = listOf(secondItemId, firstItemId)
            )

            val after = comicDao.getReadingListById(readingListId)!!

            assertTrue(after.updatedAt > before.updatedAt)
        }
}