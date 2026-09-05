package com.aschlus.comicreadingcompanion.data.database.models

import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus

data class SeriesIssue(
    val issueId: Long,
    val issueNumber: String,
    val issueTitle: String?,
    val publicationDate: String?,
    val coverUrl: String?,
    val issueType: IssueType,
    val readingStatus: ReadingStatus?
)