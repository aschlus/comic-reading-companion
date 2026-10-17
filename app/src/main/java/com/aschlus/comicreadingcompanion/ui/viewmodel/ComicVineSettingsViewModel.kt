package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.preferences.ComicVineApiKeyStore
import com.aschlus.comicreadingcompanion.data.remote.ComicVineApiClient
import com.aschlus.comicreadingcompanion.data.remote.ComicVineApiKeyValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ComicVineSettingsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val hasSavedApiKey: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class ComicVineSettingsViewModel(
    private val apiKeyStore: ComicVineApiKeyStore,
    private val apiClient: ComicVineApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComicVineSettingsUiState())
    val uiState: StateFlow<ComicVineSettingsUiState> = _uiState.asStateFlow()

    init {
        loadApiKeyStatus()
    }

    private fun loadApiKeyStatus() {
        viewModelScope.launch {
            try {
                val hasApiKey =
                    !apiKeyStore.get().isNullOrBlank()

                _uiState.value =
                    ComicVineSettingsUiState(
                        isLoading = false,
                        hasSavedApiKey = hasApiKey
                    )
            } catch (exception: Exception) {
                _uiState.value =
                    ComicVineSettingsUiState(
                        isLoading = false,
                        error = exception.message
                            ?: "Could not load Comic Vine API-key status"
                    )
            }
        }
    }

    fun saveApiKey(apiKey: String) {
        if (_uiState.value.isSaving) {
            return
        }

        if (apiKey.isBlank()) {
            _uiState.value =
                _uiState.value.copy(
                    message = null,
                    error = "Enter a Comic Vine API Key."
                )
            return
        }

        _uiState.value =
            _uiState.value.copy(
                isSaving = true,
                message = null,
                error = null
            )

        viewModelScope.launch {
            try {
                when (apiClient.validateApiKey(apiKey)) {
                    ComicVineApiKeyValidationResult.Valid -> {
                        apiKeyStore.save(apiKey)

                        _uiState.value =
                            ComicVineSettingsUiState(
                                isLoading = false,
                                hasSavedApiKey = true,
                                message = "Comic Vine API key validated and saved securely."
                            )
                    }

                    ComicVineApiKeyValidationResult.MissingApiKey -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSaving = false,
                            message = null,
                            error = "Enter a Comic Vine API Key."
                        )
                    }

                    ComicVineApiKeyValidationResult.InvalidApiKey -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSaving = false,
                            message = null,
                            error = "Comic Vine rejected this API key. Check it and try again."
                        )
                    }

                    ComicVineApiKeyValidationResult.NetworkError -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSaving = false,
                            message = null,
                            error = "Could not reach Comic Vine. " +
                                    "Check your connection and try again."
                        )
                    }

                    ComicVineApiKeyValidationResult.ServiceError ->  {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSaving = false,
                            message = null,
                            error = "Comic Vine could not validate this key right now. " +
                                    "Try again later."
                        )
                    }
                }
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    message = null,
                    error = "Could not validate or save the Comic Vine API key. Please try again."
                )
            }
        }
    }

    fun clearApiKey() {
        if (_uiState.value.isSaving) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                isSaving = true,
                message = null,
                error = null
            )

        viewModelScope.launch {
            try {
                apiKeyStore.clear()

                _uiState.value =
                    ComicVineSettingsUiState(
                        isLoading = false,
                        hasSavedApiKey = false,
                        message = "Comic Vine API key removed."
                    )
            } catch (exception: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        isSaving = false,
                        error = exception.message
                            ?: "Could not remove the Comic Vine API key."
                    )
            }
        }
    }

    fun clearFeedback() {
        _uiState.value =
            _uiState.value.copy(
                message = null,
                error = null
            )
    }
}