package com.aschlus.comicreadingcompanion.data.exporter

import androidx.room3.withWriteTransaction
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import kotlinx.serialization.json.Json

class ComicAppBackupExporter(
    private val comicDao: ComicDao,
    private val database: ComicDatabase
) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun exportJson(): String {
        return json.encodeToString(export())
    }

    suspend fun export(): ComicAppBackupDto {
        return database.withWriteTransaction {
            val publishers = comicDao.getAllPublishers()
            val universes = comicDao.getAllUniversesForBackup()
            val series = comicDao.getAllSeriesForBackup()
            val seriesExternalIds = comicDao.getAllSeriesExternalIdsForBackup()
            val issues = comicDao.getAllIssuesForBackup()
            val issueExternalIds = comicDao.getAllIssueExternalIdsForBackup()
            val readingLists = comicDao.getAllReadingListsForBackup()
            val sections = comicDao.getAllReadingListSectionsForBackup()
            val items = comicDao.getAllReadingListItemsForBackup()
            val progress = comicDao.getAllReadingProgressForBackup()
            val ownership = comicDao.getAllIssueOwnershipForBackup()

            ComicAppBackupDto(
                exportedAtEpochMillis = System.currentTimeMillis(),

                publishers = publishers.map { publisher ->
                    BackupPublisherDto(
                        id = publisher.id,
                        name = publisher.name
                    )
                },

                universes = universes.map { universe ->
                    BackupUniverseDto(
                        id = universe.id,
                        publisherId = universe.publisherId,
                        name = universe.name,
                        designation = universe.designation,
                        description = universe.description
                    )
                },

                series = series.map { item ->
                    BackupSeriesDto(
                        id = item.id,
                        publisherId = item.publisherId,
                        title = item.title,
                        volume = item.volume,
                        startYear = item.startYear,
                        endYear = item.endYear,
                        universeId = item.universeId
                    )
                },

                seriesExternalIds = seriesExternalIds.map { externalId ->
                    BackupSeriesExternalIdDto(
                        id = externalId.id,
                        seriesId = externalId.seriesId,
                        source = externalId.source,
                        externalId = externalId.externalId,
                        url = externalId.url
                    )
                },

                issues = issues.map { issue ->
                    BackupIssueDto(
                        id = issue.id,
                        seriesId = issue.seriesId,
                        universeId = issue.universeId,
                        issueNumber = issue.issueNumber,
                        title = issue.title,
                        publicationDate = issue.publicationDate,
                        coverUrl = issue.coverUrl,
                        description = issue.description,
                        issueType = issue.issueType.name,
                        isIssueTypeManuallyOverridden =
                            issue.isIssueTypeManuallyOverridden
                    )
                },

                issueExternalIds = issueExternalIds.map { externalId ->
                    BackupIssueExternalIdDto(
                        id = externalId.id,
                        issueId = externalId.issueId,
                        source = externalId.source,
                        externalId = externalId.externalId,
                        url = externalId.url
                    )
                },

                readingLists = readingLists.map { readingList ->
                    BackupReadingListDto(
                        id = readingList.id,
                        title = readingList.title,
                        description = readingList.description,
                        publisherId = readingList.publisherId,
                        universeId = readingList.universeId,
                        style = readingList.style.name,
                        source = readingList.source.name,
                        sourceKey = readingList.sourceKey,
                        createdAt = readingList.createdAt,
                        updatedAt = readingList.updatedAt
                    )
                },

                readingListSections = sections.map { section ->
                    BackupReadingListSectionDto(
                        id = section.id,
                        readingListId = section.readingListId,
                        title = section.title,
                        description = section.description,
                        position = section.position
                    )
                },

                readingListItems = items.map { item ->
                    BackupReadingListItemDto(
                        id = item.id,
                        readingListId = item.readingListId,
                        sectionId = item.sectionId,
                        issueId = item.issueId,
                        position = item.position,
                        required = item.required,
                        notes = item.notes
                    )
                },

                readingProgress = progress.map { item ->
                    BackupReadingProgressDto(
                        id = item.id,
                        issueId = item.issueId,
                        status = item.status.name,
                        startedAt = item.startedAt,
                        completedAt = item.completedAt,
                        notes = item.notes
                    )
                },

                issueOwnership = ownership.map { item ->
                    BackupIssueOwnershipDto(
                        id = item.id,
                        issueId = item.issueId
                    )
                }
            )
        }
    }
}