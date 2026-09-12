package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.component.ComicActionPanel
import com.aschlus.comicreadingcompanion.ui.component.ComicContinueCard
import com.aschlus.comicreadingcompanion.ui.component.ComicRecentCard
import com.aschlus.comicreadingcompanion.ui.component.ComicSectionBanner
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onBrowseClick: () -> Unit,
    onCreateReadingListClick: () -> Unit,
    onReadingListClick: (Long, Int) -> Unit
) {
    val recentlyOpenedReadingLists by viewModel.recentlyOpenedReadingLists.collectAsState()

    val continueReadingLists by viewModel.continueReadingLists.collectAsState()

    val readingListSummaries by
        viewModel.readingListSummaries.collectAsState()

    val continueItems by
        viewModel.continueItems.collectAsState()

    val recentlyOpenedListState =
        rememberLazyListState()

    LaunchedEffect(
        recentlyOpenedReadingLists.map { it.id }
    ) {
        if (recentlyOpenedReadingLists.isNotEmpty()) {
            recentlyOpenedListState.scrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Comic Reading Companion")
                }
            )
        }
    ) { innerPadding: PaddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    ComicActionPanel(
                        title = "Browse Comics",
                        icon = Icons.Default.Search,
                        onClick = onBrowseClick,
                        modifier = Modifier.weight(1f)
                    )

                    ComicActionPanel(
                        title = "Create Reading List",
                        icon = Icons.Default.Add,
                        onClick = onCreateReadingListClick,
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (continueReadingLists.isNotEmpty()) {
                item {
                    ComicSectionBanner(
                        text = "Continue Reading",
                        backgroundColor = MaterialTheme.colorScheme.tertiary
                    )
                }

                items(
                    items =
                        continueReadingLists.take(3),
                    key = {
                        "continue-${it.id}"
                    }
                ) { readingList ->
                    val summary =
                        readingListSummaries
                            .firstOrNull {
                                it.readingListId ==
                                        readingList.id
                            }

                    val continueItem =
                        continueItems[
                            readingList.id
                        ]

                    ComicContinueCard(
                        readingList = readingList,
                        readCount =
                            summary?.readCount ?: 0,
                        totalCount =
                            summary?.totalCount ?: 0,
                        continueItem =
                            continueItem,
                        onClick = {
                            onReadingListClick(
                                readingList.id,
                                continueItem
                                    ?.position
                                    ?: -1
                            )
                        }
                    )
                }
            }

            if (recentlyOpenedReadingLists.isNotEmpty()) {
                item {
                    ComicSectionBanner(
                        text = "Recently Opened"
                    )
                }

                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        state = recentlyOpenedListState,
                        contentPadding =
                            PaddingValues(
                                start = 4.dp,
                                end = 8.dp
                            ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items =
                                recentlyOpenedReadingLists
                                    .take(3),
                            key = {
                                "recent-${it.id}"
                            }
                        ) { readingList ->

                            val summary =
                                readingListSummaries
                                    .firstOrNull() {
                                        it.readingListId == readingList.id
                                    }

                            val continueItem =
                                continueItems[
                                    readingList.id
                                ]

                            ComicRecentCard(
                                readingList = readingList,
                                readCount = summary?.readCount ?: 0,
                                totalCount = summary?.totalCount ?: 0,
                                continueItem = continueItem,
                                onClick = {
                                    onReadingListClick(
                                        readingList.id,
                                        continueItem
                                            ?.position
                                            ?: -1
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