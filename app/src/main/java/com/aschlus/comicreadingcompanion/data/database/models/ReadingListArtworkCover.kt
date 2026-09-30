package com.aschlus.comicreadingcompanion.data.database.models

data class ReadingListArtworkCover(
    val readingListId: Long,
    val coverUrl: String,
    val position: Int
)