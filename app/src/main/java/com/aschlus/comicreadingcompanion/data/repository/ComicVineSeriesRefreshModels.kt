package com.aschlus.comicreadingcompanion.data.repository

import com.aschlus.comicreadingcompanion.data.database.entities.IssueType

data class ComicVineRefreshIssue(
    val comicVineIssueId: Long,
    val issueNumber: String,
    val title: String?,
    val publicationDate: String?,
    val coverUrl: String?,
    val issueType: IssueType,
    val siteDetailsUrl: String?
)

data class ComicVineRefreshIssueChange(
    val issueNumber: String,
    val title: String?,
    val changeType: ChangeType,
    val localIssueId: Long? = null
) {
    enum class ChangeType {
        ADDED,
        UPDATED,
        UNCHANGED,
        MISSING_FROM_SOURCE
    }
}

data class ComicVineSeriesRefreshPreview(
    val seriesId: Long,
    val comicVineVolumeId: Long,
    val refreshedTitle: String,
    val refreshedStartYear: Int?,
    val issues: List<ComicVineRefreshIssueChange>
) {
    val addedCount: Int
        get() = issues.count {
            it.changeType == ComicVineRefreshIssueChange.ChangeType.ADDED
        }

    val updatedCount: Int
        get() = issues.count {
            it.changeType == ComicVineRefreshIssueChange.ChangeType.UPDATED
        }

    val unchangedCount: Int
        get() = issues.count {
            it.changeType == ComicVineRefreshIssueChange.ChangeType.UNCHANGED
        }

    val missingFromSourceCount: Int
        get() = issues.count {
            it.changeType == ComicVineRefreshIssueChange.ChangeType.MISSING_FROM_SOURCE
        }
}