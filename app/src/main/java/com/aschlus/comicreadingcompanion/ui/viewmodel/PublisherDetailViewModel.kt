package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.models.PublisherSeries
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PublisherDetailViewModel(
    private val repository: ComicRepository
) : ViewModel() {

    private var publisherJob: Job? = null
    private var seriesJob: Job? = null

    private val _publisher =
        MutableStateFlow<Publisher?>(null)

    val publisher: StateFlow<Publisher?> =
        _publisher.asStateFlow()

    private val _series =
        MutableStateFlow<List<PublisherSeries>>(emptyList())

    val series: StateFlow<List<PublisherSeries>> =
        _series.asStateFlow()

    fun loadPublisher(
        publisherId: Long
    ) {
        publisherJob?.cancel()
        seriesJob?.cancel()

        publisherJob = viewModelScope.launch {
            repository
                .getPublisherById(publisherId)
                .collect { updatedPublisher ->
                    _publisher.value =
                        updatedPublisher
                }
        }

        seriesJob = viewModelScope.launch {
            repository
                .getPublisherSeries(publisherId)
                .collect { updatedSeries ->
                    _series.value =
                        updatedSeries
                }
        }
    }
}