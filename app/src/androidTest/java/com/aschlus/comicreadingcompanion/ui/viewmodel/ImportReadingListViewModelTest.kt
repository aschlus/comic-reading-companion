package com.aschlus.comicreadingcompanion.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.importer.ReadingListAssetParser
import com.aschlus.comicreadingcompanion.data.importer.ReadingListImporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class ImportReadingListViewModelTest {

    private lateinit var database:
            ComicDatabase

    private lateinit var viewModel:
            ImportReadingListViewModel

    @Before
    fun setUp() {
        val context =
            ApplicationProvider
                .getApplicationContext<Context>()

        database =
            Room.inMemoryDatabaseBuilder(
                context,
                ComicDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()

        viewModel =
            ImportReadingListViewModel(
                parser =
                    ReadingListAssetParser(
                        context = context
                    ),
                importer =
                    ReadingListImporter(
                        comicDao =
                            database.comicDao(),
                        database = database
                    )
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun importReadingList_validJson_returnsSuccess() =
        runBlocking {
            val jsonText =
                """
                {
                  "title": "User Import Test",
                  "description": "Imported by user",
                  "publisher": "Test Publisher",
                  "universe": {
                    "name": "Test Universe",
                    "designation": "Earth-Test"
                  },
                  "items": [
                    {
                      "position": 1,
                      "series": {
                        "title": "Test Series",
                        "volume": 1,
                        "startYear": 2000,
                        "endYear": 2001
                      },
                      "issue": {
                        "number": "1",
                        "title": "Test Issue",
                        "publicationDate": "2000-01",
                        "coverUrl": null,
                        "description": null,
                        "type": "REGULAR"
                      },
                      "required": true,
                      "notes": null
                    }
                  ]
                }
                """.trimIndent()

            viewModel.importReadingList(
                jsonText = jsonText,
                sourceDescription =
                    "reading-list file 'test.json'"
            )

            val state =
                withTimeout(5_000L.milliseconds) {
                    viewModel.state.first {
                        it is ImportReadingListState.Success
                    }
                }

            assertEquals(
                "User Import Test",
                (
                        state as
                                ImportReadingListState.Success
                        ).title
            )

            val readingLists =
                database
                    .comicDao()
                    .getAllReadingLists()
                    .first()

            assertTrue(
                readingLists.any {
                    it.title ==
                            "User Import Test"
                }
            )
        }

    @Test
    fun importReadingList_malformedJson_returnsError() =
        runBlocking {
            viewModel.importReadingList(
                jsonText = "{",
                sourceDescription =
                    "reading-list file 'broken.json'"
            )

            val state =
                withTimeout(5_000L.milliseconds) {
                    viewModel.state.first {
                        it is ImportReadingListState.Error
                    }
                }

            assertTrue(
                (
                        state as
                                ImportReadingListState.Error
                        ).message.contains(
                        "reading-list file " +
                                "'broken.json'"
                    )
            )
        }

    @Test
    fun factory_createsImportReadingListViewModel() {
        val context =
            ApplicationProvider
                .getApplicationContext<Context>()

        val factory =
            ImportReadingListViewModelFactory(
                parser =
                    ReadingListAssetParser(
                        context = context
                    ),
                importer =
                    ReadingListImporter(
                        comicDao =
                            database.comicDao(),
                        database = database
                    )
            )

        val viewModelStore =
            ViewModelStore()

        val provider =
            ViewModelProvider(
                viewModelStore,
                factory
            )

        val createdViewModel =
            provider[
                ImportReadingListViewModel::class.java
            ]

        assertNotNull(
            createdViewModel
        )

        viewModelStore.clear()
    }
}