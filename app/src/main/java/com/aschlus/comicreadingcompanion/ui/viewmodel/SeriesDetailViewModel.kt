package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.models.SeriesDetail
import com.aschlus.comicreadingcompanion.data.database.models.SeriesIssue
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SeriesDetailViewModel(
    private val repository: ComicRepository
) : ViewModel() {

    private var seriesJob: Job? = null
    private var issuesJob: Job? = null
    private var comicVineVolumeIdJob: Job? = null
    private var pullListJob: Job? = null

    private val _comicVineVolumeId =
        MutableStateFlow<Long?>(null)

    val comicVineVolumeId: StateFlow<Long?> =
        _comicVineVolumeId.asStateFlow()

    private val _series =
        MutableStateFlow<SeriesDetail?>(null)

    val series: StateFlow<SeriesDetail?> =
        _series.asStateFlow()

    private val _issues =
        MutableStateFlow<List<SeriesIssue>>(emptyList())

    val issues: StateFlow<List<SeriesIssue>> =
        _issues.asStateFlow()

    private val _isOnPullList =
        MutableStateFlow(false)

    val isOnPullList: StateFlow<Boolean> =
        _isOnPullList.asStateFlow()

    fun loadSeries(
        seriesId: Long
    ) {
        seriesJob?.cancel()
        issuesJob?.cancel()
        comicVineVolumeIdJob?.cancel()
        pullListJob?.cancel()

        _comicVineVolumeId.value = null
        _isOnPullList.value = false

        comicVineVolumeIdJob = viewModelScope.launch {
            _comicVineVolumeId.value =
                repository.getComicVineVolumeIdForSeries(seriesId)
        }

        seriesJob = viewModelScope.launch {
            repository
                .getSeriesDetail(seriesId)
                .collect { updatedSeries ->
                    _series.value = updatedSeries
                }
        }

        issuesJob = viewModelScope.launch {
            repository
                .getSeriesIssues(seriesId)
                .collect { updatedIssues ->
                    _issues.value = updatedIssues
                }
        }

        pullListJob = viewModelScope.launch {
            repository
                .isSeriesOnPullList(seriesId)
                .collect { isTracked ->
                    _isOnPullList.value = isTracked
                }
        }
    }

    fun addSeriesToPullList(
        seriesId: Long
    ) {
        viewModelScope.launch {
            repository.addSeriesToPullList(seriesId)
        }
    }

    fun removeSeriesFromPullList(
        seriesId: Long
    ) {
        viewModelScope.launch {
            repository.removeSeriesFromPullList(seriesId)
        }
    }
}