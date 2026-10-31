package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aschlus.comicreadingcompanion.data.preferences.MetronApiTokenStore
import com.aschlus.comicreadingcompanion.data.remote.MetronApiClient

class MetronSettingsViewModelFactory(
    private val tokenStore: MetronApiTokenStore,
    private val apiClient: MetronApiClient
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                MetronSettingsViewModel::class.java
            )
        ) {
            return MetronSettingsViewModel(
                tokenStore = tokenStore,
                apiClient = apiClient
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}