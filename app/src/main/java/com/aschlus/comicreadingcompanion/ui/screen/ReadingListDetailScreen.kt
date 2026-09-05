package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.withFrameNanos
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

    var collapsedSectionIds by rememberSaveable(
        readingListId
    ) {
        mutableStateOf(longArrayOf())
    }

    var searchQuery by rememberSaveable(
        readingListId
    ) {
        mutableStateOf("")
    }

    var readingStatusFilter by rememberSaveable(
        readingListId
    ) {
        mutableStateOf("ALL")
    }

    var requiredFilter by rememberSaveable(
        readingListId
    ) { mutableStateOf("ALL")}

    var selectedSeriesKey by rememberSaveable(
        readingListId
    ) {
        mutableStateOf<String?>(null)
    }

    var seriesMenuExpanded by remember {
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
                val targetIssue = issues[targetIndex]
                val targetSectionId = targetIssue.sectionId
                val sectionWasCollapsed =
                    targetSectionId != null &&
                            collapsedSectionIds.contains(
                                targetSectionId
                            )

                if (
                    targetSectionId != null &&
                    sectionWasCollapsed
                ) {
                    collapsedSectionIds =
                        collapsedSectionIds.filter { sectionId ->
                            sectionId != targetSectionId
                        }
                        .toLongArray()

                    withFrameNanos {  }
                }

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
                val seriesOptions =
                    issues
                        .map { issue ->
                            SeriesFilterOption(
                                title = issue.seriesTitle,
                                volume = issue.seriesVolume
                            )
                        }
                        .distinct()
                        .sortedWith(
                            compareBy<SeriesFilterOption> {
                                it.title
                            }.thenBy {
                                it.volume ?: 0
                            }
                        )
                val selectedSeries =
                    seriesOptions.find { option ->
                        option.key == selectedSeriesKey
                    }
                val trimmedSearchQuery = searchQuery.trim()
                val isSearching = trimmedSearchQuery.isNotEmpty()

                val matchingSectionIds =
                    if (!isSearching) {
                        emptySet()
                    } else {
                        issues
                            .filter { issue ->
                                sectionMatchesSearch(
                                    issue = issue,
                                    query = trimmedSearchQuery
                                )
                            }
                            .mapNotNull { issue ->
                                issue.sectionId
                            }
                            .toSet()
                    }

                val searchMatchedIssues =
                    if (!isSearching) {
                        issues
                    } else {
                        issues.filter { issue ->
                            issue.sectionId in matchingSectionIds ||
                                issueMatchesSearch(
                                    issue = issue,
                                    query = trimmedSearchQuery
                                )
                        }
                    }

                val hasActiveFilters =
                    readingStatusFilter != "ALL" ||
                        requiredFilter != "ALL" ||
                        selectedSeriesKey != null

                val visibleIssues =
                    searchMatchedIssues.filter { issue ->

                        val matchesReadingStatus =
                            when (readingStatusFilter) {
                                "UNREAD" ->
                                    issue.readingStatus == null ||
                                        issue.readingStatus == ReadingStatus.UNREAD

                                "READING" ->
                                    issue.readingStatus == ReadingStatus.READING

                                "READ" ->
                                    issue.readingStatus == ReadingStatus.READ

                                else ->
                                    true
                            }

                        val matchesRequiredStatus =
                            when (requiredFilter) {
                                "REQUIRED" ->
                                    issue.required

                                "OPTIONAL" ->
                                    !issue.required

                                else ->
                                    true
                            }

                        val matchesSeries =
                            selectedSeriesKey == null ||
                                SeriesFilterOption(
                                    title = issue.seriesTitle,
                                    volume = issue.seriesVolume
                                ).key == selectedSeriesKey

                        matchesReadingStatus && matchesRequiredStatus && matchesSeries
                    }

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

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { newQuery ->
                        searchQuery = newQuery
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text("Search this reading list")
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
                                    searchQuery = ""
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    }
                )

                Text(
                    text = "Reading status",
                    style =
                        MaterialTheme.typography.labelLarge
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(
                            rememberScrollState()
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected =
                            readingStatusFilter == "UNREAD",
                        onClick = {
                            readingStatusFilter =
                                if (readingStatusFilter == "UNREAD") {
                                    "ALL"
                                } else {
                                    "UNREAD"
                                }
                        },
                        label = {
                            Text("Unread")
                        }
                    )

                    FilterChip(
                        selected =
                            readingStatusFilter == "READING",
                        onClick = {
                            readingStatusFilter =
                                if (readingStatusFilter == "READING") {
                                    "ALL"
                                } else {
                                    "READING"
                                }
                        },
                        label = {
                            Text("Reading")
                        }
                    )

                    FilterChip(
                        selected =
                            readingStatusFilter == "READ",
                        onClick = {
                            readingStatusFilter =
                                if (readingStatusFilter == "READ") {
                                    "ALL"
                                } else {
                                    "READ"
                                }
                        },
                        label = {
                            Text("Read")
                        }
                    )
                }

                Text(
                    text = "List status",
                    style =
                        MaterialTheme.typography.labelLarge
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected =
                            requiredFilter == "REQUIRED",
                        onClick = {
                            requiredFilter =
                                if(requiredFilter == "REQUIRED") {
                                    "ALL"
                                } else {
                                    "REQUIRED"
                                }
                        },
                        label = {
                            Text("Required")
                        }
                    )

                    FilterChip(
                        selected =
                            requiredFilter == "OPTIONAL",
                        onClick = {
                            requiredFilter =
                                if (requiredFilter == "OPTIONAL") {
                                    "ALL"
                                } else {
                                    "OPTIONAL"
                                }
                        },
                        label = {
                            Text("Optional")
                        }
                    )

                    Text(
                        text = "Series",
                        style =
                            MaterialTheme.typography.labelLarge
                    )

                    Box {
                        OutlinedButton(
                            onClick = {
                                seriesMenuExpanded = true
                            }
                        ) {
                            Text(
                                text =
                                    selectedSeries?.displayName
                                        ?: "All series"
                            )
                        }

                        DropdownMenu(
                            expanded = seriesMenuExpanded,
                            onDismissRequest = {
                                seriesMenuExpanded = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("All series")
                                },
                                onClick = {
                                    selectedSeriesKey = null
                                    seriesMenuExpanded = false
                                }
                            )

                            seriesOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(option.displayName)
                                    },
                                    onClick = {
                                        selectedSeriesKey = option.key
                                        seriesMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (isSearching || hasActiveFilters) {
                    Text(
                        text =
                            "${visibleIssues.size} of " +
                                "${issues.size} issues shown",
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }

                val firstUnreadIndex = issues.indexOfFirst { issue ->
                    issue.readingStatus != ReadingStatus.READ
                }

                if (
                    firstUnreadIndex >= 0 &&
                    !isSearching &&
                    !hasActiveFilters
                ) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val targetIssue = issues[firstUnreadIndex]
                                val targetSectionId = targetIssue.sectionId
                                val sectionWasCollapsed =
                                    targetSectionId != null &&
                                            collapsedSectionIds.contains(
                                                targetSectionId
                                            )

                                if (
                                    targetSectionId != null &&
                                    sectionWasCollapsed
                                ) {
                                    collapsedSectionIds =
                                        collapsedSectionIds.filter { sectionId ->
                                            sectionId != targetSectionId
                                        }
                                            .toLongArray()

                                    withFrameNanos { }
                                }

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
                } else if (
                    (isSearching || hasActiveFilters) && visibleIssues.isEmpty()
                ) {
                    Text(
                        text =
                            if (isSearching) {
                                "No issues match " +
                                    "\"$trimmedSearchQuery\" " +
                                    "with the current filters."
                            } else {
                                "No issues match the current filters."
                            }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        state = listState
                    ) {
                        itemsIndexed(
                            items = visibleIssues,
                            key = { _, issue ->
                                issue.readingListItemId
                            }
                        ) { index, issue ->

                            val previousSectionId =
                                if (index > 0) {
                                    visibleIssues[index - 1].sectionId
                                } else {
                                    null
                                }

                            val sectionId =
                                issue.sectionId

                            val isFirstIssueInSection =
                                sectionId != null &&
                                    sectionId != previousSectionId

                            val isSectionCollapsed =
                                !isSearching &&
                                    !hasActiveFilters &&
                                    sectionId != null &&
                                    collapsedSectionIds.contains(
                                        sectionId
                                    )

                            if (isFirstIssueInSection) {
                                ReadingListSectionHeader(
                                    title =
                                        issue.sectionTitle
                                            ?: "Section",
                                    description =
                                        issue.sectionDescription,
                                    isCollapsed =
                                        isSectionCollapsed,
                                    onToggleCollapsed = {
                                        collapsedSectionIds =
                                            if (
                                                collapsedSectionIds.contains(
                                                    sectionId
                                                )
                                            ) {
                                                collapsedSectionIds
                                                    .filter {
                                                        it != sectionId
                                                    }
                                                    .toLongArray()
                                            } else {
                                                collapsedSectionIds +
                                                    sectionId
                                            }
                                    }
                                )
                            }

                            if (!isSectionCollapsed) {
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
    description: String?,
    isCollapsed: Boolean,
    onToggleCollapsed: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick =
                    onToggleCollapsed
            )
            .padding(
                top = 16.dp,
                bottom = 8.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Column(
            modifier =
                Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style =
                    MaterialTheme.typography
                        .headlineSmall
            )

            description?.let {
                    sectionDescription ->
                Text(
                    text =
                        sectionDescription,
                    style =
                        MaterialTheme.typography
                            .bodyMedium,
                    modifier =
                        Modifier.padding(
                            top = 4.dp
                        )
                )
            }
        }

        Icon(
            imageVector =
                if (isCollapsed) {
                    Icons.Default.ExpandMore
                } else {
                    Icons.Default.ExpandLess
                },
            contentDescription =
                if (isCollapsed) {
                    "Expand section"
                } else {
                    "Collapse section"
                }
        )
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

private data class SeriesFilterOption(
    val title: String,
    val volume: Int?
) {
    val key: String
        get() = "$title|${volume ?: "none"}"

    val displayName: String
        get() =
            if (volume == null) {
                title
            } else {
                "$title (Vol. $volume)"
            }
}

private fun issueMatchesSearch(
    issue: ReadingListIssue,
    query: String
): Boolean {
    val searchText = buildString {
        append(issue.seriesTitle)
        append(" #")
        append(issue.issueNumber)

        issue.issueTitle?.let { notes ->
            append(' ')
            append(notes)
        }
    }

    return searchText.contains(
        other = query,
        ignoreCase = true
    )
}

private fun sectionMatchesSearch(
    issue: ReadingListIssue,
    query: String
): Boolean {
    val searchText = buildString {
        issue.seriesTitle?.let { title ->
            append(title)
        }

        issue.sectionDescription?.let { description ->
            append(' ')
            append(description)
        }
    }

    return searchText.contains(
        other = query,
        ignoreCase = true
    )
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