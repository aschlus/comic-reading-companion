package com.aschlus.comicreadingcompanion.data.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = Issue::class,
            parentColumns = ["id"],
            childColumns = ["issueId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["issueId"], unique = true)
    ]
)
data class ReadingProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val issueId: Long,
    val status: ReadingStatus = ReadingStatus.UNREAD,
    val startedAt: Long?,
    val completedAt: Long?,
    val notes: String?
)

enum class ReadingStatus {
    UNREAD,
    READING,
    READ
}