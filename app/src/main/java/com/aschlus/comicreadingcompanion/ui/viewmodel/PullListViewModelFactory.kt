
package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aschlus.comicreadingcompanion.data.preferences.MetronApiTokenStore
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import com.aschlus.comicreadingcompanion.data.repository.ScheduledReleaseRepository

class PullListViewModelFactory(
    private val repository: ComicRepository,
    private val scheduledReleaseRepository: ScheduledReleaseRepository,
    private val metronApiTokenStore: MetronApiTokenStore
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                PullListViewModel::class.java
            )
        ) {
            return PullListViewModel(
                repository = repository,
                scheduledReleaseRepository = scheduledReleaseRepository,
                metronApiTokenStore = metronApiTokenStore
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
