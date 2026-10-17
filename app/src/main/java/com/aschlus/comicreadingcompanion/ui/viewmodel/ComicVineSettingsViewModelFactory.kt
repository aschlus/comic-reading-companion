package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aschlus.comicreadingcompanion.data.preferences.ComicVineApiKeyStore

class ComicVineSettingsViewModelFactory(
    private val apiKeyStore: ComicVineApiKeyStore
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                ComicVineSettingsViewModel::class.java
            )
        ) {
            return ComicVineSettingsViewModel(
                apiKeyStore = apiKeyStore
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}