package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.models.IssueSearchResult
import com.aschlus.comicreadingcompanion.data.database.models.SeriesSearchResult
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BrowseViewModel(
    private val repository: ComicRepository
) : ViewModel() {

    private val _publishers =
        MutableStateFlow<List<Publisher>>(emptyList())

    val publishers: StateFlow<List<Publisher>> =
        _publishers.asStateFlow()

    private var searchJob: Job? = null

    private val _searchQuery =
        MutableStateFlow("")

    val searchQuery: StateFlow<String> =
        _searchQuery.asStateFlow()

    private val _seriesResults =
        MutableStateFlow<List<SeriesSearchResult>>(emptyList())

    val seriesResults: StateFlow<List<SeriesSearchResult>> =
        _seriesResults.asStateFlow()

    private val _issueResults =
        MutableStateFlow<List<IssueSearchResult>>(emptyList())

    val issueResults:
            StateFlow<List<IssueSearchResult>> =
        _issueResults.asStateFlow()

    init {
        viewModelScope.launch {
            repository
                .getPublishersFlow()
                .collect { updatedPublishers ->
                    _publishers.value = updatedPublishers
                }
        }
    }

    fun updateSearchQuery(
        query: String
    ) {
        _searchQuery.value = query

        searchJob?.cancel()

        val trimmedQuery = query.trim()

        if (trimmedQuery.isBlank()) {
            _seriesResults.value = emptyList()
            _issueResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            delay(250)

            combine(
                repository.searchSeries(
                    trimmedQuery
                ),
                repository.searchIssues(
                    trimmedQuery
                )
            ) { seriesResults, issueResults ->
                seriesResults to issueResults
            }.collect { results ->
                _seriesResults.value =
                    results.first

                _issueResults.value =
                    results.second
            }
        }
    }
}