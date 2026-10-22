package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aschlus.comicreadingcompanion.data.preferences.ComicVineApiKeyStore
import com.aschlus.comicreadingcompanion.data.remote.ComicVineApiClient
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository

class ComicVineSeriesRefreshViewModelFactory(
    private val apiKeyStore: ComicVineApiKeyStore,
    private val apiClient: ComicVineApiClient,
    private val repository: ComicRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                ComicVineSeriesRefreshViewModel::class.java
            )
        ) {
            return ComicVineSeriesRefreshViewModel(
                apiKeyStore = apiKeyStore,
                apiClient = apiClient,
                repository = repository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}