package com.aschlus.comicreadingcompanion.data.importer

import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListItem
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import com.aschlus.comicreadingcompanion.data.importer.models.ReadingListImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.ReadingListItemImportDto
import kotlinx.coroutines.flow.first

class ReadingListImporter(
    private val comicDao: ComicDao
) {

    suspend fun import(
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

        importItems(
            importData = importData,
            publisher = publisher,
            universe = universe,
            readingList = readingList
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

    private suspend fun importItems(
        importData: ReadingListImportDto,
        publisher: Publisher,
        universe: Universe,
        readingList: ReadingList
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

            importedIssueIds.add(issue.id)

            val existingItem =
                comicDao.getReadingListItem(
                    readingListId = readingList.id,
                    issueId = issue.id
                )

            if (existingItem == null) {
                comicDao.insertReadingListItem(
                    ReadingListItem(
                        readingListId = readingList.id,
                        sectionId = null,
                        issueId = issue.id,
                        position = itemData.position,
                        required = itemData.required,
                        notes = itemData.notes
                    )
                )
            } else {
                val updatedItem =
                    existingItem.copy(
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