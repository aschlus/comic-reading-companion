package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import com.aschlus.comicreadingcompanion.data.database.models.IssueSearchResult
import com.aschlus.comicreadingcompanion.data.database.models.PublisherBrowseResult
import com.aschlus.comicreadingcompanion.data.database.models.SeriesSearchResult
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class BrowseViewModel(
    private val repository: ComicRepository
) : ViewModel() {

    private val _publishers =
        MutableStateFlow<List<PublisherBrowseResult>>(emptyList())

    val publishers: StateFlow<List<PublisherBrowseResult>> =
        _publishers.asStateFlow()

    private var searchJob: Job? = null

    private val _searchQuery =
        MutableStateFlow("")

    val searchQuery: StateFlow<String> =
        _searchQuery.asStateFlow()

    private val _selectedPublisherId =
        MutableStateFlow<Long?>(null)

    val selectedPublisherId: StateFlow<Long?> =
        _selectedPublisherId.asStateFlow()

    private val _selectedUniverseId =
        MutableStateFlow<Long?>(null)

    val selectedUniverseId: StateFlow<Long?> =
        _selectedUniverseId.asStateFlow()

    private val _selectedPublicationYear =
        MutableStateFlow<Int?>(null)

    val selectedPublicationYear: StateFlow<Int?> =
        _selectedPublicationYear.asStateFlow()

    private val _availableUniverses = MutableStateFlow<List<Universe>>(emptyList())
    val availableUniverses: StateFlow<List<Universe>> = _availableUniverses.asStateFlow()

    private val _availablePublicationYears = MutableStateFlow<List<Int>>(emptyList())
    val availablePublicationYears: StateFlow<List<Int>> = _availablePublicationYears.asStateFlow()

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
                .getPublisherBrowseResults()
                .collect { updatedPublishers ->
                    _publishers.value = updatedPublishers
                }
        }

        viewModelScope.launch {
            repository.getAllUniverses().collect {
                _availableUniverses.value = it
            }
        }

        viewModelScope.launch {
            repository.getAvailablePublicationYears().collect {
                _availablePublicationYears.value = it
            }
        }
    }

    fun updateSearchQuery(
        query: String
    ) {
        _searchQuery.value = query
        refreshSearchResults()
    }

    fun updatePublisherFilter(
        publisherId: Long?
    ) {
        _selectedPublisherId.value = publisherId
        refreshSearchResults()
    }

    fun updateUniverseFilter(
        universeId: Long?
    ) {
        _selectedUniverseId.value = universeId
        refreshSearchResults()
    }

    fun updatePublicationYearFilter(
        year: Int?
    ) {
        _selectedPublicationYear.value = year
        refreshSearchResults()
    }

    private fun refreshSearchResults() {
        searchJob?.cancel()

        val trimmedQuery = _searchQuery.value.trim()
        val publisherId = _selectedPublisherId.value
        val universeId = _selectedUniverseId.value
        val publicationYear = _selectedPublicationYear.value

        if (
            trimmedQuery.isBlank() &&
            publisherId == null &&
            universeId == null &&
            publicationYear == null
        ) {
            _seriesResults.value = emptyList()
            _issueResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            delay(250.milliseconds)

            combine(
                repository.searchSeries(
                    query = trimmedQuery,
                    publisherId = publisherId,
                    universeId = universeId,
                    publicationYear = publicationYear
                ),
                repository.searchIssues(
                    query = trimmedQuery,
                    publisherId = publisherId,
                    universeId = universeId,
                    publicationYear = publicationYear
                )
            ) { seriesResults, issueResults ->
                seriesResults to issueResults
            }.collect { results ->
                _seriesResults.value = results.first
                _issueResults.value = results.second
            }
        }
    }
}