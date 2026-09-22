package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.models.IssueDetail
import com.aschlus.comicreadingcompanion.data.database.models.IssueReadingListResult
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IssueDetailViewModel(
    private val repository: ComicRepository
) : ViewModel() {

    private var issueJob: Job? = null

    private var readingListsJob: Job? = null

    private val _issue =
        MutableStateFlow<IssueDetail?>(null)

    val issue: StateFlow<IssueDetail?> =
        _issue.asStateFlow()

    private val _readingLists =
        MutableStateFlow<List<IssueReadingListResult>>(emptyList())

    val readingLists: StateFlow<List<IssueReadingListResult>> =
        _readingLists.asStateFlow()

    fun loadIssue(
        issueId: Long
    ) {
        issueJob?.cancel()
        readingListsJob?.cancel()

        issueJob = viewModelScope.launch {
            repository
                .getIssueDetail(issueId)
                .collect { updatedIssue ->
                    _issue.value = updatedIssue
                }
        }

        readingListsJob = viewModelScope.launch {
            repository
                .getReadingListsContainingIssue(issueId)
                .collect { updatedLists ->
                    _readingLists.value = updatedLists
                }
        }
    }

    fun markAsUnread() {
        val currentIssue = _issue.value
            ?: return

        viewModelScope.launch {
            repository.markIssueAsUnread(
                currentIssue.issueId
            )
        }
    }

    fun markAsReading() {
        val currentIssue = _issue.value
            ?: return

        viewModelScope.launch {
            repository.markIssueAsReading(
                currentIssue.issueId
            )
        }
    }

    fun markAsRead() {
        val currentIssue = _issue.value
            ?: return

        viewModelScope.launch {
            repository.markIssueAsRead(
                currentIssue.issueId
            )
        }
    }
}