package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.data.remote.ComicVineVolumeSearchHit
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeaderBackButton
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeaderTitle
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicMutedInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineVolumeSearchViewModel

@Composable
fun ComicVineVolumeSearchScreen(
    viewModel: ComicVineVolumeSearchViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var hasSubmittedSearch by rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        containerColor = ComicPaper,
        topBar = {
            ComicPrimaryHeader {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ComicPrimaryHeaderBackButton(
                        onClick = onBackClick
                    )

                    Spacer(Modifier.width(16.dp))

                    ComicPrimaryHeaderTitle(
                        text = "SEARCH"
                    )
                }
            }
        }
    ) { innerPadding: PaddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ComicHomeSectionHeader(
                    text = "SERIES SEARCH"
                )
            }

            item {
                Text(
                    text = "Search for comic-book series." +
                        "Searching doesn't add anything to your catalog.",
                    color = ComicMutedInk
                )
            }

            item {
                val fieldShape = RoundedCornerShape(9.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .background(Color.White, fieldShape)
                        .border(
                            width = 2.dp,
                            color = ComicInk,
                            shape = fieldShape
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = uiState.query,
                        onValueChange = { query ->
                            hasSubmittedSearch = false
                            viewModel.updateQuery(query)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Series search"
                            },
                        singleLine = true,
                        textStyle = androidx.compose.material3.MaterialTheme
                            .typography.bodyLarge
                            .copy(color = ComicInk),
                        cursorBrush = SolidColor(ComicInk),
                        decorationBox = { innerTextField ->
                            Box {
                                if (uiState.query.isEmpty()) {
                                    Text(
                                        text = "Enter a series title",
                                        color = ComicMutedInk
                                    )
                                }

                                innerTextField()
                            }
                        }
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        hasSubmittedSearch = true
                        viewModel.search()
                    },
                    enabled = !uiState.isLoadingApiKey &&
                        !uiState.isSearching,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (uiState.isSearching) {
                            "SEARCHING..."
                        } else {
                            "SEARCH VOLUMES"
                        }
                    )
                }
            }

            if (uiState.isLoadingApiKey) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = ComicInk
                    )
                }
            } else if (!uiState.hasApiKey) {
                item {
                    Text(
                        text = "Configure your API key " +
                            "in Settings before searching.",
                        color = ComicMutedInk
                    )
                }
            }

            uiState.error?.let { errorMessage ->
                item {
                    Text(
                        text = errorMessage,
                        color = ComicRed
                    )
                }
            }

            if (uiState.isSearching) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = ComicInk
                        )

                        Text(
                            text = "Contacting Source...",
                            color = ComicMutedInk
                        )
                    }
                }
            }

            if (uiState.results.isNotEmpty()) {
                item {
                    Text(
                        text = "${uiState.results.size} of " +
                                "${uiState.totalResults} results",
                        color = ComicMutedInk,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(
                    items = uiState.results,
                    key = { volume -> volume.id }
                ) { volume ->
                    ComicVineVolumeResultRow(
                        volume = volume
                    )
                }
            } else if (
                hasSubmittedSearch &&
                !uiState.isSearching &&
                uiState.error == null &&
                uiState.hasApiKey
            ) {
                item {
                    Text(
                        text = "No volumes found.",
                        color = ComicInk
                    )
                }
            }

            if (uiState.hasMoreResults && uiState.results.isNotEmpty()) {
                item {
                    TextButton(
                        onClick = viewModel::loadMore,
                        enabled = !uiState.isSearching,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("LOAD MORE RESULTS")
                    }
                }
            }
        }
    }
}

@Composable
private fun ComicVineVolumeResultRow(
    volume: ComicVineVolumeSearchHit
) {
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape)
            .border(
                width = 2.dp,
                color = ComicInk,
                shape = shape
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = volume.name,
            color = ComicInk,
            fontWeight = FontWeight.Bold,
            style = androidx.compose.material3.MaterialTheme
                .typography.titleMedium
        )

        val details = listOfNotNull(
            volume.startYear?.toString(),
            volume.publisherName,
            volume.countOfIssues?.let { "$it issues" }
        ).joinToString(" • ")

        if (details.isNotBlank()) {
            Text(
                text = details,
                color = ComicMutedInk
            )
        }

        Text(
            text = "Source ID: ${volume.id}",
            color = ComicMutedInk,
            style = androidx.compose.material3.MaterialTheme
                .typography.bodySmall
        )
    }
}