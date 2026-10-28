package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSection
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSource
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListStyle
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListIssue
import com.aschlus.comicreadingcompanion.data.preferences.ReadingListUiPreferences
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.aschlus.comicreadingcompanion.data.database.models.IssueSearchResult
import com.aschlus.comicreadingcompanion.data.database.models.SeriesSearchResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.milliseconds

class ReadingListDetailViewModel(
    private val repository: ComicRepository,
    private val readingListUiPreferences: ReadingListUiPreferences
) : ViewModel() {

    private var issuesJob: Job? = null
    private var collapsedSectionsJob: Job? = null
    private var editAddToListSearchJob: Job? = null

    private var activeReadingListId: Long? = null

    private val _readingList =
        MutableStateFlow<ReadingList?>(null)

    val readingList: StateFlow<ReadingList?> =
        _readingList.asStateFlow()

    private val _sections =
        MutableStateFlow<List<ReadingListSection>>(emptyList())

    val sections: StateFlow<List<ReadingListSection>> = _sections.asStateFlow()

    private val _issues =
        MutableStateFlow<List<ReadingListIssue>>(emptyList())

    val issues: StateFlow<List<ReadingListIssue>> =
        _issues.asStateFlow()

    private val _publisherName =
        MutableStateFlow("")
    val publisherName: StateFlow<String> =
        _publisherName.asStateFlow()

    private val _continuityName =
        MutableStateFlow("")

    val continuityName: StateFlow<String> =
        _continuityName.asStateFlow()

    private val _selectedReadingListItemIds =
        MutableStateFlow<Set<Long>>(emptySet())

    val selectedReadingListItemIds: StateFlow<Set<Long>> = _selectedReadingListItemIds.asStateFlow()

    private val _readingListDeleted =
        MutableStateFlow(false)

    val readingListDeleted: StateFlow<Boolean> =
        _readingListDeleted.asStateFlow()

    private val _duplicatedReadingListId =
        MutableStateFlow<Long?>(null)

    val duplicatedReadingListId: StateFlow<Long?> =
        _duplicatedReadingListId.asStateFlow()

    private val _collapsedSectionIds =
        MutableStateFlow<Set<Long>>(emptySet())

    val collapsedSectionIds: StateFlow<Set<Long>> =
        _collapsedSectionIds.asStateFlow()

    private val _collapsedSectionsLoaded =
        MutableStateFlow(false)

    val collapsedSectionsLoaded: StateFlow<Boolean> =
        _collapsedSectionsLoaded.asStateFlow()

    private val _editAddToListDraftIssues =
        MutableStateFlow<List<PendingReadingListIssue>>(emptyList())

    val editAddToListDraftIssues: StateFlow<List<PendingReadingListIssue>> =
        _editAddToListDraftIssues.asStateFlow()

    private val _editAddToListQuery = MutableStateFlow("")

    val editAddToListQuery: StateFlow<String> = _editAddToListQuery.asStateFlow()

    private val _editAddToListFilter = MutableStateFlow(AddToListFilter.ALL)

    val editAddToListFilter: StateFlow<AddToListFilter> = _editAddToListFilter.asStateFlow()

    private val _editAddToListSeriesResults = MutableStateFlow<List<SeriesSearchResult>>(emptyList())

    val editAddToListSeriesResults: StateFlow<List<SeriesSearchResult>> =
        _editAddToListSeriesResults.asStateFlow()

    private val _editAddToListIssueResults =
        MutableStateFlow<List<IssueSearchResult>>(emptyList())

    val editAddToListIssueResults: StateFlow<List<IssueSearchResult>> =
        _editAddToListIssueResults.asStateFlow()

    fun loadReadingList(readingListId: Long) {
        activeReadingListId = readingListId
        _selectedReadingListItemIds.value = emptySet()
        _collapsedSectionsLoaded.value = false

        collapsedSectionsJob?.cancel()

        collapsedSectionsJob =
            viewModelScope.launch {
                readingListUiPreferences
                    .getCollapsedSectionIds(readingListId)
                    .collect { collapsedIds ->
                        _collapsedSectionIds.value = collapsedIds
                        _collapsedSectionsLoaded.value = true
                    }
            }

        viewModelScope.launch {
            val loadedReadingList =
                repository.getReadingListById(readingListId)

            _readingList.value = loadedReadingList

            if (loadedReadingList != null) {
                refreshReadingListLabels(loadedReadingList)
            }

            _sections.value =
                repository.getSectionsForReadingList(readingListId)
        }

        issuesJob?.cancel()

        issuesJob = viewModelScope.launch {
            repository
                .getReadingListIssues(readingListId)
                .collect { updatedIssues ->
                    _issues.value = updatedIssues
                }
        }
    }

    fun toggleSectionCollapsed(
        sectionId: Long
    ) {
        val readingListId = activeReadingListId
            ?: return

        viewModelScope.launch {
            readingListUiPreferences
                .toggleSectionCollapsed(
                    readingListId = readingListId,
                    sectionId = sectionId
                )
        }
    }

    fun expandSection(
        sectionId: Long
    ) {
        val readingListId = activeReadingListId
            ?: return

        viewModelScope.launch {
            readingListUiPreferences
                .setSectionCollapsed(
                    readingListId = readingListId,
                    sectionId = sectionId,
                    collapsed = false
                )
        }
    }

    fun updateReadingListDetails(
        title: String,
        description: String?,
        style: ReadingListStyle
    ) {
        val readingList = _readingList.value
            ?: return

        if (readingList.source != ReadingListSource.USER) {
            return
        }

        viewModelScope.launch {
            repository.updateUserReadingListDetails(
                readingListId = readingList.id,
                title = title,
                description = description,
                style = style
            )

            _readingList.value = repository.getReadingListById(readingList.id)
        }
    }

    fun deleteReadingList() {
        val readingList = _readingList.value
            ?: return

        if (readingList.source != ReadingListSource.USER) {
            return
        }

        viewModelScope.launch {
            repository.deleteUserReadingList(readingList.id)
            issuesJob?.cancel()
            _readingListDeleted.value = true
        }
    }

    fun duplicateReadingList() {
        val readingList = _readingList.value
            ?: return

        viewModelScope.launch {
            _duplicatedReadingListId.value =
                repository.duplicateReadingList(readingList.id)
        }
    }

    fun clearDuplicatedReadingList() {
        _duplicatedReadingListId.value = null
    }

    fun toggleIssueRead(
        issue: ReadingListIssue
    ) {
        viewModelScope.launch {
            if (issue.readingStatus == ReadingStatus.READ) {
                repository.markIssueAsUnread(issue.issueId)
            } else {
                repository.markIssueAsRead(issue.issueId)
            }
        }
    }

    fun markIssueAsReading(
        issue: ReadingListIssue
    ) {
        viewModelScope.launch {
            repository.markIssueAsReading(
                issue.issueId
            )
        }
    }

    fun toggleIssueSelection(
        issue: ReadingListIssue
    ) {
        val itemId = issue.readingListItemId
        val currentSelection = _selectedReadingListItemIds.value

        _selectedReadingListItemIds.value =
            if (itemId in currentSelection) {
                currentSelection - itemId
            } else {
                currentSelection + itemId
            }
    }

    fun clearIssueSelection() {
        _selectedReadingListItemIds.value = emptySet()
    }

    fun createSection(
        title: String,
        description: String?
    ) {
        val readingList = _readingList.value
            ?: return

        if (readingList.source != ReadingListSource.USER) {
            return
        }

        viewModelScope.launch {
            repository
                .createUserReadingListSection(
                    readingListId = readingList.id,
                    title = title,
                    description = description
                )

            _sections.value = repository.getSectionsForReadingList(readingList.id)
        }
    }

    fun moveIssueToSection(
        issue: ReadingListIssue,
        targetSectionId: Long?
    ) {
        val readingList = _readingList.value
            ?: return

        if (readingList.source != ReadingListSource.USER) {
            return
        }

        if (issue.sectionId == targetSectionId) {
            return
        }

        viewModelScope.launch {
            repository
                .moveUserReadingListItemToSection(
                    readingListId = readingList.id,
                    readingListItemId = issue.readingListItemId,
                    targetSectionId = targetSectionId
                )
        }
    }

    fun moveSectionUp(
        sectionId: Long
    ) {
        moveSection(
            sectionId = sectionId,
            offset = -1
        )
    }

    fun moveSectionDown(
        sectionId: Long
    ) {
        moveSection(
            sectionId = sectionId,
            offset = 1
        )
    }

    private fun moveSection(
        sectionId: Long,
        offset: Int
    ) {
        val readingList = _readingList.value
            ?: return

        if (readingList.source != ReadingListSource.USER) {
            return
        }

        viewModelScope.launch {
            repository.moveUserReadingListSection(
                readingListId = readingList.id,
                sectionId = sectionId,
                offset = offset
            )

            _sections.value = repository.getSectionsForReadingList(readingList.id)
        }
    }

    fun moveIssueUp(
        issue: ReadingListIssue
    ) {
        moveIssue(
            issue = issue,
            offset = -1
        )
    }

    fun moveIssueDown(
        issue: ReadingListIssue
    ) {
        moveIssue(
            issue = issue,
            offset = 1
        )
    }

    fun moveIssueToPosition(
        issue: ReadingListIssue,
        position: Int
    ) {
        val readingList = _readingList.value
            ?: return

        if (readingList.source != ReadingListSource.USER) {
            return
        }

        val currentIssues = _issues.value

        val currentIndex = currentIssues.indexOfFirst { currentIssue ->
            currentIssue.readingListItemId == issue.readingListItemId
        }

        if (currentIndex < 0) {
            return
        }

        val currentIssue = currentIssues[currentIndex]

        val sectionIssues = currentIssues.filter { currentItem ->
            currentItem.sectionId == currentIssue.sectionId
        }

        if (position !in 1..sectionIssues.size) {
            return
        }

        val currentPosition = sectionIssues.indexOfFirst { sectionIssue->
            sectionIssue.readingListItemId == currentIssue.readingListItemId
        } + 1

        if (currentPosition == position) {
            return
        }

        val targetIssue = sectionIssues[position - 1]

        val targetIndex = currentIssues.indexOfFirst { currentItem ->
            currentItem.readingListItemId == targetIssue.readingListItemId
        }

        if (targetIndex < 0) {
            return
        }

        val orderedItemIds = currentIssues.map { currentItem ->
            currentItem.readingListItemId
        }.toMutableList()

        val movedItemId = orderedItemIds.removeAt(currentIndex)
        orderedItemIds.add(targetIndex, movedItemId)

        viewModelScope.launch {
            repository.reorderUserReadingListItems(
                readingListId = readingList.id,
                orderedItemIds = orderedItemIds
            )
        }
    }

    private fun moveIssue(
        issue: ReadingListIssue,
        offset: Int
    ) {
        if (offset != -1 && offset != 1) {
            return
        }

        val readingList = _readingList.value
            ?: return

        if (readingList.source != ReadingListSource.USER) {
            return
        }

        val currentIssues = _issues.value

        val currentIndex =
            currentIssues.indexOfFirst { currentIssue ->
                currentIssue.readingListItemId == issue.readingListItemId
            }

        if (currentIndex < 0) {
            return
        }

        val currentIssue = currentIssues[currentIndex]
        val targetIndex = currentIndex + offset

        if (targetIndex !in currentIssues.indices) {
            return
        }

        val adjacentIssue = currentIssues[targetIndex]

        val reorderedIssues =
            currentIssues.toMutableList().apply {
                removeAt(currentIndex)
            }

        val targetSectionId = adjacentIssue.sectionId
        val movingSectionId = currentIssue.sectionId

        val mustDetachFromSection =
            movingSectionId != null &&
                movingSectionId != targetSectionId

        val insertionIndex =
            when {
                movingSectionId == targetSectionId -> targetIndex

                movingSectionId == null && targetSectionId != null -> {
                    val firstTargetIndex =
                        reorderedIssues.indexOfFirst { candidate ->
                            candidate.sectionId == targetSectionId
                        }

                    val lastTargetIndex =
                        reorderedIssues.indexOfLast { candidate ->
                            candidate.sectionId == targetSectionId
                        }

                    if (offset < 0) {
                        firstTargetIndex
                    } else {
                        lastTargetIndex + 1
                    }
                }

                movingSectionId != null && targetSectionId == null -> targetIndex

                else -> {
                    val firstTargetIndex =
                        reorderedIssues.indexOfFirst { candidate ->
                            candidate.sectionId == targetSectionId
                        }

                    val lastTargetIndex =
                        reorderedIssues.indexOfLast { candidate ->
                            candidate.sectionId == targetSectionId
                        }

                    if (offset < 0) {
                        firstTargetIndex
                    } else {
                        lastTargetIndex + 1
                    }
                }
            }

        val orderedItemIds =
            reorderedIssues.map { it.readingListItemId }.toMutableList()

        orderedItemIds.add(
            insertionIndex,
            currentIssue.readingListItemId
        )

        val detachedItemIds =
            if (mustDetachFromSection) {
                setOf(currentIssue.readingListItemId)
            } else {
                emptySet()
            }

        viewModelScope.launch {
            repository.reorderUserReadingListItems(
                readingListId = readingList.id,
                orderedItemIds = orderedItemIds,
                detachedItemIds = detachedItemIds
            )
        }
    }

    fun removeIssue(
        issue: ReadingListIssue
    ) {
        val readingListId = _readingList.value?.id
            ?: return

        viewModelScope.launch {
            repository
                .removeIssueFromUserReadingList(
                    readingListId = readingListId,
                    issueId = issue.issueId
                )

            val updatedReadingList =
                repository.getReadingListById(readingListId)

            _readingList.value = updatedReadingList

            if (updatedReadingList != null) {
                refreshReadingListLabels(updatedReadingList)
            }
        }
    }

    fun markAllBeforeAsRead(
        selectedIssue: ReadingListIssue
    ) {
        viewModelScope.launch {
            val issueIdsToMark = _issues.value
                .filter { issue ->
                    issue.position < selectedIssue.position
                }
                .filter { issue ->
                    issue.readingStatus != ReadingStatus.READ
                }
                .map { issue ->
                    issue.issueId
                }

            repository.markIssuesAsRead(
                issueIdsToMark
            )
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val issueIdsToMark = _issues.value
                .filter { issue ->
                    issue.readingStatus != ReadingStatus.READ
                }
                .map { issue ->
                    issue.issueId
                }
                .distinct()

            repository.markIssuesAsRead(
                issueIdsToMark
            )
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            val issueIdsToReset = _issues.value
                .filter { issue ->
                    issue.readingStatus != null
                }
                .map { issue ->
                    issue.issueId
                }
                .distinct()

            repository.markIssuesAsUnread(
                issueIdsToReset
            )
        }
    }

    fun markSelectedIssuesAsRead() {
        val selectedItemIds = _selectedReadingListItemIds.value

        if (selectedItemIds.isEmpty()) {
            return
        }

        val issueIds =
            _issues.value
                .filter { issue ->
                    issue.readingListItemId in selectedItemIds
                }
                .map { issue ->
                    issue.issueId
                }
                .distinct()

        if (issueIds.isEmpty()) {
            clearIssueSelection()
            return
        }

        viewModelScope.launch {
            repository.markIssuesAsRead(issueIds)

            clearIssueSelection()
        }
    }

    fun markSelectedIssuesAsUnread() {
        val selectedItemIds = _selectedReadingListItemIds.value

        if (selectedItemIds.isEmpty()) {
            return
        }

        val issueIds =
            _issues.value
                .filter { issue ->
                    issue.readingListItemId in selectedItemIds
                }
                .map { issue ->
                    issue.issueId
                }
                .distinct()

        if (issueIds.isEmpty()) {
            clearIssueSelection()
            return
        }

        viewModelScope.launch {
            repository.markIssuesAsUnread(issueIds)

            clearIssueSelection()
        }
    }

    fun getReadCount(): Int {
        return _issues.value.count {
            it.readingStatus == ReadingStatus.READ
        }
    }

    fun beginEditAddToListSession(
        currentIssues: List<PendingReadingListIssue>
    ) {
        _editAddToListDraftIssues.value =
            currentIssues
    }

    fun cancelEditAddToListSession() {
        _editAddToListDraftIssues.value =
            emptyList()

        clearEditAddToListSearch()
    }

    fun applyEditAddToListSession():
            List<PendingReadingListIssue> {

        val appliedIssues =
            _editAddToListDraftIssues.value

        _editAddToListDraftIssues.value =
            emptyList()

        clearEditAddToListSearch()

        return appliedIssues
    }

    fun toggleEditAddToListIssue(
        result: IssueSearchResult
    ) {
        val selected =
            _editAddToListDraftIssues
                .value
                .any {
                    it.issueId ==
                            result.issueId
                }

        if (selected) {
            _editAddToListDraftIssues.value =
                _editAddToListDraftIssues
                    .value
                    .filterNot {
                        it.issueId ==
                                result.issueId
                    }

            return
        }

        _editAddToListDraftIssues.value +=
            PendingReadingListIssue(
                issueId =
                    result.issueId,
                seriesId =
                    result.seriesId,
                seriesTitle =
                    result.seriesTitle,
                issueNumber =
                    result.issueNumber,
                issueTitle =
                    result.issueTitle,
                publicationDate =
                    result.publicationDate,
                coverUrl =
                    null
            )
    }

    fun toggleEditAddToListSeries(
        result: SeriesSearchResult
    ) {
        viewModelScope.launch {
            val seriesIssues =
                repository.getIssuesForSeries(
                    result.seriesId
                )

            val seriesIssueIds =
                seriesIssues
                    .map { it.id }
                    .toSet()

            val allSelected =
                seriesIssueIds.isNotEmpty() &&
                        seriesIssueIds.all { issueId ->
                            _editAddToListDraftIssues
                                .value
                                .any {
                                    it.issueId ==
                                            issueId
                                }
                        }

            if (allSelected) {
                _editAddToListDraftIssues.value =
                    _editAddToListDraftIssues
                        .value
                        .filterNot {
                            it.issueId in
                                    seriesIssueIds
                        }

                return@launch
            }

            val existingIds =
                _editAddToListDraftIssues
                    .value
                    .map { it.issueId }
                    .toSet()

            val newIssues =
                seriesIssues
                    .filterNot {
                        it.id in existingIds
                    }
                    .map { issue ->
                        PendingReadingListIssue(
                            issueId =
                                issue.id,
                            seriesId =
                                issue.seriesId,
                            seriesTitle =
                                result.title,
                            issueNumber =
                                issue.issueNumber,
                            issueTitle =
                                issue.title,
                            publicationDate =
                                issue.publicationDate,
                            coverUrl =
                                issue.coverUrl
                        )
                    }

            _editAddToListDraftIssues.value +=
                newIssues
        }
    }

    fun updateEditAddToListQuery(
        query: String
    ) {
        _editAddToListQuery.value =
            query

        editAddToListSearchJob
            ?.cancel()

        val trimmedQuery =
            query.trim()

        if (trimmedQuery.isBlank()) {
            _editAddToListSeriesResults.value =
                emptyList()

            _editAddToListIssueResults.value =
                emptyList()

            return
        }

        editAddToListSearchJob =
            viewModelScope.launch {
                delay(
                    250.milliseconds
                )

                combine(
                    repository.searchSeries(
                        trimmedQuery
                    ),
                    repository.searchIssues(
                        trimmedQuery
                    )
                ) { seriesResults, issueResults ->
                    seriesResults to
                            issueResults
                }.collect { results ->
                    _editAddToListSeriesResults.value =
                        results.first

                    _editAddToListIssueResults.value =
                        results.second
                }
            }
    }

    fun selectEditAddToListFilter(
        filter: AddToListFilter
    ) {
        _editAddToListFilter.value =
            filter
    }

    fun clearEditAddToListSearch() {
        editAddToListSearchJob
            ?.cancel()

        _editAddToListQuery.value =
            ""

        _editAddToListSeriesResults.value =
            emptyList()

        _editAddToListIssueResults.value =
            emptyList()

        _editAddToListFilter.value =
            AddToListFilter.ALL
    }

    fun saveReadingListEdits(
        title: String,
        description: String?,
        style: ReadingListStyle,
        issues: List<PendingReadingListIssue>
    ) {
        val readingList =
            _readingList.value
                ?: return

        if (readingList.source != ReadingListSource.USER) {
            return
        }

        viewModelScope.launch {
            repository
                .updateUserReadingListWithIssues(
                    readingListId = readingList.id,
                    title = title,
                    description = description,
                    style = style,
                    issueIds = issues.map { issue -> issue.issueId }
                )

           val updatedReadingList =
               repository.getReadingListById(readingList.id)

            _readingList.value = updatedReadingList

            if (updatedReadingList != null) {
                refreshReadingListLabels(updatedReadingList)
            }
        }
    }

    private suspend fun refreshReadingListLabels(
        readingList: ReadingList
    ) {
        _publisherName.value =
            repository.getPublisherById(readingList.publisherId)
                .first()
                ?.name
                .orEmpty()

        val universeIds =
            repository.getUniverseIdsForReadingList(readingList.id)
                .distinct()

        if (universeIds.size > 1) {
            _continuityName.value = "Multiple continuities"

            return
        }

        val universeId =
            if (universeIds.isEmpty()) {
                readingList.universeId
            } else {
                universeIds.single()
            }

        if (universeId == null) {
            _continuityName.value = "No specific continuity"

            return
        }

        val universes =
            repository.getUniverseForPublisher(readingList.publisherId)

        _continuityName.value =
            universes.firstOrNull { universe ->
                universe.id == universeId
            }
                ?.name
                ?: "No specific continuity"
    }
}