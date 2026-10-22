package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditSeriesViewModel(
    private val repository: ComicRepository
) : ViewModel() {

    val publishers: StateFlow<List<Publisher>> =
        repository.getPublishersFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _volume = MutableStateFlow("")
    val volume: StateFlow<String> = _volume.asStateFlow()

    private val _startYear = MutableStateFlow("")
    val startYear: StateFlow<String> = _startYear.asStateFlow()

    private val _endYear = MutableStateFlow("")
    val endYear: StateFlow<String> = _endYear.asStateFlow()

    private val _selectedPublisherId = MutableStateFlow<Long?>(null)
    val selectedPublisherId: StateFlow<Long?> = _selectedPublisherId.asStateFlow()

    private val _universes = MutableStateFlow<List<Universe>>(emptyList())
    val universes: StateFlow<List<Universe>> = _universes.asStateFlow()

    private val _selectedUniverseId = MutableStateFlow<Long?>(null)
    val selectedUniverseId: StateFlow<Long?> = _selectedUniverseId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _seriesSaved = MutableStateFlow(false)
    val seriesSaved: StateFlow<Boolean> = _seriesSaved.asStateFlow()

    private var currentSeriesId: Long? = null
    private var loadJob: Job? = null
    private var universeLoadJob: Job? = null

    fun loadSeries(seriesId: Long) {
        if (
            currentSeriesId == seriesId &&
            !_isLoading.value
        ) {
            return
        }

        loadJob?.cancel()
        universeLoadJob?.cancel()

        _isLoading.value = true
        _errorMessage.value = null
        _seriesSaved.value = false

        loadJob = viewModelScope.launch {
            try {
                val series =
                    repository.getSeriesById(seriesId)
                        ?: throw IllegalArgumentException(
                            "Series $seriesId does not exist"
                        )

                currentSeriesId = series.id
                _title.value = series.title
                _volume.value = series.volume?.toString().orEmpty()
                _startYear.value = series.startYear?.toString().orEmpty()
                _endYear.value = series.endYear?.toString().orEmpty()

                _selectedPublisherId.value = series.publisherId
                _selectedUniverseId.value = series.universeId

                val loadedUniverses =
                    repository.getUniverseForPublisher(
                        series.publisherId
                    )

                _universes.value = loadedUniverses

                if (
                    series.universeId != null &&
                    loadedUniverses.none {
                        it.id == series.universeId
                    }
                ) {
                    _selectedUniverseId.value = null
                }
            } catch (exception: Exception) {
                _errorMessage.value =
                    exception.message ?: "Could not load series"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTitle(value: String) {
        _title.value = value
    }

    fun updateVolume(value: String) {
        _volume.value = value
    }

    fun updateStartYear(value: String) {
        _startYear.value = value
    }

    fun updateEndYear(value: String) {
        _endYear.value = value
    }

    fun selectPublisher(publisherId: Long) {
        if (_selectedPublisherId.value == publisherId) {
            return
        }

        _selectedPublisherId.value = publisherId
        _selectedUniverseId.value = null
        _universes.value = emptyList()
        _errorMessage.value = null

        universeLoadJob?.cancel()
        universeLoadJob = viewModelScope.launch {
            try {
                val loadedUniverses =
                    repository.getUniverseForPublisher(publisherId)

                if (_selectedPublisherId.value == publisherId) {
                    _universes.value = loadedUniverses
                }
            } catch (exception: Exception) {
                _errorMessage.value =
                    exception.message ?: "Could not load universes"
            }
        }
    }

    fun selectUniverse(universeId: Long?) {
        _selectedUniverseId.value = universeId
        _errorMessage.value = null
    }

    fun createPublisher(name: String) {
        viewModelScope.launch {
            try {
                val trimmedName = name.trim()

                require(trimmedName.isNotEmpty()) {
                    "Publisher name cannot be blank"
                }

                val existingPublisher =
                    repository.getPublishers().firstOrNull {
                        it.name.equals(
                            trimmedName,
                            ignoreCase = true
                        )
                    }

                val publisherId =
                    existingPublisher?.id
                        ?: repository.addPublisher(
                            Publisher(name = trimmedName)
                        )

                _errorMessage.value = null
                selectPublisher(publisherId)
            } catch (exception: Exception) {
                _errorMessage.value =
                    exception.message ?: "Could not create publisher"
            }
        }
    }

    fun createUniverse(
        name: String,
        designation: String?,
        description: String?
    ) {
        viewModelScope.launch {
            try {
                val publisherId =
                    _selectedPublisherId.value
                        ?: throw IllegalArgumentException(
                            "Select a publisher before creating a universe"
                        )

                val trimmedName = name.trim()

                require(trimmedName.isNotEmpty()) {
                    "Universe name cannot be blank"
                }

                val universe =
                    Universe(
                        publisherId = publisherId,
                        name = trimmedName,
                        designation = designation
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() },
                        description = description
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                    )

                val universeId = repository.addUniverse(universe)

                _universes.value =
                    repository.getUniverseForPublisher(publisherId)

                _selectedUniverseId.value = universeId
                _errorMessage.value = null
            } catch (exception: Exception) {
                _errorMessage.value =
                    exception.message ?: "Could not create universe"
            }
        }
    }

    fun saveSeries() {
        if (_isSaving.value) {
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null

            try {
                val seriesId =
                    currentSeriesId
                        ?: throw IllegalStateException(
                            "No series has been loaded"
                        )

                val publisherId =
                    _selectedPublisherId.value
                        ?: throw IllegalArgumentException(
                            "Select a publisher"
                        )

                repository.updateSeriesDetails(
                    seriesId = seriesId,
                    title = _title.value,
                    volume = parseOptionalInt(
                        _volume.value,
                        "Volume"
                    ),
                    startYear = parseOptionalInt(
                        _startYear.value,
                        "Start year"
                    ),
                    endYear = parseOptionalInt(
                        _endYear.value,
                        "End year"
                    ),
                    publisherId = publisherId,
                    universeId = _selectedUniverseId.value
                )

                _seriesSaved.value = true
            } catch (exception: Exception) {
                _errorMessage.value =
                    exception.message ?: "Could not save series"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun parseOptionalInt(
        value: String,
        fieldName: String
    ): Int? {
        val trimmedValue = value.trim()

        if (trimmedValue.isEmpty()) {
            return null
        }

        return trimmedValue.toIntOrNull()
            ?: throw IllegalArgumentException(
                "$fieldName must be a whole number"
            )
    }
}