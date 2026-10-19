package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.preferences.ComicVineApiKeyStore
import com.aschlus.comicreadingcompanion.data.remote.ComicVineApiClient
import com.aschlus.comicreadingcompanion.data.remote.ComicVineIssueHit
import com.aschlus.comicreadingcompanion.data.remote.ComicVineIssuePageOutcome
import com.aschlus.comicreadingcompanion.data.remote.ComicVineVolumeDetails
import com.aschlus.comicreadingcompanion.data.remote.ComicVineVolumeDetailsOutcome
import com.aschlus.comicreadingcompanion.data.remote.ComicVineVolumeSearchHit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ComicVineVolumeReviewUiState(
    val isLoading: Boolean = false,
    val volumeId: Long? = null,
    val volume: ComicVineVolumeDetails? = null,
    val editedTitle: String = "",
    val issues: List<ComicVineIssueHit> = emptyList(),
    val totalIssueRecords: Int? = null,
    val error: String? = null
)

class ComicVineVolumeReviewViewModel(
    private val apiKeyStore: ComicVineApiKeyStore,
    private val apiClient: ComicVineApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComicVineVolumeReviewUiState())
    val uiState: StateFlow<ComicVineVolumeReviewUiState> =
        _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun updateEditedTitle(title: String){
        _uiState.value = _uiState.value.copy(
            editedTitle = title
        )
    }

    fun load(
        volumeId: Long,
        selectedVolume: ComicVineVolumeSearchHit? = null
    ) {
        loadJob?.cancel()

        if (volumeId <= 0L) {
            _uiState.value = ComicVineVolumeReviewUiState(
                error = "The selected series has an invalid source ID."
            )
            return
        }

        _uiState.value = ComicVineVolumeReviewUiState(
            isLoading = true,
            volumeId = volumeId
        )

        loadJob = viewModelScope.launch {
            try {
                val apiKey = apiKeyStore.get()
                    ?.takeIf { it.isNotBlank() }

                if (apiKey == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Configure your Comic Vine API key in " +
                            "Settings before loading series details."
                    )
                    return@launch
                }

                val selectedSearchResult = selectedVolume
                    ?.takeIf { it.id == volumeId }

                val volume = if (selectedSearchResult != null) {
                    ComicVineVolumeDetails(
                        id = selectedSearchResult.id,
                        name = selectedSearchResult.name,
                        startYear = selectedSearchResult.startYear,
                        countOfIssues = selectedSearchResult.countOfIssues,
                        publisherName = selectedSearchResult.publisherName,
                        siteDetailUrl = selectedSearchResult.siteDetailUrl,
                        imageUrl = selectedSearchResult.imageUrl
                    )
                } else {
                    val detailsOutcome = apiClient.getVolumeDetails(
                        apiKey = apiKey,
                        volumeId = volumeId
                    )

                    when (detailsOutcome) {
                        is ComicVineVolumeDetailsOutcome.Success -> {
                            detailsOutcome.volume
                        }

                        else -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = detailsErrorMessage(detailsOutcome)
                            )
                            return@launch
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    volume = volume,
                    editedTitle = volume.name
                )

                val allIssues = mutableListOf<ComicVineIssueHit>()
                var totalRecords: Int? = null
                var offset = 0

                while (totalRecords == null || offset < totalRecords) {
                    val pageOutcome = apiClient.getVolumeIssues(
                        apiKey = apiKey,
                        volumeId = volumeId,
                        offset = offset
                    )

                    val page = when (pageOutcome) {
                        is ComicVineIssuePageOutcome.Success -> {
                            pageOutcome
                        }

                        else -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                issues = allIssues.toList(),
                                totalIssueRecords = totalRecords,
                                error = issueErrorMessage(pageOutcome)
                            )
                            return@launch
                        }
                    }

                    if (totalRecords == null) {
                        totalRecords = page.totalResults.coerceAtLeast(0)
                    }

                    allIssues.addAll(page.issues)

                    _uiState.value = _uiState.value.copy(
                        issues = allIssues.toList(),
                        totalIssueRecords = totalRecords
                    )

                    if (page.pageResultCount <= 0) {
                        break
                    }

                    offset += page.pageResultCount
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    volume = volume,
                    issues = allIssues.toList(),
                    totalIssueRecords = totalRecords,
                    error = null
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Could not load the selected series. Please try again."
                )
            }
        }
    }

    private fun detailsErrorMessage(
        outcome: ComicVineVolumeDetailsOutcome
    ): String {
        return when (outcome) {
            ComicVineVolumeDetailsOutcome.MissingApiKey ->
                "Configure your Comic Vine API key in Settings."

            ComicVineVolumeDetailsOutcome.InvalidApiKey ->
                "Comic Vine rejected the saved API key. Open Settings to replace it."

            ComicVineVolumeDetailsOutcome.NetworkError ->
                "Could not reach Comic Vine. Check your connection and try again."

            ComicVineVolumeDetailsOutcome.ServiceError ->
                "Could not retrieve the selected series details."

            is ComicVineVolumeDetailsOutcome.Success ->
                ""
        }
    }

    private fun issueErrorMessage(
        outcome: ComicVineIssuePageOutcome
    ): String {
        return when (outcome) {
            ComicVineIssuePageOutcome.MissingApiKey ->
                "Configure your Comic Vine API key in Settings."

            ComicVineIssuePageOutcome.InvalidApiKey ->
                "Comic Vine rejected the saved API key. Open Settings to replace it."

            ComicVineIssuePageOutcome.NetworkError ->
                "Could not reach Comic Vine. Check your connection and try again."

            ComicVineIssuePageOutcome.ServiceError ->
                "Could not retrieve the complete issue list."

            is ComicVineIssuePageOutcome.Success ->
                ""
        }
    }
}