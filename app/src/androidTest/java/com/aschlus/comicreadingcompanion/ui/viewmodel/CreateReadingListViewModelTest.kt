package com.aschlus.comicreadingcompanion.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSource
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

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
}