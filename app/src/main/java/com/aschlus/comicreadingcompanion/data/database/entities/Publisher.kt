package com.aschlus.comicreadingcompanion.data.database.entities

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "publishers",
    indices = [
        Index(
            value = ["name"],
            unique = true
        )
    ]
)
data class Publisher(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)