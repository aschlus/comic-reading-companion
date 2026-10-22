package com.aschlus.comicreadingcompanion.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.Series
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

@RunWith(AndroidJUnit4::class)
class EditSeriesViewModelTest {

    private lateinit var database: ComicDatabase
    private lateinit var comicDao: ComicDao
    private lateinit var repository: ComicRepository
    private lateinit var viewModel: EditSeriesViewModel

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
            EditSeriesViewModel(
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
    fun loadSeries_populatesExistingFieldsAndUniverseOptions() =
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

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Amazing Spider-Man",
                        volume = 2,
                        startYear = 1999,
                        endYear = 2003,
                        universeId = universeId
                    )
                )

            viewModel.loadSeries(seriesId)

            withTimeout(5_000L) {
                viewModel.isLoading.first { !it }
            }

            assertEquals(
                "Amazing Spider-Man",
                viewModel.title.value
            )
            assertEquals("2", viewModel.volume.value)
            assertEquals("1999", viewModel.startYear.value)
            assertEquals("2003", viewModel.endYear.value)
            assertEquals(
                publisherId,
                viewModel.selectedPublisherId.value
            )
            assertEquals(
                universeId,
                viewModel.selectedUniverseId.value
            )
            assertEquals(
                listOf(universeId),
                viewModel.universes.value.map { it.id }
            )
        }

    @Test
    fun selectPublisher_clearsUniverseAndLoadsOnlyMatchingUniverses() =
        runBlocking {
            val marvelId =
                comicDao.insertPublisher(
                    Publisher(name = "Marvel")
                )

            val dcId =
                comicDao.insertPublisher(
                    Publisher(name = "DC Comics")
                )

            val marvelUniverseId =
                comicDao.insertUniverse(
                    Universe(
                        publisherId = marvelId,
                        name = "Earth-616",
                        designation = "Earth-616",
                        description = null
                    )
                )

            val dcUniverseId =
                comicDao.insertUniverse(
                    Universe(
                        publisherId = dcId,
                        name = "Prime Earth",
                        designation = "Earth-0",
                        description = null
                    )
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = marvelId,
                        title = "Publisher Switch Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2001,
                        universeId = marvelUniverseId
                    )
                )

            viewModel.loadSeries(seriesId)

            withTimeout(5_000L) {
                viewModel.isLoading.first { !it }
            }

            viewModel.selectPublisher(dcId)

            assertEquals(
                dcId,
                viewModel.selectedPublisherId.value
            )
            assertNull(viewModel.selectedUniverseId.value)

            withTimeout(5_000L) {
                viewModel.universes.first {
                    it.map(Universe::id) == listOf(dcUniverseId)
                }
            }

            assertEquals(
                listOf(dcUniverseId),
                viewModel.universes.value.map { it.id }
            )
        }

    @Test
    fun createPublisher_createsAndSelectsPublisher() =
        runBlocking {
            viewModel.createPublisher(" Image Comics ")

            val selectedPublisherId =
                withTimeout(5_000L) {
                    viewModel.selectedPublisherId.first {
                        it != null
                    }
                }

            val createdPublisher =
                repository.getPublishers()
                    .single { it.id == selectedPublisherId }

            assertNotNull(createdPublisher)
            assertEquals("Image Comics", createdPublisher.name)
            assertEquals(
                selectedPublisherId,
                viewModel.selectedPublisherId.value
            )
        }

    @Test
    fun createUniverse_createsAndSelectsUniverseForSelectedPublisher() =
        runBlocking {
            val publisherId =
                comicDao.insertPublisher(
                    Publisher(name = "Dark Horse")
                )

            val seriesId =
                comicDao.insertSeries(
                    Series(
                        publisherId = publisherId,
                        title = "Universe Creation Test",
                        volume = 1,
                        startYear = 2000,
                        endYear = 2000
                    )
                )

            viewModel.loadSeries(seriesId)

            withTimeout(5_000L) {
                viewModel.isLoading.first { !it }
            }

            viewModel.createUniverse(
                name = "Hellboy Universe",
                designation = "Hellboy",
                description = "Test universe"
            )

            val selectedUniverseId =
                withTimeout(5_000L) {
                    viewModel.selectedUniverseId.first {
                        it != null
                    }
                }

            val createdUniverse =
                comicDao.getUniversesForPublisher(publisherId)
                    .single { it.id == selectedUniverseId }

            assertEquals("Hellboy Universe", createdUniverse.name)
            assertEquals("Hellboy", createdUniverse.designation)
            assertEquals("Test universe", createdUniverse.description)
            assertEquals(publisherId, createdUniverse.publisherId)
        }
}