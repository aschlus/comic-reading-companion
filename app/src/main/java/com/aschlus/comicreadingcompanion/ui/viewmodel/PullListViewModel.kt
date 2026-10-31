package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.models.PullListSeriesResult
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PullListViewModel(
    repository: ComicRepository
) : ViewModel() {

    private val _series =
        MutableStateFlow<List<PullListSeriesResult>>(emptyList())

    val series: StateFlow<List<PullListSeriesResult>> =
        _series.asStateFlow()

    private val _isLoading =
        MutableStateFlow(true)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getPullListSeries().collect { trackedSeries ->
                _series.value = trackedSeries
                _isLoading.value = false
            }
        }
    }
}