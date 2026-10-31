
package com.aschlus.comicreadingcompanion.data.database.entities

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "scheduled_release_changes",
    indices = [
        Index(
            value = [
                "providerId",
                "externalIssueId",
                "detectedAt"
            ]
        )
    ]
)
data class ScheduledReleaseChangeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val providerId: String,
    val externalIssueId: String,
    val externalSeriesId: String,

    val seriesTitleSnapshot: String,
    val issueNumberSnapshot: String?,

    val previousStoreReleaseDate: String?,
    val newStoreReleaseDate: String?,

    val previousFocDate: String?,
    val newFocDate: String?,

    val sourceModifiedAt: String?,
    val detectedAt: Long,

    val previousManuallyCanceledAt: Long? = null,
    val newManuallyCanceledAt: Long? = null
)
