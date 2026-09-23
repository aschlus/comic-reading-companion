package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.component.ComicAddToListFilters
import com.aschlus.comicreadingcompanion.ui.component.ComicAddToListSearchField
import com.aschlus.comicreadingcompanion.ui.component.ComicBrowseHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicBrowseIssueResult
import com.aschlus.comicreadingcompanion.ui.component.ComicBrowsePublisherRow
import com.aschlus.comicreadingcompanion.ui.component.ComicBrowseSeriesResult
import com.aschlus.comicreadingcompanion.ui.component.ComicEmptyState
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
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

    var selectedFilter by rememberSaveable {
        mutableStateOf(AddToListFilter.ALL)
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
                .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
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

            if (searchQuery.isNotBlank()) {
                ComicAddToListFilters(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it }
                )
            }

            if (searchQuery.isBlank()) {

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
                            Arrangement.spacedBy(8.dp)
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
}