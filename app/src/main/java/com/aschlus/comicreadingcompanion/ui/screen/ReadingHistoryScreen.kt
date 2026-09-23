package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.component.ComicEmptyState
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicIssueDetailHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicReadingHistoryRow
import com.aschlus.comicreadingcompanion.ui.component.recentReadSectionLabel
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModel

@Composable
fun ReadingHistoryScreen(
    viewModel: HomeViewModel,
    onIssueClick: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val recentlyReadIssues by viewModel.recentlyReadIssues.collectAsState()

    val nowMillis = System.currentTimeMillis()

    val groupedHistory =
        recentlyReadIssues.groupBy { issue ->
            recentReadSectionLabel(
                completedAt = issue.completedAt,
                nowMillis = nowMillis
            )
        }

    Scaffold(
        containerColor = ComicPaper,
        topBar = {
            ComicIssueDetailHeader(
                title = "READING HISTORY",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = 24.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            if (recentlyReadIssues.isEmpty()) {
                item {
                    ComicEmptyState(
                        message = "No reading history yet. " +
                                "Finish an issue and " +
                                "we’ll keep track of it here."
                    )
                }
            } else {
                groupedHistory
                    .forEach { (section, issues) ->
                        item(
                            key = "history-section-$section"
                        ) {
                            ComicHomeSectionHeader(
                                text = section,
                                modifier =
                                    Modifier.padding(
                                        top = 2.dp
                                    )
                            )
                        }

                        items(
                            items = issues,
                            key = { issue -> issue.issueId}
                        ) { issue ->
                            ComicReadingHistoryRow(
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
}