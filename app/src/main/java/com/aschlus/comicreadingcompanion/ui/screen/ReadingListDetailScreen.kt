package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListIssue
import com.aschlus.comicreadingcompanion.ui.component.ComicCoverImage
import com.aschlus.comicreadingcompanion.ui.viewmodel.ReadingListDetailViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListDetailScreen(
    readingListId: Long,
    startPosition: Int,
    viewModel: ReadingListDetailViewModel,
    onIssueClick: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val readingList by
        viewModel.readingList.collectAsState()

    val issues by
        viewModel.issues.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var hasAutoScrolled by rememberSaveable(
        readingListId,
        startPosition
    ) {
        mutableStateOf(false)
    }

    var listMenuExpanded by remember {
        mutableStateOf(false)
    }

    var showResetProgressDialog by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(readingListId) {
        viewModel.loadReadingList(readingListId)
    }

    LaunchedEffect(
        issues,
        startPosition,
        hasAutoScrolled
    ) {
        if (
            !hasAutoScrolled &&
            startPosition >= 0 &&
            issues.isNotEmpty()
        ) {
            val targetIndex = issues.indexOfFirst { issue ->
                issue.position == startPosition
            }

            if (targetIndex >= 0) {
                listState.scrollToItem(targetIndex)
            }

            hasAutoScrolled = true
        }
    }

    val hasUnreadIssues = issues.any { issue ->
        issue.readingStatus != ReadingStatus.READ
    }

    val hasAnyProgress = issues.any { issue ->
        issue.readingStatus != null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        readingList?.title ?: "Reading List"
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
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = {
                                listMenuExpanded = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription =
                                    "Reading list options"
                            )
                        }

                        DropdownMenu(
                            expanded = listMenuExpanded,
                            onDismissRequest = {
                                listMenuExpanded = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("Mark all as read")
                                },
                                enabled = hasUnreadIssues,
                                onClick = {
                                    listMenuExpanded = false
                                    viewModel.markAllAsRead()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Reset reading progress")
                                },
                                enabled = hasAnyProgress,
                                onClick = {
                                    listMenuExpanded = false
                                    showResetProgressDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    )
    { innerPadding: PaddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            val currentReadingList = readingList

            if (currentReadingList == null) {
                Text("Loading...")
            } else {

                currentReadingList.description
                    ?.let { description ->
                        Text(description)
                    }

                val readCount = issues.count {
                    it.readingStatus == ReadingStatus.READ
                }

                val totalCount = issues.size

                val progress =
                    if (totalCount == 0) {
                        0f
                    } else {
                        readCount.toFloat() / totalCount.toFloat()
                    }

                val completionPercentage =
                    if (totalCount == 0) {
                        0
                    } else {
                        (readCount * 100) / totalCount
                    }

                Text(
                    text = "$readCount of $totalCount read • " +
                        "$completionPercentage% complete"
                )

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Issues")

                val firstUnreadIndex = issues.indexOfFirst { issue ->
                    issue.readingStatus != ReadingStatus.READ
                }

                if (firstUnreadIndex >= 0) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(
                                    firstUnreadIndex
                                )
                            }
                        }
                    ) {
                        Text("Jump to first unread")
                    }
                }

                if (issues.isEmpty()) {
                    Text("No issues in this reading list")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        state = listState
                    ) {
                        itemsIndexed(
                            items = issues,
                            key = { _, issue ->
                                issue.readingListItemId
                            }
                        ) { index, issue ->

                            val previousSectionId =
                                if (index > 0) {
                                    issues[index - 1].sectionId
                                } else {
                                    null
                                }

                            if (
                                issue.sectionId != null &&
                                issue.sectionId != previousSectionId
                            ) {
                                ReadingListSectionHeader(
                                    title =
                                        issue.sectionTitle
                                            ?: "Section",
                                    description =
                                        issue.sectionDescription
                                )
                            }

                            ReadingListIssueRow(
                                issue = issue,
                                onIssueClick = {
                                    onIssueClick(issue.issueId)
                                },
                                onToggleRead = {
                                    viewModel.toggleIssueRead(
                                        issue = issue
                                    )
                                },
                                onMarkAsReading = {
                                    viewModel.markIssueAsReading(
                                        issue = issue
                                    )
                                },
                                onMarkAllBeforeRead = {
                                    viewModel.markAllBeforeAsRead(
                                        selectedIssue = issue
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showResetProgressDialog) {
        AlertDialog(
            onDismissRequest = {
                showResetProgressDialog = false
            },
            title = {
                Text("Reset reading progress?")
            },
            text = {
                Text(
                    "This will mark every issue in this " +
                    "reading list as unread. Issue " +
                    "progress is shared across reading " +
                    "lists, so these issues will also " +
                    "appear unread in any other lists " +
                    "that contain them."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetProgressDialog = false
                        viewModel.resetProgress()
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showResetProgressDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ReadingListSectionHeader(
    title: String,
    description: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 16.dp,
                bottom = 8.dp
            )
    ) {
        Text(
            text = title,
            style =
                MaterialTheme.typography.headlineSmall
        )

        description?.let { sectionDescription ->
            Text(
                text = sectionDescription,
                style =
                    MaterialTheme.typography.bodyMedium,
                modifier =
                    Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ReadingListIssueRow(
    issue: ReadingListIssue,
    onIssueClick: () -> Unit,
    onToggleRead: () -> Unit,
    onMarkAsReading: () -> Unit,
    onMarkAllBeforeRead: () -> Unit
) {

    var menuExpanded by remember {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked =
                issue.readingStatus == ReadingStatus.READ,
            onCheckedChange = {
                onToggleRead()
            }
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    onClick = onIssueClick
                )
                .padding(
                    vertical = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ComicCoverImage(
                coverUrl = issue.coverUrl,
                contentDescription =
                    "${issue.seriesTitle} #${issue.issueNumber} cover",
                modifier = Modifier
                    .width(56.dp)
                    .aspectRatio(2f / 3f),
                placeholderText = "No Cover"
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text =
                        "${issue.seriesTitle} #${issue.issueNumber}",
                    style =
                        MaterialTheme.typography.titleMedium
                )

                issue.issueTitle?.let { title ->
                    Text(
                        text = title,
                        style =
                            MaterialTheme.typography.bodyMedium
                    )
                }

                val metadata = buildList {
                    issue.publicationDate?.let { publicationDate ->
                        add(formatPublicationDate(publicationDate))
                    }

                    add("Order #${issue.position}")

                    if (!issue.required) {
                        add("Optional")
                    }
                }

                Text(
                    text = metadata.joinToString(" • "),
                    style =
                        MaterialTheme.typography.bodySmall
                )

                if (
                    issue.readingStatus ==
                    ReadingStatus.READING
                ) {
                    Text(
                        text = "Currently reading",
                        style =
                            MaterialTheme.typography.labelMedium
                    )
                }

                issue.notes?.let { notes ->
                    Text(
                        text = notes,
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Column {
            IconButton(
                onClick = {
                    menuExpanded = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options"
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = {
                    menuExpanded = false
                }
            ) {
                DropdownMenuItem(
                    text = {
                        Text("Mark as reading")
                    },
                    onClick = {
                        menuExpanded = false
                        onMarkAsReading()
                    }
                )

                if (issue.position > 1) {
                    DropdownMenuItem(
                        text = {
                            Text("Mark all before as read")
                        },
                        onClick = {
                            menuExpanded = false
                            onMarkAllBeforeRead()
                        }
                    )
                }
            }
        }
    }
}

private fun formatPublicationDate(
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