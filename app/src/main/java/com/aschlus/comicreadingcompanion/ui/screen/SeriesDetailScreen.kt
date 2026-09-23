package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicIssueDetailHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicProgressBar
import com.aschlus.comicreadingcompanion.ui.component.ComicSeriesIssueRow
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import com.aschlus.comicreadingcompanion.ui.viewmodel.SeriesDetailViewModel
import androidx.compose.ui.text.font.FontStyle
import com.aschlus.comicreadingcompanion.ui.component.ComicEmptyState

@Composable
fun SeriesDetailScreen(
    seriesId: Long,
    viewModel: SeriesDetailViewModel,
    onPublisherClick: (Long) -> Unit,
    onIssueClick: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val series by viewModel.series.collectAsState()
    val issues by viewModel.issues.collectAsState()

    LaunchedEffect(seriesId) {
        viewModel.loadSeries(seriesId)
    }

    Scaffold(
        containerColor = ComicPaper,
        topBar = {
            ComicIssueDetailHeader(
                onBackClick = onBackClick,
                title = "SERIES DETAIL"
            )
        }
    ) { innerPadding: PaddingValues ->

        val currentSeries = series

        if (currentSeries == null) {
            Text(
                text = "Loading...",
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(18.dp),
                color = ComicInk
            )
        } else {
            val readCount =
                issues.count { issue ->
                    issue.readingStatus == ReadingStatus.READ
                }

            val totalCount = issues.size

            val progress =
                if (totalCount == 0) {
                    0f
                } else {
                    readCount.toFloat() / totalCount.toFloat()
                }

            val completionPercentage =
                if (totalCount == 0) {
                    0
                } else {
                    (readCount * 100) / totalCount
                }

            val seriesMetadata =
                buildList {
                    currentSeries
                        .volume
                        ?.let { volume ->
                            add("Volume $volume")
                        }

                    when {
                        currentSeries.startYear != null &&
                            currentSeries.endYear != null &&
                            currentSeries.startYear != currentSeries.endYear -> {

                            add(
                                "${currentSeries.startYear}" +
                                "-" +
                                "${currentSeries.endYear}"
                            )
                        }

                        currentSeries.startYear != null -> {

                            add(currentSeries.startYear.toString())
                        }
                    }
                }
                    .joinToString(" • ")

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 24.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text =
                            currentSeries.title,
                        style =
                            MaterialTheme.typography.headlineLarge
                                .copy(
                                    fontSize = 34.sp,
                                    lineHeight = 38.sp,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Normal
                                ),
                        color = ComicInk
                    )
                }

                if (
                    seriesMetadata.isNotBlank()
                ) {
                    item {
                        Text(
                            text =
                                seriesMetadata,
                            style =
                                MaterialTheme.typography.titleMedium
                                    .copy(
                                        fontSize = 18.sp,
                                        lineHeight = 21.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                            color = ComicInk
                        )
                    }
                }

                item {
                    Text(
                        text =
                            currentSeries.publisherName,
                        modifier =
                            Modifier.clickable {
                                onPublisherClick(currentSeries.publisherId)
                            },
                        style =
                            MaterialTheme.typography.titleMedium
                                .copy(
                                    fontSize = 17.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                ),
                        color = ComicBlue
                    )
                }

                item {
                    Text(
                        text =
                            "$readCount of " +
                            "$totalCount read • " +
                            "$completionPercentage% complete",
                        modifier =
                            Modifier.padding(top = 10.dp),
                        style =
                            MaterialTheme.typography.titleMedium
                                .copy(
                                    fontSize = 16.sp,
                                    lineHeight = 19.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                        color = ComicInk
                    )
                }

                item {
                    ComicProgressBar(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                        progressColor = ComicYellow,
                        trackColor = ComicPaper,
                        borderColor = ComicInk,
                        height = 16.dp,
                        shape = RoundedCornerShape(percent = 50)
                    )
                }

                item {
                    ComicHomeSectionHeader(
                        text = "ISSUES",
                        modifier =
                            Modifier.padding(top = 8.dp)
                    )
                }

                if (issues.isEmpty()) {
                    item {
                        ComicEmptyState(
                            message = "No issues found for this series"
                        )
                    }
                } else {
                    items(
                        items = issues,
                        key = { issue -> issue.issueId }
                    ) { issue ->
                        ComicSeriesIssueRow(
                            issue = issue,
                            onClick = {
                                onIssueClick(
                                    issue.issueId
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}