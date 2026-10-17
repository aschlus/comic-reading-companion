package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room3.Query
import com.aschlus.comicreadingcompanion.data.preferences.ComicVineApiKeyStore
import com.aschlus.comicreadingcompanion.data.remote.ComicVineApiClient
import com.aschlus.comicreadingcompanion.data.remote.ComicVineVolumeSearchHit
import com.aschlus.comicreadingcompanion.data.remote.ComicVineVolumeSearchOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ComicVineVolumeSearchUiState(
    val isLoadingApiKey: Boolean = true,
    val isSearching: Boolean = false,
    val hasApiKey: Boolean = false,
    val query: String = "",
    val publisherFilter: String = "",
    val startYearFilter: String = "",
    val expectedIssueCountFilter: String = "",
    val results: List<ComicVineVolumeSearchHit> = emptyList(),
    val totalResults: Int = 0,
    val nextOffset: Int = 0,
    val error: String? = null
) {
    val hasMoreResults: Boolean
        get() = results.size < totalResults
}

class ComicVineVolumeSearchViewModel(
    private val apiKeyStore: ComicVineApiKeyStore,
    private val apiClient: ComicVineApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComicVineVolumeSearchUiState())
    val uiState: StateFlow<ComicVineVolumeSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadApiKeyStatus()
    }

    private fun loadApiKeyStatus() {
        viewModelScope.launch {
            try {
                val hasKey = !apiKeyStore.get().isNullOrBlank()

                _uiState.value = _uiState.value.copy(
                    isLoadingApiKey = false,
                    hasApiKey = hasKey,
                    error = null
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingApiKey = false,
                    hasApiKey = false,
                    error = "Could not check the saved Comic Vine API key."
                )
            }
        }
    }

    fun updateQuery(query: String) {
        if (_uiState.value.query == query) {
            return
        }

        searchJob?.cancel()
        searchJob = null

        _uiState.value = _uiState.value.copy(
            query = query,
            isSearching = false,
            results = emptyList(),
            totalResults = 0,
            nextOffset = 0,
            error = null
        )
    }

    fun updateMatchingOptions(
        publisher: String,
        startYear: String,
        expectedIssueCount: String
    ) {
        val currentState = _uiState.value

        if (
            currentState.publisherFilter == publisher &&
            currentState.startYearFilter == startYear &&
            currentState.expectedIssueCountFilter == expectedIssueCount
        ) {
            return
        }

        searchJob?.cancel()
        searchJob = null

        _uiState.value = currentState.copy(
            publisherFilter = publisher,
            startYearFilter = startYear,
            expectedIssueCountFilter = expectedIssueCount,
            isSearching = false,
            results = emptyList(),
            totalResults = 0,
            nextOffset = 0,
            error = null
        )
    }

    fun search() {
        val state = _uiState.value

        if (state.isLoadingApiKey || state.isSearching) {
            return
        }

        val query = state.query.trim()

        if (query.isBlank()) {
            _uiState.value = state.copy(
                error = "Enter a series title to search."
            )
            return
        }

        val startYearText = state.startYearFilter.trim()
        val startYear = startYearText.toIntOrNull()

        if (startYearText.isNotEmpty() && startYear == null) {
            _uiState.value = state.copy(
                error = "Start year mush be a whole number."
            )
            return
        }

        val expectedCountText = state.expectedIssueCountFilter.trim()
        val expectedIssueCount = expectedCountText.toIntOrNull()

        if (
            expectedCountText.isNotEmpty() &&
            (expectedIssueCount == null || expectedIssueCount < 0)
        ) {
            _uiState.value = state.copy(
                error = "Expected issue count must be a non-negative whole number."
            )
            return
        }

        requestVolumes(
            query = query,
            offset = 0,
            appendResults = false,
            publisherFilter = state.publisherFilter.trim(),
            startYearFilter = startYear,
            expectedIssueCountFilter = expectedIssueCount
        )
    }

    fun loadMore() {
        val state = _uiState.value

        if (
            state.isLoadingApiKey ||
            state.isSearching ||
            !state.hasMoreResults ||
            state.query.isBlank()
        ) {
            return
        }

        requestVolumes(
            query = state.query.trim(),
            offset = state.nextOffset,
            appendResults = true,
            publisherFilter = state.publisherFilter.trim(),
            startYearFilter = state.startYearFilter.trim().toIntOrNull(),
            expectedIssueCountFilter = state.expectedIssueCountFilter.trim().toIntOrNull()
        )
    }

    private fun requestVolumes(
        query: String,
        offset: Int,
        appendResults: Boolean,
        publisherFilter: String,
        startYearFilter: Int?,
        expectedIssueCountFilter: Int?
    ) {
        val previousState = _uiState.value

        _uiState.value = previousState.copy(
            isSearching = true,
            error = null,
            results = if (appendResults) {
                previousState.results
            } else {
                emptyList()
            },
            totalResults = if (appendResults) {
                previousState.totalResults
            } else {
                0
            },
            nextOffset = if (appendResults) {
                previousState.nextOffset
            } else {
                0
            }
        )

        searchJob = viewModelScope.launch {
            try {
                val apiKey = apiKeyStore.get()
                    ?.takeIf { it.isNotBlank() }

                if (apiKey == null) {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        hasApiKey = false,
                        error = "Configure your Comic Vine API key in " +
                                "Settings before searching."
                    )
                    return@launch
                }

                when (
                    val outcome = apiClient.searchVolumes(
                        apiKey = apiKey,
                        query = query,
                        offset = offset
                    )
                ) {
                    is ComicVineVolumeSearchOutcome.Success -> {
                        val currentState = _uiState.value

                        val combinedResults = (
                            if (appendResults) {
                                currentState.results + outcome.volumes
                            } else {
                                outcome.volumes
                            }
                        ).distinctBy { it.id }

                        _uiState.value = currentState.copy(
                            isSearching = false,
                            hasApiKey = true,
                            results = rankVolumes(
                                title = query,
                                publisherFilter = publisherFilter,
                                startYearFilter = startYearFilter,
                                expectedIssueCountFilter = expectedIssueCountFilter,
                                volumes = combinedResults
                            ),
                            totalResults = outcome.totalResults,
                            nextOffset = offset + outcome.volumes.size,
                            error = null
                        )
                    }

                    ComicVineVolumeSearchOutcome.MissingApiKey -> {
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            hasApiKey = false,
                            error = "Configure your Comic Vine API key in " +
                                    "Settings before searching."
                        )
                    }

                    ComicVineVolumeSearchOutcome.InvalidApiKey -> {
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            hasApiKey = true,
                            error =
                                "Comic Vine rejected the saved API key. " +
                                        "Open Settings to replace it."
                        )
                    }

                    ComicVineVolumeSearchOutcome.NetworkError -> {
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            error =
                                "Could not reach Comic Vine. " +
                                        "Check your connection and try again."
                        )
                    }

                    ComicVineVolumeSearchOutcome.ServiceError -> {
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            error =
                                "Comic Vine could not complete the search. " +
                                        "Try again later."
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    error = "Could not search Comic Vine. Please try again."
                )
            }
        }
    }
}

