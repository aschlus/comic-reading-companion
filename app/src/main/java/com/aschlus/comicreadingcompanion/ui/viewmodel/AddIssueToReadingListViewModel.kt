package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSource
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddIssueToReadingListViewModel(
    private val issueId: Long,
    private val repository: ComicRepository
) : ViewModel() {

    val userReadingLists: StateFlow<List<ReadingList>> =
        repository.getReadingLists()
            .map { readingLists ->
                readingLists.filter { readingList ->
                    readingList.source == ReadingListSource.USER
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private val _isAdding = MutableStateFlow(false)
    val isAdding: StateFlow<Boolean> = _isAdding.asStateFlow()

    private val _addedReadingListId = MutableStateFlow<Long?>(null)
    val addedReadingListId: StateFlow<Long?> = _addedReadingListId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun addToReadingList(
        readingListId: Long
    ) {
        if (_isAdding.value) {
            return
        }

        viewModelScope.launch {
            _isAdding.value = true
            _errorMessage.value = null

            try {
                repository.addIssueToUserReadingList(
                    readingListId = readingListId,
                    issueId = issueId
                )

                _addedReadingListId.value = readingListId
            } catch (
                exception: IllegalArgumentException
            ) {
                _errorMessage.value = exception.message
                    ?: "Could not add issue"
            } finally {
                _isAdding.value = false
            }
        }
    }

    fun clearResult() {
        _addedReadingListId.value = null
        _errorMessage.value = null
    }
}