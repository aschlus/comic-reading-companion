package com.aschlus.comicreadingcompanion.data.database.entities

import android.graphics.pdf.content.PdfPageGotoLinkContent
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "universes",
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
data class Universe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val publisherId: Long,
    val name: String,
    val destination: String?,
    val description: String?
)