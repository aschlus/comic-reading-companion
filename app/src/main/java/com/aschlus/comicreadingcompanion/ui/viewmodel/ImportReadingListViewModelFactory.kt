package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aschlus.comicreadingcompanion.data.importer.ReadingListAssetParser
import com.aschlus.comicreadingcompanion.data.importer.ReadingListImporter

class ImportReadingListViewModelFactory(
    private val parser: ReadingListAssetParser,
    private val importer: ReadingListImporter
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        return ImportReadingListViewModel(
            parser = parser,
            importer = importer
        ) as T
    }
}