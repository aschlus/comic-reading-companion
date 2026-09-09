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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class ReadingListDetailViewModelTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: ReadingListDetailViewModel

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

        viewModel =
            ReadingListDetailViewModel(
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
    fun loadReadingList_loadsReadingListAndIssues() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
            )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "ViewModel Test Series",
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
                        title = "ViewModel Test List",
                        description = "Test description",
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
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            viewModel.loadReadingList(readingListId)

            val loadedList =
                withTimeout(5000L.milliseconds) {
                    viewModel.readingList.first { it?.id == readingListId }
                }

            val loadedIssues =
                withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { it.size == 1 }
                }

            assertNotNull(loadedList)
            assertEquals("ViewModel Test List", loadedList?.title)
            assertEquals(1, loadedIssues.size)
            assertEquals(issueId, loadedIssues.first().issueId)
            assertEquals(1, loadedIssues.first().position)
        }

    @Test
    fun toggleIssueRead_unreadIssueBecomesRead() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Toggle Test Series",
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
                        title = "Toggle Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Toggle Test List",
                        description = "Test description",
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
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            viewModel.loadReadingList(readingListId)
            val issue =
                withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { it.size == 1}
                }.first()
            assertEquals(null, issue.readingStatus)
            viewModel.toggleIssueRead(issue)
            val updatedIssue =
                withTimeout(5000L.milliseconds) {
                    viewModel.issues.first {
                        it.firstOrNull()?.readingStatus == ReadingStatus.READ
                    }
                }.first()
            assertEquals(ReadingStatus.READ, updatedIssue.readingStatus)
        }

    @Test
    fun toggleIssueRead_readIssueBecomesUnread() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Toggle Unread Test Series",
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
                        title = "Toggle Back Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Toggle Unread Test List",
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
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            repository.markIssueAsRead(issueId)
            viewModel.loadReadingList(readingListId)
            val readIssue =
                withTimeout(5000L.milliseconds) {
                    viewModel.issues.first {
                        it.firstOrNull()?.readingStatus == ReadingStatus.READ
                    }
                }.first()

            viewModel.toggleIssueRead(readIssue)

            val unreadIssue =
                withTimeout(5000L.milliseconds) {
                    viewModel.issues.first {
                        it.first().readingStatus == null
                    }
                }.first()
            assertEquals(null, unreadIssue.readingStatus)
        }

    @Test
    fun markIssueAsReading_updatesIssueStatus() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Reading State Test Series",
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
                        title = "Reading State Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Reading State Test List",
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
                    issueId = issueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            viewModel.loadReadingList(readingListId)
            val issue =
                withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { it.size == 1 }
                }.first()

            viewModel.markIssueAsReading(issue)

            val updatedIssue =
                withTimeout(5000L.milliseconds) {
                    viewModel.issues.first {
                        it.firstOrNull()?.readingStatus == ReadingStatus.READING
                    }
                }.first()
            assertEquals(ReadingStatus.READING, updatedIssue.readingStatus)
        }

    @Test
    fun markAllBeforeAsRead_marksOnlyEarlierUnreadIssues() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Mark Before Test Series",
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

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Mark Before Test List",
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

            repository.markIssueAsRead(issueIds[1])
            viewModel.loadReadingList(readingListId)
            val loadedIssues =
                withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { it.size == 4 }
                }
            val selectedIssue = loadedIssues.first { it.position == 3 }

            viewModel.markAllBeforeAsRead(selectedIssue)

            val updatedIssues =
                withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { issues ->
                        issues.size == 4 &&
                            issues.first { it.position == 1 }
                                .readingStatus == ReadingStatus.READ
                    }
                }

            val firstIssue = updatedIssues.first { it.position == 1 }
            val secondIssue = updatedIssues.first { it.position == 2 }
            val thirdIssue = updatedIssues.first { it.position == 3 }
            val fourthIssue = updatedIssues.first { it.position == 4 }
            assertEquals(ReadingStatus.READ, firstIssue.readingStatus)
            assertEquals(ReadingStatus.READ, secondIssue.readingStatus)
            assertEquals(null, thirdIssue.readingStatus)
            assertEquals(null, fourthIssue.readingStatus)
        }

    @Test
    fun markAllAsRead_marksAllUnreadIssues() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Mark All Test Series",
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
                        title = "Mark All Test List",
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

            repository.markIssueAsRead(issueIds[0])
            repository.markIssueAsRead(issueIds[1])
            viewModel.loadReadingList(readingListId)

            withTimeout(5000L.milliseconds) {
                viewModel.issues.first { it.size == 3 }
            }

            viewModel.markAllAsRead()

            val updatedIssues =
                withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { issues ->
                        issues.size == 3 &&
                                issues.all { it.readingStatus == ReadingStatus.READ}
                    }
                }

            assertEquals(
                3,
                updatedIssues.count { it.readingStatus == ReadingStatus.READ }
            )
        }

    @Test
    fun resetProgress_clearsAllProgressForLoadedList() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Reset Progress Test Series",
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
                        title = "Reset Progress Test List",
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

            repository.markIssueAsRead(issueIds[0])
            repository.markIssueAsReading(issueIds[1])
            viewModel.loadReadingList(readingListId)

            withTimeout(5000L.milliseconds) {
                viewModel.issues.first { issues ->
                    issues.size == 3 &&
                        issues[0].readingStatus == ReadingStatus.READ &&
                        issues[1].readingStatus == ReadingStatus.READING
                }
            }

            viewModel.resetProgress()

            val updatedIssues =
                withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { issues ->
                        issues.size == 3 &&
                                issues.all { it.readingStatus == null }
                    }
                }

            assertEquals(
                3,
                updatedIssues.size
            )
            assertEquals(
                0,
                updatedIssues.count { it.readingStatus != null }
            )
            issueIds.forEach { issueId ->
                assertEquals(
                    null,
                    repository.getReadingProgressForIssue(issueId)
                )
            }
        }

    @Test
    fun getReadCount_returnsCorrectCount() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Read Count Test Series",
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

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Read Count Test List",
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

            repository.markIssueAsRead(issueIds[0])
            repository.markIssueAsRead(issueIds[1])
            repository.markIssueAsReading(issueIds[2])
            viewModel.loadReadingList(readingListId)

            withTimeout(5000L.milliseconds) {
                viewModel.issues.first { issues ->
                    issues.size == 4 &&
                            issues[0].readingStatus == ReadingStatus.READ &&
                            issues[1].readingStatus == ReadingStatus.READ &&
                            issues[2].readingStatus == ReadingStatus.READING
                }
            }
            assertEquals(
                2,
                viewModel.getReadCount()
            )
        }

    @Test
    fun removeIssue_removesIssueAndUpdatesLoadedList() =
        runBlocking {
            val publisherId = comicDao.insertPublisher(Publisher(name = "Marvel"))
            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Remove Issue Test Series",
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
                repository
                    .createUserReadingList(
                        title = "Remove Issue Test List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null
                    )
            issueIds.forEach { issueId ->
                repository
                    .addIssueToUserReadingList(
                        readingListId = readingListId,
                        issueId = issueId
                    )
            }
            viewModel.loadReadingList(readingListId)
            val loadedIssues = withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { it.size == 3 }
                }
            val issueToRemove = loadedIssues.first { it.issueId == issueIds[1] }
            viewModel.removeIssue(issueToRemove)
            val updatedIssues = withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { it.size == 2 }
                }
            assertEquals(
                listOf(issueIds[0], issueIds[2]),
                updatedIssues.map { it.issueId }
            )
            assertEquals(
                listOf(1, 2),
                updatedIssues.map { it.position }
            )
        }

    @Test
    fun removeIssue_preservesGlobalReadingProgress() =
        runBlocking {
            val publisherId = comicDao.insertPublisher(Publisher(name = "Marvel"))
            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Remove Progress Test Series",
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
                        title = "Read Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType = IssueType.REGULAR
                    )
                )
            val readingListId =
                repository
                    .createUserReadingList(
                        title = "Remove Progress Test List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null
                    )

            repository.addIssueToUserReadingList(readingListId = readingListId, issueId = issueId)
            repository.markIssueAsRead(issueId)
            viewModel.loadReadingList(readingListId)

            val loadedIssue = withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { it.size == 1 && it.first()
                                    .readingStatus == ReadingStatus.READ
                    }
                }.first()

            viewModel.removeIssue(loadedIssue)

            withTimeout(5000L.milliseconds) {
                viewModel.issues.first { it.isEmpty() }
            }

            val progress = repository.getReadingProgressForIssue(issueId)
            assertNotNull(progress)
            assertEquals(ReadingStatus.READ, progress?.status)
            assertNotNull(comicDao.getIssueById(issueId))
        }
}