
package com.aschlus.comicreadingcompanion.data.database.entities

import androidx.room3.Entity
import androidx.room3.Index

@Entity(
    tableName = "scheduled_releases",
    primaryKeys = [
        "providerId",
        "externalIssueId"
    ],
    indices = [
        Index(value = ["externalSeriesId"]),
        Index(value = ["storeReleaseDate"]),
        Index(value = ["focDate"]),
        Index(value = ["seriesTitleSnapshot"])
    ]
)
data class ScheduledReleaseEntity(
    val providerId: String,
    val externalIssueId: String,
    val externalSeriesId: String,

    // Provider-supplied identity snapshots.
    // These are useful before a local catalog match exists.
    val seriesTitleSnapshot: String,
    val seriesVolumeSnapshot: Int?,
    val seriesStartYearSnapshot: Int?,
    val publisherNameSnapshot: String?,
    val issueNumberSnapshot: String?,
    val displayTitleSnapshot: String?,

    // Provider-supplied release information.
    val coverDate: String?,
    val storeReleaseDate: String?,
    val focDate: String?,
    val price: String?,
    val priceCurrency: String?,
    val distributorSku: String?,
    val upc: String?,

    // Optional external catalog identifiers.
    val comicVineId: String?,
    val gcdId: String?,

    val sourceUrl: String?,
    val sourceModifiedAt: String?,
    val lastSyncedAt: Long
)
