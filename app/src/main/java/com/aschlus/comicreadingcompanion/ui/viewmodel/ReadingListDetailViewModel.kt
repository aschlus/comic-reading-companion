package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReadingListDetailViewModel(
    private val repository: ComicRepository
) : ViewModel() {

    private val _readingList =
        MutableStateFlow<ReadingList?>(null)

    val readingList: StateFlow<ReadingList?> =
        _readingList.asStateFlow()

    fun loadReadingList(readingListId: Long) {
        viewModelScope.launch {
            _readingList.value =
                repository.getReadingListById(readingListId)
        }
    }
}