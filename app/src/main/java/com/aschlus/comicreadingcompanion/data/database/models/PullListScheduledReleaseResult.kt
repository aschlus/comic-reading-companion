package com.aschlus.comicreadingcompanion.data.database.models

data class PullListScheduledReleaseResult(
    val seriesId: Long,
    val providerId: String,
    val externalIssueId: String,
    val externalSeriesId: String,
    val seriesTitle: String,
    val publisherName: String,
    val issueNumber: String?,
    val displayTitle: String?,
    val coverDate: String?,
    val storeReleaseDate: String?,
    val focDate: String?,
    val price: String?,
    val priceCurrency: String?,
    val sourceUrl: String?,
    val lastSyncedAt: Long,
    val manuallyCanceledAt: Long?
)