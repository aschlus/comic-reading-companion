package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import com.aschlus.comicreadingcompanion.data.importer.ComicCatalogImporter
import com.aschlus.comicreadingcompanion.data.preferences.ComicVineApiKeyStore
import com.aschlus.comicreadingcompanion.data.remote.ComicVineApiClient
import kotlinx.coroutines.flow.Flow
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository

class ComicVineVolumeReviewViewModelFactory(
    private val apiKeyStore: ComicVineApiKeyStore,
    private val apiClient: ComicVineApiClient,
    private val comicRepository: ComicRepository,
    private val universeFlow: Flow<List<Universe>>,
    private val comicCatalogImporter: ComicCatalogImporter
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                ComicVineVolumeReviewViewModel::class.java
            )
        ) {
            return ComicVineVolumeReviewViewModel(
                apiKeyStore = apiKeyStore,
                apiClient = apiClient,
                comicRepository = comicRepository,
                universeFlow = universeFlow,
                comicCatalogImporter = comicCatalogImporter
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}