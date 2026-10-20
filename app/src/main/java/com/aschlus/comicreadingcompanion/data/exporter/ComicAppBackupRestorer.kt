package com.aschlus.comicreadingcompanion.data.exporter

import androidx.room3.withWriteTransaction
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.ExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.IssueOwnership
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListItem
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSection
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSource
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListStyle
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingProgress
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.SeriesExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import com.aschlus.comicreadingcompanion.data.preferences.BackupRestorePreferences

class ComicAppBackupRestorer(
    private val comicDao: ComicDao,
    private val database: ComicDatabase,
    private val parser: ComicAppBackupParser,
    private val backupRestorePreferences: BackupRestorePreferences? = null
) {

    suspend fun restoreJson(
        jsonText: String,
        sourceDescription: String = "comic-app backup JSON"
    ): ComicAppBackupDto {
        // Parsing and validation happen before any database writes.
        val backup = parser.parseJson(
            jsonText = jsonText,
            sourceDescription = sourceDescription
        )

        val restorePreferences = backupRestorePreferences

        if (restorePreferences == null) {
            replaceDatabase(backup)
        } else {
            restorePreferences.withInitializationLock {
                replaceDatabase(backup)
                restorePreferences.markRestoreCompleted()
            }
        }

        return backup
    }

    private suspend fun replaceDatabase(
        backup: ComicAppBackupDto
    ) {
        database.withWriteTransaction {
            deleteCurrentData()
            insertBackupData(backup)
        }
    }

    private suspend fun deleteCurrentData() {
        comicDao.deleteAllReadingListItemsForRestore()
        comicDao.deleteAllReadingListSectionsForRestore()
        comicDao.deleteAllReadingProgressForRestore()
        comicDao.deleteAllIssueOwnershipForRestore()
        comicDao.deleteAllIssueExternalIdsForRestore()
        comicDao.deleteAllSeriesExternalIdsForRestore()
        comicDao.deleteAllIssuesForRestore()
        comicDao.deleteAllReadingListsForRestore()
        comicDao.deleteAllSeriesForRestore()
        comicDao.deleteAllUniversesForRestore()
        comicDao.deleteAllPublishersForRestore()
    }

    private suspend fun insertBackupData(
        backup: ComicAppBackupDto
    ) {
        comicDao.insertPublishersForRestore(
            backup.publishers.map { item ->
                Publisher(
                    id = item.id,
                    name = item.name
                )
            }
        )

        comicDao.insertUniversesForRestore(
            backup.universes.map { item ->
                Universe(
                    id = item.id,
                    publisherId = item.publisherId,
                    name = item.name,
                    designation = item.designation,
                    description = item.description
                )
            }
        )

        comicDao.insertSeriesForRestore(
            backup.series.map { item ->
                Series(
                    id = item.id,
                    publisherId = item.publisherId,
                    title = item.title,
                    volume = item.volume,
                    startYear = item.startYear,
                    endYear = item.endYear
                )
            }
        )

        comicDao.insertSeriesExternalIdsForRestore(
            backup.seriesExternalIds.map { item ->
                SeriesExternalId(
                    id = item.id,
                    seriesId = item.seriesId,
                    source = item.source,
                    externalId = item.externalId,
                    url = item.url
                )
            }
        )

        comicDao.insertIssuesForRestore(
            backup.issues.map { item ->
                Issue(
                    id = item.id,
                    seriesId = item.seriesId,
                    universeId = item.universeId,
                    issueNumber = item.issueNumber,
                    title = item.title,
                    publicationDate = item.publicationDate,
                    coverUrl = item.coverUrl,
                    description = item.description,
                    issueType = IssueType.valueOf(item.issueType)
                )
            }
        )

        comicDao.insertIssueExternalIdsForRestore(
            backup.issueExternalIds.map { item ->
                ExternalId(
                    id = item.id,
                    issueId = item.issueId,
                    source = item.source,
                    externalId = item.externalId,
                    url = item.url
                )
            }
        )

        comicDao.insertReadingListsForRestore(
            backup.readingLists.map { item ->
                ReadingList(
                    id = item.id,
                    title = item.title,
                    description = item.description,
                    publisherId = item.publisherId,
                    universeId = item.universeId,
                    style = ReadingListStyle.valueOf(item.style),
                    source = ReadingListSource.valueOf(item.source),
                    sourceKey = item.sourceKey,
                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt
                )
            }
        )

        comicDao.insertReadingListSectionsForRestore(
            backup.readingListSections.map { item ->
                ReadingListSection(
                    id = item.id,
                    readingListId = item.readingListId,
                    title = item.title,
                    description = item.description,
                    position = item.position
                )
            }
        )

        comicDao.insertReadingListItemsForRestore(
            backup.readingListItems.map { item ->
                ReadingListItem(
                    id = item.id,
                    readingListId = item.readingListId,
                    sectionId = item.sectionId,
                    issueId = item.issueId,
                    position = item.position,
                    required = item.required,
                    notes = item.notes
                )
            }
        )

        comicDao.insertReadingProgressForRestore(
            backup.readingProgress.map { item ->
                ReadingProgress(
                    id = item.id,
                    issueId = item.issueId,
                    status = ReadingStatus.valueOf(item.status),
                    startedAt = item.startedAt,
                    completedAt = item.completedAt,
                    notes = item.notes
                )
            }
        )

        comicDao.insertIssueOwnershipForRestore(
            backup.issueOwnership.map { item ->
                IssueOwnership(
                    id = item.id,
                    issueId = item.issueId
                )
            }
        )
    }
}