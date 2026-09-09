package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository

class AddIssueToReadingListViewModelFactory(
    private val issueId: Long,
    private val repository: ComicRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                AddIssueToReadingListViewModel::class.java
            )
        ) {
            @Suppress("UNCHECKED_CAST")
            return AddIssueToReadingListViewModel(
                issueId = issueId,
                repository = repository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}