package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.importer.ReadingListAssetParser
import com.aschlus.comicreadingcompanion.data.importer.ReadingListImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ImportReadingListState {
    data object Idle:
        ImportReadingListState

    data object Importing:
        ImportReadingListState

    data class Success(
        val title: String
    ) : ImportReadingListState

    data class Error(
        val message: String
    ) : ImportReadingListState
}

class ImportReadingListViewModel(
    private val parser: ReadingListAssetParser,
    private val importer: ReadingListImporter
) : ViewModel() {

    private val _state =
        MutableStateFlow<ImportReadingListState>(ImportReadingListState.Idle)

    val state: StateFlow<ImportReadingListState> =
        _state.asStateFlow()

    fun importReadingList(
        jsonText: String,
        sourceDescription: String
    ) {
        if (_state.value == ImportReadingListState.Importing) {
            return
        }

        _state.value = ImportReadingListState.Importing

        viewModelScope.launch {
            try {
                val importData =
                    parser.parseJson(
                        jsonText = jsonText,
                        sourceDescription = sourceDescription
                    )

                importer.import(importData)

                _state.value = ImportReadingListState.Success(
                    title = importData.title
                )
            } catch (
                exception: Exception
            ) {
                _state.value =
                    ImportReadingListState.Error(
                        message =
                            exception.message
                                ?: "Reading-list import failed"
                    )
            }
        }
    }

    fun reportError(
        message: String
    ) {
        _state.value = ImportReadingListState.Error(
            message = message
        )
    }

    fun clearResult() {
        _state.value = ImportReadingListState.Idle
    }
}