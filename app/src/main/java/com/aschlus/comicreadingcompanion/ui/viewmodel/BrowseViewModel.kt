package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BrowseViewModel(
    private val repository: ComicRepository
) : ViewModel() {

    private val _publishers =
        MutableStateFlow<List<Publisher>>(emptyList())

    val publishers: StateFlow<List<Publisher>> =
        _publishers.asStateFlow()

    init {
        viewModelScope.launch {
            repository
                .getPublishersFlow()
                .collect { updatedPublishers ->
                    _publishers.value = updatedPublishers
                }
        }
    }
}