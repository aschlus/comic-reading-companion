package com.aschlus.comicreadingcompanion.data.database.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "issues",
    foreignKeys = [
        ForeignKey(
            entity = Series::class,
            parentColumns = ["id"],
            childColumns = ["seriesId"],
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
        Index(value = ["seriesId"]),
        Index(value = ["universeId"])
    ]
)
data class Issue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val seriesId: Long,
    val universeId: Long?,
    val issueNumber: String,
    val title: String?,
    val publicationDate: String?,
    val coverUrl: String?,
    val description: String?,
    val issueType: IssueType
)

enum class IssueType {
    REGULAR,
    ANNUAL,
    SPECIAL,
    ONE_SHOT,
    GIANT_SIZE,
    PREVIEW
}