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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSource
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListStyle
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListIssue
import com.aschlus.comicreadingcompanion.ui.component.ComicAddIssuesTrigger
import com.aschlus.comicreadingcompanion.ui.component.ComicConfirmationDialog
import com.aschlus.comicreadingcompanion.ui.component.ComicCoverImage
import com.aschlus.comicreadingcompanion.ui.component.ComicDropdownMenu
import com.aschlus.comicreadingcompanion.ui.component.ComicDropdownMenuItem
import com.aschlus.comicreadingcompanion.ui.component.ComicEditReadingListHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicEmptyState
import com.aschlus.comicreadingcompanion.ui.component.ComicFormDialog
import com.aschlus.comicreadingcompanion.ui.component.ComicFormDropdownField
import com.aschlus.comicreadingcompanion.ui.component.ComicFormTextField
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicPendingReadingListIssueRow
import com.aschlus.comicreadingcompanion.ui.component.ComicReadingListDetailControls
import com.aschlus.comicreadingcompanion.ui.component.ComicReadingListDetailHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicReadingListDetailHero
import com.aschlus.comicreadingcompanion.ui.component.ComicReadingListFilterSeriesOption
import com.aschlus.comicreadingcompanion.ui.component.ComicReadingListFilterSheetContent
import com.aschlus.comicreadingcompanion.ui.component.ComicReadingListSearchHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicReadingListSelectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicReadingListStylePicker
import com.aschlus.comicreadingcompanion.ui.component.ComicSectionPickerDialog
import com.aschlus.comicreadingcompanion.ui.component.ComicSectionPickerOption
import com.aschlus.comicreadingcompanion.ui.component.ComicSlantedShape
import com.aschlus.comicreadingcompanion.ui.component.ComicWideActionButton
import com.aschlus.comicreadingcompanion.ui.theme.ComicAccentTextTransform
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardBorderWidth
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShadowOffsetX
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShadowOffsetY
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingLg
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingMd
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingSm
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingXs
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import com.aschlus.comicreadingcompanion.ui.theme.LilitaOneFontFamily
import com.aschlus.comicreadingcompanion.ui.viewmodel.PendingReadingListIssue
import com.aschlus.comicreadingcompanion.ui.viewmodel.ReadingListDetailViewModel
import com.aschlus.comicreadingcompanion.util.formatPublicationDate
import kotlinx.coroutines.launch

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

    val publisherName by
            viewModel.publisherName.collectAsState()

    val continuityName by
            viewModel.continuityName.collectAsState()

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

    val editAddToListQuery by
        viewModel.editAddToListQuery.collectAsState()

    val editAddToListFilter by
        viewModel.editAddToListFilter.collectAsState()

    val editAddToListSeriesResults by
        viewModel.editAddToListSeriesResults.collectAsState()

    val editAddToListIssueResults by
        viewModel.editAddToListIssueResults.collectAsState()

    val editAddToListDraftIssues by
        viewModel.editAddToListDraftIssues.collectAsState()

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

    var showJumpToPositionDialog by remember(readingListId) {
        mutableStateOf(false)
    }

    var jumpToPositionInput by remember(readingListId) {
        mutableStateOf("")
    }

    var editMode by remember(
        readingListId
    ) {
        mutableStateOf(false)
    }

    var editAddToListMode by remember(
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

    var editReadingListStyle by remember(
        readingListId
    ) {
        mutableStateOf(ReadingListStyle.GREEN)
    }

    var editReadingListIssues by remember(
        readingListId
    ) {
        mutableStateOf<List<PendingReadingListIssue>>(emptyList())
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

    var issuePendingPositionMove by remember(
        readingListId
    ) {
        mutableStateOf<ReadingListIssue?>(null)
    }

    var positionMoveInput by remember(readingListId) {
        mutableStateOf("")
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
        ).count { it }

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

    if (editAddToListMode) {
        AddToReadingListScreen(
            query =
                editAddToListQuery,
            filter =
                editAddToListFilter,
            seriesResults =
                editAddToListSeriesResults,
            issueResults =
                editAddToListIssueResults,
            draftIssues =
                editAddToListDraftIssues,
            onQueryChange =
                viewModel::
                updateEditAddToListQuery,
            onFilterSelected =
                viewModel::
                selectEditAddToListFilter,
            onSeriesClick =
                viewModel::
                toggleEditAddToListSeries,
            onIssueClick =
                viewModel::
                toggleEditAddToListIssue,
            onBackClick = {
                viewModel
                    .cancelEditAddToListSession()

                editAddToListMode =
                    false
            },
            onDoneClick = {
                editReadingListIssues =
                    viewModel
                        .applyEditAddToListSession()

                editAddToListMode =
                    false
            }
        )

        return
    }

    if (editMode) {
        val currentList = readingList

        if (currentList != null) {
            EditReadingListContent(
                title = editReadingListTitle,
                description = editReadingListDescription,
                style = editReadingListStyle,
                publisherName = publisherName,
                continuityName = continuityName,
                issues = editReadingListIssues,
                onTitleChange = {
                    editReadingListTitle = it
                },
                onDescriptionChange = {
                    editReadingListDescription = it
                },
                onStyleChange = {
                    editReadingListStyle = it
                },
                onBackClick = {
                    listMenuExpanded = false
                    editMode = false
                },
                onCancelClick = {
                    listMenuExpanded = false
                    editMode = false
                },
                onSaveClick = {
                    listMenuExpanded = false
                    viewModel.saveReadingListEdits(
                        title = editReadingListTitle,
                        description = editReadingListDescription
                            .takeIf { it.isNotBlank() },
                        style = editReadingListStyle,
                        issues = editReadingListIssues
                    )

                    editMode = false
                },
                onMoveIssue = { issueId, offset ->
                    val currentIndex =
                        editReadingListIssues
                            .indexOfFirst { it.issueId == issueId }

                    if (currentIndex != -1) {
                        val targetIndex = currentIndex + offset

                        if (targetIndex in editReadingListIssues.indices) {
                            val reordered = editReadingListIssues.toMutableList()
                            val moved = reordered.removeAt(currentIndex)
                            reordered.add(targetIndex, moved)
                            editReadingListIssues = reordered
                        }
                    }
                },
                onRemoveIssue = { issueId ->
                    editReadingListIssues =
                        editReadingListIssues
                            .filterNot { it.issueId == issueId }
                },
                onAddIssuesClick = {
                    viewModel
                        .beginEditAddToListSession(
                            editReadingListIssues
                        )

                    editAddToListMode =
                        true
                },
            )

            return
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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

                                            val currentList = readingList
                                                ?: return@ComicDropdownMenuItem
                                            editReadingListTitle =
                                                currentList.title
                                            editReadingListDescription =
                                                currentList.description.orEmpty()
                                            editReadingListStyle =
                                                currentList.style

                                            editReadingListIssues =
                                                issues.map { issue ->
                                                    PendingReadingListIssue(
                                                        issueId = issue.issueId,
                                                        seriesId = issue.seriesId,
                                                        seriesTitle = issue.seriesTitle,
                                                        issueNumber = issue.issueNumber,
                                                        issueTitle = issue.issueTitle,
                                                        publicationDate = issue.publicationDate,
                                                        coverUrl = issue.coverUrl,
                                                        readingListItemId = issue.readingListItemId,
                                                        sectionId = issue.sectionId
                                                    )
                                                }
                                            editMode = true
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
                                    text = "Jump to position",
                                    enabled = issues.isNotEmpty() && collapsedSectionsLoaded,
                                    onClick = {
                                        listMenuExpanded = false
                                        jumpToPositionInput = ""
                                        showJumpToPositionDialog = true
                                    }
                                )

                                ComicDropdownMenuItem(
                                    text = "Scroll to top",
                                    enabled = listState.firstVisibleItemIndex > 0 ||
                                        listState.firstVisibleItemScrollOffset > 0,
                                    onClick = {
                                        listMenuExpanded = false

                                        coroutineScope.launch {
                                            listState.animateScrollToItem(
                                                index = 0
                                            )
                                        }
                                    }
                                )

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
            val artworkCoverUrls =
                remember(issues) {
                    issues
                        .asSequence()
                        .mapNotNull { it.coverUrl }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .take(4)
                        .toList()
                }

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                state =
                    listState,
                contentPadding =
                    PaddingValues(
                        start = ComicSpacingLg,
                        end = ComicSpacingLg,
                        bottom = ComicSpacingLg
                    )
            ) {
                item(
                    key = "reading-list-hero"
                ) {
                    ComicReadingListDetailHero(
                        title = currentReadingList.title,
                        description = currentReadingList.description,
                        coverUrls = artworkCoverUrls,
                        readingListId = currentReadingList.id,
                        style = currentReadingList.style,
                        modifier =
                            Modifier.padding(
                                top = ComicSpacingLg,
                                bottom = ComicSpacingMd
                            )
                    )
                }

                stickyHeader {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.background
                                )
                                .onGloballyPositioned { coordinates ->
                                    stickyControlsHeightPx =
                                        coordinates.size.height
                                }
                                .padding(
                                    top = ComicSpacingSm,
                                    bottom = ComicSpacingMd
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
                        ComicEmptyState(
                            message = "No issues in this reading list",
                            modifier =
                                Modifier.padding(
                                    top = ComicSpacingSm
                                )
                        )
                    }
                } else if (
                    (isSearching ||
                            hasActiveFilters) &&
                    visibleIssues.isEmpty()
                ) {
                    item {
                        ComicEmptyState(
                            message =
                                if (isSearching) {
                                    "No issues match " +
                                            "\"$trimmedSearchQuery\" " +
                                            "with the current filters."
                                } else {
                                    "No issues match the current filters."
                                },
                            modifier =
                                Modifier.padding(
                                    top = ComicSpacingSm
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

                        val nextIssue = visibleIssues.getOrNull(index + 1)

                        val isLastIssueInSection =
                            sectionId != null &&
                                nextIssue?.sectionId != sectionId

                        val shouldShowSectionBottomDivider =
                            isLastIssueInSection &&
                                (nextIssue?.sectionId == null)

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

                            val sectionFirstIssueIndex =
                                issues.indexOfFirst { it.sectionId == sectionId }

                            val sectionLastIssueIndex =
                                issues.indexOfLast { it.sectionId == sectionId }

                            val canReorderSection =
                                readingList?.source == ReadingListSource.USER &&
                                    !isSearching &&
                                    !hasActiveFilters

                            val canMoveSectionUp =
                                canReorderSection && sectionFirstIssueIndex > 0

                            val canMoveSectionDown =
                                canReorderSection &&
                                    sectionLastIssueIndex >= 0 &&
                                    sectionLastIssueIndex < issues.lastIndex

                            ReadingListSectionHeader(
                                title =
                                    issue.sectionTitle
                                        ?: "Section",
                                issueCount =
                                    sectionIssueCount,
                                isCollapsed =
                                    isSectionCollapsed,
                                canCollapse = !isSearching && !hasActiveFilters,
                                isSelectionMode = isSelectionMode,
                                canMoveSectionUp = canMoveSectionUp,
                                canMoveSectionDown = canMoveSectionDown,
                                onMoveSectionUp = {
                                    sectionId?.let(viewModel::moveSectionUp)
                                },
                                onMoveSectionDown = {
                                    sectionId?.let(viewModel::moveSectionDown)
                                },
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
                                                ComicSpacingSm
                                            },
                                        bottom =
                                            if (isSectionCollapsed) {
                                                ComicSpacingXs
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

                            val canMoveUp = canReorder && fullIssueIndex > 0

                            val canMoveDown =
                                canReorder &&
                                    fullIssueIndex >= 0 &&
                                    fullIssueIndex < issues.lastIndex

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
                                onMoveToPositionRequest = {
                                    val sectionIssues =
                                        issues.filter { it.sectionId == issue.sectionId }

                                    val currentPosition =
                                        sectionIssues.indexOfFirst {
                                            it.readingListItemId == issue.readingListItemId
                                        } + 1

                                    positionMoveInput = currentPosition.toString()
                                    issuePendingPositionMove = issue
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

                        if (shouldShowSectionBottomDivider) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = ComicSpacingSm,
                                        vertical = ComicSpacingXs
                                    )
                                    .height(1.dp)
                                    .background(
                                        MaterialTheme.colorScheme.outlineVariant
                                    )
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
            containerColor = MaterialTheme.colorScheme.surface,
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

    issuePendingPositionMove?.let { issue ->
        val sectionIssues =
            issues.filter { it.sectionId == issue.sectionId }

        val targetPosition = positionMoveInput.toIntOrNull()

        ComicFormDialog(
            title = "Move to Position",
            primaryLabel = "Position (1-${sectionIssues.size})",
            primaryValue = positionMoveInput,
            onPrimaryValueChange = { value ->
                if (value.all { it.isDigit() }) {
                    positionMoveInput = value
                }
            },
            secondaryLabel = "",
            secondaryValue = "",
            onSecondaryValueChange = {},
            confirmText = "Move",
            confirmEnabled = targetPosition != null &&
                targetPosition in 1..sectionIssues.size,
            onConfirm = {
                targetPosition?.let { position ->
                    issuePendingPositionMove = null

                    viewModel.moveIssueToPosition(
                        issue = issue,
                        position = position
                    )
                }
            },
            onDismiss = {
                issuePendingPositionMove = null
                positionMoveInput = ""
            },
            showSecondaryField = false,
            primaryKeyboardType = KeyboardType.Number
        )
    }

    if (showJumpToPositionDialog) {
        val targetPosition = jumpToPositionInput.toIntOrNull()

        ComicFormDialog(
            title = "Jump to Position",
            primaryLabel = "Reading-order position (1-${issues.size})",
            primaryValue = jumpToPositionInput,
            onPrimaryValueChange = { value ->
                if (value.all { it.isDigit() }) {
                    jumpToPositionInput = value
                }
            },
            secondaryLabel = "",
            secondaryValue = "",
            onSecondaryValueChange = {},
            confirmText = "Jump",
            confirmEnabled = targetPosition != null &&
                targetPosition in 1..issues.size,
            onConfirm = {
                targetPosition?.let { requestedPosition ->
                    val targetIndex = requestedPosition - 1
                    val targetIssue = issues.getOrNull(targetIndex)

                    if (targetIssue != null) {
                        showJumpToPositionDialog = false
                        jumpToPositionInput = ""

                        searchQuery = ""
                        isSearchActive = false
                        keyboardController?.hide()
                        readingStatusFilter = "ALL"
                        requiredFilter = "ALL"
                        selectedSeriesKey = null

                        coroutineScope.launch {
                            withFrameNanos {  }

                            val targetSectionId = targetIssue.sectionId

                            if (
                                targetSectionId != null &&
                                collapsedSectionIds.contains(targetSectionId)
                            ) {
                                viewModel.expandSection(targetSectionId)
                                withFrameNanos {  }
                            }

                            listState.animateScrollToItem(
                                index = targetIndex + 2,
                                scrollOffset =  -stickyControlsHeightPx
                            )
                        }
                    }
                }
            },
            onDismiss = {
                showJumpToPositionDialog = false
                jumpToPositionInput = ""
            },
            showSecondaryField = false,
            primaryKeyboardType = KeyboardType.Number
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
    isSelectionMode: Boolean,
    canMoveSectionUp: Boolean,
    canMoveSectionDown: Boolean,
    onMoveSectionUp: () -> Unit,
    onMoveSectionDown: () -> Unit,
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

    var sectionMenuExpanded by remember {
        mutableStateOf(false)
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable(
                    enabled = canCollapse,
                    role = Role.Button,
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
                                ComicCardShadowOffsetX,
                                ComicCardShadowOffsetY
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
                                width = ComicCardBorderWidth,
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
                    start = ComicSpacingSm
                ),
            style = MaterialTheme.typography.bodySmall
                .copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )

        if (canCollapse) {
            IconButton(
                onClick = onToggleCollapsed,
                modifier = Modifier.size(48.dp)
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
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }

        if (!isSelectionMode) {
            Box {
                IconButton(
                    onClick = {
                        sectionMenuExpanded = true
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Section options",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                ComicDropdownMenu(
                    expanded = sectionMenuExpanded,
                    onDismissRequest = {
                        sectionMenuExpanded = false
                    }
                ) {
                    ComicDropdownMenuItem(
                        text = "Move section up",
                        enabled = canMoveSectionUp,
                        onClick = {
                            sectionMenuExpanded = false
                            onMoveSectionUp()
                        }
                    )

                    ComicDropdownMenuItem(
                        text = "Move section down",
                        enabled = canMoveSectionDown,
                        onClick = {
                            sectionMenuExpanded = false
                            onMoveSectionDown()
                        }
                    )
                }
            }
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
    onMoveToPositionRequest: () -> Unit,
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
                    role = Role.Button,
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
                modifier = Modifier.width(ComicSpacingMd)
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
                    color = MaterialTheme.colorScheme.onBackground,
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
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                issue.publicationDate
                    ?.takeIf { it.isNotBlank() }
                    ?.let { publicationDate ->
                        Text(
                            text = formatPublicationDate(publicationDate),
                            style = MaterialTheme.typography.bodySmall
                                .copy(
                                    fontSize = 13.sp,
                                    lineHeight = 15.sp
                                ),
                            color = MaterialTheme.colorScheme.onBackground
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
                        tint = MaterialTheme.colorScheme.onBackground
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

                        ComicDropdownMenuItem(
                            text = "Move to position…",
                            onClick = {
                                menuExpanded = false
                                onMoveToPositionRequest()
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

@Composable
private fun EditReadingListContent(
    title: String,
    description: String,
    style: ReadingListStyle,
    publisherName: String,
    continuityName: String,
    issues: List<PendingReadingListIssue>,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onStyleChange: (ReadingListStyle) -> Unit,
    onBackClick: () -> Unit,
    onCancelClick: () -> Unit,
    onSaveClick: () -> Unit,
    onMoveIssue: (issueId: Long, offset: Int) -> Unit,
    onRemoveIssue: (Long) -> Unit,
    onAddIssuesClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ComicEditReadingListHeader(
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(ComicSpacingLg)
                    .verticalScroll(
                        rememberScrollState()
                    ),
            verticalArrangement =
                Arrangement.spacedBy(ComicSpacingLg)
        ) {
            ComicHomeSectionHeader(
                text = "LIST DETAILS"
            )

            ComicFormTextField(
                label = "TITLE",
                value = title,
                onValueChange = onTitleChange,
                placeholder = "Reading list title"
            )

            ComicFormTextField(
                label = "Description (optional)",
                value = description,
                onValueChange = onDescriptionChange,
                placeholder = "Add a short description",
                singleLine = false,
                minHeight = 96
            )

            ComicHomeSectionHeader(
                text = "LIST STYLE"
            )

            ComicReadingListStylePicker(
                selectedStyle = style,
                onStyleSeclected = onStyleChange
            )

            ComicHomeSectionHeader(
                text = "PUBLISHER & CONTINUITY"
            )

            ComicFormDropdownField(
                text = publisherName,
                expanded = false,
                onExpandedChange = {},
                enabled = false
            ) { }

            ComicFormDropdownField(
                text = continuityName,
                expanded = false,
                onExpandedChange = {},
                enabled = false
            ) { }

            Text(
                text = "Publisher and continuity are fixed for this list.",
                style = MaterialTheme.typography.bodySmall
            )

            ComicHomeSectionHeader(
                text = "ADD ISSUES OR SERIES"
            )

            ComicAddIssuesTrigger(
                onClick = onAddIssuesClick
            )

            if (issues.isNotEmpty()) {
                ComicHomeSectionHeader(
                    text = "ADDED TO LIST"
                )

                issues.forEachIndexed { index, issue ->
                    ComicPendingReadingListIssueRow(
                        issue = issue,
                        canMoveUp = index > 0 &&
                            issues[index - 1].sectionId == issue.sectionId,
                        canMoveDown = index < issues.lastIndex &&
                            issues[index + 1].sectionId == issue.sectionId,
                        onMoveUp = { onMoveIssue(issue.issueId, -1) },
                        onMoveDown = { onMoveIssue(issue.issueId, 1) },
                        onRemove = { onRemoveIssue(issue.issueId) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                ComicWideActionButton(
                    text = "CANCEL",
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = onCancelClick,
                    modifier = Modifier.weight(1f)
                )

                ComicWideActionButton(
                    text = "SAVE CHANGES",
                    backgroundColor = ComicYellow,
                    enabled = title.trim().isNotEmpty(),
                    onClick = onSaveClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}