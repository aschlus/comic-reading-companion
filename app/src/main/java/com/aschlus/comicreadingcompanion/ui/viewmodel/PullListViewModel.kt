package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.models.PullListScheduledReleaseResult
import com.aschlus.comicreadingcompanion.data.database.models.PullListSeriesResult
import com.aschlus.comicreadingcompanion.data.preferences.MetronApiTokenStore
import com.aschlus.comicreadingcompanion.data.remote.ReleaseProviderResult
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import com.aschlus.comicreadingcompanion.data.repository.ScheduledReleaseRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PullListReleaseDateGroup(
    val date: LocalDate,
    val releases: List<PullListScheduledReleaseResult>
)

class PullListViewModel(
    private val repository: ComicRepository,
    private val scheduledReleaseRepository: ScheduledReleaseRepository,
    private val metronApiTokenStore: MetronApiTokenStore,
    currentDate: () -> LocalDate = LocalDate::now
) : ViewModel() {

    private val today = currentDate()

    private val syncFromDate = today.withDayOfMonth(1)

    private val syncToDate =
        today.withDayOfMonth(1)
            .plusMonths(2)
            .minusDays(1)

    private val _series =
        MutableStateFlow<List<PullListSeriesResult>>(emptyList())

    val series: StateFlow<List<PullListSeriesResult>> =
        _series.asStateFlow()

    private val _isLoading =
        MutableStateFlow(true)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()

    private val _storeReleaseGroups =
        MutableStateFlow<List<PullListReleaseDateGroup>>(emptyList())

    val storeReleaseGroups: StateFlow<List<PullListReleaseDateGroup>> =
        _storeReleaseGroups.asStateFlow()

    private val _focDeadlineGroups =
        MutableStateFlow<List<PullListReleaseDateGroup>>(emptyList())

    val focDeadlineGroups: StateFlow<List<PullListReleaseDateGroup>> =
        _focDeadlineGroups.asStateFlow()

    private val _isLoadingReleases =
        MutableStateFlow(true)

    val isLoadingReleases: StateFlow<Boolean> =
        _isLoadingReleases.asStateFlow()

    private val _isRefreshing =
        MutableStateFlow(false)

    val isRefreshing: StateFlow<Boolean> =
        _isRefreshing.asStateFlow()

    private val _refreshMessage =
        MutableStateFlow<String?>(null)

    val refreshMessage: StateFlow<String?> =
        _refreshMessage.asStateFlow()

    private val _refreshError =
        MutableStateFlow<String?>(null)

    val refreshError: StateFlow<String?> =
        _refreshError.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getPullListSeries().collect { trackedSeries ->
                _series.value = trackedSeries
                _isLoading.value = false
            }
        }

        viewModelScope.launch {
            scheduledReleaseRepository
                .getPullListScheduledReleasesInRange(
                    providerId = "METRON",
                    fromDate = syncFromDate,
                    toDate = syncToDate
                )
                .collect { releases ->

                    _storeReleaseGroups.value =
                        groupReleases(
                            releases = releases,
                            dateSelector = { it.storeReleaseDate  }
                        )

                    _focDeadlineGroups.value =
                        groupReleases(
                            releases = releases,
                            dateSelector = { it.focDate }
                        )

                    _isLoadingReleases.value = false
                }
        }
    }

    fun refreshUpcomingReleases() {
        if (_isRefreshing.value) {
            return
        }

        viewModelScope.launch {
            _isRefreshing.value = true
            _refreshMessage.value = null
            _refreshError.value = null

            try {
                val token = metronApiTokenStore.get()

                if (token.isNullOrBlank()) {
                    _refreshError.value =
                        "Configure your Metron API token in Settings before refreshing."
                    return@launch
                }

                when (
                    val result =
                        scheduledReleaseRepository.syncScheduledReleases(
                            accessToken = token,
                            fromDate = syncFromDate,
                            toDate = syncToDate
                        )
                ) {
                    is ReleaseProviderResult.Success -> {
                        val summary = result.value

                        val matchedCount =
                            summary.linkedCount +
                                summary.alreadyLinkedCount

                        _refreshMessage.value =
                            "Refreshed ${summary.fetchedCount} releases. " +
                                "$matchedCount matched to your catalog."
                    }

                    ReleaseProviderResult.MissingCredentials -> {
                        _refreshError.value =
                            "Configure your Metron API token in Settings."
                    }

                    ReleaseProviderResult.InvalidCredentials -> {
                        _refreshError.value =
                            "Metron rejected your API token. Check it in Settings."
                    }

                    ReleaseProviderResult.AccessDenied -> {
                        _refreshError.value =
                            "Metron denied access to release data."
                    }

                    is ReleaseProviderResult.RateLimited -> {
                        _refreshError.value =
                            result.retryAfterSeconds?.let {
                                "Metron rate-limited the request. Try again in $it seconds."
                            } ?: "Metron rate-limited the request. Try again later."
                    }

                    ReleaseProviderResult.NetworkError -> {
                        _refreshError.value =
                            "Could not reach Metron. Check your connection."
                    }

                    is ReleaseProviderResult.ServiceError -> {
                        _refreshError.value = result.message
                    }
                }
            } catch (exception: Exception) {
                _refreshError.value =
                    exception.message
                        ?: "Could not refresh scheduled releases."
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun groupReleases(
        releases: List<PullListScheduledReleaseResult>,
        dateSelector: (PullListScheduledReleaseResult) -> String?
    ): List<PullListReleaseDateGroup> {

        return releases
            .mapNotNull { release ->
                val dateText = dateSelector(release)
                    ?: return@mapNotNull null

                val date = runCatching {
                    LocalDate.parse(dateText)
                }.getOrNull() ?: return@mapNotNull null

                if (date.isBefore(today) || date.isAfter(syncToDate)) {
                    return@mapNotNull null
                }

                date to release
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )
            .toSortedMap()
            .map { (date, groupedReleases) ->
                PullListReleaseDateGroup(
                    date = date,
                    releases = groupedReleases
                )
            }
    }
}