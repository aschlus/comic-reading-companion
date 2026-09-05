package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.models.IssueSearchResult
import com.aschlus.comicreadingcompanion.data.database.models.SeriesSearchResult
import com.aschlus.comicreadingcompanion.ui.viewmodel.BrowseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    viewModel: BrowseViewModel,
    onPublisherClick: (Long) -> Unit,
    onSeriesClick: (Long) -> Unit,
    onIssueClick: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val publishers by
        viewModel.publishers.collectAsState()

    val searchQuery by
        viewModel.searchQuery.collectAsState()

    val seriesResults by
        viewModel.seriesResults.collectAsState()

    val issueResults by
        viewModel.issueResults.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Browse Comics")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    viewModel.updateSearchQuery(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Search series and issues")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.updateSearchQuery("")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true
            )

            if (searchQuery.isBlank()) {

                Text(
                    text = "Publishers",
                    style =
                        MaterialTheme.typography.headlineSmall
                )

                Text(
                    text =
                        "Browse all publishers currently " +
                            "available in your comic database.",
                    style =
                        MaterialTheme.typography.bodyMedium
                )

                if (publishers.isEmpty()) {
                    Text("No publishers found")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = publishers,
                            key = { publisher ->
                                publisher.id
                            }
                        ) { publisher ->
                            PublisherCard(
                                publisher = publisher,
                                onClick = {
                                    onPublisherClick(
                                        publisher.id
                                    )
                                }
                            )
                        }
                    }
                }
            } else {

                val hasResults =
                    seriesResults.isNotEmpty() ||
                            issueResults.isNotEmpty()

                if (!hasResults) {
                    Text(
                        text = "No results found",
                        style =
                            MaterialTheme.typography.bodyLarge
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        if (seriesResults.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Series",
                                    style =
                                        MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(
                                        vertical = 4.dp
                                    )
                                )
                            }

                            items(
                                items = seriesResults,
                                key = { result ->
                                    "series-${result.seriesId}"
                                }
                            ) {result ->
                                SeriesSearchResultCard(
                                    result = result,
                                    onClick = {
                                        onSeriesClick(
                                            result.seriesId
                                        )
                                    }
                                )
                            }
                        }

                        if (issueResults.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Issues",
                                    style =
                                        MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(
                                        top = 12.dp,
                                        bottom = 4.dp
                                    )
                                )
                            }

                            items(
                                items = issueResults,
                                key = { result ->
                                    "issue-${result.issueId}"
                                }
                            ) { result ->
                                IssueSearchResultCard(
                                    result = result,
                                    onClick = {
                                        onIssueClick(
                                            result.issueId
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
}

@Composable
private fun PublisherCard(
    publisher: Publisher,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = publisher.name,
                style =
                    MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Browse series",
                style =
                    MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SeriesSearchResultCard(
    result: SeriesSearchResult,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = result.title,
                style =
                    MaterialTheme.typography.titleMedium
            )

            val metadata = buildList {
                result.volume?.let { volume ->
                    add("Volume $volume")
                }

                result.startYear?.let { year ->
                    add(year.toString())
                }

                add(result.publisherName)
            }

            Text(
                text = metadata.joinToString(" • "),
                style =
                    MaterialTheme.typography.bodySmall
            )

            Text(
                text =
                    "${result.readCount} of " +
                        "${result.totalCount} read",
                style =
                    MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun IssueSearchResultCard(
    result: IssueSearchResult,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text =
                    "${result.seriesTitle} " +
                            "#${result.issueNumber}",
                style =
                    MaterialTheme.typography.titleMedium
            )

            result.issueTitle?.let { title ->
                Text(
                    text = title,
                    style =
                        MaterialTheme.typography.bodyMedium
                )
            }

            val metadata = buildList {
                result.seriesVolume?.let { volume ->
                    add("Volume $volume")
                }

                result.publicationDate?.let { date ->
                    add(date)
                }

                add(result.publisherName)

                add(
                    when (result.readingStatus) {
                        ReadingStatus.READ -> "Read"
                        ReadingStatus.READING -> "Reading"
                        else -> "Unread"
                    }
                )
            }

            Text(
                text = metadata.joinToString(" • "),
                style =
                    MaterialTheme.typography.bodySmall
            )
        }
    }
}