package com.aschlus.comicreadingcompanion.data.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "reading_list_items",
    foreignKeys = [
        ForeignKey(
            entity = ReadingList::class,
            parentColumns = ["id"],
            childColumns = ["readingListId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ReadingListSection::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Issue::class,
            parentColumns = ["id"],
            childColumns = ["issueId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["readingListId"]),
        Index(value = ["sectionId"]),
        Index(value = ["issueId"])
    ]
)
data class ReadingListItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val readingListId: Long,
    val sectionId: Long?,
    val issueId: Long,
    val position: Int,
    val required: Boolean = true,
    val notes: String?
)