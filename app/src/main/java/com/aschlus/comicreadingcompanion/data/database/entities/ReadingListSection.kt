package com.aschlus.comicreadingcompanion.data.database.entities

import androidx.compose.material3.FabPosition
import androidx.compose.ui.tooling.preview.Preview
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "reading_list_sections",
    foreignKeys = [
        ForeignKey(
            entity = ReadingList::class,
            parentColumns = ["id"],
            childColumns = ["readingListId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["readingListId"])
    ]
)
data class ReadingListSection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val readingListId: Long,
    val title: String,
    val description: String?,
    val position: Int
)