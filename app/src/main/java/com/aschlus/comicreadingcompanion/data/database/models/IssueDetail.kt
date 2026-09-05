package com.aschlus.comicreadingcompanion.data.database.models

import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus

data class IssueDetail(
    val issueId: Long,
    val seriesTitle: String,
    val seriesVolume: Int?,
    val issueNumber: String,
    val issueTitle: String?,
    val publicationDate: String?,
    val coverUrl: String?,
    val description: String?,
    val issueType: IssueType,
    val publisherName: String,
    val universeName: String?,
    val universeDesignation: String?,
    val readingStatus: ReadingStatus?
)