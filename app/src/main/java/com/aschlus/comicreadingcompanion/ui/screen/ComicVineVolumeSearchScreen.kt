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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.data.remote.ComicVineVolumeSearchHit
import com.aschlus.comicreadingcompanion.ui.component.ComicAddToListSearchField
import com.aschlus.comicreadingcompanion.ui.component.ComicBrowseResultRow
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeaderBackButton
import com.aschlus.comicreadingcompanion.ui.component.ComicPrimaryHeaderTitle
import com.aschlus.comicreadingcompanion.ui.component.ComicWideActionButton
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardBorderWidth
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPurple
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingLg
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingMd
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingSm
import com.aschlus.comicreadingcompanion.ui.viewmodel.ComicVineVolumeSearchViewModel

@Composable
fun ComicVineVolumeSearchScreen(
    viewModel: ComicVineVolumeSearchViewModel,
    onBackClick: () -> Unit,
    onVolumeClick: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    var hasSubmittedSearch by rememberSaveable {
        mutableStateOf(false)
    }

    var matchingOptionsExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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

                    Spacer(Modifier.width(ComicSpacingLg))

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
            contentPadding = PaddingValues(ComicSpacingLg),
            verticalArrangement = Arrangement.spacedBy(ComicSpacingMd)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                ComicAddToListSearchField(
                    query = uiState.query,
                    onQueryChange = { query ->
                        hasSubmittedSearch = false
                        viewModel.updateQuery(query)
                    },
                    placeholder = "Enter a series title",
                    contentDescription = "Series search"
                )
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(ComicSpacingMd)
                ) {
                    Box(
                        modifier = Modifier.height(44.dp)
                    ) {
                        val shape = RoundedCornerShape(9.dp)

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = 2.dp,y = 3.dp)
                                .background(
                                    color = ComicInk,
                                    shape = shape
                                )
                        )

                        Row(
                            modifier = Modifier
                                .height(41.dp)
                                .clip(shape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    width = ComicCardBorderWidth,
                                    color = ComicInk,
                                    shape = shape
                                )
                                .clickable(
                                    role = Role.Button,
                                    onClick = {
                                        matchingOptionsExpanded = !matchingOptionsExpanded
                                    }
                                )
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(Modifier.width(ComicSpacingSm))

                            Text(
                                text = if (matchingOptionsExpanded) {
                                    "HIDE MATCHING OPTIONS"
                                } else {
                                    "ADDITIONAL MATCHING OPTIONS"
                                },
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    if (matchingOptionsExpanded) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(ComicSpacingMd)
                        ) {
                            MatchingOptionField(
                                label = "Publisher",
                                placeholder = "e.g. Marvel Comics",
                                contentDescription = "Publisher filter",
                                value = uiState.publisherFilter,
                                keyboardType = KeyboardType.Text,
                                onValueChange = { publisher ->
                                    hasSubmittedSearch = false
                                    viewModel.updateMatchingOptions(
                                        publisher = publisher,
                                        startYear = uiState.startYearFilter,
                                        expectedIssueCount = uiState.expectedIssueCountFilter
                                    )
                                }
                            )

                            MatchingOptionField(
                                label = "Start year",
                                placeholder = "e.g. 1963",
                                contentDescription = "Start year filter",
                                value = uiState.startYearFilter,
                                keyboardType = KeyboardType.Number,
                                onValueChange = { startYear ->
                                    hasSubmittedSearch = false
                                    viewModel.updateMatchingOptions(
                                        publisher = uiState.publisherFilter,
                                        startYear = startYear,
                                        expectedIssueCount = uiState.expectedIssueCountFilter
                                    )
                                }
                            )

                            MatchingOptionField(
                                label = "Expected issue count",
                                placeholder = "e.g. 100",
                                contentDescription = "Expected issue count filter",
                                value = uiState.expectedIssueCountFilter,
                                keyboardType = KeyboardType.Number,
                                onValueChange = { expectedCount ->
                                    hasSubmittedSearch = false
                                    viewModel.updateMatchingOptions(
                                        publisher = uiState.publisherFilter,
                                        startYear = uiState.startYearFilter,
                                        expectedIssueCount = expectedCount
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item {
                ComicWideActionButton(
                    text = if (uiState.isSearching) {
                        "SEARCHING..."
                    } else {
                        "SEARCH VOLUMES"
                    },
                    backgroundColor = ComicBlue,
                    onClick = {
                        hasSubmittedSearch = true
                        viewModel.search()
                    },
                    enabled = !uiState.isLoadingApiKey &&
                        !uiState.isSearching,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.isLoadingApiKey) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (!uiState.hasApiKey) {
                item {
                    Text(
                        text = "Configure your API key " +
                            "in Settings before searching.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        horizontalArrangement = Arrangement.spacedBy(ComicSpacingMd)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Contacting Source...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (uiState.results.isNotEmpty()) {
                item {
                    Text(
                        text = "${uiState.results.size} of " +
                                "${uiState.totalResults} results",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(
                    items = uiState.results,
                    key = { volume -> volume.id }
                ) { volume ->
                    ComicVineVolumeResultRow(
                        volume = volume,
                        onClick = {
                            onVolumeClick(volume.id)
                        }
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
                        color = MaterialTheme.colorScheme.onBackground
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
private fun MatchingOptionField(
    label: String,
    placeholder: String,
    contentDescription: String,
    value: String,
    keyboardType: KeyboardType,
    onValueChange: (String) -> Unit
) {
    val fieldShape = RoundedCornerShape(9.dp)

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, fieldShape)
                .border(
                    width = ComicCardBorderWidth,
                    color = ComicInk,
                    shape = fieldShape
                )
                .padding(horizontal = ComicSpacingMd),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        this.contentDescription = contentDescription
                    },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge
                    .copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(ComicInk),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    autoCorrectEnabled = false
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun ComicVineVolumeResultRow(
    volume: ComicVineVolumeSearchHit,
    onClick: () -> Unit
) {
    val details = listOfNotNull(
        volume.startYear?.toString(),
        volume.publisherName,
        volume.countOfIssues?.let { "$it issues" }
    ).joinToString(" • ")

    ComicBrowseResultRow(
        badgeText = volume.name
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString()
            ?: "?",
        badgeColor = ComicPurple,
        title = volume.name,
        subtitle = details,
        supportingText = "Source ID: ${volume.id}",
        statusText = null,
        onClick = onClick,
        contentDescription = "Series result ${volume.id}",
        coverUrl = null
    )
}