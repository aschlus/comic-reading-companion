package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.component.ComicLibraryHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicLibraryReadingListCard
import com.aschlus.comicreadingcompanion.ui.component.ComicLibrarySearchField
import com.aschlus.comicreadingcompanion.ui.component.ComicLibrarySectionHeader
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
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
        containerColor = ComicPaper,
        contentWindowInsets =
            WindowInsets(
                left = 0,
                top = 0,
                right = 0,
                bottom = 0
            ),
        topBar = {
            ComicLibraryHeader(
                onCreateReadingListClick = onCreateReadingListClick,
                onImportReadingListClick = onImportReadingListClick,
                isImporting = importState is ImportReadingListState.Importing
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
        ) {
            if (readingLists.isNotEmpty()) {
                item {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 12.dp,
                                    end = 12.dp,
                                    top = 6.dp
                                ),
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        ComicLibrarySearchField(
                            value = searchQuery,
                            onValueChange = {
                                viewModel
                                    .updateSearchQuery(it)
                            },
                            onClearClick = {
                                viewModel
                                    .clearSearchQuery()
                            },
                            modifier =
                                Modifier.weight(1f)
                        )

                        Box {
                            Row(
                                modifier =
                                    Modifier
                                        .width(142.dp)
                                        .height(42.dp)
                                        .background(
                                            color = ComicPaper,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = 2.dp,
                                            color = ComicInk,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            focusManager.clearFocus()
                                            sortMenuExpanded = true
                                        }
                                        .padding(
                                            start = 12.dp,
                                            end = 7.dp
                                        ),
                                verticalAlignment =
                                    Alignment.CenterVertically,
                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {
                                Text(
                                    when (sort) {
                                        HomeReadingListSort.RECENTLY_UPDATED ->
                                            "Recently updated"

                                        HomeReadingListSort.TITLE_ASCENDING ->
                                            "Title A-Z"

                                        HomeReadingListSort.TITLE_DESCENDING ->
                                            "Title Z-A"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ComicInk,
                                    maxLines = 1
                                )

                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = ComicInk,
                                    modifier = Modifier.size(20.dp)
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

            item {
                ComicLibrarySectionHeader(
                    modifier =
                        Modifier.padding(
                            start = 12.dp,
                            end = 12.dp,
                            top = 6.dp,
                            bottom = 4.dp
                        )
                )
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

                    ComicLibraryReadingListCard(
                        title = readingList.title,
                        description = readingList.description,
                        readCount = readCount,
                        totalCount = totalCount,
                        progress = progress,
                        continueText =
                            continueItem?.let {
                                "${it.seriesTitle} #${it.issueNumber}"
                            },
                        onClick = {
                            onReadingListClick(
                                readingList.id,
                                continueItem
                                    ?.position
                                    ?: -1
                            )
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 12.dp,
                                    end = 12.dp,
                                    top = 4.dp,
                                    bottom = 4.dp
                                ),
                        style = readingList.style
                    )
                }
            }
        }
    }
}