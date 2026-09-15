package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeQuickAccessTile
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeRecentReadCard
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicLibraryReadingListCard
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlueLight
import com.aschlus.comicreadingcompanion.ui.theme.ComicGreen
import com.aschlus.comicreadingcompanion.ui.theme.ComicMutedInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onBrowseClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onCreateReadingListClick: () -> Unit,
    onReadingListClick: (Long, Int) -> Unit,
    onIssueClick: (Long) -> Unit
) {
    val continueReadingLists by viewModel.continueReadingLists.collectAsState()

    val continueReadingList = continueReadingLists.firstOrNull()

    val readingListSummaries by
        viewModel.readingListSummaries.collectAsState()

    val continueItems by
        viewModel.continueItems.collectAsState()

    val recentlyReadIssues by
        viewModel.recentlyReadIssues.collectAsState()


    Scaffold(
        contentWindowInsets =
            WindowInsets(
                left = 0,
                top = 0,
                right = 0,
                bottom = 0
            ),
        containerColor = ComicPaper,
        topBar = {
            ComicHomeHeader(
                onSettingsClick = {
                    // Settings screen will be added later
                }
            )
        }
    ) { innerPadding: PaddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                ComicHomeSectionHeader(
                    text = "CONTINUE READING",
                    onSeeAllClick = onLibraryClick,
                    modifier =
                        Modifier
                            .padding(
                                start = 12.dp,
                                end = 12.dp,
                                top = 6.dp,
                                bottom = 4.dp
                            )
                )
            }

            item {
                if (continueReadingList != null) {
                    val summary =
                        readingListSummaries
                            .firstOrNull {
                                it.readingListId ==
                                        continueReadingList.id
                            }

                    val continueItem =
                        continueItems[continueReadingList.id]

                    val readCount = summary?.readCount ?: 0
                    val totalCount = summary?.totalCount ?: 0

                    val progress =
                        if (totalCount > 0) {
                            readCount.toFloat() / totalCount.toFloat()
                        } else {
                            0f
                        }
                    ComicLibraryReadingListCard(
                        title = continueReadingList.title,
                        description = continueReadingList.description,
                        readCount = readCount,
                        totalCount = totalCount,
                        progress = progress,
                        continueText =
                            continueItem?.let {
                                "${it.seriesTitle} #${it.issueNumber}"
                            },
                        onClick = {
                            onReadingListClick(
                                continueReadingList.id,
                                continueItem?.position
                                    ?: -1
                            )
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 4.dp
                                )
                    )
                } else {
                    Text(
                        text =
                            "Ready for your next story? " +
                            "Start a reading list to " +
                            "continue it here.",
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 28.dp,
                                    vertical = 18.dp
                                ),
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontStyle = FontStyle.Italic,
                        color = ComicMutedInk,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                ComicHomeSectionHeader(
                    text = "QUICK ACCESS",
                    modifier =
                        Modifier
                            .padding(
                                start = 12.dp,
                                end = 12.dp,
                                top = 4.dp,
                                bottom = 4.dp
                            )
                )
            }

            item {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 12.dp,
                                end = 12.dp,
                                top = 4.dp,
                                bottom = 10.dp
                            ),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    ComicHomeQuickAccessTile(
                        text = "NEW\nLIST",
                        icon = Icons.Default.Add,
                        backgroundColor = ComicYellow,
                        onClick = onCreateReadingListClick,
                        modifier = Modifier.weight(1f),
                        iconSize = 36.dp,
                        textFontSize = 16.sp,
                        textLineHeight = 17.sp,
                        drawCustomPlus = true
                    )

                    ComicHomeQuickAccessTile(
                        text = "DISCOVER\nCOMICS",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        backgroundColor = ComicBlueLight,
                        onClick = onBrowseClick,
                        modifier = Modifier.weight(1f),
                        iconSize = 32.dp,
                        textFontSize = 16.sp,
                        textLineHeight = 17.sp
                    )

                    ComicHomeQuickAccessTile(
                        text = "READING\nHISTORY",
                        icon = Icons.Default.History,
                        backgroundColor = ComicGreen,
                        onClick = {
                            // Reading History screen will be added later
                        },
                        modifier = Modifier.weight(1f),
                        iconSize = 33.dp,
                        textFontSize = 16.sp,
                        textLineHeight = 17.sp
                    )
                }
            }

            item {
                ComicHomeSectionHeader(
                    text = "RECENTLY READ",
                    onSeeAllClick = {
                        // Reading History screen will be added later
                    },
                    modifier =
                        Modifier.padding(
                            start = 12.dp,
                            end = 12.dp,
                            top = 4.dp,
                            bottom = 4.dp
                        )
                )
            }

            item {
                if (recentlyReadIssues.isEmpty()) {
                    Text(
                        text =
                            "No recent reads yet. " +
                            "Finish an issue and " +
                            "we’ll keep track of it here.",
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 28.dp,
                                    vertical = 18.dp
                                ),
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontStyle = FontStyle.Italic,
                        color = ComicMutedInk,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 12.dp,
                                    end = 12.dp,
                                    top = 4.dp,
                                    bottom = 10.dp
                                ),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        recentlyReadIssues
                            .take(3)
                            .forEach { issue ->
                                ComicHomeRecentReadCard(
                                    seriesTitle = issue.seriesTitle,
                                    issueNumber = issue.issueNumber,
                                    coverUrl = issue.coverUrl,
                                    completedAt = issue.completedAt,
                                    onClick = {
                                        onIssueClick(issue.issueId)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                    }
                }
            }
        }
    }
}