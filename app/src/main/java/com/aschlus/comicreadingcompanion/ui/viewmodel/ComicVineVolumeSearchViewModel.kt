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

        requestVolumes(
            query = query,
            offset = 0,
            appendResults = false
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
            appendResults = true
        )
    }

    private fun requestVolumes(
        query: String,
        offset: Int,
        appendResults: Boolean
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

                        val combinedResults =
                            if (appendResults) {
                                (
                                    currentState.results + outcome.volumes
                                ).distinctBy { it.id }
                            } else {
                                outcome.volumes
                            }

                        _uiState.value = currentState.copy(
                            isSearching = false,
                            hasApiKey = true,
                            results = combinedResults,
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