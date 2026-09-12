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
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSource
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class AddIssueToReadingListViewModelTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: AddIssueToReadingListViewModel

    private var issueId: Long = 0

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

        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Repository Test Series",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            issueId =
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
        }

        viewModel =
            AddIssueToReadingListViewModel(
                issueId = issueId,
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
    fun userReadingLists_exposesOnlyUserOwnedReadingLists() =
        runBlocking {
            val publisher = comicDao.getPublisherByName("Marvel")!!
            val userReadingListId =
                repository.createUserReadingList(
                    title = "My List",
                    description = null,
                    publisherId = publisher.id,
                    universeId = null
                )

            comicDao.insertReadingList(
                ReadingList(
                    title = "Bundled List",
                    description = null,
                    publisherId = publisher.id,
                    universeId = null,
                    source = ReadingListSource.BUNDLED,
                    sourceKey = "bundled-test",
                    createdAt = 1000L,
                    updatedAt = 2000L
                )
            )

            val readingLists =
                withTimeout(5000L.milliseconds) {
                    viewModel.userReadingLists.first { it.isNotEmpty() }
                }
            assertEquals(1, readingLists.size)
            assertEquals(userReadingListId, readingLists.first().id)
            assertEquals(ReadingListSource.USER, readingLists.first().source)
        }

    @Test
    fun addToReadingList_addsIssueAndReportsSuccess() =
        runBlocking {
            val publisher = comicDao.getPublisherByName("Marvel")!!
            val readingListId =
                repository.createUserReadingList(
                    title = "My List",
                    description = null,
                    publisherId = publisher.id,
                    universeId = null
                )

            viewModel.addToReadingList(readingListId)

            val addedReadingListId =
                withTimeout(5000L.milliseconds) {
                    viewModel.addedReadingListId.first { it != null }
                }

            withTimeout(5000L.milliseconds) {
                viewModel.isAdding.first {
                    !it
                }
            }

            assertEquals(readingListId, addedReadingListId)

            val items = comicDao.getItemsForReadingList(readingListId)

            assertEquals(1, items.size)
            assertEquals(issueId, items.first().issueId)
            assertEquals(false, viewModel.isAdding.value)
            assertNull(viewModel.errorMessage.value)
        }

    @Test
    fun addToReadingList_reportsRepositoryError() =
        runBlocking {
            val dcPublisherId =
                comicDao.insertPublisher(
                    Publisher(name = "DC")
                )
            val readingListId =
                repository.createUserReadingList(
                    title = "DC List",
                    description = null,
                    publisherId = dcPublisherId,
                    universeId = null
                )

            viewModel.addToReadingList(readingListId)

            val errorMessage =
                withTimeout(5000L.milliseconds) {
                    viewModel.errorMessage.first { it != null }
                }

            withTimeout(5000L.milliseconds) {
                viewModel.isAdding.first {
                    !it
                }
            }

            assertEquals(
                "Issue $issueId belongs to a " +
                "different publisher",
                errorMessage
            )
            assertNull(viewModel.addedReadingListId.value)
            assertEquals(false, viewModel.isAdding.value)

            val items = comicDao.getItemsForReadingList(readingListId)

            assertEquals(0, items.size)
        }

    @Test
    fun clearResult_clearsSuccessResult() =
        runBlocking {
            val publisher = comicDao.getPublisherByName("Marvel")!!
            val readingListId =
                repository.createUserReadingList(
                    title = "My List",
                    description = null,
                    publisherId = publisher.id,
                    universeId = null
                )

            viewModel.addToReadingList(readingListId)

            withTimeout(5000L.milliseconds) {
                viewModel.addedReadingListId.first { it != null }
            }

            assertEquals(readingListId, viewModel.addedReadingListId.value)

            viewModel.clearResult()

            assertNull(viewModel.addedReadingListId.value)
            assertNull(viewModel.errorMessage.value)
        }
}
