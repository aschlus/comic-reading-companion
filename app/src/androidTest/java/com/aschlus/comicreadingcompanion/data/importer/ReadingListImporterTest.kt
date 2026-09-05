package com.aschlus.comicreadingcompanion.data.importer

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingProgress
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.importer.models.ExternalIdImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.IssueImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.ReadingListImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.ReadingListItemImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.SeriesImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.UniverseImportDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingListImporterTest {

    private lateinit var database: ComicDatabase
    private lateinit var importer: ReadingListImporter

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

        importer =
            ReadingListImporter(
                comicDao = database.comicDao(),
                database = database
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun importValidReadingList_createsReadingListAndItem() =
        runBlocking {
            val importData =
                createValidImportData()

            importer.import(importData)

            val readingLists =
                database.comicDao()
                    .getAllReadingLists()
                    .first()

            assertEquals(
                1,
                readingLists.size
            )

            val readingList = readingLists.first()

            assertEquals(
                "Test Reading List",
                readingList.title
            )

            val items =
                database.comicDao()
                    .getItemsForReadingList(
                        readingList.id
                    )

            assertEquals(
                1,
                items.size
            )

            assertEquals(
                1,
                items.first().position
            )

            val issue =
                database.comicDao()
                    .getIssueById(
                        items.first().issueId
                    )

            assertNotNull(issue)

            assertEquals(
                "1",
                issue?.issueNumber
            )
        }

    @Test
    fun importFailure_rollsBackAllDatabaseChanges() =
        runBlocking {
            val originalImport =
                createValidImportData(
                    externalId = "shared-test-id"
                )

            importer.import(
                originalImport
            )

            val originalExternalId =
                database.comicDao()
                    .getExternalId(
                        source = "test_provider",
                        externalId = "shared-test-id"
                    )

            assertNotNull(
                originalExternalId
            )

            val conflictingImport =
                createValidImportData(
                    title = "Rollback Test Reading List",
                    publisher = "Rollback Test Publisher",
                    seriesTitle = "Rollback Test Series",
                    issueNumber = "2",
                    externalId = "shared-test-id"
                )

            try {
                importer.import(
                    conflictingImport
                )

                fail(
                    "Expected import to fail because " +
                    "the external ID is already assigned"
                )
            } catch (exception: IllegalStateException) {
                // Expected
            }

            val readingListsAfterFailure =
                database.comicDao()
                    .getAllReadingLists()
                    .first()

            assertEquals(
                1,
                readingListsAfterFailure.size
            )

            assertEquals(
                "Test Reading List",
                readingListsAfterFailure.first().title
            )

            val rolledBackPublisher =
                database.comicDao()
                    .getPublisherByName(
                        "Rollback Test Publisher"
                    )

            assertNull(
                rolledBackPublisher
            )

            val externalIdAfterFailure =
                database.comicDao()
                    .getExternalId(
                        source = "test_provider",
                        externalId = "shared-test-id"
                    )

            assertNotNull(
                externalIdAfterFailure
            )

            assertEquals(
                originalExternalId?.issueId,
                externalIdAfterFailure?.issueId
            )
        }

    @Test
    fun importSameReadingListTwice_doesNotCreateDuplicates() =
        runBlocking {
            val importData =
                createValidImportData()

            importer.import(importData)
            importer.import(importData)

            val publishers =
                database.comicDao()
                    .getAllPublishers()

            assertEquals(
                1,
                publishers.size
            )

            val publisher = publishers.first()

            val series =
                database.comicDao()
                    .getSeriesForPublisher(
                        publisher.id
                    )

            assertEquals(
                1,
                series.size
            )

            val issues =
                database.comicDao()
                    .getIssuesForSeries(
                        series.first().id
                    )

            assertEquals(
                1,
                issues.size
            )

            val readingLists =
                database.comicDao()
                    .getAllReadingLists()
                    .first()

            assertEquals(
                1,
                readingLists.size
            )

            val items =
                database.comicDao()
                    .getItemsForReadingList(
                        readingLists.first().id
                    )

            assertEquals(
                1,
                items.size
            )
        }

    @Test
    fun reimportReadingList_preservesReadingProgress() =
        runBlocking {
            val importData =
                createValidImportData()

            importer.import(importData)

            val readingList =
                database.comicDao()
                    .getAllReadingLists()
                    .first()
                    .first()

            val item =
                database.comicDao()
                    .getItemsForReadingList(
                        readingList.id
                    )
                    .first()

            database.comicDao()
                .upsertReadingProgress(
                    ReadingProgress(
                        issueId = item.issueId,
                        status = ReadingStatus.READ,
                        startedAt = 1000L,
                        completedAt = 2000L,
                        notes = "Preserve this progress"
                    )
                )

            val updatedImportData =
                importData.copy(
                    description = "Updated reading-list description"
                )

            importer.import(updatedImportData)

            val progressAfterReimport =
                database.comicDao()
                    .getReadingProgressForIssue(
                        item.issueId
                    )

            assertNotNull(progressAfterReimport)

            assertEquals(
                ReadingStatus.READ,
                progressAfterReimport?.status
            )

            assertEquals(
                1000L,
                progressAfterReimport?.startedAt
            )

            assertEquals(
                2000L,
                progressAfterReimport?.completedAt
            )

            assertEquals(
                "Preserve this progress",
                progressAfterReimport?.notes
            )
        }

    @Test
    fun reimportWithoutIssue_removesListItemButPreservesIssueAndProgress() =
        runBlocking {
            val baseImport =
                createValidImportData()

            val firstItem =
                baseImport.items.first()

            val secondItem =
                firstItem.copy(
                    position = 2,
                    issue = firstItem.issue.copy(
                        number = "2",
                        title = "Second Test Issue",
                        publicationDate = "2000-02"
                    )
                )

            val twoIssueImport =
                baseImport.copy(
                    items = listOf(
                        firstItem,
                        secondItem
                    )
                )

            importer.import(twoIssueImport)

            val readingList =
                database.comicDao()
                    .getAllReadingLists()
                    .first()
                    .first()

            val originalItems =
                database.comicDao()
                    .getItemsForReadingList(
                        readingList.id
                    )

            assertEquals(
                2,
                originalItems.size
            )

            val secondReadingListItem =
                originalItems.first { item ->
                    item.position == 2
                }

            database.comicDao()
                .upsertReadingProgress(
                    ReadingProgress(
                        issueId = secondReadingListItem.issueId,
                        status = ReadingStatus.READ,
                        startedAt = 1000L,
                        completedAt = 2000L,
                        notes = "Keep this"
                    )
                )

            importer.import(baseImport)

            val remainingItems =
                database.comicDao()
                    .getItemsForReadingList(
                        readingList.id
                    )

            assertEquals(
                1,
                remainingItems.size
            )

            assertEquals(
                1,
                remainingItems.first().position
            )

            val removedIssue =
                database.comicDao()
                    .getIssueById(
                        secondReadingListItem.issueId
                    )

            assertNotNull(removedIssue)

            val preservedProgress =
                database.comicDao()
                    .getReadingProgressForIssue(
                        secondReadingListItem.issueId
                    )

            assertNotNull(
                preservedProgress
            )

            assertEquals(
                ReadingStatus.READ,
                preservedProgress?.status
            )

            assertEquals(
                "Keep this",
                preservedProgress?.notes
            )
        }

    @Test
    fun importWithDuplicateItemPositions_failsValidation() =
        runBlocking {
            val baseImport =
                createValidImportData()

            val firstItem =
                baseImport.items.first()

            val secondItem =
                firstItem.copy(
                    position = 1,
                    issue = firstItem.issue.copy(
                        number = "2",
                        title = "Second Test Issue"
                    )
                )

            val invalidImport =
                baseImport.copy(
                    items = listOf(
                        firstItem,
                        secondItem
                    )
                )

            try {
                importer.import(invalidImport)

                fail(
                    "Expected duplicate item positions " +
                    "to fail validation"
                )
            } catch (exception: IllegalArgumentException) {
                assertTrue(
                    exception.message?.contains(
                        "duplicate item positions"
                    ) == true
                )
            }

            val readingLists =
                database.comicDao()
                    .getAllReadingLists()
                    .first()

            assertEquals(
                0,
                readingLists.size
            )
        }

    @Test
    fun importWithDuplicateIssue_failsValidation() =
        runBlocking {
            val baseImport =
                createValidImportData()

            val firstitem =
                baseImport.items.first()

            val duplicateIssueItem =
                firstitem.copy(
                    position = 2
                )

            val invalidImport =
                baseImport.copy(
                    items = listOf(
                        firstitem,
                        duplicateIssueItem
                    )
                )

            try {
                importer.import(invalidImport)

                fail(
                    "Expected duplicate issue " +
                    "to fail validation"
                )
            } catch (exception: IllegalArgumentException) {
                assertTrue(
                    exception.message?.contains(
                        "duplicate issues"
                    ) == true
                )
            }

            val readingLists =
                database.comicDao()
                    .getAllReadingLists()
                    .first()

            assertEquals(
                0,
                readingLists.size
            )
        }

    @Test
    fun importWithUnknownSectionReference_failsValidation() =
        runBlocking {
            val baseImport =
                createValidImportData()

            val invalidItems =
                baseImport.items
                    .first()
                    .copy(
                        sectionPosition = 99
                    )

            val invalidImport =
                baseImport.copy(
                    items = listOf(
                        invalidItems
                    )
                )

            try {
                importer.import(invalidImport)

                fail(
                    "Expected unknown section reference " +
                    "to fail validation"
                )
            } catch (exception: IllegalArgumentException) {
                assertTrue(
                    exception.message?.contains(
                        "references unknown section"
                    ) == true
                )
            }

            val readingLists =
                database.comicDao()
                    .getAllReadingLists()
                    .first()

            assertEquals(
                0,
                readingLists.size
            )
        }

    @Test
    fun importWithUnknownIssueType_failsValidation() =
        runBlocking {
            val baseImport =
                createValidImportData()

            val invalidItem =
                baseImport.items
                    .first()
                    .copy(
                        issue =
                            baseImport.items
                                .first()
                                .issue
                                .copy(
                                    type = "BANANA"
                                )
                    )

            val invalidImport =
                baseImport.copy(
                    items = listOf(
                        invalidItem
                    )
                )

            try {
                importer.import(invalidImport)

                fail(
                    "Expected unknown issue type " +
                    "to fail validation"
                )
            } catch (exception: IllegalArgumentException) {
                assertTrue(
                    exception.message?.contains(
                        "unknown issue type"
                    ) == true
                )
            }

            val readingList =
                database.comicDao()
                    .getAllReadingLists()
                    .first()

            assertEquals(
                0,
                readingList.size
            )
        }

    @Test
    fun importWithInvalidPublicationDate_failsValidation() =
        runBlocking {
            val baseImport =
                createValidImportData()

            val invalidItem =
                baseImport.items
                    .first()
                    .copy(
                        issue =
                            baseImport.items
                                .first()
                                .issue
                                .copy(
                                    publicationDate = "2000-99"
                                )
                    )

            val invalidImport =
                baseImport.copy(
                    items = listOf(
                        invalidItem
                    )
                )

            try {
                importer.import(invalidImport)

                fail(
                    "Expected invalid publication date " +
                    "to fail validation"
                )
            } catch (exception: IllegalArgumentException) {
                assertTrue(
                    exception.message?.contains(
                        "invalid publication date"
                    ) == true
                )

                assertTrue(
                    exception.message?.contains(
                        "Expected YYYY-MM"
                    ) == true
                )
            }

            val readingLists =
                database.comicDao()
                    .getAllReadingLists()
                    .first()

            assertEquals(
                0,
                readingLists.size
            )
        }


    //Write tests above

    private fun createValidImportData(
        title: String = "Test Reading List",
        publisher: String = "Test Publisher",
        seriesTitle: String = "Test Series",
        issueNumber: String = "1",
        externalId: String? = null
    ): ReadingListImportDto {

        val externalIds =
            if (externalId == null) {
                emptyList()
            } else {
                listOf(
                    ExternalIdImportDto(
                        source = "test_provider",
                        externalId = externalId,
                        url = null
                    )
                )
            }

        return ReadingListImportDto(
            title = title,
            description = "Importer test",
            publisher = publisher,
            universe = UniverseImportDto(
                name = "Test Universe",
                designation = "Earth-Test"
            ),
            items = listOf(
                ReadingListItemImportDto(
                    position = 1,
                    series = SeriesImportDto(
                        title = seriesTitle,
                        volume = 1,
                        startYear = 2000,
                        endYear = 2001
                    ),
                    issue = IssueImportDto(
                        number = issueNumber,
                        title = "Test Issue",
                        publicationDate = "2000-01",
                        coverUrl = null,
                        description = "Test issue",
                        type = "REGULAR",
                        externalIds = externalIds
                    ),
                    required = true,
                    notes = null
                )
            )
        )
    }
}