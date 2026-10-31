
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.component.ComicEmptyState
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeaderTitle
import com.aschlus.comicreadingcompanion.ui.component.ComicPullListReleaseRow
import com.aschlus.comicreadingcompanion.ui.component.ComicPullListSeriesRow
import com.aschlus.comicreadingcompanion.ui.component.ComicWideActionButton
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingMd
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingXl
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import com.aschlus.comicreadingcompanion.ui.viewmodel.PullListReleaseDateGroup
import com.aschlus.comicreadingcompanion.ui.viewmodel.PullListViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PullListScreen(
    viewModel: PullListViewModel,
    onBrowseClick: () -> Unit,
    onSeriesClick: (Long) -> Unit,
    onMetronSettingsClick: () -> Unit
) {
    val trackedSeries by viewModel.series.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val storeReleaseGroups by
    viewModel.storeReleaseGroups.collectAsState()

    val focDeadlineGroups by
    viewModel.focDeadlineGroups.collectAsState()

    val isLoadingReleases by
    viewModel.isLoadingReleases.collectAsState()

    val isRefreshing by
    viewModel.isRefreshing.collectAsState()

    val refreshMessage by
    viewModel.refreshMessage.collectAsState()

    val refreshError by
    viewModel.refreshError.collectAsState()

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
                item(key = "pull-list-series-loading") {
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

            item(key = "pull-list-refresh-action") {
                ComicWideActionButton(
                    text = if (isRefreshing) {
                        "REFRESHING RELEASES..."
                    } else {
                        "REFRESH RELEASES"
                    },
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    enabled = !isRefreshing,
                    onClick = {
                        viewModel.refreshUpcomingReleases()
                    }
                )
            }

            item(key = "pull-list-metron-settings") {
                ComicWideActionButton(
                    text = "METRON SETTINGS",
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onMetronSettingsClick
                )
            }

            if (refreshMessage != null) {
                item(key = "pull-list-refresh-message") {
                    Text(
                        text = refreshMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (refreshError != null) {
                item(key = "pull-list-refresh-error") {
                    Text(
                        text = refreshError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item(key = "pull-list-store-release-header") {
                ComicHomeSectionHeader(
                    text = "UPCOMING STORE RELEASES"
                )
            }

            if (isLoadingReleases) {
                item(key = "pull-list-store-release-loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (storeReleaseGroups.isEmpty()) {
                item(key = "pull-list-store-release-empty") {
                    ComicEmptyState(
                        message = "No upcoming store releases are cached " +
                            "for your tracked series. Refresh to check Metron."
                    )
                }
            } else {
                addReleaseGroups(
                    groups = storeReleaseGroups,
                    keyPrefix = "store-release",
                    secondaryDateLabel = "FOC"
                )
            }

            item(key = "pull-list-foc-header") {
                ComicHomeSectionHeader(
                    text = "FINAL ORDER CUTOFFS"
                )
            }

            if (isLoadingReleases) {
                item(key = "pull-list-foc-loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (focDeadlineGroups.isEmpty()) {
                item(key = "pull-list-foc-empty") {
                    ComicEmptyState(
                        message = "No upcoming FOC deadlines are cached " +
                            "for your tracked series."
                    )
                }
            } else {
                addReleaseGroups(
                    groups = focDeadlineGroups,
                    keyPrefix = "foc-deadline",
                    secondaryDateLabel = "STORE DATE"
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.addReleaseGroups(
    groups: List<PullListReleaseDateGroup>,
    keyPrefix: String,
    secondaryDateLabel: String
) {
    groups.forEach { group ->
        item(key = "$keyPrefix-date-${group.date}") {
            Text(
                text = group.date.format(
                    DateTimeFormatter.ofPattern(
                        "MMM d, yyyy",
                        Locale.getDefault()
                    )
                ),
                modifier = Modifier.padding(
                    top = 4.dp,
                    bottom = 2.dp
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(
            items = group.releases,
            key = { release ->
                "$keyPrefix-${release.providerId}-${release.externalIssueId}"
            }
        ) { release ->
            val secondaryDate =
                if (secondaryDateLabel == "FOC") {
                    release.focDate
                } else {
                    release.storeReleaseDate
                }

            ComicPullListReleaseRow(
                release = release,
                secondaryDateLabel = secondaryDateLabel,
                secondaryDate = secondaryDate
            )
        }
    }
}