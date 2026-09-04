package com.aschlus.comicreadingcompanion.data.database.models

data class ReadingListContinueItem(
    val readingListId: Long,
    val issueId: Long,
    val position: Int,
    val seriesTitle: String,
    val issueNumber: String,
    val issueTitle: String?
)