package com.aschlus.comicreadingcompanion.data.database.entities

import androidx.room3.Entity

@Entity(
    tableName = "scheduled_release_sync_state",
    primaryKeys = [
        "providerId",
        "fromDate",
        "toDate"
    ]
)
data class ScheduledReleaseSyncStateEntity(
    val providerId: String,
    val fromDate: String,
    val toDate: String,
    val lastSuccessfulSyncAt: Long
)