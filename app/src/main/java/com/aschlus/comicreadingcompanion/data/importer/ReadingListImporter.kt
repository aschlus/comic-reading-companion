package com.aschlus.comicreadingcompanion.data.importer

import androidx.room3.withWriteTransaction
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.entities.ExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListItem
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSection
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import com.aschlus.comicreadingcompanion.data.importer.models.ReadingListImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.ReadingListItemImportDto
import kotlinx.coroutines.flow.first

class ReadingListImporter(
    private val comicDao: ComicDao,
    private val database: ComicDatabase
) {

    suspend fun import(
        importData: ReadingListImportDto
    ) {
        database.withWriteTransaction {
            importReadingList(
                importData = importData
            )
        }
    }

    private suspend fun importReadingList(
        importData: ReadingListImportDto
    ) {
        val publisher =
            getOrCreatePublisher(importData)

        val universe =
            getOrCreateUniverse(
                importData = importData,
                publisher = publisher
            )

        val readingList =
            getOrCreateReadingList(
                importData = importData,
                publisher = publisher,
                universe = universe
            )

        val sectionsByPosition =
            importSections(
                importData = importData,
                readingList = readingList
            )

        importItems(
            importData = importData,
            publisher = publisher,
            universe = universe,
            readingList = readingList,
            sectionsByPosition = sectionsByPosition
        )
    }

    private suspend fun getOrCreatePublisher(
        importData: ReadingListImportDto
    ): Publisher {
        val existing =
            comicDao.getPublisherByName(
                importData.publisher
            )

        if (existing != null) {
            return existing
        }

        val id = comicDao.insertPublisher(
            Publisher(
                name = importData.publisher
            )
        )

        return Publisher(
            id = id,
            name = importData.publisher
        )
    }

    private suspend fun getOrCreateUniverse(
        importData: ReadingListImportDto,
        publisher: Publisher
    ): Universe {
        val universeData = importData.universe

        val existing =
            comicDao.getUniverseByDesignation(
                publisherId = publisher.id,
                designation = universeData.designation
            )

        if (existing != null) {
            return existing
        }

        val id = comicDao.insertUniverse(
            Universe(
                publisherId = publisher.id,
                name = universeData.name,
                designation = universeData.designation,
                description = null
            )
        )

        return Universe(
            id = id,
            publisherId = publisher.id,
            name = universeData.name,
            designation = universeData.designation,
            description = null
        )
    }

    private suspend fun getOrCreateReadingList(
        importData: ReadingListImportDto,
        publisher: Publisher,
        universe: Universe
    ): ReadingList {
        val existing =
            comicDao.getAllReadingLists()
                .first()
                .find {
                    it.title == importData.title &&
                            it.publisherId == publisher.id
                }

        if (existing != null) {
            val updated =
                existing.copy(
                    description = importData.description,
                    universeId = universe.id,
                    updatedAt = System.currentTimeMillis()
                )

            if (
                existing.description != updated.description ||
                existing.universeId != updated.universeId
            ) {
                comicDao.updateReadingList(updated)
                return updated
            }

            return existing
        }

        val currentTime =
            System.currentTimeMillis()

        val id = comicDao.insertReadingList(
            ReadingList(
                title = importData.title,
                description = importData.description,
                publisherId = publisher.id,
                universeId = universe.id,
                createdAt = currentTime,
                updatedAt = currentTime
            )
        )

        return ReadingList(
            id = id,
            title = importData.title,
            description = importData.description,
            publisherId = publisher.id,
            universeId = universe.id,
            createdAt = currentTime,
            updatedAt = currentTime
        )
    }

    private suspend fun importSections(
        importData: ReadingListImportDto,
        readingList: ReadingList
    ): Map<Int, ReadingListSection> {

        val duplicatePositions =
            importData.sections
                .groupBy { section ->
                    section.position
                }
                .filterValues { sections ->
                    sections.size > 1
                }
                .keys

        if (duplicatePositions.isNotEmpty()) {
            throw IllegalArgumentException(
                "Duplicate reading-list section " +
                    "positions: " +
                    duplicatePositions
                        .sorted()
                        .joinToString(", ")
            )
        }

        val existingSections =
            comicDao.getSectionsForReadingList(
                readingListId = readingList.id
            )

        val importedSections =
            mutableMapOf<Int, ReadingListSection>()

        importData.sections.forEach { sectionData ->

            val existing =
                existingSections.find { section ->
                    section.position == sectionData.position
                }

            val section =
                if (existing == null) {
                    val id =
                        comicDao.insertReadingListSection(
                            ReadingListSection(
                                readingListId =
                                    readingList.id,
                                title =
                                    sectionData.title,
                                description =
                                    sectionData.description,
                                position =
                                    sectionData.position
                            )
                        )

                    ReadingListSection(
                        id = id,
                        readingListId =
                            readingList.id,
                        title =
                            sectionData.title,
                        description =
                            sectionData.description,
                        position =
                            sectionData.position
                    )
                } else {
                    val updated =
                        existing.copy(
                            title =
                                sectionData.title,
                            description =
                                sectionData.description
                        )

                    if (updated != existing) {
                        comicDao.updateReadingListSection(
                            updated
                        )
                    }

                    updated
                }

            importedSections[section.position] = section
        }

        val importedPositions =
            importData.sections.map { section ->
                section.position
            }
            .toSet()

        existingSections
            .filter { section ->
                section.position !in importedPositions
            }
            .forEach { staleSection ->
                comicDao.deleteReadingListSection(
                    staleSection
                )
            }

        return importedSections
    }

    private suspend fun importItems(
        importData: ReadingListImportDto,
        publisher: Publisher,
        universe: Universe,
        readingList: ReadingList,
        sectionsByPosition: Map<Int, ReadingListSection>
    ) {
        val importedIssueIds =
            mutableSetOf<Long>()

        importData.items.forEach { itemData ->

            val series =
                getOrCreateSeries(
                    itemData = itemData,
                    publisher = publisher
                )

            val issue =
                getOrCreateIssue(
                    itemData = itemData,
                    series = series,
                    universe = universe
                )

            importExternalIds(
                itemData = itemData,
                issue = issue
            )

            importedIssueIds.add(issue.id)

            val sectionId =
                itemData.sectionPosition?.let { sectionPosition ->

                    sectionsByPosition[sectionPosition]?.id
                        ?: throw IllegalArgumentException(
                            "Reading-list item at position " +
                                "${itemData.position} references " +
                                "unknown section position " +
                            sectionPosition
                        )
                }

            val existingItem =
                comicDao.getReadingListItem(
                    readingListId = readingList.id,
                    issueId = issue.id
                )

            if (existingItem == null) {
                comicDao.insertReadingListItem(
                    ReadingListItem(
                        readingListId = readingList.id,
                        sectionId = sectionId,
                        issueId = issue.id,
                        position = itemData.position,
                        required = itemData.required,
                        notes = itemData.notes
                    )
                )
            } else {
                val updatedItem =
                    existingItem.copy(
                        sectionId = sectionId,
                        position = itemData.position,
                        required = itemData.required,
                        notes = itemData.notes
                    )

                if (updatedItem != existingItem) {
                    comicDao.updateReadingListItem(
                        updatedItem
                    )
                }
            }
        }

        val existingItems =
            comicDao.getItemsForReadingList(
                readingListId = readingList.id
            )

        existingItems
            .filter { existingItem ->
                existingItem.issueId !in importedIssueIds
            }
            .forEach { staleItem ->
                comicDao.deleteReadingListItem(
                    staleItem
                )
            }
    }

    private suspend fun importExternalIds(
        itemData: ReadingListItemImportDto,
        issue: Issue
    ) {
        itemData.issue.externalIds.forEach { externalIdData ->

            val existing =
                comicDao.getExternalId(
                    source = externalIdData.source,
                    externalId = externalIdData.externalId
                )

            if (existing == null) {
                comicDao.insertExternalId(
                    ExternalId(
                        issueId = issue.id,
                        source = externalIdData.source,
                        externalId =
                            externalIdData.externalId,
                        url = externalIdData.url
                    )
                )

                return@forEach
            }

            if (existing.issueId != issue.id) {
                throw IllegalStateException(
                    "External ID " +
                        "'${externalIdData.source}:" +
                        "${externalIdData.externalId}' " +
                        "is already assigned to " +
                        "issue ${existing.issueId}, " +
                        "but import attempted to assign " +
                        "it to issue ${issue.id}"
                )
            }

            if (existing.url != externalIdData.url) {
                comicDao.updateExternalId(
                    existing.copy(
                        url = externalIdData.url
                    )
                )
            }
        }
    }

    private suspend fun getOrCreateSeries(
        itemData: ReadingListItemImportDto,
        publisher: Publisher
    ): Series {
        val seriesData = itemData.series

        val existing =
            comicDao.getSeries(
                publisherId = publisher.id,
                title = seriesData.title,
                volume = seriesData.volume
            )

        if (existing != null) {
            val updated =
                existing.copy(
                    startYear = seriesData.startYear,
                    endYear = seriesData.endYear
                )

            if (updated != existing) {
                comicDao.updateSeries(updated)
            }

            return updated
        }

        val id = comicDao.insertSeries(
            Series(
                publisherId = publisher.id,
                title = seriesData.title,
                volume = seriesData.volume,
                startYear = seriesData.startYear,
                endYear = seriesData.endYear
            )
        )

        return Series(
            id = id,
            publisherId = publisher.id,
            title = seriesData.title,
            volume = seriesData.volume,
            startYear = seriesData.startYear,
            endYear = seriesData.endYear
        )
    }

    private suspend fun getOrCreateIssue(
        itemData: ReadingListItemImportDto,
        series: Series,
        universe: Universe
    ): Issue {
        val issueData = itemData.issue

        val issueType =
            try {
                IssueType.valueOf(
                    issueData.type.uppercase()
                )
            } catch (exception: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Unknown issue type '${issueData.type}' " +
                            "for ${series.title} #${issueData.number}",
                    exception
                )
            }

        val existing =
            comicDao.getIssue(
                seriesId = series.id,
                issueNumber = issueData.number
            )

        if (existing != null) {
            val updated =
                existing.copy(
                    universeId = universe.id,
                    title = issueData.title,
                    publicationDate = issueData.publicationDate,
                    coverUrl = issueData.coverUrl,
                    description = issueData.description,
                    issueType = issueType
                )

            if (updated != existing) {
                comicDao.updateIssue(updated)
            }

            return updated
        }

        val id = comicDao.insertIssue(
            Issue(
                seriesId = series.id,
                universeId = universe.id,
                issueNumber = issueData.number,
                title = issueData.title,
                publicationDate = issueData.publicationDate,
                coverUrl = issueData.coverUrl,
                description = issueData.description,
                issueType = issueType
            )
        )

        return Issue(
            id = id,
            seriesId = series.id,
            universeId = universe.id,
            issueNumber = issueData.number,
            title = issueData.title,
            publicationDate = issueData.publicationDate,
            coverUrl = issueData.coverUrl,
            description = issueData.description,
            issueType = issueType
        )
    }
}