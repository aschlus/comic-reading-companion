package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.component.ComicAddToListFilters
import com.aschlus.comicreadingcompanion.ui.component.ComicAddToListSearchField
import com.aschlus.comicreadingcompanion.ui.component.ComicBrowseFilterSheetContent
import com.aschlus.comicreadingcompanion.ui.component.ComicBrowseHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicBrowseIssueResult
import com.aschlus.comicreadingcompanion.ui.component.ComicBrowsePublisherRow
import com.aschlus.comicreadingcompanion.ui.component.ComicBrowseSeriesResult
import com.aschlus.comicreadingcompanion.ui.component.ComicEmptyState
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardBorderWidth
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingLg
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingMd
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingSm
import com.aschlus.comicreadingcompanion.ui.viewmodel.AddToListFilter
import com.aschlus.comicreadingcompanion.ui.viewmodel.BrowseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    viewModel: BrowseViewModel,
    onPublisherClick: (Long) -> Unit,
    onSeriesClick: (Long) -> Unit,
    onIssueClick: (Long) -> Unit
) {
    val publishers by
        viewModel.publishers.collectAsState()

    val searchQuery by
        viewModel.searchQuery.collectAsState()

    val seriesResults by
        viewModel.seriesResults.collectAsState()

    val issueResults by
        viewModel.issueResults.collectAsState()

    val selectedPublisherId by
        viewModel.selectedPublisherId.collectAsState()

    val availableUniverses by
        viewModel.availableUniverses.collectAsState()

    val availablePublicationYears by
        viewModel.availablePublicationYears.collectAsState()

    val selectedUniverseId by
        viewModel.selectedUniverseId.collectAsState()

    val selectedPublicationYear by
        viewModel.selectedPublicationYear.collectAsState()

    var selectedFilter by rememberSaveable {
        mutableStateOf(AddToListFilter.ALL)
    }

    var showFilterSheet by rememberSaveable {
        mutableStateOf(false)
    }

    val filterSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

    val activeFilterCount = listOf(
        selectedPublisherId != null,
        selectedUniverseId != null,
        selectedPublicationYear != null
    ).count { it }

    val resultCount = when (selectedFilter) {
        AddToListFilter.ALL ->
            seriesResults.size + issueResults.size

        AddToListFilter.SERIES ->
            seriesResults.size

        AddToListFilter.ISSUES ->
            issueResults.size
    }

    Scaffold(
        containerColor = ComicPaper,
        topBar = {
            ComicBrowseHeader()
        }
    ) { innerPadding: PaddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(ComicSpacingLg),
            verticalArrangement =
                Arrangement.spacedBy(ComicSpacingMd)
        ) {
            ComicAddToListSearchField(
                query = searchQuery,
                onQueryChange = { query ->
                    viewModel.updateSearchQuery(query)

                    if (query.isBlank()) {
                        selectedFilter = AddToListFilter.ALL
                    }
                },
                placeholder = "Search comics, series, or issues"
            )

            ComicBrowseFiltersButton(
                activeFilterCount = activeFilterCount,
                onClick = {
                    showFilterSheet = true
                }
            )

            if (
                searchQuery.isNotBlank() ||
                activeFilterCount > 0
            ) {
                ComicAddToListFilters(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it }
                )
            }

            if (
                searchQuery.isBlank() &&
                activeFilterCount == 0
            ) {

                ComicHomeSectionHeader(
                    text = "PUBLISHERS"
                )

                if (publishers.isEmpty()) {
                    ComicEmptyState(
                        message = "No publishers found"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(9.dp)
                    ) {
                        items(
                            items = publishers,
                            key = { publisher ->
                                publisher.publisherId
                            }
                        ) { publisher ->
                            ComicBrowsePublisherRow(
                                publisher = publisher,
                                onClick = {
                                    onPublisherClick(publisher.publisherId)
                                }
                            )
                        }
                    }
                }
            } else {

                val showSeries =
                    selectedFilter !=
                        AddToListFilter.ISSUES &&
                        seriesResults.isNotEmpty()

                val showIssues =
                    selectedFilter !=
                        AddToListFilter.SERIES &&
                        issueResults.isNotEmpty()

                val hasResults =
                    showSeries || showIssues

                if (!hasResults) {
                    ComicEmptyState(
                        message = "No results found"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(ComicSpacingSm)
                    ) {

                        if (showSeries) {
                            item {
                                ComicHomeSectionHeader(
                                    text = "SERIES"
                                )
                            }

                            items(
                                items = seriesResults,
                                key = { result ->
                                    "series-${result.seriesId}"
                                }
                            ) {result ->
                                ComicBrowseSeriesResult(
                                    result = result,
                                    onClick = {
                                        onSeriesClick(result.seriesId)
                                    }
                                )
                            }
                        }

                        if (showIssues) {
                            item {
                                ComicHomeSectionHeader(
                                    text = "ISSUES"
                                )
                            }

                            items(
                                items = issueResults,
                                key = { result ->
                                    "issue-${result.issueId}"
                                }
                            ) { result ->
                                ComicBrowseIssueResult(
                                    result = result,
                                    onClick = {
                                        onIssueClick(result.issueId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showFilterSheet = false
            },
            sheetState = filterSheetState,
            containerColor = ComicPaper,
            dragHandle = null
        ) {
            ComicBrowseFilterSheetContent(
                publishers = publishers,
                selectedPublisherId = selectedPublisherId,
                onPublisherSelected = { publisherId ->
                    viewModel.updatePublisherFilter(
                        publisherId
                    )
                },
                universes = availableUniverses,
                selectedUniverseId = selectedUniverseId,
                onUniverseSelected = { universeId ->
                    viewModel.updateUniverseFilter(
                        universeId
                    )
                },
                publicationYears = availablePublicationYears,
                selectedPublicationYear = selectedPublicationYear,
                onPublicationYearSelected = { year ->
                    viewModel.updatePublicationYearFilter(
                        year
                    )
                },
                activeFilterCount = activeFilterCount,
                resultCount = resultCount,
                onClearAll = {
                    viewModel.updatePublisherFilter(null)
                    viewModel.updateUniverseFilter(null)
                    viewModel.updatePublicationYearFilter(null)
                },
                onDone = {
                    showFilterSheet = false
                },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun ComicBrowseFiltersButton(
    activeFilterCount: Int,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(9.dp)

    Box(
        modifier = Modifier.height(44.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(
                    x = 2.dp,
                    y = 3.dp
                )
                .background(
                    color = ComicInk,
                    shape = shape
                )
        )

        Box(
            modifier = Modifier
                .height(41.dp)
                .clip(shape)
                .background(ComicPaper)
                .border(
                    width = ComicCardBorderWidth,
                    color = ComicInk,
                    shape = shape
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = ComicInk,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(ComicSpacingSm))

                Text(
                    text = if (activeFilterCount > 0) {
                        "Filters ($activeFilterCount)"
                    } else {
                        "Filters"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = ComicInk
                )
            }
        }
    }
}