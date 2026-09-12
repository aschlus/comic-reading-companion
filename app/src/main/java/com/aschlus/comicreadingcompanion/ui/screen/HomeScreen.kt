package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListContinueItem
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeReadingListSort
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.ImportReadingListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onBrowseClick: () -> Unit,
    onCreateReadingListClick: () -> Unit,
    onImportReadingListClick: () -> Unit,
    importState: ImportReadingListState = ImportReadingListState.Idle,
    onImportResultConsumed: () -> Unit = {},
    onReadingListClick: (Long, Int) -> Unit
) {
    val sort by viewModel.sort.collectAsState()

    var sortMenuExpanded by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    val readingLists by viewModel.readingLists.collectAsState()

    val visibleReadingLists by viewModel.visibleReadingLists.collectAsState()

    val recentlyOpenedReadingLists by viewModel.recentlyOpenedReadingLists.collectAsState()

    val continueReadingLists by viewModel.continueReadingLists.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()

    val readingListSummaries by
        viewModel.readingListSummaries.collectAsState()

    val continueItems by
        viewModel.continueItems.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(importState) {
        when (val state = importState) {
            is ImportReadingListState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Imported \"${state.title}\""
                )

                onImportResultConsumed()
            }

            is ImportReadingListState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message
                )

                onImportResultConsumed()
            }

            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Comic Reading Companion")
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
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
                OutlinedButton(
                    onClick = onBrowseClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Browse Comics")
                }
            }

            item {
                OutlinedButton(
                    onClick = onCreateReadingListClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Reading List")
                }
            }

            item {
                OutlinedButton(
                    onClick = onImportReadingListClick,
                    enabled =
                        importState !is
                                ImportReadingListState.Importing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (
                            importState is
                                    ImportReadingListState.Importing
                        ) {
                            "Importing Reading List…"
                        } else {
                            "Import Reading List"
                        }
                    )
                }
            }

            if (continueReadingLists.isNotEmpty()) {
                item {
                    Text(
                        text = "Continue Reading",
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
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

                    ReadingListCard(
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
                    Text(
                        text = "Recently Opened",
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )
                }

                items(
                    items =
                        recentlyOpenedReadingLists
                            .take(3),
                    key = {
                        "recent-${it.id}"
                    }
                ) { readingList ->
                    val continueItem =
                        continueItems[
                            readingList.id
                        ]

                    OutlinedButton(
                        onClick = {
                            onReadingListClick(
                                readingList.id,
                                continueItem
                                    ?.position
                                    ?: -1
                            )
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text =
                                readingList.title
                        )
                    }
                }
            }

            item {
                Text("My Reading Lists")
            }

            if (readingLists.isNotEmpty()) {
                item {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                viewModel
                                    .updateSearchQuery(
                                        it
                                    )
                            },
                            modifier =
                                Modifier.weight(1f),
                            label = {
                                Text(
                                    "Search reading lists"
                                )
                            },
                            singleLine = true,
                            trailingIcon =
                                if (
                                    searchQuery
                                        .isNotEmpty()
                                ) {
                                    {
                                        IconButton(
                                            onClick = {
                                                viewModel
                                                    .clearSearchQuery()
                                            }
                                        ) {
                                            Icon(
                                                imageVector =
                                                    Icons.Default.Close,
                                                contentDescription =
                                                    "Clear reading list search"
                                            )
                                        }
                                    }
                                } else {
                                    null
                                }
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Column {
                            TextButton(
                                onClick = {
                                    focusManager
                                        .clearFocus()

                                    sortMenuExpanded =
                                        true
                                }
                            ) {
                                Text(
                                    text =
                                        when (sort) {
                                            HomeReadingListSort.RECENTLY_UPDATED ->
                                                "Recently updated"

                                            HomeReadingListSort.TITLE_ASCENDING ->
                                                "Title A-Z"

                                            HomeReadingListSort.TITLE_DESCENDING ->
                                                "Title Z-A"
                                        }
                                )
                            }

                            DropdownMenu(
                                expanded =
                                    sortMenuExpanded,
                                onDismissRequest = {
                                    sortMenuExpanded =
                                        false
                                }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Recently updated"
                                        )
                                    },
                                    onClick = {
                                        viewModel
                                            .updateSort(
                                                HomeReadingListSort
                                                    .RECENTLY_UPDATED
                                            )

                                        sortMenuExpanded =
                                            false
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Title A-Z"
                                        )
                                    },
                                    onClick = {
                                        viewModel
                                            .updateSort(
                                                HomeReadingListSort
                                                    .TITLE_ASCENDING
                                            )

                                        sortMenuExpanded =
                                            false
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Title Z-A"
                                        )
                                    },
                                    onClick = {
                                        viewModel
                                            .updateSort(
                                                HomeReadingListSort
                                                    .TITLE_DESCENDING
                                            )

                                        sortMenuExpanded =
                                            false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (readingLists.isEmpty()) {
                item {
                    Text("No reading lists yet")
                }
            } else if (visibleReadingLists.isEmpty()) {
                item {
                    Text(
                        "No reading lists match " +
                                "\"${searchQuery.trim()}\""
                    )
                }
            } else {
                items(
                    items = visibleReadingLists,
                    key = {
                        "library-${it.id}"
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

                    ReadingListCard(
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
        }
    }
}

@Composable
private fun ReadingListCard(
    readingList: ReadingList,
    readCount: Int,
    totalCount: Int,
    continueItem: ReadingListContinueItem?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = readingList.title,
                style = MaterialTheme.typography.titleMedium
            )

            readingList.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            val progress =
                if (totalCount == 0) {
                    0f
                } else {
                    readCount.toFloat() / totalCount.toFloat()
                }

            val completionPercentage =
                if (totalCount == 0) {
                    0f
                } else {
                    (readCount * 100) / totalCount
                }

            Text(
                text = "$readCount of $totalCount read • " +
                    "$completionPercentage% complete",
                style = MaterialTheme.typography.bodySmall
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )

            if (continueItem != null) {
                Text(
                    text = "Continue: " +
                        "${continueItem.seriesTitle} " +
                        "#${continueItem.issueNumber}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}