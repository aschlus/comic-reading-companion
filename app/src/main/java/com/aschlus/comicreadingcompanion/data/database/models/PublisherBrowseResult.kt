package com.aschlus.comicreadingcompanion.data.database.models

data class PublisherBrowseResult(
    val publisherId: Long,
    val name: String,
    val seriesCount: Int
)