package com.aschlus.comicreadingcompanion.ui.screen

import android.graphics.drawable.Icon
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.aschlus.comicreadingcompanion.ui.component.ComicActionPanel
import com.aschlus.comicreadingcompanion.ui.component.ComicProgressBar
import com.aschlus.comicreadingcompanion.ui.component.ComicSectionBanner
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeReadingListSort
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.ImportReadingListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: HomeViewModel,
    onCreateReadingListClick: () -> Unit,
    onImportReadingListClick: () -> Unit,
    onReadingListClick: (Long, Int) -> Unit,
    importState: ImportReadingListState = ImportReadingListState.Idle,
    onImportResultConsumed: () -> Unit = {}
) {
    val readingLists by
        viewModel.readingLists.collectAsState()

    val visibleReadingLists by
        viewModel.visibleReadingLists.collectAsState()

    val searchQuery by
        viewModel.searchQuery.collectAsState()

    val sort by
        viewModel.sort.collectAsState()

    val summaries by
        viewModel.readingListSummaries.collectAsState()

    val continueItem by
        viewModel.continueItems.collectAsState()

    val focusManager = LocalFocusManager.current

    var sortMenuExpanded by remember {
        mutableStateOf(false)
    }

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    LaunchedEffect(importState) {
        when (val state = importState) {
            is ImportReadingListState.Success -> {
                snackbarHostState
                    .showSnackbar(
                        "Imported \"${state.title}\""
                    )

                onImportResultConsumed()
            }

            is ImportReadingListState.Error -> {
                snackbarHostState
                    .showSnackbar(
                        state.message
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
                    Text("Library")
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState =
                    snackbarHostState
            )
        }
    ) { innerPadding: PaddingValues ->

        LazyColumn(
            modifier =
                Modifier
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
                        title = "Create Reading List",
                        icon = Icons.Default.Add,
                        onClick = onCreateReadingListClick,
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.secondary
                    )

                    ComicActionPanel(
                        title =
                            if (importState is ImportReadingListState.Importing) {
                                "Importing Reading List…"
                            } else {
                                "Import Reading List"
                            },
                        icon = Icons.Default.Upload,
                        onClick = onImportReadingListClick,
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.primary,
                        enabled = importState !is ImportReadingListState.Importing
                    )
                }
            }

            item {
                ComicSectionBanner(
                    text = "Reading Lists"
                )
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
                                    .updateSearchQuery(it)
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
                                if (searchQuery.isNotEmpty()) {
                                    {
                                        IconButton(
                                            onClick = {
                                                viewModel.clearSearchQuery()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear reading list search"
                                            )
                                        }
                                    }
                                } else {
                                    null
                                }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            TextButton(
                                onClick = {
                                    focusManager.clearFocus()

                                    sortMenuExpanded = true
                                }
                            ) {
                                Text(
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
                                expanded = sortMenuExpanded,
                                onDismissRequest = {
                                    sortMenuExpanded = false
                                }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text("Recently updated")
                                    },
                                    onClick = {
                                        viewModel
                                            .updateSort(
                                                HomeReadingListSort.RECENTLY_UPDATED
                                            )

                                        sortMenuExpanded = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text("Title A-Z")
                                    },
                                    onClick = {
                                        viewModel
                                            .updateSort(
                                                HomeReadingListSort.TITLE_ASCENDING
                                            )

                                        sortMenuExpanded = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text("Title Z-A")
                                    },
                                    onClick = {
                                        viewModel
                                            .updateSort(
                                                HomeReadingListSort.TITLE_DESCENDING
                                            )

                                        sortMenuExpanded = false
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
            } else if (
                visibleReadingLists.isEmpty()
            ) {
                item {
                    Text("No reading lists match \"${searchQuery.trim()}\"")
                }
            } else {
                items(
                    items = visibleReadingLists,
                    key = {
                        "library-${it.id}"
                    }
                ) { readingList ->

                    val summary =
                        summaries
                            .firstOrNull {
                                it.readingListId == readingList.id
                            }

                    val continueItem =
                        continueItem[readingList.id]

                    val readCount = summary?.readCount ?: 0
                    val totalCount = summary?.totalCount ?: 0

                    val progress =
                        if (totalCount == 0) {
                            0f
                        } else {
                            readCount.toFloat() / totalCount.toFloat()
                        }

                    Card(
                        onClick = {
                            onReadingListClick(
                                readingList.id,
                                continueItem
                                    ?.position
                                    ?: -1
                            )
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        border =
                            BorderStroke(
                                2.dp,
                                MaterialTheme.colorScheme.outline
                            ),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                    ) {
                        Column(
                            modifier =
                                Modifier.padding(16.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = readingList.title,
                                style = MaterialTheme.typography.titleMedium
                            )

                            readingList
                                .description
                                ?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                            Text(
                                text = "$readCount of $totalCount read",
                                style = MaterialTheme.typography.labelMedium
                            )

                            ComicProgressBar(
                                progress = progress,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (continueItem != null) {
                                Text(
                                    text =
                                        "Continue: " +
                                        "${continueItem.seriesTitle} " +
                                        "#${continueItem.issueNumber}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}