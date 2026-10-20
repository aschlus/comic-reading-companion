package com.aschlus.comicreadingcompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import com.aschlus.comicreadingcompanion.data.preferences.ComicVineApiKeyStore
import com.aschlus.comicreadingcompanion.data.remote.ComicVineApiClient
import com.aschlus.comicreadingcompanion.data.remote.ComicVineIssueHit
import com.aschlus.comicreadingcompanion.data.remote.ComicVineIssuePageOutcome
import com.aschlus.comicreadingcompanion.data.remote.ComicVineVolumeDetails
import com.aschlus.comicreadingcompanion.data.remote.ComicVineVolumeDetailsOutcome
import com.aschlus.comicreadingcompanion.data.remote.ComicVineVolumeSearchHit
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import com.aschlus.comicreadingcompanion.data.importer.ComicCatalogImporter
import com.aschlus.comicreadingcompanion.data.importer.models.CatalogExternalIdImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.CatalogIssueImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.CatalogSeriesImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.CatalogUniverseImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.ComicCatalogImportDto

data class ComicVineVolumeReviewUiState(
    val isLoading: Boolean = false,
    val volumeId: Long? = null,
    val volume: ComicVineVolumeDetails? = null,
    val editedTitle: String = "",
    val editedPublisher: String = "",
    val editedVolume: String = "",
    val editedStartYear: String = "",
    val editedEndYear: String = "",
    val issues: List<ComicVineIssueHit> = emptyList(),
    val totalIssueRecords: Int? = null,
    val availableUniverses: List<Universe> = emptyList(),
    val selectedUniverseId: Long? = null,
    val isCreatingUniverse: Boolean = false,
    val universeCreationError: String? = null,
    val isImporting: Boolean = false,
    val importError: String? = null,
    val importSuccessMessage: String? = null,
    val error: String? = null
)

