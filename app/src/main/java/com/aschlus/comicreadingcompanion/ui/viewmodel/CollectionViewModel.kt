package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.models.OwnedIssueResult
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.collections.filter
import kotlin.text.trim

data class CollectionStats(
    val totalIssues: Int = 0,
    val seriesCount: Int = 0,
    val publisherCount: Int = 0,
    val readCount: Int = 0,
    val readingCount: Int = 0,
    val unreadCount: Int = 0
)

enum class CollectionSort {
    TITLE_ASCENDING,
    PUBLICATION_NEWEST,
    PUBLICATION_OLDEST
}

class CollectionViewModel(
    private val repository: ComicRepository
) : ViewModel() {

    private val _ownedIssues = MutableStateFlow<List<OwnedIssueResult>>(emptyList())
    val ownedIssues: StateFlow<List<OwnedIssueResult>> = _ownedIssues.asStateFlow()

    private val _stats = MutableStateFlow(CollectionStats())
    val stats: StateFlow<CollectionStats> = _stats.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sort = MutableStateFlow(CollectionSort.TITLE_ASCENDING)
    val sort: StateFlow<CollectionSort> = _sort.asStateFlow()

    private val _visibleIssues = MutableStateFlow<List<OwnedIssueResult>>(emptyList())
    val visibleIssues: StateFlow<List<OwnedIssueResult>> = _visibleIssues.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getOwnedIssues().collect { issues ->
                _ownedIssues.value = issues

                refreshVisibleIssues()

                _stats.value = CollectionStats(
                    totalIssues = issues.size,
                    seriesCount = issues
                        .distinctBy { it.seriesId }
                        .size,
                    publisherCount = issues
                        .distinctBy { it.publisherName }
                        .size,
                    readCount = issues.count { it.readingStatus == ReadingStatus.READ },
                    readingCount = issues.count { it.readingStatus == ReadingStatus.READING },
                    unreadCount = issues.count {
                        it.readingStatus == null ||
                            it.readingStatus == ReadingStatus.UNREAD
                    }
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        refreshVisibleIssues()
    }

    fun updateSort(sort: CollectionSort) {
        _sort.value = sort
        refreshVisibleIssues()
    }

    private fun refreshVisibleIssues() {
        val query = _searchQuery.value.trim()

        val matchingIssues =
            _ownedIssues.value.filter { issue ->
                query.isBlank() ||
                    issue.seriesTitle.contains(
                        query,
                        ignoreCase = true
                    ) ||
                    issue.issueNumber.contains(
                        query,
                        ignoreCase = true
                    ) ||
                    issue.issueTitle.orEmpty().contains(
                        query,
                        ignoreCase = true
                    ) ||
                    issue.publisherName.contains(
                        query,
                        ignoreCase = true
                    )
            }

        _visibleIssues.value =
            when (_sort.value) {
                CollectionSort.TITLE_ASCENDING ->
                    matchingIssues.sortedWith(
                        compareBy<OwnedIssueResult>(
                            { it.seriesTitle.lowercase() },
                            { it.publicationDate.orEmpty() },
                            { it.issueNumber }
                        )
                    )

                CollectionSort.PUBLICATION_NEWEST ->
                    matchingIssues.sortedWith(
                        compareByDescending<OwnedIssueResult> {
                            it.publicationDate.orEmpty()
                        }.thenBy {
                            it.seriesTitle.lowercase()
                        }
                    )

                CollectionSort.PUBLICATION_OLDEST ->
                    matchingIssues.sortedWith(
                        compareBy<OwnedIssueResult>(
                            { it.publicationDate.orEmpty() },
                            { it.seriesTitle.lowercase() }
                        )
                    )
            }
    }
}