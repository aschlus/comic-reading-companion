package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aschlus.comicreadingcompanion.data.preferences.ComicVineApiKeyStore
import com.aschlus.comicreadingcompanion.data.remote.ComicVineApiClient

class ComicVineVolumeSearchViewModelFactory(
    private val apiKeyStore: ComicVineApiKeyStore,
    private val apiClient: ComicVineApiClient
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                ComicVineVolumeSearchViewModel::class.java
            )
        ) {
            return ComicVineVolumeSearchViewModel(
                apiKeyStore = apiKeyStore,
                apiClient = apiClient
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}