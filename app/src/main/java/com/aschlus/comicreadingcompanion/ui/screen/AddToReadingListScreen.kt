package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.component.ComicAddToListActionBar
import com.aschlus.comicreadingcompanion.ui.component.ComicAddToListFilters
import com.aschlus.comicreadingcompanion.ui.component.ComicAddToListHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicAddToListIssueResult
import com.aschlus.comicreadingcompanion.ui.component.ComicAddToListSearchField
import com.aschlus.comicreadingcompanion.ui.component.ComicAddToListSeriesResult
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.viewmodel.AddToListFilter
import com.aschlus.comicreadingcompanion.ui.viewmodel.CreateReadingListViewModel

@Composable
fun AddToReadingListScreen(
    viewModel: CreateReadingListViewModel,
    onBackClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    val query by viewModel.addToListQuery.collectAsState()
    val filter by viewModel.addToListFilter.collectAsState()
    val seriesResults by viewModel.addToListSeriesResults.collectAsState()
    val issueResults by viewModel.addToListIssueResults.collectAsState()
    val draftIssues by viewModel.addToListDraftIssues.collectAsState()

    val selectedIssueIds = draftIssues.map { it.issueId }.toSet()

    Scaffold(
        containerColor = ComicPaper,
        topBar = {
            ComicAddToListHeader(
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            ComicAddToListActionBar(
                selectedCount = draftIssues.size,
                onBackClick = onBackClick,
                onDoneClick = onDoneClick
            )
        }
    ) { innerPadding: PaddingValues ->

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(
                        horizontal = 16.dp,
                        vertical = 18.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {
             ComicAddToListSearchField(
                 query = query,
                 onQueryChange =
                     viewModel::updateAddToListQuery,
                 modifier = Modifier.fillMaxWidth()
             )

            ComicAddToListFilters(
                selectedFilter = filter,
                onFilterSelected =
                    viewModel::selectAddToListFilter
            )

            if (query.isBlank()) {
                Text(
                    text = "Search for a series or issue to add to this reading list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ComicInk.copy(alpha = 0.7f)
                )
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    if (
                        filter !=
                        AddToListFilter.ISSUES &&
                        seriesResults.isNotEmpty()
                    ) {
                        item(
                            key = "series-heading"
                        ) {
                            AddToListResultHeading(
                                text = "SERIES"
                            )
                        }

                        items(
                            items =
                                seriesResults,
                            key = {
                                "series-${it.seriesId}"
                            }
                        ) { result ->
                            val selected =
                                result.totalCount > 0 &&
                                        draftIssues
                                            .count {
                                                it.seriesId == result.seriesId
                                            } ==
                                        result.totalCount

                            ComicAddToListSeriesResult(
                                result = result,
                                selected = selected,
                                onClick = {
                                    viewModel.toggleAddToListSeries(result)
                                }
                            )
                        }
                    }

                    if (
                        filter !=
                        AddToListFilter.SERIES &&
                        issueResults.isNotEmpty()
                    ) {
                        item(
                            key = "issue-heading"
                        ) {
                            AddToListResultHeading(
                                text = "ISSUES",
                                modifier =
                                    Modifier.padding(
                                        top =
                                            if (
                                                filter == AddToListFilter.ALL &&
                                                seriesResults.isNotEmpty()
                                            ) {
                                                6.dp
                                            } else {
                                                0.dp
                                            }
                                    )
                            )
                        }

                        items(
                            items = issueResults,
                            key = {
                                "issue-${it.issueId}"
                            }
                        ) { result ->
                            ComicAddToListIssueResult(
                                result = result,
                                selected = result.issueId in selectedIssueIds,
                                onClick = {
                                    viewModel.toggleAddToListIssue(result)
                                }
                            )
                        }
                    }

                    val hasVisibleResults =
                        when (filter) {
                            AddToListFilter.ALL ->
                                seriesResults.isNotEmpty() ||
                                        issueResults.isNotEmpty()

                            AddToListFilter.SERIES ->
                                seriesResults.isNotEmpty()

                            AddToListFilter.ISSUES ->
                                issueResults.isNotEmpty()
                        }

                    if (!hasVisibleResults) {
                        item(
                            key = "no-results"
                        ) {
                            Text(
                                text = "No results found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = ComicInk.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddToListResultHeading(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge
            .copy(
                fontWeight = FontWeight.ExtraBold,
            ),
        color = ComicInk
    )
}