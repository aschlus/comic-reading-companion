package com.aschlus.comicreadingcompanion.data.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "external_ids",
    foreignKeys = [
        ForeignKey(
            entity = Issue::class,
            parentColumns = ["id"],
            childColumns = ["issueId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["issueId"]),
        Index(value = ["source", "externalId"], unique = true)
    ]
)
data class ExternalId(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val issueId: Long,
    val source: String,
    val externalId: String,
    val url: String?
)