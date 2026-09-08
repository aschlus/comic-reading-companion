package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CreateReadingListViewModel(
    private val repository: ComicRepository
) : ViewModel() {

    val publishers: StateFlow<List<Publisher>> =
        repository
            .getPublishersFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _selectedPublisherId = MutableStateFlow<Long?>(null)
    val selectedPublisherId: StateFlow<Long?> = _selectedPublisherId.asStateFlow()

    val canCreateReadingList: StateFlow<Boolean> =
        combine(
            _title,
            _selectedPublisherId
        ) { title, publisherId ->
            title.isNotBlank() && publisherId != null
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    private val _universes = MutableStateFlow<List<Universe>>(emptyList())
    val universes: StateFlow<List<Universe>> = _universes.asStateFlow()

    private val _selectedUniverseId = MutableStateFlow<Long?>(null)
    val selectedUniverseId: StateFlow<Long?> = _selectedUniverseId.asStateFlow()

    private var universeLoadJob: Job? = null

    private val _createdReadingListId = MutableStateFlow<Long?>(null)
    val createdReadingListId: StateFlow<Long?> = _createdReadingListId.asStateFlow()

    fun updateTitle(
        title: String
    ) {
        _title.value = title
    }

    fun updateDescription(
        description: String
    ) {
        _description.value = description
    }

    fun selectPublisher(
        publisherId: Long
    ) {
        _selectedPublisherId.value = publisherId

        _selectedUniverseId.value = null
        _universes.value = emptyList()

        universeLoadJob?.cancel()

        universeLoadJob =
            viewModelScope.launch {
                val universes =
                    repository
                        .getUniverseForPublisher(publisherId)

                if (_selectedPublisherId.value == publisherId) {
                    _universes.value = universes
                }
            }
    }

    fun selectUniverse(
        universeId: Long?
    ) {
        _selectedUniverseId.value = universeId
    }

    fun createReadingList() {
        val publisherId = _selectedPublisherId.value
            ?: return

        val trimmedTitle = _title.value.trim()

        if (trimmedTitle.isEmpty()) {
            return
        }

        val description =
            _description.value
                .trim()
                .takeIf { it.isNotEmpty() }

        viewModelScope.launch {
            val readingListId =
                repository
                    .createUserReadingList(
                        title = trimmedTitle,
                        description = description,
                        publisherId = publisherId,
                        universeId = _selectedUniverseId.value
                    )

            _createdReadingListId.value = readingListId
        }
    }
}