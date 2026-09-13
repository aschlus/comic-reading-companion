package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.component.ComicLibraryActionCard
import com.aschlus.comicreadingcompanion.ui.component.ComicLibraryActionIcon
import com.aschlus.comicreadingcompanion.ui.component.ComicLibraryHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicLibraryReadingListCard
import com.aschlus.comicreadingcompanion.ui.component.ComicSectionBanner
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicMutedInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
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
            item{
                ComicLibraryHeader()
            }

            item {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 10.dp,
                                end = 10.dp,
                                top = 10.dp,
                                bottom = 4.dp
                            ),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {
                    ComicLibraryActionCard(
                        title =
                            "Create Reading List",
                        icon =
                            ComicLibraryActionIcon.CREATE,
                        backgroundColor =
                            ComicYellow,
                        onClick =
                            onCreateReadingListClick,
                        modifier =
                            Modifier.weight(1f)
                    )

                    ComicLibraryActionCard(
                        title =
                            if (
                                importState is
                                        ImportReadingListState.Importing
                            ) {
                                "Importing Reading List…"
                            } else {
                                "Import Reading List"
                            },
                        icon =
                            ComicLibraryActionIcon.IMPORT,
                        backgroundColor =
                            ComicBlue,
                        onClick =
                            onImportReadingListClick,
                        modifier =
                            Modifier.weight(1f),
                        enabled =
                            importState !is
                                    ImportReadingListState.Importing
                    )
                }
            }

            item {
                ComicSectionBanner(
                    text = "Your Lists",
                    modifier =
                        Modifier.padding(
                            start = 18.dp,
                            top = 2.dp
                        ),
                    backgroundColor = ComicRed
                )
            }

            if (readingLists.isNotEmpty()) {
                item {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 14.dp,
                                    end = 14.dp,
                                    top = 2.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                viewModel
                                    .updateSearchQuery(it)
                            },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(56.dp),
                            label = {
                                Text(
                                    text = "Search reading lists",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyMedium
                                            .copy(
                                                fontSize = 15.sp
                                            )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector =
                                        Icons.Default.Search,
                                    contentDescription = null,
                                    modifier =
                                        Modifier.size(25.dp),
                                    tint = ComicInk
                                )
                            },
                            singleLine = true,
                            shape =
                                RoundedCornerShape(
                                    28.dp
                                ),
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor =
                                        ComicPaper,
                                    unfocusedContainerColor =
                                        ComicPaper,
                                    focusedBorderColor =
                                        ComicInk,
                                    unfocusedBorderColor =
                                        ComicInk,
                                    focusedTextColor =
                                        ComicInk,
                                    unfocusedTextColor =
                                        ComicInk,
                                    focusedLabelColor =
                                        ComicMutedInk,
                                    unfocusedLabelColor =
                                        ComicMutedInk,
                                    cursorColor =
                                        ComicInk
                                ),
                            trailingIcon =
                                if (searchQuery.isNotEmpty()) {
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
                                                    "Clear reading list search",
                                                tint =
                                                    ComicInk
                                            )
                                        }
                                    }
                                } else {
                                    null
                                }
                        )

                        Column {
                            Box(
                                modifier =
                                    Modifier
                                        .height(56.dp)
                                        .background(
                                            color = ComicPaper,
                                            shape =
                                                RoundedCornerShape(
                                                    8.dp
                                                )
                                        )
                                        .border(
                                            width = 2.dp,
                                            color = ComicInk,
                                            shape =
                                                RoundedCornerShape(
                                                    8.dp
                                                )
                                        )
                                        .clickable {
                                            focusManager
                                                .clearFocus()

                                            sortMenuExpanded =
                                                true
                                        }
                                        .padding(
                                            start = 14.dp,
                                            end = 10.dp
                                        ),
                                contentAlignment =
                                    Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically,
                                    horizontalArrangement =
                                        Arrangement.spacedBy(
                                            5.dp
                                        )
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
                                            },
                                        style =
                                            MaterialTheme
                                                .typography
                                                .labelLarge
                                                .copy(
                                                    fontSize = 14.sp
                                                ),
                                        fontWeight =
                                            FontWeight.Bold,
                                        color =
                                            ComicInk,
                                        maxLines = 1
                                    )

                                    Icon(
                                        imageVector =
                                            Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier =
                                            Modifier.size(
                                                20.dp
                                            ),
                                        tint =
                                            ComicInk
                                    )
                                }
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
                        continueText = continueItem?.let {
                            "${it.seriesTitle} " +
                            "#${it.issueNumber}"
                        },
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