package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListContinueItem
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListSummary
import com.aschlus.comicreadingcompanion.data.preferences.HomeUiPreferences
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeReadingListSort {
    RECENTLY_UPDATED,
    TITLE_ASCENDING,
    TITLE_DESCENDING
}

class HomeViewModel(
    repository: ComicRepository,
    private val homeUiPreferences: HomeUiPreferences
) : ViewModel() {

    val readingLists: StateFlow<List<ReadingList>> =
        repository
            .getReadingLists()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val recentlyOpenedReadingLists: StateFlow<List<ReadingList>> =
        combine(
            readingLists,
            homeUiPreferences.recentlyOpenedReadingListIds
        ) { readingLists, recentIds ->

            val readingListsById =
                readingLists.associateBy { it.id }

            recentIds.mapNotNull { readingListId ->
                readingListsById[readingListId]
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun recordReadingListOpened(
        readingListId: Long
    ) {
        viewModelScope.launch {
            homeUiPreferences
                .recordReadingListOpened(
                    readingListId
                )
        }
    }

    private val _searchQuery = MutableStateFlow("")

    val searchQuery: StateFlow<String> =
        _searchQuery.asStateFlow()

    private val _sort =
        MutableStateFlow(HomeReadingListSort.RECENTLY_UPDATED)

    val sort: StateFlow<HomeReadingListSort> =
        _sort.asStateFlow()

    fun updateSort(
        sort: HomeReadingListSort
    ) {
        _sort.value = sort
    }

    val visibleReadingLists: StateFlow<List<ReadingList>> =
        combine(
            readingLists,
            searchQuery,
            sort
        ) {readingLists, query, sort ->
            val trimmedQuery = query.trim()

            val filteredReadingLists =
                if (trimmedQuery.isEmpty()) {
                    readingLists
                } else {
                    readingLists.filter { readingList ->
                        readingList.title.contains(
                            other = trimmedQuery,
                            ignoreCase = true
                        ) ||
                            readingList.description
                                ?.contains(
                                    other = trimmedQuery,
                                    ignoreCase = true
                                ) == true
                    }
                }

            when (sort) {
                HomeReadingListSort.RECENTLY_UPDATED -> {
                    filteredReadingLists.sortedByDescending {
                        it.updatedAt
                    }
                }

                HomeReadingListSort.TITLE_ASCENDING -> {
                    filteredReadingLists.sortedBy {
                        it.title.lowercase()
                    }
                }

                HomeReadingListSort.TITLE_DESCENDING -> {
                    filteredReadingLists.sortedByDescending {
                        it.title.lowercase()
                    }
                }
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun updateSearchQuery(
        query: String
    ) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

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

    val continueReadingLists: StateFlow<List<ReadingList>> =
        combine(
            readingLists,
            readingListSummaries,
            continueItems,
            recentlyOpenedReadingLists
        ) {
            readingLists,
            summaries,
            continueItems,
            recentlyOpened ->

            val summariesById =
                summaries.associateBy { it.readingListId}

            val recentOrder =
                recentlyOpened
                    .mapIndexed { index, readingList ->
                        readingList.id to index
                    }
                    .toMap()

            readingLists
                .filter { readingList ->
                    val summary =
                        summariesById[readingList.id]

                    summary != null &&
                        summary.readCount > 0 &&
                        summary.readCount < summary.totalCount &&
                        continueItems.containsKey(readingList.id)
                }
                .sortedWith(
                    compareBy<ReadingList> {
                        recentOrder[it.id]
                            ?: Int.MAX_VALUE
                    }
                        .thenByDescending {
                            it.updatedAt
                        }
                )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
}