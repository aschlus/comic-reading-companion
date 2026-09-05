package com.aschlus.comicreadingcompanion.data.database.models

data class PublisherSeries(
    val seriesId: Long,
    val title: String,
    val volume: Int?,
    val startYear: Int?,
    val endYear: Int?,
    val totalCount: Int,
    val readCount: Int
)