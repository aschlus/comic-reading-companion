package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aschlus.comicreadingcompanion.ui.component.ComicEmptyState
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeaderTitle
import com.aschlus.comicreadingcompanion.ui.component.ComicPullListSeriesRow
import com.aschlus.comicreadingcompanion.ui.component.ComicWideActionButton
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingMd
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingXl
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import com.aschlus.comicreadingcompanion.ui.viewmodel.PullListViewModel

@Composable
fun PullListScreen(
    viewModel: PullListViewModel,
    onBrowseClick: () -> Unit,
    onSeriesClick: (Long) -> Unit
) {
    val trackedSeries by viewModel.series.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ComicPrimaryHeader {
                ComicPrimaryHeaderTitle(
                    text = "PULL LIST",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 25.dp)
                )
            }
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = ComicSpacingMd,
                end = ComicSpacingMd,
                top = ComicSpacingMd,
                bottom = ComicSpacingXl
            ),
            verticalArrangement = Arrangement.spacedBy(ComicSpacingMd)
        ) {
            item(key = "pull-list-series-header") {
                ComicHomeSectionHeader(
                    text = "TRACKED SERIES"
                )
            }

            if (isLoading) {
                item(key = "pull-list-loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (trackedSeries.isEmpty()) {
                item(key = "pull-list-series-empty") {
                    ComicEmptyState(
                        message = "Your personal pull list is empty. " +
                            "Add ongoing series to start following " +
                            "their upcoming issues."
                    )
                }
            } else {
                items(
                    items = trackedSeries,
                    key = { series -> series.seriesId }
                ) { series ->
                    ComicPullListSeriesRow(
                        series = series,
                        onClick = {
                            onSeriesClick(series.seriesId)
                        }
                    )
                }
            }

            item(key = "pull-list-browse-action") {
                ComicWideActionButton(
                    text = "BROWSE SERIES",
                    backgroundColor = ComicYellow,
                    onClick = onBrowseClick
                )
            }

            item(key = "pull-list-upcoming-header") {
                ComicHomeSectionHeader(
                    text = "UPCOMING RELEASES"
                )
            }

            item(key = "pull-list-upcoming-empty") {
                ComicEmptyState(
                    message = "Upcoming store-release dates and " +
                        "Final Order Cutoff deadlines will " +
                        "appear here for the series you follow."
                )
            }
        }
    }
}