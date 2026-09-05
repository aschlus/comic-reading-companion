package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.data.database.models.PublisherSeries
import com.aschlus.comicreadingcompanion.ui.viewmodel.PublisherDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        publisher?.name ?: "Publisher"
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding: PaddingValues ->

        val currentPublisher = publisher

        if (currentPublisher == null) {
            Text(
                text = "Loading...",
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Text(
                   text = currentPublisher.name,
                    style =
                        MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = "${series.size} series",
                    style =
                        MaterialTheme.typography.bodyMedium
                )

                if (series.isEmpty()) {
                    Text(
                        "No series found for this publisher"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = series,
                            key = { item ->
                                item.seriesId
                            }
                        ) { item ->
                            PublisherSeriesRow(
                                series = item,
                                onClick = {
                                    onSeriesClick(
                                        item.seriesId
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublisherSeriesRow(
    series: PublisherSeries,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            )
            .padding(
                vertical = 10.dp,
                horizontal = 4.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = series.title,
                style =
                    MaterialTheme.typography.titleMedium
            )

            val metadata = buildList {
                series.volume?.let { volume ->
                    add("Volume $volume")
                }

                when {
                    series.startYear != null &&
                        series.endYear != null &&
                        series.startYear !=
                        series.endYear -> {
                        add(
                            "${series.startYear}-" +
                                    "${series.endYear}"
                        )
                    }

                    series.startYear != null -> {
                        add(
                            series.startYear.toString()
                        )
                    }
                }
            }

            if (metadata.isNotEmpty()) {
                Text(
                    text = metadata.joinToString(" • "),
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text =
                    "${series.readCount} of " +
                        "${series.totalCount} read",
                style =
                    MaterialTheme.typography.bodySmall
            )

            val progress =
                if (series.totalCount == 0) {
                    0f
                } else {
                    series.readCount.toFloat() /
                            series.totalCount.toFloat()
                }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}