package com.aschlus.comicreadingcompanion.data.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "reading_lists",
    foreignKeys = [
        ForeignKey(
            entity = Publisher::class,
            parentColumns = ["id"],
            childColumns = ["publisherId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Universe::class,
            parentColumns = ["id"],
            childColumns = ["universeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["publisherId"]),
        Index(value = ["universeId"])
    ]
)
data class ReadingList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String?,
    val publisherId: Long,
    val universeId: Long?,
    val createdAt: Long,
    val updatedAt: Long
)