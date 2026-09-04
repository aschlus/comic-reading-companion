package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListContinueItem
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListSummary
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    repository: ComicRepository
) : ViewModel() {

    val readingLists: StateFlow<List<ReadingList>> =
        repository
            .getReadingLists()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val readingListSummaries: StateFlow<List<ReadingListSummary>> =
        repository
            .getReadingListSummaries()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val continueItems: StateFlow<Map<Long, ReadingListContinueItem>> =
        repository
            .getUnreadReadingListItems()
            .map { items ->
                items
                    .groupBy { it.readingListId }
                    .mapValues { (_, listItems) ->
                        listItems.first()
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyMap()
            )
}