private fun rankVolumes(
    title: String,
    publisherFilter: String,
    startYearFilter: Int?,
    expectedIssueCountFilter: Int?,
    volumes: List<ComicVineVolumeSearchHit>
): List<ComicVineVolumeSearchHit> {
    return volumes.sortedByDescending { volume ->
        scoreVolumeCandidate(
            title = title,
            publisherFilter = publisherFilter,
            startYearFilter = startYearFilter,
            expectedIssueCountFilter = expectedIssueCountFilter,
            volume = volume
        )
    }
}

private fun scoreVolumeCandidate(
    title: String,
    publisherFilter: String,
    startYearFilter: Int?,
    expectedIssueCountFilter: Int?,
    volume: ComicVineVolumeSearchHit
): Int {
    var score = 0

    val normalizedTitle = normalizeSeriesTitle(title)
    val normalizedCandidateTitle = normalizeSeriesTitle(volume.name)

    if (normalizedTitle == normalizedCandidateTitle) {
        score += 100
    } else if (
        normalizedTitle.contains(normalizedCandidateTitle) ||
        normalizedCandidateTitle.contains(normalizedTitle)
    ) {
        score += 45
    }

    val candidateYear = volume.startYear

    if (startYearFilter != null && candidateYear != null) {
        when (kotlin.math.abs(startYearFilter - candidateYear)) {
            0 -> score += 30
            1 -> score += 20
            2 -> score += 10
        }
    }

    val candidatePublisher = volume.publisherName

    if (publisherFilter.isNotBlank() && candidatePublisher != null) {
        val normalizedPublisher = normalizeSeriesTitle(publisherFilter)
        val normalizedCandidatePublisher = normalizeSeriesTitle(candidatePublisher)

        when {
            normalizedPublisher == normalizedCandidatePublisher ->
                score += 15

            normalizedPublisher.contains(normalizedCandidatePublisher) ||
                normalizedCandidatePublisher.contains(normalizedPublisher) ->
                score += 10

            else -> score -= 15
        }
    }

    if (expectedIssueCountFilter != null) {
        val candidateIssueCount = volume.countOfIssues ?: 0

        if (candidateIssueCount >= expectedIssueCountFilter) {
            score += 5
        } else {
            score -= 20
        }
    }

    return score
}

private fun normalizeSeriesTitle(value: String): String {
    var normalized = value.lowercase(java.util.Locale.ROOT).trim()

    if (normalized.startsWith("the ")) {
        normalized = normalized.substring(4)
    }

    normalized = normalized.replace("&", "and")

    return normalized.filter { character ->
        character in 'a' .. 'z' || character in '0' .. '9'
    }
}