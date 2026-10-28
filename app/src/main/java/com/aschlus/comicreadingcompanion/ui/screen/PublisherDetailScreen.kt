package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.component.ComicDataSectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicEmptyState
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicIssueDetailHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicPublisherSeriesRow
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacing2Xl
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingLg
import com.aschlus.comicreadingcompanion.ui.viewmodel.PublisherDetailViewModel

@Composable
fun PublisherDetailScreen(
    publisherId: Long,
    viewModel: PublisherDetailViewModel,
    onSeriesClick: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val publisher by
    viewModel.publisher.collectAsState()

    val series by
    viewModel.series.collectAsState()

    LaunchedEffect(publisherId) {
        viewModel.loadPublisher(publisherId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ComicIssueDetailHeader(
                onBackClick = onBackClick,
                title = "PUBLISHER DETAIL"
            )
        }
    ) { innerPadding: PaddingValues ->

        val currentPublisher = publisher

        if (currentPublisher == null) {
            Text(
                text = "Loading...",
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(18.dp),
                color = MaterialTheme.colorScheme.onBackground
            )
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentPadding =
                    PaddingValues(
                        start = ComicSpacingLg,
                        end = ComicSpacingLg,
                        top = ComicSpacingLg,
                        bottom = ComicSpacing2Xl
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = currentPublisher.name,
                            style =
                                MaterialTheme.typography.headlineLarge
                                    .copy(
                                        fontSize = 34.sp,
                                        lineHeight = 38.sp,
                                        fontWeight = FontWeight.Black,
                                        fontStyle = FontStyle.Normal
                                    ),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "${series.size} series",
                            style =
                                MaterialTheme.typography.titleMedium
                                    .copy(
                                        fontSize = 16.sp,
                                        lineHeight = 19.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f)
                        )
                    }
                }

                item {
                    ComicDataSectionHeader(
                        text = "SERIES",
                        modifier =
                            Modifier.padding(
                                top = 6.dp
                            )
                    )
                }

                if (series.isEmpty()) {
                    item {
                        ComicEmptyState(
                            message = "No series found for this publisher"
                        )
                    }
                } else {
                    items(
                        items = series,
                        key = { item -> item.seriesId}
                    ) { item ->
                        ComicPublisherSeriesRow(
                            series = item,
                            onClick = {
                                onSeriesClick(item.seriesId)
                            }
                        )
                    }
                }
            }
        }
    }
}