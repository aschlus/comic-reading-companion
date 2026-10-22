
package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.preferences.ComicVineApiKeyStore
import com.aschlus.comicreadingcompanion.data.remote.ComicVineApiClient
import com.aschlus.comicreadingcompanion.data.remote.ComicVineIssueHit
import com.aschlus.comicreadingcompanion.data.remote.ComicVineIssuePageOutcome
import com.aschlus.comicreadingcompanion.data.remote.ComicVineVolumeDetailsOutcome
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import com.aschlus.comicreadingcompanion.data.repository.ComicVineRefreshIssue
import com.aschlus.comicreadingcompanion.data.repository.ComicVineSeriesRefreshPreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ComicVineSeriesRefreshUiState(
    val isLoading: Boolean = false,
    val preview: ComicVineSeriesRefreshPreview? = null,
    val sourceIssues: List<ComicVineRefreshIssue> = emptyList(),
    val volumeUrl: String? = null,
    val isApplying: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class ComicVineSeriesRefreshViewModel(
    private val apiKeyStore: ComicVineApiKeyStore,
    private val apiClient: ComicVineApiClient,
    private val repository: ComicRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(ComicVineSeriesRefreshUiState())

    val uiState: StateFlow<ComicVineSeriesRefreshUiState> =
        _uiState.asStateFlow()

    private var refreshJob: Job? = null

    fun loadPreview(seriesId: Long, comicVineVolumeId: Long) {
        if (
            _uiState.value.isLoading ||
            _uiState.value.isApplying
        ) {
            return
        }

        refreshJob?.cancel()
        _uiState.value = ComicVineSeriesRefreshUiState(isLoading = true)

        refreshJob = viewModelScope.launch {
            try {
                require(seriesId > 0L) { "Series ID must be positive." }
                require(comicVineVolumeId > 0L) {
                    "Comic Vine volume ID must be positive."
                }

                val apiKey = apiKeyStore.get()
                if (apiKey.isNullOrBlank()) {
                    _uiState.value = ComicVineSeriesRefreshUiState(
                        error = "Add your Comic Vine API key in Settings before refreshing."
                    )
                    return@launch
                }

                val volume = when (
                    val outcome = apiClient.getVolumeDetails(
                        apiKey = apiKey,
                        volumeId = comicVineVolumeId
                    )
                ) {
                    is ComicVineVolumeDetailsOutcome.Success -> outcome.volume
                    ComicVineVolumeDetailsOutcome.MissingApiKey ->
                        throw RefreshLoadException("Comic Vine API key is missing.")
                    ComicVineVolumeDetailsOutcome.InvalidApiKey ->
                        throw RefreshLoadException("Comic Vine rejected your API key. Check it in Settings.")
                    ComicVineVolumeDetailsOutcome.NetworkError ->
                        throw RefreshLoadException("Couldn't connect to Comic Vine. Check your connection and try again.")
                    ComicVineVolumeDetailsOutcome.ServiceError ->
                        throw RefreshLoadException("Comic Vine couldn't return details for this series.")
                }

                val sourceIssues = mutableListOf<ComicVineIssueHit>()
                var offset = 0
                var totalResults: Int

                do {
                    when (
                        val outcome = apiClient.getVolumeIssues(
                            apiKey = apiKey,
                            volumeId = comicVineVolumeId,
                            offset = offset
                        )
                    ) {
                        is ComicVineIssuePageOutcome.Success -> {
                            sourceIssues += outcome.issues
                            totalResults = outcome.totalResults

                            if (outcome.pageResultCount == 0) break
                            offset += outcome.pageResultCount
                        }

                        ComicVineIssuePageOutcome.MissingApiKey ->
                            throw RefreshLoadException("Comic Vine API key is missing.")
                        ComicVineIssuePageOutcome.InvalidApiKey ->
                            throw RefreshLoadException("Comic Vine rejected your API key. Check it in Settings.")
                        ComicVineIssuePageOutcome.NetworkError ->
                            throw RefreshLoadException("Couldn't connect to Comic Vine while loading issues.")
                        ComicVineIssuePageOutcome.ServiceError ->
                            throw RefreshLoadException("Comic Vine couldn't return the issue list.")
                    }
                } while (offset < totalResults)

                val mappedIssues = sourceIssues.mapNotNull { issue ->
                    val number = issue.issueNumber
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?: return@mapNotNull null

                    ComicVineRefreshIssue(
                        comicVineIssueId = issue.id,
                        issueNumber = number,
                        title = issue.name?.trim()?.takeIf { it.isNotEmpty() },
                        publicationDate = normalizePublicationDate(issue),
                        coverUrl = issue.imageUrl,
                        issueType = IssueType.valueOf(
                            ComicVineIssueTypeInference.infer(issue)
                        ),
                        siteDetailsUrl = issue.siteDetailUrl
                    )
                }

                val preview = repository.previewComicVineSeriesRefresh(
                    seriesId = seriesId,
                    comicVineVolumeId = volume.id,
                    refreshedTitle = volume.name,
                    refreshedStartYear = volume.startYear,
                    sourceIssues = mappedIssues
                )

                _uiState.value = ComicVineSeriesRefreshUiState(
                    preview = preview,
                    sourceIssues = mappedIssues,
                    volumeUrl = volume.siteDetailUrl,
                    successMessage =
                        "Loaded ${mappedIssues.size} Comic Vine issues for review."
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = ComicVineSeriesRefreshUiState(
                    error = exception.message
                        ?: "Couldn't prepare the Comic Vine refresh preview."
                )
            }
        }
    }

    fun clearPreview() {
        if (_uiState.value.isApplying) return
        refreshJob?.cancel()
        _uiState.value = ComicVineSeriesRefreshUiState()
    }

    fun applyPreview() {
        val currentState = _uiState.value

        if (currentState.isLoading || currentState.isApplying) {
            return
        }

        val preview = currentState.preview

        if (preview == null) {
            _uiState.value = currentState.copy(
                error = "Load a refresh preview before applying it.",
                successMessage = null
            )
            return
        }

        _uiState.value = currentState.copy(
            isApplying = true,
            error = null,
            successMessage = null
        )

        viewModelScope.launch {
            try {
                repository.applyComicVineSeriesRefresh(
                    seriesId = preview.seriesId,
                    comicVineVolumeId = preview.comicVineVolumeId,
                    refreshedTitle = preview.refreshedTitle,
                    refreshedStartYear = preview.refreshedStartYear,
                    volumeUrl = currentState.volumeUrl,
                    sourceIssues = currentState.sourceIssues
                )

                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    error = null,
                    successMessage = "Comic Vine refresh applied."
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    error = exception.message
                        ?: "Couldn't apply the Comic Vine refresh.",
                    successMessage = null
                )
            }
        }
    }

    private fun normalizePublicationDate(
        issue: ComicVineIssueHit
    ): String? {
        val rawDate = issue.coverDate
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: issue.storeDate?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null

        val match = Regex("""^(\d{4})(?:-(\d{2})(?:-(\d{2}))?)?$""")
            .matchEntire(rawDate)
            ?: return rawDate

        val year = match.groupValues[1]
        val month = match.groupValues[2]
        val day = match.groupValues[3]

        return when {
            day.isNotEmpty() -> "$year-$month-$day"
            month.isNotEmpty() -> "$year-$month"
            else -> year
        }
    }

    private class RefreshLoadException(
        message: String
    ) : Exception(message)
}