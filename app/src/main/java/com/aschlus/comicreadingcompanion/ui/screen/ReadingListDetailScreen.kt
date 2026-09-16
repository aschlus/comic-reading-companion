package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSource
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListIssue
import com.aschlus.comicreadingcompanion.ui.component.ComicConfirmationDialog
import com.aschlus.comicreadingcompanion.ui.component.ComicCoverImage
import com.aschlus.comicreadingcompanion.ui.component.ComicDropdownMenu
import com.aschlus.comicreadingcompanion.ui.component.ComicDropdownMenuItem
import com.aschlus.comicreadingcompanion.ui.component.ComicFormDialog
import com.aschlus.comicreadingcompanion.ui.component.ComicReadingListDetailControls
import com.aschlus.comicreadingcompanion.ui.component.ComicReadingListDetailHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicReadingListDetailHero
import com.aschlus.comicreadingcompanion.ui.component.ComicReadingListFilterSeriesOption
import com.aschlus.comicreadingcompanion.ui.component.ComicReadingListFilterSheetContent
import com.aschlus.comicreadingcompanion.ui.component.ComicReadingListSearchHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicReadingListSelectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicSectionPickerDialog
import com.aschlus.comicreadingcompanion.ui.component.ComicSectionPickerOption
import com.aschlus.comicreadingcompanion.ui.component.ComicSlantedShape
import com.aschlus.comicreadingcompanion.ui.theme.ComicAccentTextTransform
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed
import com.aschlus.comicreadingcompanion.ui.theme.LilitaOneFontFamily
import com.aschlus.comicreadingcompanion.ui.viewmodel.ReadingListDetailViewModel
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun ReadingListDetailScreen(
    readingListId: Long,
    startPosition: Int,
    viewModel: ReadingListDetailViewModel,
    onIssueClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    onExportReadingListClick: (Long, String) -> Unit = { _, _ -> },
    onReadingListDuplicated: (Long) -> Unit = {}
) {
    val readingList by
        viewModel.readingList.collectAsState()

    val issues by
        viewModel.issues.collectAsState()

    val sections by
        viewModel.sections.collectAsState()

    val readingListDeleted by
        viewModel.readingListDeleted.collectAsState()

    val duplicatedReadingListId by
        viewModel.duplicatedReadingListId.collectAsState()

    val collapsedSectionIds by
        viewModel.collapsedSectionIds.collectAsState()

    val collapsedSectionsLoaded by
        viewModel.collapsedSectionsLoaded.collectAsState()

    val selectedReadingListItemIds by
            viewModel.selectedReadingListItemIds.collectAsState()

    val isSelectionMode = selectedReadingListItemIds.isNotEmpty()

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

    var showEditReadingListDialog by remember(
        readingListId
    ) {
        mutableStateOf(false)
    }

    var editReadingListTitle by remember(
        readingListId
    ) {
        mutableStateOf("")
    }

    var editReadingListDescription by remember(
        readingListId
    ) {
        mutableStateOf("")
    }

    var showCreateSectionDialog by remember(
        readingListId
    ) {
        mutableStateOf(false)
    }

    var showDeleteReadingListDialog by remember(
        readingListId
    ) {
        mutableStateOf(false)
    }

    var newSectionTitle by remember(
        readingListId
    ) {
        mutableStateOf("")
    }

    var newSectionDescription by remember(
        readingListId
    ) {
        mutableStateOf("")
    }

    var stickyControlsHeightPx by remember {
        mutableIntStateOf(0)
    }

    var issuePendingSectionMove by remember(
        readingListId
    ) {
        mutableStateOf<ReadingListIssue?>(null)
    }

    var issuePendingRemoval by remember(
        readingListId) {
        mutableStateOf<ReadingListIssue?>(null)
    }

    var searchQuery by rememberSaveable(
        readingListId
    ) {
        mutableStateOf("")
    }

    var isSearchActive by rememberSaveable(
        readingListId
    ) {
        mutableStateOf(false)
    }

    val searchFocusRequester =
        remember {
            FocusRequester()
        }

    val keyboardController =
        LocalSoftwareKeyboardController.current

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

    var showFilterSheet by remember {
        mutableStateOf(false)
    }

    var filterSheetState =
        rememberModalBottomSheetState (
            skipPartiallyExpanded = true
        )

    LaunchedEffect(readingListId) {
        viewModel.loadReadingList(readingListId)
    }

    LaunchedEffect(readingListDeleted) {
        if (readingListDeleted) {
            onBackClick()
        }
    }

    LaunchedEffect(duplicatedReadingListId) {
        val duplicatedId = duplicatedReadingListId
            ?: return@LaunchedEffect

        viewModel.clearDuplicatedReadingList()

        onReadingListDuplicated(duplicatedId)
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            withFrameNanos {  }
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(
        issues,
        startPosition,
        hasAutoScrolled,
        collapsedSectionsLoaded,
        stickyControlsHeightPx
    ) {
        if (!collapsedSectionsLoaded || stickyControlsHeightPx == 0) {
            return@LaunchedEffect
        }

        if (
            !hasAutoScrolled &&
            startPosition >= 0 &&
            issues.isNotEmpty()
        ) {
            val targetIndex = issues.indexOfFirst { issue ->
                issue.position == startPosition
            }

            if (targetIndex > 0) {
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
                    viewModel.expandSection(targetSectionId)

                    withFrameNanos {  }
                }

                listState.scrollToItem(
                    index = targetIndex + 2,
                    scrollOffset = -stickyControlsHeightPx
                )
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

    var activeFilterCount =
        listOf(
            readingStatusFilter != "ALL",
            requiredFilter != "ALL",
            selectedSeriesKey != null
        ).count { it}

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

    val firstUnreadIndex = issues.indexOfFirst { issue ->
        issue.readingStatus != ReadingStatus.READ
    }

    Scaffold(
        topBar = {
            when {
                isSelectionMode -> {
                    ComicReadingListSelectionHeader(
                        selectedCount = selectedReadingListItemIds.size,
                        onCancelClick = {
                            viewModel.clearIssueSelection()
                        },
                        onMarkReadClick = {
                            viewModel.markSelectedIssuesAsRead()
                        },
                        onMarkUnreadClick = {
                            viewModel.markSelectedIssuesAsUnread()
                        }
                    )
                }

                isSearchActive -> {
                    ComicReadingListSearchHeader(
                        query = searchQuery,
                        onQueryChange = { newQuery ->
                            searchQuery = newQuery
                        },
                        onCloseClick = {
                            searchQuery = ""
                            isSearchActive = false
                            keyboardController?.hide()
                        },
                        focusRequester = searchFocusRequester
                    )
                }

                else -> {
                    ComicReadingListDetailHeader(
                        title =
                            readingList?.title
                                ?: "Reading list",
                        onBackClick =
                            onBackClick,
                        onSearchClick = {
                            isSearchActive = true
                        },
                        onMenuClick = {
                            listMenuExpanded = true
                        },
                        menuContent = {
                            ComicDropdownMenu(
                                expanded = listMenuExpanded,
                                onDismissRequest = {
                                    listMenuExpanded = false
                                }
                            ) {
                                if (readingList?.source == ReadingListSource.USER) {
                                    ComicDropdownMenuItem(
                                        text = "Edit reading list",
                                        onClick = {
                                            listMenuExpanded = false
                                            editReadingListTitle =
                                                readingList?.title.orEmpty()
                                            editReadingListDescription =
                                                readingList?.description.orEmpty()
                                            showEditReadingListDialog = true
                                        }
                                    )
                                }

                                if (readingList?.source == ReadingListSource.USER) {
                                    ComicDropdownMenuItem(
                                        text = "Add section",
                                        onClick = {
                                            listMenuExpanded = false
                                            showCreateSectionDialog = true
                                        }
                                    )
                                }

                                ComicDropdownMenuItem(
                                    text = "Duplicate reading list",
                                    enabled = readingList != null,
                                    onClick = {
                                        listMenuExpanded = false
                                        viewModel.duplicateReadingList()
                                    }
                                )

                                ComicDropdownMenuItem(
                                    text = "Export reading list",
                                    enabled = readingList != null,
                                    onClick = {
                                        val currentReadingList = readingList
                                            ?: return@ComicDropdownMenuItem

                                        listMenuExpanded = false

                                        onExportReadingListClick(
                                            currentReadingList.id,
                                            currentReadingList.title
                                        )
                                    }
                                )

                                if (readingList?.source == ReadingListSource.USER) {
                                    ComicDropdownMenuItem(
                                        text = "Delete reading list",
                                        destructive = true,
                                        onClick = {
                                            listMenuExpanded = false
                                            showDeleteReadingListDialog = true
                                        }
                                    )
                                }

                                ComicDropdownMenuItem(
                                    text = "Mark all as read",
                                    enabled = hasUnreadIssues,
                                    onClick = {
                                        listMenuExpanded = false
                                        viewModel.markAllAsRead()
                                    }
                                )

                                ComicDropdownMenuItem(
                                    text = "Reset reading progress",
                                    enabled = hasAnyProgress,
                                    onClick = {
                                        listMenuExpanded = false
                                        showResetProgressDialog = true
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    )
    { innerPadding: PaddingValues ->

        val currentReadingList =
            readingList

        if (currentReadingList == null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment =
                    Alignment.Center
            ) {
                Text("Loading...")
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                state =
                    listState,
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    )
            ) {
                item(
                    key = "reading-list-hero"
                ) {
                    ComicReadingListDetailHero(
                        description =
                            currentReadingList
                                .description,
                        coverUrl =
                            issues
                                .firstOrNull()
                                ?.coverUrl,
                        modifier =
                            Modifier.padding(
                                top = 16.dp,
                                bottom = 12.dp
                            )
                    )
                }

                stickyHeader {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    ComicPaper
                                )
                                .onGloballyPositioned { coordinates ->
                                    stickyControlsHeightPx =
                                        coordinates.size.height
                                }
                                .padding(
                                    top = 8.dp,
                                    bottom = 12.dp
                                )
                    ) {
                        ComicReadingListDetailControls(
                            readCount =
                                readCount,
                            totalCount =
                                totalCount,
                            progress =
                                progress,
                            visibleIssueCount =
                                visibleIssues.size,
                            showFilteredCount =
                                isSearching ||
                                        hasActiveFilters,
                            activeFilterCount =
                                activeFilterCount,
                            showJumpToCurrent =
                                firstUnreadIndex >= 0 &&
                                        !isSearching &&
                                        !hasActiveFilters,
                            onFiltersClick = {
                                showFilterSheet = true
                            },
                            onJumpToCurrentClick = {
                                coroutineScope.launch {
                                    val targetIssue =
                                        issues[
                                            firstUnreadIndex
                                        ]

                                    val targetSectionId =
                                        targetIssue
                                            .sectionId

                                    val sectionWasCollapsed =
                                        targetSectionId !=
                                                null &&
                                                collapsedSectionIds
                                                    .contains(
                                                        targetSectionId
                                                    )

                                    if (
                                        targetSectionId !=
                                        null &&
                                        sectionWasCollapsed
                                    ) {
                                        viewModel
                                            .expandSection(
                                                targetSectionId
                                            )

                                        withFrameNanos { }
                                    }

                                    listState
                                        .animateScrollToItem(
                                            index = firstUnreadIndex + 2,
                                            scrollOffset = -stickyControlsHeightPx
                                        )
                                }
                            }
                        )
                    }
                }

                if (issues.isEmpty()) {
                    item {
                        Text(
                            text =
                                "No issues in this reading list",
                            modifier =
                                Modifier.padding(
                                    top = 8.dp
                                )
                        )
                    }
                } else if (
                    (isSearching ||
                            hasActiveFilters) &&
                    visibleIssues.isEmpty()
                ) {
                    item {
                        Text(
                            text =
                                if (isSearching) {
                                    "No issues match " +
                                            "\"$trimmedSearchQuery\" " +
                                            "with the current filters."
                                } else {
                                    "No issues match the current filters."
                                },
                            modifier =
                                Modifier.padding(
                                    top = 8.dp
                                )
                        )
                    }
                } else {
                    itemsIndexed(
                        items =
                            visibleIssues,
                        key = { _, issue ->
                            issue.readingListItemId
                        }
                    ) { index, issue ->

                        val previousSectionId =
                            if (index > 0) {
                                visibleIssues[
                                    index - 1
                                ].sectionId
                            } else {
                                null
                            }

                        val sectionId =
                            issue.sectionId

                        val isFirstIssueInSection =
                            sectionId != null &&
                                    sectionId !=
                                    previousSectionId

                        val isSectionCollapsed =
                            !isSearching &&
                                    !hasActiveFilters &&
                                    sectionId != null &&
                                    collapsedSectionIds
                                        .contains(
                                            sectionId
                                        )

                        if (isFirstIssueInSection) {
                            if (!isSectionCollapsed) {
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            val sectionIssueCount =
                                visibleIssues.count {
                                        visibleIssue ->
                                    visibleIssue.sectionId ==
                                            sectionId
                                }

                            ReadingListSectionHeader(
                                title =
                                    issue.sectionTitle
                                        ?: "Section",
                                issueCount =
                                    sectionIssueCount,
                                isCollapsed =
                                    isSectionCollapsed,
                                canCollapse = !isSearching && !hasActiveFilters,
                                onToggleCollapsed = {
                                    viewModel
                                        .toggleSectionCollapsed(
                                            sectionId
                                        )
                                },
                                modifier =
                                    Modifier.padding(
                                        top =
                                            if (index == 0) {
                                                2.dp
                                            } else {
                                                8.dp
                                            },
                                        bottom =
                                            if (isSectionCollapsed) {
                                                4.dp
                                            } else {
                                                2.dp
                                            }
                                    )
                            )
                        }

                        if (!isSectionCollapsed) {
                            val fullIssueIndex =
                                issues.indexOfFirst {
                                        fullIssue ->
                                    fullIssue
                                        .readingListItemId ==
                                            issue
                                                .readingListItemId
                                }

                            val canReorder =
                                readingList?.source ==
                                        ReadingListSource.USER

                            val canMoveUp =
                                canReorder &&
                                        fullIssueIndex > 0 &&
                                        issues[
                                            fullIssueIndex - 1
                                        ].sectionId ==
                                        issue.sectionId

                            val canMoveDown =
                                canReorder &&
                                        fullIssueIndex >= 0 &&
                                        fullIssueIndex <
                                        issues.lastIndex &&
                                        issues[
                                            fullIssueIndex + 1
                                        ].sectionId ==
                                        issue.sectionId

                            ReadingListIssueRow(
                                issue =
                                    issue,
                                isSelectionMode =
                                    isSelectionMode,
                                isSelected =
                                    issue
                                        .readingListItemId in
                                            selectedReadingListItemIds,
                                onSelectionToggle = {
                                    viewModel
                                        .toggleIssueSelection(
                                            issue
                                        )
                                },
                                onIssueClick = {
                                    onIssueClick(
                                        issue.issueId
                                    )
                                },
                                onToggleRead = {
                                    viewModel
                                        .toggleIssueRead(
                                            issue = issue
                                        )
                                },
                                onMarkAsReading = {
                                    viewModel
                                        .markIssueAsReading(
                                            issue = issue
                                        )
                                },
                                onMarkAllBeforeRead = {
                                    viewModel
                                        .markAllBeforeAsRead(
                                            selectedIssue =
                                                issue
                                        )
                                },
                                canReorder =
                                    canReorder,
                                canMoveUp =
                                    canMoveUp,
                                canMoveDown =
                                    canMoveDown,
                                onMoveUp = {
                                    viewModel.moveIssueUp(
                                        issue = issue
                                    )
                                },
                                onMoveDown = {
                                    viewModel.moveIssueDown(
                                        issue = issue
                                    )
                                },
                                canChangeSection =
                                    canReorder &&
                                            sections.isNotEmpty(),
                                onMoveToSectionRequest = {
                                    issuePendingSectionMove =
                                        issue
                                },
                                canRemove =
                                    readingList?.source ==
                                            ReadingListSource.USER,
                                onRemoveRequest = {
                                    issuePendingRemoval =
                                        issue
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showFilterSheet = false
            },
            sheetState = filterSheetState,
            containerColor = ComicPaper,
            dragHandle = null
        ) {
            ComicReadingListFilterSheetContent(
                readingStatusFilter = readingStatusFilter,
                onReadingStatusFilterChange = { newFilter ->
                    readingStatusFilter = newFilter
                },
                requiredFilter = requiredFilter,
                onRequiredFilterChange = { newFilter ->
                    requiredFilter = newFilter
                },
                selectedSeriesLabel =
                    selectedSeries
                        ?.displayName
                        ?: "All series",
                seriesOptions =
                    seriesOptions.map { option ->
                        ComicReadingListFilterSeriesOption(
                            key = option.key,
                            label = option.displayName
                        )
                    },
                onSeriesSelected = { seriesKey ->
                    selectedSeriesKey = seriesKey
                },
                activeFilterCount = activeFilterCount,
                visibleIssueCount = visibleIssues.size,
                onClearAll = {
                    readingStatusFilter = "ALL"
                    requiredFilter = "ALL"
                    selectedSeriesKey = null
                },
                onDone = {
                    showFilterSheet = false
                }
            )
        }
    }

    if (showResetProgressDialog) {
        ComicConfirmationDialog(
            title = "Reset Progress?",
            message =
                "This will mark every issue in this " +
                "reading list as unread. Issue " +
                "progress is shared across reading " +
                "lists, so these issues will also " +
                "appear unread in any other lists " +
                "that contain them.",
            confirmText = "Reset",
            destructive = true,
            onConfirm = {
                showResetProgressDialog = false
                viewModel.resetProgress()
            },
            onDismiss = {
                showResetProgressDialog = false
            }
        )
    }

    if (showEditReadingListDialog) {
        ComicFormDialog(
            title = "Edit Reading List",
            primaryLabel = "Title",
            primaryValue = editReadingListTitle,
            onPrimaryValueChange = {
                editReadingListTitle = it
            },
            secondaryLabel = "Description (optional)",
            secondaryValue = editReadingListDescription,
            onSecondaryValueChange = {
                editReadingListDescription = it
            },
            confirmText = "Save",
            confirmEnabled = editReadingListTitle.trim().isNotEmpty(),
            onConfirm = {
                viewModel.updateReadingListDetails(
                    title = editReadingListTitle,
                    description = editReadingListDescription.takeIf { it.isNotBlank() }
                )

                showEditReadingListDialog = false
            },
            onDismiss = {
                showEditReadingListDialog = false
            }
        )
    }

    if (showCreateSectionDialog) {
        ComicFormDialog(
            title = "Add Section",
            primaryLabel = "Section title",
            primaryValue = newSectionTitle,
            onPrimaryValueChange = {
                newSectionTitle = it
            },
            secondaryLabel = "Description (optional)",
            secondaryValue = newSectionDescription,
            onSecondaryValueChange = {
                newSectionDescription = it
            },
            confirmText = "Add",
            confirmEnabled = newSectionTitle.trim().isNotEmpty(),
            onConfirm = {
                viewModel.createSection(
                    title = newSectionTitle,
                    description = newSectionDescription.takeIf { it.isNotBlank() }
                )

                showCreateSectionDialog = false
                newSectionTitle = ""
                newSectionDescription = ""
            },
            onDismiss = {
                showCreateSectionDialog = false
                newSectionTitle = ""
                newSectionDescription = ""
            }
        )
    }

    if (showDeleteReadingListDialog) {
        ComicConfirmationDialog(
            title = "Delete Reading List?",
            message =
                "Delete \"${readingList?.title.orEmpty()}\"? " +
                "This will permanently delete the " +
                "reading list and its sections. " +
                "Your comics and reading progress " +
                "will not be deleted.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = {
                showDeleteReadingListDialog = false
                viewModel.deleteReadingList()
            },
            onDismiss = {
                showDeleteReadingListDialog = false
            }
        )
    }

    issuePendingSectionMove?.let { issue ->
        ComicSectionPickerDialog(
            issueLabel =
                "${issue.seriesTitle} " +
                "#${issue.issueNumber}",
            currentSectionId = issue.sectionId,
            sections =
                sections.map { section ->
                    ComicSectionPickerOption(
                        id = section.id,
                        title = section.title
                    )
                },
            onSectionSelected = { targetSectionId ->
                issuePendingSectionMove = null

                viewModel.moveIssueToSection(
                    issue = issue,
                    targetSectionId = targetSectionId
                )
            },
            onDismiss = {
                issuePendingSectionMove = null
            }
        )
    }

    issuePendingRemoval?.let { issue ->
        ComicConfirmationDialog(
            title = "Remove Issue?",
            message =
                "Remove " +
                "${issue.seriesTitle} " +
                "#${issue.issueNumber} " +
                "from this reading list? " +
                "The issue itself and its " +
                "reading status will not " +
                "be deleted.",
            confirmText = "Remove",
            destructive = true,
            onConfirm = {
                issuePendingRemoval = null
                viewModel.removeIssue(issue)
            },
            onDismiss = {
                issuePendingRemoval = null
            }
        )
    }
}

@Composable
private fun ReadingListSectionHeader(
    title: String,
    issueCount: Int,
    isCollapsed: Boolean,
    canCollapse: Boolean,
    onToggleCollapsed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val outlineWidth =
        with (density) {
            7.dp.toPx()
        }

    val titleStyle =
        TextStyle(
            fontFamily = LilitaOneFontFamily,
            fontSize = 20.sp,
            lineHeight = 22.sp,
            textGeometricTransform =
                ComicAccentTextTransform
        )

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable(
                    enabled = canCollapse,
                    onClick = onToggleCollapsed
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(42.dp),
            contentAlignment =
                Alignment.CenterStart
        ) {
            Box(
                modifier =
                    Modifier
                        .wrapContentWidth()
                        .height(42.dp)
            ) {
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .offset(
                                x = 3.dp,
                                y = 4.dp
                            )
                            .background(
                                color = ComicInk,
                                shape = ComicSlantedShape
                            )
                )

                Box(
                    modifier =
                        Modifier
                            .height(42.dp)
                            .background(
                                color = ComicRed,
                                shape = ComicSlantedShape
                            )
                            .border(
                                width = 2.dp,
                                color = ComicInk,
                                shape = ComicSlantedShape
                            )
                            .padding(
                                horizontal = 14.dp
                            ),
                    contentAlignment =
                        Alignment.CenterStart
                ) {
                    Text(
                        text = title.uppercase(),
                        modifier =
                            Modifier
                                .offset(y = 1.dp)
                                .clearAndSetSemantics { },
                        autoSize =
                            TextAutoSize.StepBased(
                                minFontSize = 14.sp,
                                maxFontSize = 20.sp,
                                stepSize = 0.5.sp
                            ),
                        style =
                            titleStyle.copy(
                                drawStyle =
                                    Stroke(
                                        width = outlineWidth,
                                        join = StrokeJoin.Round
                                    )
                            ),
                        color = ComicInk,
                        maxLines = 1
                    )

                    Text(
                        text = title.uppercase(),
                        modifier =
                            Modifier.offset(y = 1.dp),
                        autoSize =
                            TextAutoSize.StepBased(
                                minFontSize = 14.sp,
                                maxFontSize = 20.sp,
                                stepSize = 0.5.sp
                            ),
                        style = titleStyle,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }

        Text(
            text =
                if (issueCount == 1) {
                    "1 issue"
                } else {
                    "$issueCount issues"
                },
            modifier =
                Modifier.padding(
                    start = 8.dp
                ),
            style = MaterialTheme.typography.bodySmall
                .copy(fontWeight = FontWeight.SemiBold),
            color = ComicInk
        )

        if (canCollapse) {
            IconButton(
                onClick = onToggleCollapsed,
                modifier = Modifier.size(40.dp)
            ) {
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
                        },
                    tint = ComicInk,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun ReadingListIssueRow(
    issue: ReadingListIssue,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onSelectionToggle: () -> Unit,
    onIssueClick: () -> Unit,
    onToggleRead: () -> Unit,
    onMarkAsReading: () -> Unit,
    onMarkAllBeforeRead: () -> Unit,
    canReorder: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canChangeSection: Boolean,
    onMoveToSectionRequest: () -> Unit,
    canRemove: Boolean,
    onRemoveRequest: () -> Unit
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
                if (isSelectionMode) {
                    isSelected
                } else {
                    issue.readingStatus == ReadingStatus.READ
                },
            onCheckedChange = {
                if (isSelectionMode) {
                    onSelectionToggle()
                } else {
                    onToggleRead()
                }
            }
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) {
                            onSelectionToggle()
                        } else {
                            onIssueClick()
                        }
                    },
                    onLongClick = {
                        onSelectionToggle()
                    }
                )
                .padding(
                    vertical = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val coverShape = RoundedCornerShape(4.dp)

            ComicCoverImage(
                coverUrl = issue.coverUrl,
                contentDescription =
                    "${issue.seriesTitle} #${issue.issueNumber} cover",
                modifier = Modifier
                    .width(62.dp)
                    .aspectRatio(2f / 3f),
                placeholderText = "No Cover",
                shape = coverShape,
                borderColor = ComicInk
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text =
                        "${issue.seriesTitle} #${issue.issueNumber}",
                    style =
                        MaterialTheme.typography.titleMedium
                            .copy(
                                fontSize = 17.sp,
                                lineHeight = 19.sp
                            ),
                    color = ComicInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                issue.issueTitle?.takeIf {
                    it.isNotBlank()
                }
                ?.let { issueTitle ->
                    Text(
                        text = issueTitle,
                        style =
                            MaterialTheme.typography.bodyMedium
                                .copy(
                                    fontSize = 15.sp,
                                    lineHeight = 17.sp
                                ),
                        color = ComicInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                issue.publicationDate
                    ?.let { publicationDate ->
                        Text(
                            text = formatPublicationDate(publicationDate),
                            style = MaterialTheme.typography.bodySmall
                                .copy(
                                    fontSize = 13.sp,
                                    lineHeight = 15.sp
                                ),
                            color = ComicInk
                        )
                    }
            }
        }

        if (!isSelectionMode) {
            Column {
                IconButton(
                    onClick = {
                        menuExpanded = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = ComicInk
                    )
                }

                ComicDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = {
                        menuExpanded = false
                    }
                ) {
                    ComicDropdownMenuItem(
                        text = "Mark as reading",
                        onClick = {
                            menuExpanded = false
                            onMarkAsReading()
                        }
                    )

                    if (issue.position > 1) {
                        ComicDropdownMenuItem(
                            text = "Mark all before as read",
                            onClick = {
                                menuExpanded = false
                                onMarkAllBeforeRead()
                            }
                        )
                    }

                    if (canReorder) {
                        ComicDropdownMenuItem(
                            text = "Move up",
                            enabled = canMoveUp,
                            onClick = {
                                menuExpanded = false
                                onMoveUp()
                            }
                        )

                        ComicDropdownMenuItem(
                            text = "Move down",
                            enabled = canMoveDown,
                            onClick = {
                                menuExpanded = false
                                onMoveDown()
                            }
                        )
                    }

                    if (canChangeSection) {
                        ComicDropdownMenuItem(
                            text = "Move to section",
                            onClick = {
                                menuExpanded = false
                                onMoveToSectionRequest()
                            }
                        )
                    }

                    if (canRemove) {
                        ComicDropdownMenuItem(
                            text = "Remove from reading list",
                            onClick = {
                                menuExpanded = false
                                onRemoveRequest()
                            }
                        )
                    }
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
        issue.sectionTitle?.let { title ->
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