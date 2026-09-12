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
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSection
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSource
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.preferences.ReadingListUiPreferences
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

@RunWith(AndroidJUnit4::class)
class ReadingListDetailViewModelTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: ReadingListDetailViewModel
    private lateinit var readingListUiPreferences: ReadingListUiPreferences

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

        readingListUiPreferences =
            ReadingListUiPreferences(
                context
            )

        viewModel =
            ReadingListDetailViewModel(
                repository = repository,
                readingListUiPreferences = readingListUiPreferences
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
    fun loadReadingList_restoresCollapsedSections() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name = "Collapse Restore Publisher"
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Collapse Restore List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val sectionId = 41L

            try {
                readingListUiPreferences
                    .setSectionCollapsed(
                        readingListId =
                            readingListId,
                        sectionId =
                            sectionId,
                        collapsed =
                            true
                    )

                viewModel.loadReadingList(
                    readingListId
                )

                withTimeout(
                    5000L.milliseconds
                ) {
                    viewModel
                        .collapsedSectionsLoaded
                        .first { loaded ->
                            loaded
                        }
                }

                assertEquals(
                    setOf(sectionId),
                    viewModel
                        .collapsedSectionIds
                        .value
                )
            } finally {
                readingListUiPreferences
                    .setSectionCollapsed(
                        readingListId =
                            readingListId,
                        sectionId =
                            sectionId,
                        collapsed =
                            false
                    )
            }
        }

    @Test
    fun toggleSectionCollapsed_updatesAndPersistsState() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name = "Collapse Toggle Publisher"
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Collapse Toggle List",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            val sectionId = 42L

            try {
                readingListUiPreferences
                    .setSectionCollapsed(
                        readingListId =
                            readingListId,
                        sectionId =
                            sectionId,
                        collapsed =
                            false
                    )

                viewModel.loadReadingList(
                    readingListId
                )

                withTimeout(
                    5000L.milliseconds
                ) {
                    viewModel
                        .collapsedSectionsLoaded
                        .first { loaded ->
                            loaded
                        }
                }

                viewModel.toggleSectionCollapsed(
                    sectionId
                )

                val collapsedState =
                    withTimeout(
                        5000L.milliseconds
                    ) {
                        viewModel
                            .collapsedSectionIds
                            .first { ids ->
                                sectionId in ids
                            }
                    }

                assertEquals(
                    true,
                    sectionId in collapsedState
                )

                val persistedCollapsedIds =
                    readingListUiPreferences
                        .getCollapsedSectionIds(
                            readingListId
                        )
                        .first()

                assertEquals(
                    true,
                    sectionId in persistedCollapsedIds
                )

                viewModel.toggleSectionCollapsed(
                    sectionId
                )

                val expandedState =
                    withTimeout(
                        5000L.milliseconds
                    ) {
                        viewModel
                            .collapsedSectionIds
                            .first { ids ->
                                sectionId !in ids
                            }
                    }

                assertEquals(
                    false,
                    sectionId in expandedState
                )

                val persistedExpandedIds =
                    readingListUiPreferences
                        .getCollapsedSectionIds(
                            readingListId
                        )
                        .first()

                assertEquals(
                    false,
                    sectionId in persistedExpandedIds
                )
            } finally {
                readingListUiPreferences
                    .setSectionCollapsed(
                        readingListId =
                            readingListId,
                        sectionId =
                            sectionId,
                        collapsed =
                            false
                    )
            }
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

    @Test
    fun moveIssueUp_movesIssueBeforePreviousIssue() =
        runBlocking {
            val publisherId = comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Reorder Test Series",
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
                repository.createUserReadingList(
                    title = "Move Up Test",
                    description = null,
                    publisherId = publisherId,
                    universeId = null
                )

            issueIds.forEach { issueId ->
                repository.addIssueToUserReadingList(
                    readingListId = readingListId,
                    issueId = issueId
                )
            }

            viewModel.loadReadingList(readingListId)

            val loadedIssues = withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { it.size == 3 }
                }

            viewModel.moveIssueUp(loadedIssues[1])

            val reorderedIssues = withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { issues ->
                        issues.map { it.issueNumber } == listOf("2", "1", "3")
                    }
                }

            assertEquals(
                listOf("2", "1", "3"),
                reorderedIssues.map { it.issueNumber }
            )

            assertEquals(
                listOf(1, 2, 3),
                reorderedIssues.map { it.position }
            )
        }

    @Test
    fun moveIssueDown_movesIssueAfterNextIssue() =
        runBlocking {
            val publisherId = comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Move Down Test Series",
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
                repository.createUserReadingList(
                    title = "Move Down Test",
                    description = null,
                    publisherId = publisherId,
                    universeId = null
                )

            issueIds.forEach { issueId ->
                repository.addIssueToUserReadingList(readingListId, issueId)
            }

            viewModel.loadReadingList(readingListId)

            val loadedIssues = withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { it.size == 3 }
                }

            viewModel.moveIssueDown(
                loadedIssues[1]
            )

            val reorderedIssues = withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { issues ->
                        issues.map { it.issueNumber } == listOf("1", "3", "2") }
                }

            assertEquals(
                listOf("1", "3", "2"),
                reorderedIssues.map { it.issueNumber }
            )
        }
    @Test
    fun moveIssue_doesNothingAtListBoundaries() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Boundary Test Series",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueIds =
                (1..2).map { number ->
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
                repository.createUserReadingList(
                    title = "Boundary Test",
                    description = null,
                    publisherId = publisherId,
                    universeId = null
                )

            issueIds.forEach { issueId ->
                repository.addIssueToUserReadingList(readingListId, issueId)
            }

            viewModel.loadReadingList(readingListId)

            val issues = withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { it.size == 2 }
                }

            viewModel.moveIssueUp(issues.first())
            viewModel.moveIssueDown(issues.last())

            val storedItems = comicDao.getItemsForReadingList(readingListId)

            assertEquals(
                issueIds,
                storedItems.map { it.issueId }
            )

            assertEquals(
                listOf(1, 2),
                storedItems.map { it.position }
            )
        }

    @Test
    fun moveIssue_doesNotMoveAcrossSectionBoundary() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Section Test Series",
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
                repository.createUserReadingList(
                    title = "Section Test",
                    description = null,
                    publisherId = publisherId,
                    universeId = null
                )

            val firstSectionId =
                repository.addReadingListSection(
                    ReadingListSection(
                        readingListId = readingListId,
                        title = "First",
                        description = null,
                        position = 1
                    )
                )

            val secondSectionId =
                repository.addReadingListSection(
                    ReadingListSection(
                        readingListId = readingListId,
                        title = "Second",
                        description = null,
                        position = 2
                    )
                )

            repository.addReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = firstSectionId,
                    issueId = issueIds[0],
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            repository.addReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = firstSectionId,
                    issueId = issueIds[1],
                    position = 2,
                    required = true,
                    notes = null
                )
            )

            repository.addReadingListItem(
                ReadingListItem(
                    readingListId = readingListId,
                    sectionId = secondSectionId,
                    issueId = issueIds[2],
                    position = 3,
                    required = true,
                    notes = null
                )
            )

            viewModel.loadReadingList(readingListId)

            val issues = withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { it.size == 3 }
                }

            // Last issue in section one cannot move into section two.
            viewModel.moveIssueDown(issues[1])

            // First issue in section two cannot move into section one.
            viewModel.moveIssueUp(issues[2])

            val storedItems = comicDao.getItemsForReadingList(readingListId)

            assertEquals(
                issueIds,
                storedItems.map { it.issueId }
            )

            assertEquals(
                listOf(firstSectionId, firstSectionId, secondSectionId),
                storedItems.map { it.sectionId }
            )
        }

    @Test
    fun moveIssue_doesNothingForNonUserReadingList() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Bundled Test Series",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueIds =
                (1..2).map { number ->
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
                        title = "Bundled Test",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        source = ReadingListSource.BUNDLED,
                        sourceKey = "viewmodel-bundled-test",
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

            viewModel.loadReadingList(readingListId)

            val issues = withTimeout(5000L.milliseconds) {
                    viewModel.issues.first { it.size == 2 }
                }

            viewModel.moveIssueDown(issues.first())

            val storedItems = comicDao.getItemsForReadingList(readingListId)

            assertEquals(
                issueIds,
                storedItems.map { it.issueId }
            )
        }

    @Test
    fun loadReadingList_loadsSections() =
        runBlocking {
            val publisherId = comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val readingListId =
                repository.createUserReadingList(
                    title = "Section Load Test",
                    description = null,
                    publisherId = publisherId,
                    universeId = null
                )

            val firstSectionId =
                repository.createUserReadingListSection(
                    readingListId,
                    "First Arc",
                    null
                )

            val secondSectionId =
                repository.createUserReadingListSection(
                    readingListId,
                    "Second Arc",
                    null
                )

            viewModel.loadReadingList(readingListId)

            val loadedSections = withTimeout(5000L.milliseconds) {
                    viewModel.sections.first { it.size == 2 }
                }

            assertEquals(
                listOf(firstSectionId, secondSectionId),
                loadedSections.map { it.id }
            )

            assertEquals(
                listOf("First Arc", "Second Arc"),
                loadedSections.map { it.title }
            )
        }

    @Test
    fun createSection_createsAndRefreshesSectionState() =
        runBlocking {
            val publisherId = comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val readingListId =
                repository.createUserReadingList(
                    title = "Create Section Test",
                    description = null,
                    publisherId = publisherId,
                    universeId = null
                )

            viewModel.loadReadingList(readingListId)

            withTimeout(5000L.milliseconds) {
                viewModel.readingList.first {
                    it?.id == readingListId
                }
            }

            viewModel.createSection(title = "New Arc", description = "Test description")

            val sections = withTimeout(5000L.milliseconds) {
                    viewModel.sections.first { it.size == 1 }
                }

            assertEquals("New Arc", sections.single().title)

            assertEquals("Test description", sections.single().description)
        }

    @Test
    fun moveIssueToSection_updatesIssueSection() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Move Section VM Series",
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
                        title = "Move Me",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType =
                            IssueType.REGULAR
                    )
                )

            val readingListId =
                repository.createUserReadingList(
                    title = "Move Section VM Test",
                    description = null,
                    publisherId = publisherId,
                    universeId = null
                )

            repository.addIssueToUserReadingList(
                readingListId,
                issueId
            )

            val sectionId =
                repository.createUserReadingListSection(
                    readingListId,
                    "Destination Arc",
                    null
                )

            viewModel.loadReadingList(readingListId)

            val issue = withTimeout(5000L.milliseconds) {
                    viewModel.issues.first {
                        it.size == 1
                    }
                }.single()

            assertEquals(null, issue.sectionId)

            viewModel.moveIssueToSection(issue = issue, targetSectionId = sectionId)

            val movedIssue = withTimeout(5000L.milliseconds) {
                    viewModel.issues.first {
                        it.singleOrNull()
                            ?.sectionId == sectionId
                    }
                }.single()

            assertEquals(sectionId, movedIssue.sectionId)
            assertEquals("Destination Arc", movedIssue.sectionTitle)
        }

    @Test
    fun createSection_doesNothingForNonUserReadingList() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Bundled Section Test",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        source =
                            ReadingListSource.BUNDLED,
                        sourceKey =
                            "bundled-section-test",
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            viewModel.loadReadingList(readingListId)

            withTimeout(5000L.milliseconds) {
                viewModel.readingList.first {
                    it?.id == readingListId
                }
            }

            viewModel.createSection(title = "Should Not Exist", description = null)

            kotlinx.coroutines.delay(100)

            assertEquals(
                emptyList<ReadingListSection>(),
                comicDao.getSectionsForReadingList(
                    readingListId
                )
            )

            assertEquals(
                emptyList<ReadingListSection>(),
                viewModel.sections.value
            )
        }

    @Test
    fun updateReadingListDetails_updatesLoadedUserList() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val readingListId =
                repository.createUserReadingList(
                    title = "Original Title",
                    description = "Original description",
                    publisherId = publisherId,
                    universeId = null
                )

            viewModel.loadReadingList(
                readingListId
            )

            withTimeout(5000L.milliseconds) {
                viewModel.readingList.first {
                    it?.id == readingListId
                }
            }

            viewModel.updateReadingListDetails(
                title = "Updated Title",
                description = "Updated description"
            )

            val updated =
                withTimeout(5000L.milliseconds) {
                    viewModel.readingList.first {
                        it?.title ==
                                "Updated Title"
                    }
                }

            assertEquals(
                "Updated Title",
                updated?.title
            )

            assertEquals(
                "Updated description",
                updated?.description
            )
        }

    @Test
    fun updateReadingListDetails_doesNothingForNonUserList() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Bundled Title",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        source =
                            ReadingListSource.BUNDLED,
                        sourceKey =
                            "bundled-edit-vm-test",
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            viewModel.loadReadingList(
                readingListId
            )

            withTimeout(5000L.milliseconds) {
                viewModel.readingList.first {
                    it?.id == readingListId
                }
            }

            viewModel.updateReadingListDetails(
                title = "Changed Title",
                description = "Changed"
            )

            kotlinx.coroutines.delay(100)

            val unchanged =
                comicDao.getReadingListById(
                    readingListId
                )!!

            assertEquals(
                "Bundled Title",
                unchanged.title
            )

            assertEquals(
                "Bundled Title",
                viewModel.readingList.value?.title
            )
        }

    @Test
    fun deleteReadingList_deletesUserListAndSignalsCompletion() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val readingListId =
                repository.createUserReadingList(
                    title = "Delete ViewModel Test",
                    description = null,
                    publisherId = publisherId,
                    universeId = null
                )

            viewModel.loadReadingList(
                readingListId
            )

            withTimeout(5000L.milliseconds) {
                viewModel.readingList.first {
                    it?.id == readingListId
                }
            }

            assertEquals(
                false,
                viewModel.readingListDeleted.value
            )

            viewModel.deleteReadingList()

            withTimeout(5000L.milliseconds) {
                viewModel.readingListDeleted.first {
                    it
                }
            }

            assertNull(
                comicDao.getReadingListById(
                    readingListId
                )
            )

            assertEquals(
                true,
                viewModel.readingListDeleted.value
            )
        }

    @Test
    fun deleteReadingList_doesNothingForNonUserList() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "Bundled Delete VM Test",
                        description = null,
                        publisherId = publisherId,
                        universeId = null,
                        source =
                            ReadingListSource.BUNDLED,
                        sourceKey =
                            "bundled-delete-vm-test",
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            viewModel.loadReadingList(
                readingListId
            )

            withTimeout(5000L.milliseconds) {
                viewModel.readingList.first {
                    it?.id == readingListId
                }
            }

            viewModel.deleteReadingList()

            kotlinx.coroutines.delay(100)

            assertNotNull(
                comicDao.getReadingListById(
                    readingListId
                )
            )

            assertEquals(
                false,
                viewModel.readingListDeleted.value
            )
        }

    @Test
    fun duplicateReadingList_reportsDuplicatedReadingListId() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name =
                            "Duplicate ViewModel Publisher"
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title =
                            "Duplicate ViewModel List",
                        description =
                            "Original description",
                        publisherId =
                            publisherId,
                        universeId = null,
                        source =
                            ReadingListSource.BUNDLED,
                        sourceKey =
                            "duplicate-viewmodel-test",
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            viewModel.loadReadingList(
                readingListId
            )

            withTimeout(
                5000L.milliseconds
            ) {
                viewModel.readingList.first {
                    it?.id == readingListId
                }
            }

            viewModel.duplicateReadingList()

            val duplicatedReadingListId =
                withTimeout(
                    5000L.milliseconds
                ) {
                    viewModel
                        .duplicatedReadingListId
                        .first { id ->
                            id != null
                        }
                }

            assertNotNull(
                duplicatedReadingListId
            )

            assertTrue(
                duplicatedReadingListId !=
                        readingListId
            )

            val duplicatedList =
                comicDao.getReadingListById(
                    duplicatedReadingListId!!
                )

            assertNotNull(
                duplicatedList
            )
            assertEquals(
                "Duplicate ViewModel List Copy",
                duplicatedList?.title
            )
            assertEquals(
                ReadingListSource.USER,
                duplicatedList?.source
            )

            viewModel.clearDuplicatedReadingList()

            assertNull(
                viewModel
                    .duplicatedReadingListId
                    .value
            )
        }

    @Test
    fun issueSelection_toggleAndClearUpdatesSelectedItems() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name =
                            "Selection Test Publisher"
                    )
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId =
                            publisherId,
                        title =
                            "Selection Test Series",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val firstIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId =
                            seriesId,
                        universeId = null,
                        issueNumber = "1",
                        title =
                            "First Selection Issue",
                        publicationDate =
                            "2000-01",
                        coverUrl = null,
                        description = null,
                        issueType =
                            IssueType.REGULAR
                    )
                )

            val secondIssueId =
                comicDao.insertIssue(
                    Issue(
                        seriesId =
                            seriesId,
                        universeId = null,
                        issueNumber = "2",
                        title =
                            "Second Selection Issue",
                        publicationDate =
                            "2000-02",
                        coverUrl = null,
                        description = null,
                        issueType =
                            IssueType.REGULAR
                    )
                )

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title =
                            "Selection Test List",
                        description = null,
                        publisherId =
                            publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
                    )
                )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId =
                        readingListId,
                    sectionId = null,
                    issueId =
                        firstIssueId,
                    position = 1,
                    required = true,
                    notes = null
                )
            )

            comicDao.insertReadingListItem(
                ReadingListItem(
                    readingListId =
                        readingListId,
                    sectionId = null,
                    issueId =
                        secondIssueId,
                    position = 2,
                    required = true,
                    notes = null
                )
            )

            viewModel.loadReadingList(
                readingListId
            )

            val loadedIssues =
                withTimeout(
                    5000L.milliseconds
                ) {
                    viewModel.issues.first {
                        it.size == 2
                    }
                }

            val firstIssue =
                loadedIssues[0]

            val secondIssue =
                loadedIssues[1]

            assertEquals(
                emptySet<Long>(),
                viewModel
                    .selectedReadingListItemIds
                    .value
            )

            viewModel.toggleIssueSelection(
                firstIssue
            )

            assertEquals(
                setOf(
                    firstIssue
                        .readingListItemId
                ),
                viewModel
                    .selectedReadingListItemIds
                    .value
            )

            viewModel.toggleIssueSelection(
                secondIssue
            )

            assertEquals(
                setOf(
                    firstIssue
                        .readingListItemId,
                    secondIssue
                        .readingListItemId
                ),
                viewModel
                    .selectedReadingListItemIds
                    .value
            )

            viewModel.toggleIssueSelection(
                firstIssue
            )

            assertEquals(
                setOf(
                    secondIssue
                        .readingListItemId
                ),
                viewModel
                    .selectedReadingListItemIds
                    .value
            )

            viewModel.clearIssueSelection()

            assertEquals(
                emptySet<Long>(),
                viewModel
                    .selectedReadingListItemIds
                    .value
            )
        }

    @Test
    fun markSelectedIssuesAsRead_marksOnlySelectedIssuesAndClearsSelection() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name =
                            "Selected Read Publisher"
                    )
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId =
                            publisherId,
                        title =
                            "Selected Read Series",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueIds =
                (1..3).map { number ->
                    comicDao.insertIssue(
                        Issue(
                            seriesId =
                                seriesId,
                            universeId = null,
                            issueNumber =
                                number.toString(),
                            title =
                                "Selected Read Issue $number",
                            publicationDate =
                                "2000-0$number",
                            coverUrl = null,
                            description = null,
                            issueType =
                                IssueType.REGULAR
                        )
                    )
                }

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title =
                            "Selected Read List",
                        description = null,
                        publisherId =
                            publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
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
                        issueId =
                            issueId,
                        position =
                            index + 1,
                        required = true,
                        notes = null
                    )
                )
            }

            viewModel.loadReadingList(
                readingListId
            )

            val loadedIssues =
                withTimeout(
                    5000L.milliseconds
                ) {
                    viewModel.issues.first {
                        it.size == 3
                    }
                }

            viewModel.toggleIssueSelection(
                loadedIssues[0]
            )

            viewModel.toggleIssueSelection(
                loadedIssues[2]
            )

            viewModel.markSelectedIssuesAsRead()

            withTimeout(
                5000L.milliseconds
            ) {
                viewModel.issues.first {
                    it[0].readingStatus ==
                            ReadingStatus.READ &&
                            it[2].readingStatus ==
                            ReadingStatus.READ
                }
            }

            val firstProgress =
                repository
                    .getReadingProgressForIssue(
                        issueIds[0]
                    )

            val secondProgress =
                repository
                    .getReadingProgressForIssue(
                        issueIds[1]
                    )

            val thirdProgress =
                repository
                    .getReadingProgressForIssue(
                        issueIds[2]
                    )

            assertEquals(
                ReadingStatus.READ,
                firstProgress?.status
            )
            assertNull(
                secondProgress
            )
            assertEquals(
                ReadingStatus.READ,
                thirdProgress?.status
            )
            assertEquals(
                emptySet<Long>(),
                viewModel
                    .selectedReadingListItemIds
                    .value
            )
        }

    @Test
    fun markSelectedIssuesAsUnread_marksOnlySelectedIssuesAndClearsSelection() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(
                        name =
                            "Selected Unread Publisher"
                    )
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId =
                            publisherId,
                        title =
                            "Selected Unread Series",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            val issueIds =
                (1..3).map { number ->
                    comicDao.insertIssue(
                        Issue(
                            seriesId =
                                seriesId,
                            universeId = null,
                            issueNumber =
                                number.toString(),
                            title =
                                "Selected Unread Issue $number",
                            publicationDate =
                                "2000-0$number",
                            coverUrl = null,
                            description = null,
                            issueType =
                                IssueType.REGULAR
                        )
                    )
                }

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title =
                            "Selected Unread List",
                        description = null,
                        publisherId =
                            publisherId,
                        universeId = null,
                        createdAt = 1000L,
                        updatedAt = 1000L
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
                        issueId =
                            issueId,
                        position =
                            index + 1,
                        required = true,
                        notes = null
                    )
                )
            }

            repository.markIssuesAsRead(
                issueIds
            )

            viewModel.loadReadingList(
                readingListId
            )

            val loadedIssues =
                withTimeout(
                    5000L.milliseconds
                ) {
                    viewModel.issues.first {
                        it.size == 3 &&
                                it.all { issue ->
                                    issue.readingStatus ==
                                            ReadingStatus.READ
                                }
                    }
                }

            viewModel.toggleIssueSelection(
                loadedIssues[0]
            )

            viewModel.toggleIssueSelection(
                loadedIssues[2]
            )

            viewModel.markSelectedIssuesAsUnread()

            withTimeout(
                5000L.milliseconds
            ) {
                viewModel.issues.first {
                    it[0].readingStatus == null &&
                            it[2].readingStatus == null
                }
            }

            val firstProgress =
                repository
                    .getReadingProgressForIssue(
                        issueIds[0]
                    )

            val secondProgress =
                repository
                    .getReadingProgressForIssue(
                        issueIds[1]
                    )

            val thirdProgress =
                repository
                    .getReadingProgressForIssue(
                        issueIds[2]
                    )

            assertNull(
                firstProgress
            )
            assertEquals(
                ReadingStatus.READ,
                secondProgress?.status
            )
            assertNull(
                thirdProgress
            )
            assertEquals(
                emptySet<Long>(),
                viewModel
                    .selectedReadingListItemIds
                    .value
            )
        }
}