class ComicVineVolumeReviewViewModel(
    private val apiKeyStore: ComicVineApiKeyStore,
    private val apiClient: ComicVineApiClient,
    private val comicRepository: ComicRepository? = null,
    private val universeFlow: Flow<List<Universe>> = emptyFlow(),
    private val comicCatalogImporter: ComicCatalogImporter? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComicVineVolumeReviewUiState())
    val uiState: StateFlow<ComicVineVolumeReviewUiState> =
        _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            universeFlow.collect { universes ->
                _uiState.value = _uiState.value.copy(
                    availableUniverses = universes
                )
            }
        }
    }

    fun updateEditedTitle(title: String){
        _uiState.value = _uiState.value.copy(
            editedTitle = title,
            importError = null,
            importSuccessMessage = null
        )
    }

    fun updateEditedPublisher(publisher: String) {
        _uiState.value = _uiState.value.copy(
            editedPublisher = publisher,
            importError = null,
            importSuccessMessage = null
        )
    }

    fun updateEditedVolume(volume: String) {
        _uiState.value = _uiState.value.copy(
            editedVolume = volume,
            importError = null,
            importSuccessMessage = null
        )
    }

    fun updateEditedStartYear(year: String) {
        _uiState.value = _uiState.value.copy(
            editedStartYear = year,
            importError = null,
            importSuccessMessage = null
        )
    }

    fun updateEditedEndYear(year: String) {
        _uiState.value = _uiState.value.copy(
            editedEndYear = year,
            importError = null,
            importSuccessMessage = null
        )
    }

    fun selectUniverse(universeId: Long?) {
        val availableUniverses = _uiState.value.availableUniverses

        if (
            universeId != null &&
            availableUniverses.none { it.id == universeId }
        ) {
            return
        }

        _uiState.value = _uiState.value.copy(
            selectedUniverseId = universeId
        )
    }

    fun createUniverse(
        name: String,
        designation: String
    ) {
        val normalizedName = name.trim()
        val normalizedDesignation = designation.trim()
        val currentState = _uiState.value
        val publisherName = currentState.editedPublisher
            .trim()
            .takeIf { it.isNotEmpty() }

        when {
            normalizedName.isBlank() -> {
                _uiState.value = currentState.copy(
                    universeCreationError = "Enter a universe name."
                )
                return
            }

            normalizedDesignation.isBlank() -> {
                _uiState.value = currentState.copy(
                    universeCreationError = "Enter a continuity designation."
                )
                return
            }

            publisherName == null -> {
                _uiState.value = currentState.copy(
                    universeCreationError = "A publisher is required to create a universe."
                )
                return
            }
        }

        val repository = comicRepository

        if (repository == null) {
            _uiState.value = currentState.copy(
                universeCreationError = "Universe creation is unavailable."
            )
            return
        }

        _uiState.value = currentState.copy(
            isCreatingUniverse = true,
            universeCreationError = null
        )

        viewModelScope.launch {
            try {
                val publishers = repository.getPublishers()

                val publisher = publishers.firstOrNull {
                    it.name.equals(publisherName, ignoreCase = true)
                } ?: run {
                    val newPublisher = Publisher(name = publisherName)
                    val publisherId = repository.addPublisher(newPublisher)
                    newPublisher.copy(id = publisherId)
                }

                val existingUniverse =
                    repository.getUniverseForPublisher(publisher.id)
                        .firstOrNull { universe ->
                            universe.designation?.equals(
                                normalizedDesignation,
                                ignoreCase = true
                            ) == true
                        }

                val universe = existingUniverse ?: run {
                    val newUniverse = Universe(
                        publisherId = publisher.id,
                        name = normalizedName,
                        designation = normalizedDesignation,
                        description = null
                    )

                    val universeId = repository.addUniverse(newUniverse)
                    newUniverse.copy(id = universeId)
                }

                val latestState = _uiState.value

                _uiState.value = latestState.copy(
                    availableUniverses =
                        (
                            latestState.availableUniverses
                                .filterNot { it.id == universe.id } +
                                universe
                        ).sortedBy { it.name.lowercase() },
                    selectedUniverseId = universe.id,
                    isCreatingUniverse = false,
                    universeCreationError = null
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCreatingUniverse = false,
                    universeCreationError = "Could not create the universe. Please try again."
                )
            }
        }
    }

    fun importReviewedSeries() {
        val currentState = _uiState.value

        if (currentState.isImporting) {
            return
        }

        if (currentState.isLoading) {
            _uiState.value = currentState.copy(
                importError = "Wait for the series to finish loading before importing.",
                importSuccessMessage = null
            )
            return
        }

        val volume = currentState.volume

        if (volume == null) {
            _uiState.value = currentState.copy(
                importError = "Load a series before importing it.",
                importSuccessMessage = null
            )
            return
        }

        val title = currentState.editedTitle.trim()

        if (title.isBlank()) {
            _uiState.value = currentState.copy(
                importError = "Enter a series title before importing.",
                importSuccessMessage = null
            )
            return
        }

        val publisherName = currentState.editedPublisher.trim()

        if (publisherName.isBlank()) {
            _uiState.value = currentState.copy(
                importError = "Enter a publisher before importing.",
                importSuccessMessage = null
            )
            return
        }

        val volumeText = currentState.editedVolume.trim()
        val seriesVolume =
            if (volumeText.isBlank()) null else volumeText.toIntOrNull()

        if (volumeText.isNotBlank() &&
            (seriesVolume == null || seriesVolume < 0)
        ) {
            _uiState.value = currentState.copy(
                importError = "Enter a valid non-negative volume number, or leave it blank.",
                importSuccessMessage = null
            )
            return
        }

        val startYearText = currentState.editedStartYear.trim()
        val seriesStartYear =
            if (startYearText.isBlank()) null else startYearText.toIntOrNull()

        if (startYearText.isNotBlank() &&
            (seriesStartYear == null || seriesStartYear < 0)
        ) {
            _uiState.value = currentState.copy(
                importError = "Enter a valid start year, or leave it blank.",
                importSuccessMessage = null
            )
            return
        }

        val endYearText = currentState.editedEndYear.trim()
        val seriesEndYear =
            if (endYearText.isBlank()) null else endYearText.toIntOrNull()

        if (endYearText.isNotBlank() &&
            (seriesEndYear == null || seriesEndYear < 0)
        ) {
            _uiState.value = currentState.copy(
                importError = "Enter a valid end year, or leave it blank.",
                importSuccessMessage = null
            )
            return
        }

        val selectedUniverse =
            currentState.selectedUniverseId?.let { universeId ->
                currentState.availableUniverses.firstOrNull {
                    it.id == universeId
                }
            }

        if (currentState.selectedUniverseId != null &&
            selectedUniverse == null
        ) {
            _uiState.value = currentState.copy(
                importError =
                    "The selected universe is no longer available. " +
                    "Select a universe of choose no universe.",
                importSuccessMessage = null
            )
            return
        }

        val universeDesignation =
            selectedUniverse?.designation
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        if (selectedUniverse != null && universeDesignation == null) {
            _uiState.value = currentState.copy(
                importError = "The selected universe has no continuity designation. " +
                        "Choose another universe or choose no universe.",
                importSuccessMessage = null
            )
            return
        }

        val repository = comicRepository
        val importer = comicCatalogImporter

        if (importer == null ||
            (selectedUniverse != null && repository == null)
        ) {
            _uiState.value = currentState.copy(
                importError = "Catalog importing is unavailable.",
                importSuccessMessage = null
            )
            return
        }

        _uiState.value = currentState.copy(
            isImporting = true,
            importError = null,
            importSuccessMessage = null
        )

        viewModelScope.launch {
            try {
                if (selectedUniverse != null) {
                    val universePublisher =
                        checkNotNull(repository)
                            .getPublishers()
                            .firstOrNull { it.id == selectedUniverse.publisherId }

                    if (universePublisher == null) {
                        _uiState.value = _uiState.value.copy(
                            isImporting = false,
                            importError = "The selected universe's publisher could not be found.",
                            importSuccessMessage = null
                        )
                        return@launch
                    }

                    if (!universePublisher.name.equals(publisherName, ignoreCase = true)) {
                        _uiState.value = _uiState.value.copy(
                            isImporting = false,
                            importError = "The selected universe belongs to ${universePublisher.name}. Update the publisher or choose no universe.",
                            importSuccessMessage = null
                        )
                        return@launch
                    }
                }

                val importedIssues =
                    currentState.issues.mapNotNull { issue ->
                        val number =
                            issue.issueNumber
                                ?.trim()
                                ?.takeIf { it.isNotBlank() }
                                ?: return@mapNotNull null

                        CatalogIssueImportDto(
                            number = number,
                            title = issue.name
                                ?.trim()
                                ?.takeIf { it.isNotBlank() },
                            publicationDate =
                                normalizeCatalogPublicationDate(issue.coverDate)
                                    ?: normalizeCatalogPublicationDate(issue.storeDate),
                            coverUrl = issue.imageUrl,
                            description = null,
                            type = "REGULAR",
                            universeDesignation = universeDesignation,
                            externalIds = listOf(
                                CatalogExternalIdImportDto(
                                    source = "COMIC_VINE",
                                    externalId = issue.id.toString(),
                                    url = issue.siteDetailUrl
                                )
                            )
                        )
                    }

                val importData =
                    ComicCatalogImportDto(
                        publisher = publisherName,
                        universes = if (selectedUniverse != null) {
                            listOf(
                                CatalogUniverseImportDto(
                                    name = selectedUniverse.name,
                                    designation = checkNotNull(universeDesignation),
                                    description = selectedUniverse.description
                                )
                            )
                        } else {
                            emptyList()
                        },
                        series = listOf(
                            CatalogSeriesImportDto(
                                title = title,
                                volume = seriesVolume,
                                startYear = seriesStartYear,
                                endYear = seriesEndYear,
                                externalIds = listOf(
                                    CatalogExternalIdImportDto(
                                        source = "COMIC_VINE",
                                        externalId = volume.id.toString(),
                                        url = volume.siteDetailUrl
                                    )
                                ),
                                issues = importedIssues
                            )
                        )
                    )

                importer.import(importData)

                val skippedIssueCount =
                    currentState.issues.size - importedIssues.size

                val skippedMessage =
                    if (skippedIssueCount > 0) {
                        "Skipped $skippedIssueCount issue record(s) " +
                            "without issue numbers."
                    } else {
                        ""
                    }

                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importError = null,
                    importSuccessMessage =
                        "Imported $title with ${importedIssues.size} issue(s)." + skippedMessage
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importError = "Could not import this series. Please try again.",
                    importSuccessMessage = null
                )
            }
        }
    }

    private fun normalizeCatalogPublicationDate(
        value: String?
    ): String? {
        val date = value?.trim() ?: return null

        if (
            date.length < 7 ||
            !date.substring(0, 4).all { it.isDigit() } ||
            date[4] != '-' ||
            !date.substring(5, 7).all { it.isDigit() }
        ) {
            return null
        }

        return date.substring(0, 7)
    }

    private fun sortIssuesByPublicationDate(
        issues: List<ComicVineIssueHit>
    ): List<ComicVineIssueHit> {
        return issues.sortedWith(
            Comparator { first, second ->
                val firstDate =
                    first.coverDate
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?: first.storeDate
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }

                val secondDate =
                    second.coverDate
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?: second.storeDate
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }

                val dateComparison = when {
                    firstDate == null && secondDate != null -> 1
                    firstDate != null && secondDate == null -> -1
                    firstDate != null && secondDate != null ->
                        firstDate.compareTo(secondDate)
                    else -> 0
                }

                if (dateComparison != 0) {
                    dateComparison
                } else {
                    val firstNumber =
                        first.issueNumber?.trim()?.toDoubleOrNull()
                    val secondNumber =
                        second.issueNumber?.trim()?.toDoubleOrNull()

                    when {
                        firstNumber == null && secondNumber != null -> 1
                        firstNumber != null && secondNumber == null -> -1
                        firstNumber != null && secondNumber != null &&
                            firstNumber != secondNumber ->
                                firstNumber.compareTo(secondNumber)
                        else ->
                            first.id.compareTo(second.id)
                    }
                }
            }
        )
    }

    fun load(
        volumeId: Long,
        selectedVolume: ComicVineVolumeSearchHit? = null
    ) {
        loadJob?.cancel()

        if (volumeId <= 0L) {
            _uiState.value = ComicVineVolumeReviewUiState(
                availableUniverses = _uiState.value.availableUniverses,
                error = "The selected series has an invalid source ID."
            )
            return
        }

        _uiState.value = ComicVineVolumeReviewUiState(
            isLoading = true,
            volumeId = volumeId,
            availableUniverses = _uiState.value.availableUniverses
        )

        loadJob = viewModelScope.launch {
            try {
                val apiKey = apiKeyStore.get()
                    ?.takeIf { it.isNotBlank() }

                if (apiKey == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Configure your Comic Vine API key in " +
                            "Settings before loading series details."
                    )
                    return@launch
                }

                val selectedSearchResult = selectedVolume
                    ?.takeIf { it.id == volumeId }

                val volume = if (selectedSearchResult != null) {
                    ComicVineVolumeDetails(
                        id = selectedSearchResult.id,
                        name = selectedSearchResult.name,
                        startYear = selectedSearchResult.startYear,
                        countOfIssues = selectedSearchResult.countOfIssues,
                        publisherName = selectedSearchResult.publisherName,
                        siteDetailUrl = selectedSearchResult.siteDetailUrl,
                        imageUrl = selectedSearchResult.imageUrl
                    )
                } else {
                    val detailsOutcome = apiClient.getVolumeDetails(
                        apiKey = apiKey,
                        volumeId = volumeId
                    )

                    when (detailsOutcome) {
                        is ComicVineVolumeDetailsOutcome.Success -> {
                            detailsOutcome.volume
                        }

                        else -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = detailsErrorMessage(detailsOutcome)
                            )
                            return@launch
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    volume = volume,
                    editedTitle = volume.name,
                    editedPublisher = volume.publisherName.orEmpty(),
                    editedVolume = "",
                    editedStartYear = volume.startYear?.toString().orEmpty(),
                    editedEndYear = ""
                )

                val allIssues = mutableListOf<ComicVineIssueHit>()
                var totalRecords: Int? = null
                var offset = 0

                while (totalRecords == null || offset < totalRecords) {
                    val pageOutcome = apiClient.getVolumeIssues(
                        apiKey = apiKey,
                        volumeId = volumeId,
                        offset = offset
                    )

                    val page = when (pageOutcome) {
                        is ComicVineIssuePageOutcome.Success -> {
                            pageOutcome
                        }

                        else -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                issues = sortIssuesByPublicationDate(allIssues),
                                totalIssueRecords = totalRecords,
                                error = issueErrorMessage(pageOutcome)
                            )
                            return@launch
                        }
                    }

                    if (totalRecords == null) {
                        totalRecords = page.totalResults.coerceAtLeast(0)
                    }

                    allIssues.addAll(page.issues)

                    _uiState.value = _uiState.value.copy(
                        issues = sortIssuesByPublicationDate(allIssues),
                        totalIssueRecords = totalRecords
                    )

                    if (page.pageResultCount <= 0) {
                        break
                    }

                    offset += page.pageResultCount
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    volume = volume,
                    issues = sortIssuesByPublicationDate(allIssues),
                    totalIssueRecords = totalRecords,
                    error = null
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Could not load the selected series. Please try again."
                )
            }
        }
    }

    private fun detailsErrorMessage(
        outcome: ComicVineVolumeDetailsOutcome
    ): String {
        return when (outcome) {
            ComicVineVolumeDetailsOutcome.MissingApiKey ->
                "Configure your Comic Vine API key in Settings."

            ComicVineVolumeDetailsOutcome.InvalidApiKey ->
                "Comic Vine rejected the saved API key. Open Settings to replace it."

            ComicVineVolumeDetailsOutcome.NetworkError ->
                "Could not reach Comic Vine. Check your connection and try again."

            ComicVineVolumeDetailsOutcome.ServiceError ->
                "Could not retrieve the selected series details."

            is ComicVineVolumeDetailsOutcome.Success ->
                ""
        }
    }

    private fun issueErrorMessage(
        outcome: ComicVineIssuePageOutcome
    ): String {
        return when (outcome) {
            ComicVineIssuePageOutcome.MissingApiKey ->
                "Configure your Comic Vine API key in Settings."

            ComicVineIssuePageOutcome.InvalidApiKey ->
                "Comic Vine rejected the saved API key. Open Settings to replace it."

            ComicVineIssuePageOutcome.NetworkError ->
                "Could not reach Comic Vine. Check your connection and try again."

            ComicVineIssuePageOutcome.ServiceError ->
                "Could not retrieve the complete issue list."

            is ComicVineIssuePageOutcome.Success ->
                ""
        }
    }
}