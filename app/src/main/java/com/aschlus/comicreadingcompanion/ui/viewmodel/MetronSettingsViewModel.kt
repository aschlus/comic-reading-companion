package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.preferences.MetronApiTokenStore
import com.aschlus.comicreadingcompanion.data.remote.MetronApiClient
import com.aschlus.comicreadingcompanion.data.remote.ReleaseProviderResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.aschlus.comicreadingcompanion.data.remote.ScheduledReleaseData
import java.time.LocalDate

data class MetronSettingsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val hasSavedToken: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val isCheckingReleases: Boolean = false,
    val releasePreviewRange: String? = null,
    val releasePreviewCount: Int? = null,
    val releasePreview: List<ScheduledReleaseData> = emptyList(),
    val releasePreviewError: String? = null,
    val isLoadingReleaseDetail: Boolean = false,
    val releaseDetailRequestId: String? = null,
    val inspectedReleaseDetail: ScheduledReleaseData? = null,
    val releaseDetailError: String? = null
)

class MetronSettingsViewModel(
    private val tokenStore: MetronApiTokenStore,
    private val apiClient: MetronApiClient
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(MetronSettingsUiState())

    val uiState: StateFlow<MetronSettingsUiState> =
        _uiState.asStateFlow()

    init {
        loadTokenStatus()
    }

    private fun loadTokenStatus() {
        viewModelScope.launch {
            try {
                val hasToken = !tokenStore.get().isNullOrBlank()

                _uiState.value = MetronSettingsUiState(
                    isLoading = false,
                    hasSavedToken = hasToken
                )
            } catch (exception: Exception) {
                _uiState.value = MetronSettingsUiState(
                    isLoading = false,
                    error = exception.message
                        ?: "Could not load Metron token status."
                )
            }
        }
    }

    fun saveToken(token: String) {
        if (_uiState.value.isSaving) return

        if (token.isBlank()) {
            _uiState.value = _uiState.value.copy(
                message = null,
                error = "Enter a Metron API token."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isSaving = true,
            message = null,
            error = null
        )

        viewModelScope.launch {
            try {
                when (val result = apiClient.validateConnection(token)) {
                    is ReleaseProviderResult.Success -> {
                        tokenStore.save(token)

                        _uiState.value = MetronSettingsUiState(
                            isLoading = false,
                            hasSavedToken = true,
                            message =
                                "Metron API token validated and saved securely."
                        )
                    }

                    ReleaseProviderResult.MissingCredentials -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSaving = false,
                            error = "Enter a Metron API token."
                        )
                    }

                    ReleaseProviderResult.InvalidCredentials -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSaving = false,
                            error =
                                "Metron rejected this API token. " +
                                        "Check it and try again."
                        )
                    }

                    ReleaseProviderResult.AccessDenied -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSaving = false,
                            error =
                                "Metron denied API access for this token. " +
                                        "Check your account and token."
                        )
                    }

                    is ReleaseProviderResult.RateLimited -> {
                        val retryMessage =
                            result.retryAfterSeconds?.let {
                                " Try again in $it seconds."
                            } ?: " Try again later."

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSaving = false,
                            error =
                                "Metron rate-limited the connection check." +
                                        retryMessage
                        )
                    }

                    ReleaseProviderResult.NetworkError -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSaving = false,
                            error =
                                "Could not reach Metron. " +
                                        "Check your connection and try again."
                        )
                    }

                    is ReleaseProviderResult.ServiceError -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSaving = false,
                            error =
                                "Metron could not validate the token. " +
                                        "Try again later."
                        )
                    }
                }
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    message = null,
                    error =
                        "Could not validate or save the Metron API token. " +
                                "Please try again."
                )
            }
        }
    }

    fun previewUpcomingReleases() {
        val currentState = _uiState.value

        if (
            currentState.isLoading ||
            currentState.isSaving ||
            currentState.isCheckingReleases
        ) {
            return
        }

        _uiState.value = currentState.copy(
            isCheckingReleases = true,
            releasePreviewRange = null,
            releasePreviewCount = null,
            releasePreview = emptyList(),
            releasePreviewError = null
        )

        viewModelScope.launch {
            try {
                val token = tokenStore.get()

                if (token.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isCheckingReleases = false,
                        releasePreviewError =
                            "Save a Metron API token before checking releases."
                    )
                    return@launch
                }

                val fromDate = LocalDate.now()
                val toDate = fromDate.plusDays(14)

                when (
                    val result = apiClient.getScheduledReleasePreview(
                        accessToken = token,
                        fromDate = fromDate,
                        toDate = toDate
                    )
                ) {
                    is ReleaseProviderResult.Success -> {
                        val preview = result.value

                        val releases = preview.releases.sortedWith(
                            compareBy<ScheduledReleaseData> {
                                it.storeReleaseDate ?: "9999-99-99"
                            }.thenBy {
                                it.seriesTitle
                            }.thenBy {
                                it.issueNumber.orEmpty()
                            }
                        )

                        _uiState.value = _uiState.value.copy(
                            isCheckingReleases = false,
                            releasePreviewRange =
                                "$fromDate through $toDate",
                            releasePreviewCount = preview.totalCount.takeIf { it > 0 }
                                ?: releases.size,
                            releasePreview = releases.take(8),
                            releasePreviewError = null
                        )
                    }

                    ReleaseProviderResult.MissingCredentials -> {
                        _uiState.value = _uiState.value.copy(
                            isCheckingReleases = false,
                            releasePreviewError =
                                "No Metron API token is available."
                        )
                    }

                    ReleaseProviderResult.InvalidCredentials -> {
                        _uiState.value = _uiState.value.copy(
                            isCheckingReleases = false,
                            releasePreviewError =
                                "Metron denied access to release data."
                        )
                    }

                    ReleaseProviderResult.AccessDenied -> {
                        _uiState.value = _uiState.value.copy(
                            isCheckingReleases = false,
                            releasePreviewError =
                                "Metron denied access to release data."
                        )
                    }

                    is ReleaseProviderResult.RateLimited -> {
                        val retryMessage =
                            result.retryAfterSeconds?.let {
                                " Try again in $it seconds."
                            } ?: " Try again later."

                        _uiState.value = _uiState.value.copy(
                            isCheckingReleases = false,
                            releasePreviewError =
                                "Metron rate-limited the request.$retryMessage"
                        )
                    }

                    ReleaseProviderResult.NetworkError -> {
                        _uiState.value = _uiState.value.copy(
                            isCheckingReleases = false,
                            releasePreviewError =
                                "Could not reach Metron. Check your connection."
                        )
                    }

                    is ReleaseProviderResult.ServiceError -> {
                        _uiState.value = _uiState.value.copy(
                            isCheckingReleases = false,
                            releasePreviewError = result.message
                        )
                    }
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCheckingReleases = false,
                    releasePreviewError =
                        "Could not retrieve release data. Please try again."
                )
            }
        }
    }

    fun inspectReleaseDetails(externalIssueId: String) {
        val currentState = _uiState.value

        if (
            currentState.isSaving ||
            currentState.isCheckingReleases ||
            currentState.isLoadingReleaseDetail
        ) {
            return
        }

        _uiState.value = currentState.copy(
            isLoadingReleaseDetail = true,
            releaseDetailRequestId = externalIssueId,
            inspectedReleaseDetail = null,
            releaseDetailError = null
        )

        viewModelScope.launch {
            try {
                val token = tokenStore.get()

                if (token.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingReleaseDetail = false,
                        releaseDetailError =
                            "Save a Metron token before loading issue details."
                    )
                    return@launch
                }

                when (
                    val result = apiClient.getReleaseDetails(
                        accessToken = token,
                        externalIssueId = externalIssueId
                    )
                ) {
                    is ReleaseProviderResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoadingReleaseDetail = false,
                            inspectedReleaseDetail = result.value,
                            releaseDetailError = null
                        )
                    }

                    ReleaseProviderResult.MissingCredentials -> {
                        _uiState.value = _uiState.value.copy(
                            isLoadingReleaseDetail = false,
                            releaseDetailError = "No Metron token is available."
                        )
                    }

                    ReleaseProviderResult.InvalidCredentials -> {
                        _uiState.value = _uiState.value.copy(
                            isLoadingReleaseDetail = false,
                            releaseDetailError =
                                "Metron rejected the saved token."
                        )
                    }

                    ReleaseProviderResult.AccessDenied -> {
                        _uiState.value = _uiState.value.copy(
                            isLoadingReleaseDetail = false,
                            releaseDetailError =
                                "Metron denied access to issue details."
                        )
                    }

                    is ReleaseProviderResult.RateLimited -> {
                        val retryMessage =
                            result.retryAfterSeconds?.let {
                                " Try again in $it seconds."
                            } ?: " Try again later."

                        _uiState.value = _uiState.value.copy(
                            isLoadingReleaseDetail = false,
                            releaseDetailError =
                                "Metron rate-limited the request.$retryMessage"
                        )
                    }

                    ReleaseProviderResult.NetworkError -> {
                        _uiState.value = _uiState.value.copy(
                            isLoadingReleaseDetail = false,
                            releaseDetailError =
                                "Could not reach Metron."
                        )
                    }

                    is ReleaseProviderResult.ServiceError -> {
                        _uiState.value = _uiState.value.copy(
                            isLoadingReleaseDetail = false,
                            releaseDetailError = result.message
                        )
                    }
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingReleaseDetail = false,
                    releaseDetailError =
                        "Could not retrieve issue details. Please try again."
                )
            }
        }
    }

    fun clearToken() {
        if (_uiState.value.isSaving) return

        _uiState.value = _uiState.value.copy(
            isSaving = true,
            message = null,
            error = null
        )

        viewModelScope.launch {
            try {
                tokenStore.clear()

                _uiState.value = MetronSettingsUiState(
                    isLoading = false,
                    message = "Metron API token removed."
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    error = exception.message
                        ?: "Could not remove the Metron API token."
                )
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(
            message = null,
            error = null
        )
    }
}