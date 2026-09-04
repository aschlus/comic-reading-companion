package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListIssue
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ReadingListDetailViewModel(
    private val repository: ComicRepository
) : ViewModel() {

    private var issuesJob: Job? = null

    private val _readingList =
        MutableStateFlow<ReadingList?>(null)

    val readingList: StateFlow<ReadingList?> =
        _readingList.asStateFlow()

    private val _issues =
        MutableStateFlow<List<ReadingListIssue>>(emptyList())

    val issues: StateFlow<List<ReadingListIssue>> =
        _issues.asStateFlow()

    fun loadReadingList(readingListId: Long) {
        viewModelScope.launch {
            _readingList.value =
                repository.getReadingListById(readingListId)
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

    fun getReadCount(): Int {
        return _issues.value.count {
            it.readingStatus == ReadingStatus.READ
        }
    }
}