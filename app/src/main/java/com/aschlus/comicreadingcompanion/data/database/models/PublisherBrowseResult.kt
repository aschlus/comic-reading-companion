package com.aschlus.comicreadingcompanion.data.database.models

data class PublisherBrowseResult(
    val publisherId: Long,
    val name: String,
    val seriesCount: Int,
    val coverUrl1: String?,
    val coverUrl2: String?,
    val coverUrl3: String?
)