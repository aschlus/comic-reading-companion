package com.aschlus.comicreadingcompanion.data.database.models

import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus

data class OwnedIssueResult(
    val issueId: Long,
    val seriesId: Long,
    val seriesTitle: String,
    val seriesVolume: Int?,
    val issueNumber: String,
    val issueTitle: String?,
    val publicationDate: String?,
    val coverUrl: String?,
    val issueType: IssueType,
    val publisherName: String,
    val readingStatus: ReadingStatus?
)