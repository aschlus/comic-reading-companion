package com.aschlus.comicreadingcompanion.data.database.models

data class RecentReadIssue(
    val issueId: Long,
    val seriesTitle: String,
    val issueNumber: String,
    val issueTitle: String?,
    val coverUrl: String?,
    val completedAt: Long
)