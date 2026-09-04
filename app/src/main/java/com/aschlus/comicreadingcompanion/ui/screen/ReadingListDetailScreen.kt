package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.ui.viewmodel.ReadingListDetailViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListDetailScreen(
    readingListId: Long,
    viewModel: ReadingListDetailViewModel,
    onBackClick: () -> Unit
) {
    val readingList by
        viewModel.readingList.collectAsState()

    val issues by
        viewModel.issues.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(readingListId) {
        viewModel.loadReadingList(readingListId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        readingList?.title ?: "Reading List"
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Text("←")
                    }
                }
            )
        }
    ) { innerPadding: PaddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            val currentReadingList = readingList

            if (currentReadingList == null) {
                Text("Loading...")
            } else {

                currentReadingList.description
                    ?.let { description ->
                        Text(description)
                    }

                val readCount = issues.count {
                    it.readingStatus == ReadingStatus.READ
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
                    text = "$readCount of $totalCount read • " +
                        "$completionPercentage% complete"
                )

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Issues")

                val firstUnreadIndex = issues.indexOfFirst { issue ->
                    issue.readingStatus != ReadingStatus.READ
                }

                if (firstUnreadIndex >= 0) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(
                                    firstUnreadIndex
                                )
                            }
                        }
                    ) {
                        Text("Jump to first unread")
                    }
                }

                if (issues.isEmpty()) {
                    Text("No issues in this reading list")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        state = listState
                    ) {
                        items(
                            items = issues,
                            key = { issue ->
                                issue.readingListItemId
                            }
                        ) { issue ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked =
                                        issue.readingStatus == ReadingStatus.READ,
                                    onCheckedChange = {
                                        viewModel.toggleIssueRead(
                                            issue = issue
                                        )
                                    }
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(
                                            vertical = 8.dp
                                        )
                                ) {
                                    Text(
                                        text = "${issue.seriesTitle} #${issue.issueNumber}",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    issue.issueTitle?.let { title ->
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    Text(
                                        text = "Reading Order #${issue.position}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}