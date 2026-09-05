package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.ui.component.ComicCoverImage
import com.aschlus.comicreadingcompanion.ui.viewmodel.IssueDetailViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailScreen(
    issueId: Long,
    viewModel: IssueDetailViewModel,
    onSeriesClick: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val issue by viewModel.issue.collectAsState()

    LaunchedEffect(issueId) {
        viewModel.loadIssue(issueId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        issue?.let {
                            "${it.seriesTitle} #${it.issueNumber}"
                        } ?: "Issue"
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

        val currentIssue = issue

        if (currentIssue == null) {
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
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                ComicCoverImage(
                    coverUrl = currentIssue.coverUrl,
                    contentDescription =
                        "${currentIssue.seriesTitle} #${currentIssue.issueNumber} cover",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                )

                Text(
                    text = currentIssue.seriesTitle,
                    style =
                        MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.clickable {
                        onSeriesClick(
                            currentIssue.seriesId
                        )
                    }
                )

                Text(
                    text = "#${currentIssue.issueNumber}",
                    style =
                        MaterialTheme.typography.titleLarge
                )

                currentIssue.issueTitle?.let { title ->
                    Text(
                        text = title,
                        style =
                            MaterialTheme.typography.titleLarge
                    )
                }

                val metadata = buildList {
                    currentIssue.publicationDate
                        ?.let { publicationDate ->
                            add(
                                formatIssuePublicationDate(
                                    publicationDate
                                )
                            )
                        }

                    add(
                        currentIssue.issueType
                            .name
                            .replace("_", " ")
                            .lowercase()
                            .replaceFirstChar {
                                it.titlecase(
                                    Locale.getDefault()
                                )
                            }
                    )

                    currentIssue.universeDesignation
                        ?.let { designation ->
                            add(designation)
                        }
                }

                Text(
                    text = metadata.joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = currentIssue.publisherName,
                    style =
                        MaterialTheme.typography.bodyMedium
                )

                when (currentIssue.readingStatus) {
                    ReadingStatus.READ -> {
                        Text(
                            text = "Read",
                            style =
                                MaterialTheme.typography.labelLarge
                        )
                    }

                    ReadingStatus.READING -> {
                        Text(
                            text = "Currently reading",
                            style =
                                MaterialTheme.typography.labelLarge
                        )
                    }

                    else -> {
                        Text(
                            text = "Unread",
                            style =
                                MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentIssue.readingStatus == null) {
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Unread")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                viewModel.markAsUnread()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Unread")
                        }
                    }

                    if (currentIssue.readingStatus ==
                        ReadingStatus.READING
                    ) {
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reading")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                viewModel.markAsReading()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reading")
                        }
                    }

                    if (
                        currentIssue.readingStatus ==
                        ReadingStatus.READ
                    ) {
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Read")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                viewModel.markAsRead()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Read")
                        }
                    }
                }

                currentIssue.description
                    ?.let { description ->
                        Text(
                            text = "Description",
                            style =
                                MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = description,
                            style =
                                MaterialTheme.typography.bodyLarge
                        )
                    }
            }
        }
    }
}

private fun formatIssuePublicationDate(
    publicationDate: String
): String {
    return try {
        YearMonth
            .parse(publicationDate)
            .format(
                DateTimeFormatter.ofPattern(
                    "MMM yyyy",
                    Locale.getDefault()
                )
            )
    } catch (_: Exception) {
        publicationDate
    }
}