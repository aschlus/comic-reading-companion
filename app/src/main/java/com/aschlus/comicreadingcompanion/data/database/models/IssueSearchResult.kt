package com.aschlus.comicreadingcompanion.data.database.models

import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus

data class IssueSearchResult(
    val issueId: Long,
    val seriesId: Long,
    val seriesTitle: String,
    val seriesVolume: Int?,
    val issueNumber: String,
    val issueTitle: String?,
    val publicationDate: String?,
    val publisherName: String,
    val readingStatus: ReadingStatus?
)