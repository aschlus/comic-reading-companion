package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.preferences.ComicVineApiKeyStore
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
    private val apiKeyStore: ComicVineApiKeyStore
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
                apiKeyStore.save(apiKey)

                _uiState.value =
                    ComicVineSettingsUiState(
                        isLoading = false,
                        hasSavedApiKey = true,
                        message = "Comic Vine API key saved securely."
                    )
            } catch (exception: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        isSaving = false,
                        error = exception.message
                            ?: "Could not save the Comic Vine API key."
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
}