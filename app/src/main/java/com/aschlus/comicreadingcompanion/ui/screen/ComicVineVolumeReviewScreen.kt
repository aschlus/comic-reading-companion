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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlueLight

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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(ComicRed.copy(alpha = 0.12f))
                            .border(
                                width = 2.dp,
                                color = ComicRed,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = error,
                            color = ComicRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "SERIES TITLE",
                                color = ComicInk,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            val titleFieldShape = RoundedCornerShape(6.dp)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(titleFieldShape)
                                    .background(ComicPaper)
                                    .border(
                                        width = 2.dp,
                                        color = ComicInk,
                                        shape = titleFieldShape
                                    )
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                BasicTextField(
                                    value = uiState.editedTitle,
                                    onValueChange = viewModel::updateEditedTitle,
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = ComicInk,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ReviewMetadataItem(
                                    label = "START YEAR",
                                    value = volume.startYear?.toString() ?: "Unknown",
                                    modifier = Modifier.weight(1f)
                                )

                                ReviewMetadataItem(
                                    label = "PUBLISHER",
                                    value = volume.publisherName ?: "Unknown",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ReviewMetadataItem(
                                    label = "REPORTED ISSUES",
                                    value = volume.countOfIssues?.toString() ?: "Unknown",
                                    modifier = Modifier.weight(1f)
                                )

                                ReviewMetadataItem(
                                    label = "SOURCE ID",
                                    value = volume.id.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(ComicBlueLight)
                                .border(
                                    width = 2.dp,
                                    color = ComicInk,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Loaded ${uiState.issues.size} of ${
                                    uiState.totalIssueRecords
                                        ?: "an unknown number of"
                                } source issue records",
                                color = ComicInk,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(ComicPaper)
                                .border(
                                    width = 2.dp,
                                    color = ComicInk,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "No issues records were retrieved",
                                color = ComicMutedInk,
                                fontSize = 13.sp
                            )
                        }
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
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 4.dp)
                .background(
                    color = ComicInk,
                    shape = shape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(ComicPaper)
                .border(
                    width = 2.dp,
                    color = ComicInk,
                    shape = shape
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ReviewMetadataItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = ComicMutedInk,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = value,
            color = ComicInk,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReviewIssueRow(
    issue: ComicVineIssueHit
) {
    val shape = RoundedCornerShape(10.dp)
    val badgeShape = RoundedCornerShape(6.dp)

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 4.dp)
                .background(
                    color = ComicInk,
                    shape = shape
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(ComicPaper)
                .border(
                    width = 2.dp,
                    color = ComicInk,
                    shape = shape
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 9.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(64.dp)
                    .clip(badgeShape)
                    .background(ComicBlueLight)
                    .border(
                        width = 2.dp,
                        color = ComicInk,
                        shape = badgeShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = issue.issueNumber?.let { "#$it" }
                        ?: "ISSUE",
                    color = ComicInk,
                    style = androidx.compose.material3.MaterialTheme
                        .typography.titleMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Black
                        ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = issue.name
                        ?.takeIf { it.isNotBlank() }
                        ?: "Untitled issue",
                    color = ComicInk,
                    style = androidx.compose.material3.MaterialTheme
                        .typography.titleMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                (issue.coverDate ?: issue.storeDate)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { date ->
                        Text(
                            text = "Publication date: $date",
                            color = ComicInk.copy(alpha = 0.8f),
                            style = androidx.compose.material3.MaterialTheme
                                .typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp
                                ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                Text(
                    text = "Source ID: ${issue.id}",
                    color = ComicInk.copy(alpha = 0.7f),
                    style = androidx.compose.material3.MaterialTheme
                        .typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}