package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.component.AddIssueToReadingListSheet
import com.aschlus.comicreadingcompanion.ui.component.ComicCoverImage
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicIssueDetailHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicIssueReadingListRow
import com.aschlus.comicreadingcompanion.ui.component.ComicIssueStatusSelector
import com.aschlus.comicreadingcompanion.ui.component.ComicWideActionButton
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import com.aschlus.comicreadingcompanion.ui.viewmodel.AddIssueToReadingListViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.IssueDetailViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun IssueDetailScreen(
    issueId: Long,
    viewModel: IssueDetailViewModel,
    addToReadingListViewModel:
            AddIssueToReadingListViewModel? = null,
    onSeriesClick: (Long) -> Unit,
    onReadingListClick: (Long, Int) -> Unit = { _, _ -> },
    onBackClick: () -> Unit
) {
    val issue by viewModel.issue.collectAsState()

    val readingLists by viewModel.readingLists.collectAsState()

    var showAddToReadingListSheet by remember(issueId) {
        mutableStateOf(false)
    }

    LaunchedEffect(issueId) {
        viewModel.loadIssue(issueId)
    }

    Scaffold(
        containerColor = ComicPaper,
        topBar = {
            ComicIssueDetailHeader(
                onBackClick = onBackClick
            )
        }
    ) { innerPadding: PaddingValues ->

        val currentIssue = issue

        if (currentIssue == null) {
            Text(
                text = "Loading...",
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(18.dp),
                color = ComicInk
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 14.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(14.dp),
                    verticalAlignment =
                        Alignment.Top
                ) {
                    ComicIssueCover(
                        coverUrl = currentIssue.coverUrl,
                        contentDescription =
                            "${currentIssue.seriesTitle} " +
                            "#${currentIssue.issueNumber} " +
                            "cover",
                        modifier =
                            Modifier
                                .weight(0.43f)
                                .aspectRatio(2f / 3f)
                    )

                    Column(
                        modifier =
                            Modifier.weight(0.57f),
                        verticalArrangement =
                            Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            text = currentIssue.seriesTitle,
                            modifier =
                                Modifier.clickable {
                                    onSeriesClick(currentIssue.seriesId)
                                },
                            style =
                                MaterialTheme.typography.titleLarge
                                    .copy(
                                        fontSize = 21.sp,
                                        lineHeight = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline
                                    ),
                            color = ComicBlue,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text =
                                "#${currentIssue.issueNumber}",
                            style =
                                MaterialTheme.typography.headlineLarge
                                    .copy(
                                        fontSize = 34.sp,
                                        lineHeight = 36.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                            color = ComicInk
                        )

                        currentIssue
                            .issueTitle
                            ?.takeIf { it.isNotBlank() }
                            ?.let { title ->
                                Text(
                                    text = title,
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

                        Spacer(modifier = Modifier.height(2.dp))

                        IssueMetadataRow(
                            label = "Publisher",
                            value = currentIssue.publisherName
                        )

                        currentIssue
                            .publicationDate
                            ?.let { publicationDate ->
                                IssueMetadataRow(
                                    label = "Publication Date",
                                    value = formatIssuePublicationDate(publicationDate)
                                )
                            }

                        IssueMetadataRow(
                            label = "Issue Type",
                            value = currentIssue.issueType
                                .name.replace("_", " ")
                                .lowercase()
                                .replaceFirstChar { it.titlecase(Locale.getDefault()) }
                        )

                        currentIssue.universeDesignation
                            ?.let { designation ->
                                IssueMetadataRow(
                                    label = "Continuity",
                                    value = designation
                                )
                            }
                    }
                }

                HorizontalDivider(
                    thickness = 2.dp,
                    color = ComicInk
                )

                ComicHomeSectionHeader(
                    text = "READING STATUS"
                )

                ComicIssueStatusSelector(
                    readingStatus = currentIssue.readingStatus,
                    onUnreadClick = { viewModel.markAsUnread() },
                    onReadingClick = { viewModel.markAsReading() },
                    onReadClick = { viewModel.markAsRead() }
                )

                if (addToReadingListViewModel != null) {
                    ComicWideActionButton(
                        text = "ADD TO READING LIST",
                        backgroundColor = ComicYellow,
                        onClick = {
                            showAddToReadingListSheet = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (readingLists.isNotEmpty()) {
                    HorizontalDivider(
                        modifier =
                            Modifier.padding(
                                top = 4.dp
                            ),
                        thickness = 2.dp,
                        color = ComicInk
                    )

                    ComicHomeSectionHeader(
                        text = "IN READING LISTS"
                    )

                    readingLists.forEach { readingList ->
                        ComicIssueReadingListRow(
                            readingList = readingList,
                            onClick = {
                                onReadingListClick(readingList.readingListId, readingList.position)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    if (
        showAddToReadingListSheet &&
        addToReadingListViewModel != null
    ) {
        AddIssueToReadingListSheet(
            viewModel = addToReadingListViewModel,
            onDismissRequest = {
                showAddToReadingListSheet = false
            }
        )
    }
}

@Composable
private fun ComicIssueCover(
    coverUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(5.dp)

    Box(
        modifier = modifier
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .offset(
                        x = 3.dp,
                        y = 4.dp
                    )
                    .background(
                        color = ComicInk,
                        shape = shape
                    )
        )

        ComicCoverImage(
            coverUrl = coverUrl,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            shape = shape,
            borderColor = ComicInk
        )
    }
}

@Composable
private fun IssueMetadataRow(
    label: String,
    value: String
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.bodySmall
                    .copy(
                        fontSize = 11.sp,
                        lineHeight = 13.sp
                    ),
            color = ComicInk.copy(alpha = 0.6f)
        )

        Text(
            text = value,
            style =
                MaterialTheme.typography.bodyMedium
                    .copy(
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
            color = ComicInk
        )
    }
}

private fun formatIssuePublicationDate(
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