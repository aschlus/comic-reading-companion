package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.data.remote.ComicVineIssueHit
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeaderBackButton
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeaderTitle
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicMutedInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineVolumeReviewViewModel

@Composable
fun ComicVineVolumeReviewScreen(
    viewModel: ComicVineVolumeReviewViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = ComicPaper,
        topBar = {
            ComicPrimaryHeader {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ComicPrimaryHeaderBackButton(
                        onClick = onBackClick
                    )

                    Spacer(Modifier.width(16.dp))

                    ComicPrimaryHeaderTitle(
                        text = "SERIES REVIEW"
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isLoading && uiState.volume == null) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = ComicInk
                    )
                }
            }

            uiState.error?.let { error ->
                item {
                    Text(
                        text = error,
                        color = ComicRed
                    )
                }
            }

            uiState.volume?.let { volume ->
                item {
                    ComicHomeSectionHeader(
                        text = "SERIES DETAILS"
                    )
                }

                item {
                    ReviewInfoCard {
                        Text(
                            text = volume.name,
                            color = ComicInk,
                            style = androidx.compose.material3.MaterialTheme
                                .typography.headlineSmall
                        )

                        Text(
                            text = "Start year: ${
                                volume.startYear ?: "Unknown"
                            }",
                            color = ComicInk
                        )

                        Text(
                            text = "Publisher: ${
                                volume.publisherName ?: "Unknown"
                            }",
                            color = ComicInk
                        )

                        Text(
                            text = "Reported issue count: ${
                                volume.countOfIssues ?: "Unknown"
                            }",
                            color = ComicInk
                        )

                        Text(
                            text = "Source ID: ${volume.id}",
                            color = ComicMutedInk
                        )

                        volume.description
                            ?.takeIf { it.isNotBlank() }
                            ?.let { description ->
                                Text(
                                    text = description,
                                    color = ComicInk
                                )
                            }
                    }
                }

                item {
                    ComicHomeSectionHeader(
                        text = "ISSUES"
                    )
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Loaded ${uiState.issues.size} of ${
                                uiState.totalIssueRecords
                                    ?: "an unknown number of"
                            } source issue records",
                            color = ComicInk
                        )

                        if (uiState.isLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = ComicInk
                                )

                                Text(
                                    text = "Loading issues...",
                                    color = ComicMutedInk
                                )
                            }
                        }
                    }
                }

                items(
                    items = uiState.issues,
                    key = { issue -> issue.id }
                ) { issue ->
                    ReviewIssueRow(issue)
                }

                if (!uiState.isLoading && uiState.issues.isEmpty()) {
                    item {
                        Text(
                            text = "No issue records were retrieved.",
                            color = ComicMutedInk
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewInfoCard(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White,
                RoundedCornerShape(10.dp)
            )
            .border(
                width = 2.dp,
                color = ComicInk,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun ReviewIssueRow(
    issue: ComicVineIssueHit
) {
    ReviewInfoCard {
        Text(
            text = issue.issueNumber?.let { "Issue #$it" }
                ?: "Issue number unavailable",
            color = ComicInk,
            style = androidx.compose.material3.MaterialTheme
                .typography.titleMedium
        )

        Text(
            text = issue.name ?: "Untitled issue",
            color = ComicInk
        )

        (issue.coverDate ?: issue.storeDate)
            ?.takeIf { it.isNotBlank() }
            ?.let { date ->
                Text(
                    text = "Publication date: $date",
                    color = ComicMutedInk
                )
            }

        issue.description
            ?.takeIf { it.isNotBlank() }
            ?.let { description ->
                Text(
                    text = description,
                    color = ComicMutedInk
                )
            }

        Text(
            text = "Source ID: ${issue.id}",
            color = ComicMutedInk,
            style = androidx.compose.material3.MaterialTheme
                .typography.bodySmall
        )
    }
}