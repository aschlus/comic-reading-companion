package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository

class CreateReadingListViewModelFactory(
    private val repository: ComicRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                CreateReadingListViewModel::class.java
            )
        ) {
            @Suppress("UNCHECKED_CAST")
            return CreateReadingListViewModel(
                repository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}