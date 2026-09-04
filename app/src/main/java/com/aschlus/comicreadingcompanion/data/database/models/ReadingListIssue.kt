package com.aschlus.comicreadingcompanion.data.database.models

import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus

data class ReadingListIssue(
    val readingListItemId: Long,
    val issueId: Long,
    val position: Int,
    val required: Boolean,
    val notes: String?,
    val issueNumber: String,
    val issueTitle: String?,
    val publicationDate: String?,
    val coverUrl: String?,
    val seriesTitle: String,
    val seriesVolume: Int?,
    val readingStatus: ReadingStatus?
)