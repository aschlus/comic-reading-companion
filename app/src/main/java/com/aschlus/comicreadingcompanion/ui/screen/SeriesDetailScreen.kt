package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.room3.Update
import com.aschlus.comicreadingcompanion.data.database.entities.ExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.models.SeriesIssue
import com.aschlus.comicreadingcompanion.ui.viewmodel.SeriesDetailViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    seriesId: Long,
    viewModel: SeriesDetailViewModel,
    onIssueClick: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val series by viewModel.series.collectAsState()
    val issues by viewModel.issues.collectAsState()

    LaunchedEffect(seriesId) {
        viewModel.loadSeries(seriesId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        series?.title ?: "Series"
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding: PaddingValues ->

        val currentSeries = series

        if (currentSeries == null) {
            Text(
                text = "Loading...",
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = currentSeries.title,
                    style =
                        MaterialTheme.typography.headlineMedium
                )

                val seriesMetadata = buildList {
                    currentSeries.volume?.let { volume ->
                        add("Volume $volume")
                    }

                    when {
                        currentSeries.startYear != null &&
                                currentSeries.endYear != null &&
                                currentSeries.startYear !=
                                currentSeries.endYear -> {
                                add(
                                    "${currentSeries.seriesId}-" +
                                        "${currentSeries.endYear}"
                                )
                            }

                        currentSeries.startYear != null -> {
                            add(
                                currentSeries.startYear.toString()
                            )
                        }
                    }

                    add(currentSeries.publisherName)
                }

                Text(
                    text = seriesMetadata.joinToString(" • "),
                    style =
                        MaterialTheme.typography.bodyMedium
                )

                val readCount = issues.count { issue ->
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

                Text(
                    text =
                        "$readCount or $totalCount read • " +
                            "$completionPercentage% complete"
                )

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Issues",
                    style =
                        MaterialTheme.typography.titleMedium
                )

                if (issues.isEmpty()) {
                    Text("No issues found for this series")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = issues,
                            key = { issue ->
                                issue.issueId
                            }
                        ) { issue ->
                            SeriesIssueRow(
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
}

@Composable
private fun SeriesIssueRow(
    issue: SeriesIssue,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            )
            .padding(
                vertical = 10.dp,
                horizontal = 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement =
                Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "#${issue.issueNumber}",
                style =
                    MaterialTheme.typography.titleMedium
            )

            issue.issueTitle?.let { title ->
                Text(
                    text = title,
                    style =
                        MaterialTheme.typography.bodyMedium
                )
            }

            val metadata = buildList {
                issue.publicationDate?.let {
                    add(
                        formatSeriesIssuePublicationDate(it)
                    )
                }

                add(
                    issue.issueType.name
                        .replace("_"," ")
                        .lowercase()
                        .replaceFirstChar {
                            it.titlecase(
                                Locale.getDefault()
                            )
                        }
                )
            }

            Text(
                text = metadata.joinToString(" • "),
                style =
                    MaterialTheme.typography.bodySmall
            )
        }

        Text(
            text = when (issue.readingStatus) {
                ReadingStatus.READ -> "Read"
                ReadingStatus.READING -> "Reading"
                else -> "Unread"
            },
            style =
                MaterialTheme.typography.labelMedium
        )
    }
}

private fun formatSeriesIssuePublicationDate(
    publicationDate: String
): String {
    return try {
        YearMonth
            .parse(publicationDate)
            .format(
                DateTimeFormatter.ofPattern(
                    "MMM yyyy",
                    Locale.getDefault()
                )
            )
    } catch (_: Exception) {
        publicationDate
    }
}