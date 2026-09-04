package com.aschlus.comicreadingcompanion.data.database.entities

import android.health.connect.datatypes.units.Volume
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "series",
    foreignKeys = [
        ForeignKey(
            entity = Publisher::class,
            parentColumns = ["id"],
            childColumns = ["publisherId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["publisherId"])
    ]
)
data class Series(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val publisherId: Long,
    val title: String,
    val volume: Int?,
    val startYear: Int?,
    val endYear: Int?
)