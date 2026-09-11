package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aschlus.comicreadingcompanion.data.preferences.ReadingListUiPreferences
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository

class ReadingListDetailViewModelFactory(
    private val repository: ComicRepository,
    private val readingListUiPreferences: ReadingListUiPreferences
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                ReadingListDetailViewModel::class.java
            )
        ) {
            @Suppress("UNCHECKED_CAST")
            return ReadingListDetailViewModel(
                repository = repository,
                readingListUiPreferences = readingListUiPreferences
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}