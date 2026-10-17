package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.models.OwnedIssueResult
import com.aschlus.comicreadingcompanion.ui.component.ComicAddToListSearchField
import com.aschlus.comicreadingcompanion.ui.component.ComicCoverImage
import com.aschlus.comicreadingcompanion.ui.component.ComicDropdownMenu
import com.aschlus.comicreadingcompanion.ui.component.ComicDropdownMenuItem
import com.aschlus.comicreadingcompanion.ui.component.ComicEmptyState
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeaderTitle
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlueLight
import com.aschlus.comicreadingcompanion.ui.theme.ComicGreen
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicOrange
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import com.aschlus.comicreadingcompanion.ui.viewmodel.CollectionSort
import com.aschlus.comicreadingcompanion.ui.viewmodel.CollectionStats
import com.aschlus.comicreadingcompanion.ui.viewmodel.CollectionViewModel


@Composable
fun CollectionScreen(
    viewModel: CollectionViewModel,
    onIssueClick: (Long) -> Unit
) {
    val ownedIssues by viewModel.ownedIssues.collectAsState()
    val visibleIssues by viewModel.visibleIssues.collectAsState()
    val stats by viewModel.stats.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val sort by viewModel.sort.collectAsState()

    var sortMenuExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        containerColor = ComicPaper,
        topBar = {
            ComicPrimaryHeader {
                ComicPrimaryHeaderTitle(
                    text = "COLLECTION",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 25.dp)
                )
            }
        }
    ) { innerPadding: PaddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "collection-statistics") {
                CollectionStatisticsPanel(stats)
            }

            item(key = "collection-search") {
                ComicAddToListSearchField(
                    query = searchQuery,
                    onQueryChange = { query ->
                        viewModel.updateSearchQuery(query)
                    },
                    placeholder = "Search your collection"
                )
            }

            item(key = "collection-sort") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box {
                        Row(
                            modifier = Modifier
                                .width(190.dp)
                                .height(42.dp)
                                .background(
                                    color = ComicPaper,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = 2.dp,
                                    color = ComicInk,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    sortMenuExpanded = true
                                }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = when (sort) {
                                    CollectionSort.TITLE_ASCENDING ->
                                        "Title A-Z"

                                    CollectionSort.PUBLICATION_NEWEST ->
                                        "Publication: Newest"

                                    CollectionSort.PUBLICATION_OLDEST ->
                                        "Publication: Oldest"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = ComicInk,
                                maxLines = 1
                            )

                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change sort order",
                                tint = ComicInk,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        ComicDropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = {
                                sortMenuExpanded = false
                            }
                        ) {
                            ComicDropdownMenuItem(
                                text = "Title A-Z",
                                onClick = {
                                    viewModel.updateSort(
                                        CollectionSort.TITLE_ASCENDING
                                    )
                                    sortMenuExpanded = false
                                }
                            )

                            ComicDropdownMenuItem(
                                text = "Publication: Newest",
                                onClick = {
                                    viewModel.updateSort(
                                        CollectionSort.PUBLICATION_NEWEST
                                    )
                                    sortMenuExpanded = false
                                }
                            )

                            ComicDropdownMenuItem(
                                text = "Publication: Oldest",
                                onClick = {
                                    viewModel.updateSort(
                                        CollectionSort.PUBLICATION_OLDEST
                                    )
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item(key = "collection-list-header") {
                ComicHomeSectionHeader(
                    text = "OWNED ISSUES"
                )

                Text(
                    text = if (searchQuery.isBlank()) {
                        "${visibleIssues.size} issues"
                    } else {
                        "${visibleIssues.size} matching issues"
                    },
                    modifier = Modifier.padding(
                        top = 2.dp,
                        bottom = 2.dp
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ComicInk.copy(alpha = 0.7f)
                )
            }

            if (ownedIssues.isEmpty()) {
                item(key = "collection-empty") {
                    ComicEmptyState(
                        message = "Your collection is empty. " +
                            "Open an issue and mark it as owned " +
                            "to add it here."
                    )
                }
            } else if (visibleIssues.isEmpty()) {
                item(key = "collection-no-matcher") {
                    ComicEmptyState(
                        message = "No owned issues match " +
                            "\"${searchQuery.trim()}\"."
                    )
                }
            } else {
                items(
                    items = visibleIssues,
                    key = { issue -> issue.issueId }
                ) { issue ->
                    OwnedIssueCard(
                        issue = issue,
                        onClick = {
                            onIssueClick(issue.issueId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionStatisticsPanel(
    stats: CollectionStats
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CollectionStatCard(
                label = "TOTAL ISSUES",
                value = stats.totalIssues.toString(),
                cardColor = ComicYellow,
                modifier = Modifier.weight(1f)
            )

            CollectionStatCard(
                label = "SERIES",
                value = stats.seriesCount.toString(),
                cardColor = ComicBlueLight,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            CollectionStatCard(
                label = "PUBLISHERS",
                value = stats.publisherCount.toString(),
                cardColor = ComicOrange,
                modifier = Modifier.weight(1f)
            )

            CollectionReadingStatusCard(
                stats = stats,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CollectionStatCard(
    label: String,
    value: String,
    cardColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 4.dp)
                .background(ComicInk, shape)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardColor, shape)
                .border(2.dp, ComicInk, shape)
                .padding(
                    horizontal = 12.dp,
                    vertical = 11.dp
                ),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = ComicInk
            )

            Text(
                text = value,
                fontSize = 31.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Black,
                color = ComicInk
            )
        }
    }
}

@Composable
private fun CollectionReadingStatusCard(
    stats: CollectionStats,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
    ) {
         Box(
             modifier = Modifier
                 .matchParentSize()
                 .offset(x = 3.dp, y = 4.dp)
                 .background(ComicInk, shape)
         )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ComicPaper, shape)
                .border(2.dp, ComicInk, shape)
                .padding(
                    horizontal = 10.dp,
                    vertical = 9.dp
                ),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "READING STATUS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = ComicInk
            )

            CollectionStatusCount(
                label = "Read",
                count = stats.readCount,
                color = ComicGreen
            )

            CollectionStatusCount(
                label = "Reading",
                count = stats.readingCount,
                color = ComicYellow
            )

            CollectionStatusCount(
                label = "Unread",
                count = stats.unreadCount,
                color = ComicBlueLight
            )
        }
    }
}

@Composable
private fun CollectionStatusCount(
    label: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(
                        color = color,
                        shape = RoundedCornerShape(3.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = ComicInk,
                        shape = RoundedCornerShape(3.dp)
                    )
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = ComicInk
            )
        }

        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = ComicInk
        )
    }
}

@Composable
private fun OwnedIssueCard(
    issue: OwnedIssueResult,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)

    val readingStatus =
        issue.readingStatus ?: ReadingStatus.UNREAD

    val statusColor = when (readingStatus) {
        ReadingStatus.READ -> ComicGreen
        ReadingStatus.READING -> ComicYellow
        ReadingStatus.UNREAD -> ComicBlueLight
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 4.dp)
                .background(ComicInk, shape)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ComicPaper, shape)
                .border(2.dp, ComicInk, shape)
                .clickable(onClick = onClick)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            ComicCoverImage(
                coverUrl = issue.coverUrl,
                contentDescription =
                    "${issue.seriesTitle} #${issue.issueNumber} cover",
                modifier = Modifier
                    .width(76.dp)
                    .aspectRatio(2f / 3f),
                shape = RoundedCornerShape(5.dp),
                borderColor = ComicInk
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    issue.seriesTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = ComicInk
                )

                Text(
                    text = if (issue.issueTitle.isNullOrBlank()) {
                        "#${issue.issueNumber}"
                    } else {
                        "#${issue.issueNumber} · ${issue.issueTitle}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ComicInk
                )

                Text(
                    text = buildString {
                        append(issue.publisherName)

                        issue.publicationDate
                            ?.takeIf { it.isNotBlank() }
                            ?.let {
                                append(" · ")
                                append(it)
                            }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ComicInk.copy(alpha = 0.7f)
                )

                Text(
                    text = when (readingStatus) {
                        ReadingStatus.READ -> "READ"
                        ReadingStatus.READING -> "READING"
                        ReadingStatus.UNREAD -> "UNREAD"
                    },
                    modifier = Modifier
                        .background(
                            color = statusColor,
                            shape = RoundedCornerShape(5.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = ComicInk,
                            shape = RoundedCornerShape(5.dp)
                        )
                        .padding(
                            horizontal = 7.dp,
                            vertical = 3.dp
                        ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = ComicInk
                )
            }
        }
    }
}