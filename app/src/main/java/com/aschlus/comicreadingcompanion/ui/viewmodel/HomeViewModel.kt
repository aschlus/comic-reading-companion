package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: ComicRepository
) : ViewModel() {

    private val _readingLists =
        MutableStateFlow<List<ReadingList>>(emptyList())

    val readingLists: StateFlow<List<ReadingList>> =
        _readingLists.asStateFlow()

    fun loadReadingLists() {
        viewModelScope.launch {
            _readingLists.value = repository.getReadingLists()
        }
    }
}