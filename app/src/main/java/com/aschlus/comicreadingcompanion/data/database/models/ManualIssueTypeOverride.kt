package com.aschlus.comicreadingcompanion.data.database.models

import com.aschlus.comicreadingcompanion.data.database.entities.IssueType

data class ManualIssueTypeOverride(
    val externalId: String,
    val issueType: IssueType
)