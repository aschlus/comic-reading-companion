package com.aschlus.comicreadingcompanion.data.database.models

data class SeriesDetail(
    val seriesId: Long,
    val title: String,
    val volume: Int?,
    val startYear: Int?,
    val endYear: Int?,
    val publisherName: String
)