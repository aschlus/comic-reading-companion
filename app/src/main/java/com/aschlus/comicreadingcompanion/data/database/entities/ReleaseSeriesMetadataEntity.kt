package com.aschlus.comicreadingcompanion.data.database.entities

import androidx.room3.Entity

@Entity(
    tableName = "release_series_metadata",
    primaryKeys = [
        "providerId",
        "externalSeriesId"
    ]
)
data class ReleaseSeriesMetadataEntity(
    val providerId: String,
    val externalSeriesId: String,
    val comicVineId: String?,
    val gcdId: String?,
    val sourceUrl: String?,
    val sourceModifiedAt: String?,
    val lastFetchedAt: Long
)