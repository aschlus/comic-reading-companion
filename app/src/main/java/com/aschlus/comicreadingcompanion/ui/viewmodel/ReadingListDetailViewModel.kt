package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSection
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSource
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListIssue
import com.aschlus.comicreadingcompanion.data.preferences.ReadingListUiPreferences
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ReadingListDetailViewModel(
    private val repository: ComicRepository,
    private val readingListUiPreferences: ReadingListUiPreferences
) : ViewModel() {

    private var issuesJob: Job? = null
    private var collapsedSectionsJob: Job? = null

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

    fun loadReadingList(readingListId: Long) {
        activeReadingListId = readingListId
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
            _readingList.value =
                repository.getReadingListById(readingListId)

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
        description: String?
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
                description = description
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

    private fun moveIssue(
        issue: ReadingListIssue,
        offset: Int
    ) {
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


        val targetIndex = currentIndex + offset

        if (targetIndex !in currentIssues.indices) {
            return
        }

        val targetIssue = currentIssues[targetIndex]

        if (targetIssue.sectionId != issue.sectionId) {
            return
        }

        val orderedItemIds = currentIssues.map { currentIssue ->
            currentIssue.readingListItemId
        }.toMutableList()

        val movedItemId = orderedItemIds.removeAt(currentIndex)

        orderedItemIds.add(targetIndex, movedItemId)

        viewModelScope.launch {
            repository
                .reorderUserReadingListItems(
                    readingListId = readingList.id,
                    orderedItemIds = orderedItemIds
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

    fun getReadCount(): Int {
        return _issues.value.count {
            it.readingStatus == ReadingStatus.READ
        }
    }
}