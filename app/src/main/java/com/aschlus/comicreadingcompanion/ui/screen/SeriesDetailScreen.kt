package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.repository.ComicVineRefreshIssueChange
import com.aschlus.comicreadingcompanion.ui.component.ComicEmptyState
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicIssueDetailHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicProgressBar
import com.aschlus.comicreadingcompanion.ui.component.ComicSeriesIssueRow
import com.aschlus.comicreadingcompanion.ui.component.ComicWideActionButton
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacing2Xl
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingLg
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingSm
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingXs
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineSeriesRefreshUiState
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineSeriesRefreshViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.SeriesDetailViewModel

@Composable
fun SeriesDetailScreen(
    seriesId: Long,
    viewModel: SeriesDetailViewModel,
    onPublisherClick: (Long) -> Unit,
    onIssueClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit = {},
    refreshViewModel: ComicVineSeriesRefreshViewModel? = null
) {
    val series by viewModel.series.collectAsState()
    val issues by viewModel.issues.collectAsState()
    val comicVineVolumeId by viewModel.comicVineVolumeId.collectAsState()
    val refreshUiState = refreshViewModel?.uiState?.collectAsState()?.value
    var showRefreshDialog by remember(seriesId) { mutableStateOf(false) }

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
                        start = ComicSpacingLg,
                        end = ComicSpacingLg,
                        top = ComicSpacingLg,
                        bottom = ComicSpacing2Xl
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
                    ComicWideActionButton(
                        text = "EDIT SERIES DETAILS",
                        backgroundColor = ComicYellow,
                        onClick = onEditClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = ComicSpacingXs)
                    )
                }

                val linkedVolumeId = comicVineVolumeId

                if (
                    linkedVolumeId != null &&
                    refreshViewModel != null
                ) {
                    item {
                        ComicWideActionButton(
                            text = "REFRESH FROM COMIC VINE",
                            backgroundColor = ComicYellow,
                            onClick = {
                                showRefreshDialog = true

                                refreshViewModel.loadPreview(
                                    seriesId = seriesId,
                                    comicVineVolumeId = linkedVolumeId
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = ComicSpacingXs)
                        )
                    }
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
                            Modifier.padding(top = ComicSpacingSm)
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

    if (
        showRefreshDialog &&
        refreshViewModel != null &&
        refreshUiState != null
    ) {
        ComicVineSeriesRefreshDialog(
            uiState = refreshUiState,
            onDismiss = {
                refreshViewModel.clearPreview()
                showRefreshDialog = false
            },
            onConfirm = {
                refreshViewModel.applyPreview()
            }
        )
    }
}

@Composable
private fun ComicVineSeriesRefreshDialog(
    uiState: ComicVineSeriesRefreshUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val refreshApplied =
        uiState.successMessage == "Comic Vine refresh applied."

    AlertDialog(
        onDismissRequest = {
            if (!uiState.isApplying) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = "REVIEW COMIC VINE REFRESH",
                color = ComicInk,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement =
                    Arrangement.spacedBy(ComicSpacingSm)
            ) {
                if (uiState.isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )

                        Text("Loading Comic Vine details...")
                    }
                }

                if (uiState.isApplying) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )

                        Text("Applying refresh...")
                    }
                }

                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                uiState.preview?.let { preview ->
                    Text(
                        text = preview.refreshedTitle,
                        fontWeight = FontWeight.Bold,
                        color = ComicInk
                    )

                    preview.refreshedStartYear?.let { year ->
                        Text("Source start year: $year")
                    }

                    Text(
                        text = "Added: ${preview.addedCount}  •  " +
                                "Updated: ${preview.updatedCount}"
                    )

                    Text(
                        text = "Unchanged: ${preview.unchangedCount}  •  " +
                                "Missing from source: " +
                                preview.missingFromSourceCount
                    )

                    Text(
                        text = "ISSUE-BY-ISSUE CHANGES",
                        fontWeight = FontWeight.Bold,
                        color = ComicInk,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    preview.issues.forEach { change ->
                        val status = when (change.changeType) {
                            ComicVineRefreshIssueChange.ChangeType.ADDED ->
                                "ADDED"

                            ComicVineRefreshIssueChange.ChangeType.UPDATED ->
                                "UPDATED"

                            ComicVineRefreshIssueChange.ChangeType.UNCHANGED ->
                                "UNCHANGED"

                            ComicVineRefreshIssueChange.ChangeType.MISSING_FROM_SOURCE ->
                                "MISSING FROM SOURCE"
                        }

                        val title = change.title
                            ?.takeIf { it.isNotBlank() }
                            ?.let { " — $it" }
                            .orEmpty()

                        Text(
                            text = "$status  #${change.issueNumber}$title",
                            color = ComicInk,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        text = if (refreshApplied) {
                            "Comic Vine refresh applied."
                        } else {
                            "No database changes will be made unless you confirm."
                        },
                        color = ComicInk,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (refreshApplied) {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("DONE")
                }
            } else {
                TextButton(
                    enabled = uiState.preview != null &&
                            !uiState.isLoading &&
                            !uiState.isApplying,
                    onClick = onConfirm
                ) {
                    Text(
                        if (uiState.isApplying) {
                            "APPLYING..."
                        } else {
                            "CONFIRM REFRESH"
                        }
                    )
                }
            }
        },
        dismissButton = {
            if (!refreshApplied) {
                TextButton(
                    enabled = !uiState.isApplying,
                    onClick = onDismiss
                ) {
                    Text("CANCEL")
                }
            }
        }
    )
}