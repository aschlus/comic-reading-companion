package com.aschlus.comicreadingcompanion.data.database.models

import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListStyle

data class IssueReadingListResult(
    val readingListId: Long,
    val title: String,
    val style: ReadingListStyle,
    val position: Int,
    val totalCount: Int,
    val readCount: Int
)