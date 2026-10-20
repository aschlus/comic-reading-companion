package com.aschlus.comicreadingcompanion.data.exporter

import kotlinx.serialization.Serializable

/**
 * Portable snapshot of the app's database.
 *
 * The IDs in this file identify records and relationships inside the
 * backup. A restore operation must map them to the destination database's
 * IDs instead of assuming the IDs are the same on another installation.
 */
@Serializable
data class ComicAppBackupDto(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val exportedAtEpochMillis: Long,
    val publishers: List<BackupPublisherDto> = emptyList(),
    val universes: List<BackupUniverseDto> = emptyList(),
    val series: List<BackupSeriesDto> = emptyList(),
    val seriesExternalIds: List<BackupSeriesExternalIdDto> = emptyList(),
    val issues: List<BackupIssueDto> = emptyList(),
    val issueExternalIds: List<BackupIssueExternalIdDto> = emptyList(),
    val readingLists: List<BackupReadingListDto> = emptyList(),
    val readingListSections: List<BackupReadingListSectionDto> = emptyList(),
    val readingListItems: List<BackupReadingListItemDto> = emptyList(),
    val readingProgress: List<BackupReadingProgressDto> = emptyList(),
    val issueOwnership: List<BackupIssueOwnershipDto> = emptyList()
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}

@Serializable
data class BackupPublisherDto(
    val id: Long,
    val name: String
)

@Serializable
data class BackupUniverseDto(
    val id: Long,
    val publisherId: Long,
    val name: String,
    val designation: String?,
    val description: String?
)

@Serializable
data class BackupSeriesDto(
    val id: Long,
    val publisherId: Long,
    val title: String,
    val volume: Int?,
    val startYear: Int?,
    val endYear: Int?
)

@Serializable
data class BackupSeriesExternalIdDto(
    val id: Long,
    val seriesId: Long,
    val source: String,
    val externalId: String,
    val url: String?
)

@Serializable
data class BackupIssueDto(
    val id: Long,
    val seriesId: Long,
    val universeId: Long?,
    val issueNumber: String,
    val title: String?,
    val publicationDate: String?,
    val coverUrl: String?,
    val description: String?,
    val issueType: String,
    val isIssueTypeManuallyOverridden: Boolean = false
)

@Serializable
data class BackupIssueExternalIdDto(
    val id: Long,
    val issueId: Long,
    val source: String,
    val externalId: String,
    val url: String?
)

@Serializable
data class BackupReadingListDto(
    val id: Long,
    val title: String,
    val description: String?,
    val publisherId: Long,
    val universeId: Long?,
    val style: String,
    val source: String,
    val sourceKey: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class BackupReadingListSectionDto(
    val id: Long,
    val readingListId: Long,
    val title: String,
    val description: String?,
    val position: Int
)

@Serializable
data class BackupReadingListItemDto(
    val id: Long,
    val readingListId: Long,
    val sectionId: Long?,
    val issueId: Long,
    val position: Int,
    val required: Boolean,
    val notes: String?
)

@Serializable
data class BackupReadingProgressDto(
    val id: Long,
    val issueId: Long,
    val status: String,
    val startedAt: Long?,
    val completedAt: Long?,
    val notes: String?
)

@Serializable
data class BackupIssueOwnershipDto(
    val id: Long,
    val issueId: Long
)