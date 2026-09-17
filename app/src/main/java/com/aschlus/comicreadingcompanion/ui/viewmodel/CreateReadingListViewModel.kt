package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListStyle
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import com.aschlus.comicreadingcompanion.data.database.models.IssueSearchResult
import com.aschlus.comicreadingcompanion.data.database.models.SeriesSearchResult
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class PendingReadingListIssue(
    val issueId: Long,
    val seriesId: Long,
    val seriesTitle: String,
    val issueNumber: String,
    val issueTitle: String?,
    val publicationDate: String?,
    val coverUrl: String?
)

enum class AddToListFilter {
    ALL,
    SERIES,
    ISSUES
}

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

    private val _selectedStyle = MutableStateFlow(ReadingListStyle.GREEN)
    val selectedStyle: StateFlow<ReadingListStyle> = _selectedStyle.asStateFlow()

    private val _pendingIssues = MutableStateFlow<List<PendingReadingListIssue>>(emptyList())
    val pendingIssues: StateFlow<List<PendingReadingListIssue>> = _pendingIssues.asStateFlow()

    private val _addToListDraftIssues =
        MutableStateFlow<List<PendingReadingListIssue>>(emptyList())
    val addToListDraftIssues: StateFlow<List<PendingReadingListIssue>> =
        _addToListDraftIssues.asStateFlow()

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

    private var addToListSearchJob: Job? = null

    private val _addToListQuery = MutableStateFlow("")
    val addToListQuery: StateFlow<String> = _addToListQuery.asStateFlow()

    private val _addToListFilter = MutableStateFlow(AddToListFilter.ALL)
    val addToListFilter: StateFlow<AddToListFilter> = _addToListFilter.asStateFlow()

    private val _addToListSeriesResults =
        MutableStateFlow<List<SeriesSearchResult>>(emptyList())
    val addToListSeriesResults: StateFlow<List<SeriesSearchResult>> =
        _addToListSeriesResults.asStateFlow()

    private val _addToListIssueResults =
        MutableStateFlow<List<IssueSearchResult>>(emptyList())
    val addToListIssueResults: StateFlow<List<IssueSearchResult>> =
        _addToListIssueResults.asStateFlow()

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

    fun selectStyle(
        style: ReadingListStyle
    ) {
        _selectedStyle.value = style
    }

    fun addPendingIssue(
        issue: PendingReadingListIssue
    ) {
        if (_pendingIssues.value.any { it.issueId == issue.issueId }) {
            return
        }

        _pendingIssues.value += issue
    }

    fun removePendingIssue(
        issueId: Long
    ) {
        _pendingIssues.value = _pendingIssues.value
            .filterNot { it.issueId == issueId }
    }

    fun clearPendingIssues() {
        _pendingIssues.value = emptyList()
    }

    fun movePendingIssue(
        issueId: Long,
        offset: Int
    ) {
        val currentIssues = _pendingIssues.value

        val currentIndex =
            currentIssues.indexOfFirst {
                it.issueId == issueId
            }

        if (currentIndex == -1) {
            return
        }

        val targetIndex = currentIndex + offset

        if (targetIndex !in currentIssues.indices) {
            return
        }

        val reorderedIssues =
            currentIssues.toMutableList()

        val movedIssue =
            reorderedIssues.removeAt(currentIndex)

        reorderedIssues.add(targetIndex, movedIssue)

        _pendingIssues.value = reorderedIssues

    }

    fun addPendingSeries(
        seriesId: Long,
        seriesTitle: String
    ) {
        viewModelScope.launch {
            val issues =
                repository.getIssuesForSeries(seriesId)

            val newPendingIssues =
                issues.map { issue ->
                    PendingReadingListIssue(
                        issueId = issue.id,
                        seriesId = issue.seriesId,
                        seriesTitle = seriesTitle,
                        issueNumber = issue.issueNumber,
                        issueTitle = issue.title,
                        publicationDate = issue.publicationDate,
                        coverUrl = issue.coverUrl
                    )
                }

            val existingIssueIds =
                _pendingIssues.value.map { it.issueId }.toSet()

            _pendingIssues.value += newPendingIssues
                            .filterNot { it.issueId in existingIssueIds }
        }
    }

    fun beginAddToListSession() {
        _addToListDraftIssues.value = _pendingIssues.value
    }

    fun cancelAddToListSession() {
        _addToListDraftIssues.value = emptyList()

        clearAddToListSearch()
    }

    fun applyAddToListSession() {
        _pendingIssues.value = _addToListDraftIssues.value

        _addToListDraftIssues.value = emptyList()

        clearAddToListSearch()
    }

    fun toggleAddToListIssue(
        result: IssueSearchResult
    ) {
        val isSelected =
            _addToListDraftIssues.value.any {
                it.issueId == result.issueId
            }

        if (isSelected) {
            _addToListDraftIssues.value =
                _addToListDraftIssues.value
                    .filterNot {
                        it.issueId == result.issueId
                    }

            return
        }

        _addToListDraftIssues.value += PendingReadingListIssue(
                                issueId = result.issueId,
                                seriesId = result.seriesId,
                                seriesTitle = result.seriesTitle,
                                issueNumber = result.issueNumber,
                                issueTitle = result.issueTitle,
                                publicationDate = result.publicationDate,
                                coverUrl = null
                            )
    }

    fun isAddToListIssueSelected(
        issueId: Long
    ): Boolean =
        _addToListDraftIssues.value.any {
            it.issueId == issueId
        }

    fun toggleAddToListSeries(
        result: SeriesSearchResult
    ) {
        viewModelScope.launch {
            val issues =
                repository.getIssuesForSeries(
                    result.seriesId
                )

            val seriesIssueIds =
                issues.map { it.id }.toSet()

            val allSelected =
                seriesIssueIds.isNotEmpty() &&
                        seriesIssueIds.all { issueId ->
                            _addToListDraftIssues
                                .value
                                .any {
                                    it.issueId == issueId
                                }
                        }

            if (allSelected) {
                _addToListDraftIssues.value =
                    _addToListDraftIssues
                        .value
                        .filterNot {
                            it.issueId in seriesIssueIds
                        }

                return@launch
            }

            val existingIssueIds =
                _addToListDraftIssues
                    .value.map { it.issueId }.toSet()

            val newIssues =
                issues
                    .filterNot { it.id in existingIssueIds }
                    .map { issue ->
                        PendingReadingListIssue(
                            issueId = issue.id,
                            seriesId = issue.seriesId,
                            seriesTitle = result.title,
                            issueNumber = issue.issueNumber,
                            issueTitle = issue.title,
                            publicationDate = issue.publicationDate,
                            coverUrl = issue.coverUrl
                        )
                    }

            _addToListDraftIssues.value += newIssues
        }
    }

    fun isAddToListSeriesSelected(
        seriesId: Long,
        totalCount: Int
    ): Boolean {
        if (totalCount == 0) {
            return false
        }

        return _addToListDraftIssues
            .value
            .count {
                it.seriesId == seriesId
            } == totalCount
    }

    fun updateAddToListQuery(
        query: String
    ) {
        _addToListQuery.value = query

        addToListSearchJob?.cancel()

        val trimmedQuery = query.trim()

        if (trimmedQuery.isBlank()) {
            _addToListSeriesResults.value = emptyList()
            _addToListIssueResults.value = emptyList()
            return
        }

        addToListSearchJob =
            viewModelScope.launch {
                delay(250.milliseconds)

                combine(
                    repository.searchSeries(trimmedQuery),
                    repository.searchIssues(trimmedQuery)
                ) {
                    seriesResults, issueResults ->

                    seriesResults to issueResults
                }.collect { results ->
                    _addToListSeriesResults.value = results.first
                    _addToListIssueResults.value = results.second
                }
            }
    }

    fun selectAddToListFilter(
        filter: AddToListFilter
    ) {
        _addToListFilter.value = filter
    }

    fun clearAddToListSearch() {
        addToListSearchJob?.cancel()

        _addToListQuery.value = ""
        _addToListSeriesResults.value = emptyList()
        _addToListIssueResults.value = emptyList()
        _addToListFilter.value = AddToListFilter.ALL
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
                    .createUserReadingListWithIssues(
                        title = trimmedTitle,
                        description = description,
                        publisherId = publisherId,
                        universeId = _selectedUniverseId.value,
                        style = _selectedStyle.value,
                        issueIds = _pendingIssues.value.map { it.issueId }
                    )

            _createdReadingListId.value = readingListId
        }
    }
}