package com.aschlus.comicreadingcompanion.data.repository

import androidx.room3.withWriteTransaction
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.ExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.ReleaseSeriesMetadataEntity
import com.aschlus.comicreadingcompanion.data.database.entities.ScheduledReleaseChangeEntity
import com.aschlus.comicreadingcompanion.data.database.entities.ScheduledReleaseEntity
import com.aschlus.comicreadingcompanion.data.database.entities.ScheduledReleaseSyncStateEntity
import com.aschlus.comicreadingcompanion.data.database.entities.SeriesExternalId
import com.aschlus.comicreadingcompanion.data.database.models.PullListScheduledReleaseResult
import com.aschlus.comicreadingcompanion.data.database.models.PullListSeriesResult
import com.aschlus.comicreadingcompanion.data.remote.ReleaseDataProvider
import com.aschlus.comicreadingcompanion.data.remote.ReleaseProviderResult
import com.aschlus.comicreadingcompanion.data.remote.ReleaseSeriesData
import com.aschlus.comicreadingcompanion.data.remote.ScheduledReleaseData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

data class ScheduledReleaseSyncSummary(
    val fetchedCount: Int,
    val linkedCount: Int,
    val alreadyLinkedCount: Int,
    val unmatchedCount: Int,
    val ambiguousMatchCount: Int,
    val conflictingExternalIdCount: Int
)

class ScheduledReleaseRepository(
    private val comicDao: ComicDao,
    private val database: ComicDatabase,
    private val releaseDataProvider: ReleaseDataProvider,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {

    suspend fun syncScheduledReleases(
        accessToken: String,
        fromDate: LocalDate,
        toDate: LocalDate,
    ): ReleaseProviderResult<ScheduledReleaseSyncSummary> {

        val syncStartedAt = currentTimeMillis()

        val previousSyncState =
            comicDao.getScheduledReleaseSyncState(
                providerId = releaseDataProvider.providerId,
                fromDate = fromDate.toString(),
                toDate = toDate.toString()
            )

        val modifiedAfter =
            previousSyncState?.lastSuccessfulSyncAt?.let { timestamp ->
                Instant.ofEpochMilli(timestamp).minusSeconds(1)
            }

        val trackedSeries =
            comicDao.getPullListSeries().first()

        val fetchedReleases =
            mutableListOf<ScheduledReleaseData>()

        val resolvedMetronSeriesIds =
            mutableSetOf<String>()

        var syncFailure: ReleaseProviderResult<*>? = null

        for (tracked in trackedSeries) {

            val existingLinks =
                comicDao.getSeriesExternalIdsForSeries(
                    tracked.seriesId
                )

            val existingMetronLink =
                existingLinks.firstOrNull {
                    it.source == releaseDataProvider.providerId
                }

            var metronSeriesId =
                existingMetronLink?.externalId

            if (metronSeriesId == null) {

                val comicVineId =
                    existingLinks.firstOrNull {
                        it.source == "COMIC_VINE"
                    }?.externalId
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }

                val lookupResult =
                    if (comicVineId != null) {
                        releaseDataProvider.findSeriesByComicVineId(
                            accessToken = accessToken,
                            comicVineId = comicVineId
                        )
                    } else {
                        releaseDataProvider.findSeriesByName(
                            accessToken = accessToken,
                            seriesName = tracked.title
                        )
                    }

                val candidates =
                    when (lookupResult) {
                        is ReleaseProviderResult.Success ->
                            lookupResult.value

                        else -> {
                            syncFailure = lookupResult
                            break
                        }
                    }

                val matchingSeries =
                    findMatchingMetronSeries(
                        trackedSeries = tracked,
                        candidates = candidates,
                        comicVineId = comicVineId
                    ) ?: continue

                val existingMetronMapping =
                    comicDao.getSeriesExternalId(
                        source = releaseDataProvider.providerId,
                        externalId = matchingSeries.externalSeriesId
                    )

                // Never attach a Metron series that is already assigned
                // to a different local series.
                if (
                    existingMetronMapping != null &&
                    existingMetronMapping.seriesId != tracked.seriesId
                ) {
                    continue
                }

                if (existingMetronMapping == null) {
                    comicDao.insertSeriesExternalId(
                        SeriesExternalId(
                            seriesId = tracked.seriesId,
                            source = releaseDataProvider.providerId,
                            externalId = matchingSeries.externalSeriesId,
                            url = matchingSeries.sourceUrl
                        )
                    )
                }

                // Retain the external metadata returned by the series lookup.
                comicDao.upsertReleaseSeriesMetadata(
                    listOf(
                        ReleaseSeriesMetadataEntity(
                            providerId = matchingSeries.providerId,
                            externalSeriesId =
                                matchingSeries.externalSeriesId,
                            comicVineId = matchingSeries.comicVineId,
                            gcdId = matchingSeries.gcdId,
                            sourceUrl = matchingSeries.sourceUrl,
                            sourceModifiedAt =
                                matchingSeries.sourceModifiedAt,
                            lastFetchedAt = currentTimeMillis()
                        )
                    )
                )

                metronSeriesId =
                    matchingSeries.externalSeriesId
            }

            val resolvedSeriesId =
                metronSeriesId ?: continue

            resolvedMetronSeriesIds += resolvedSeriesId

            val providerResult =
                releaseDataProvider.getScheduledReleasesForSeries(
                    accessToken = accessToken,
                    externalSeriesId = resolvedSeriesId,
                    fromDate = fromDate,
                    toDate = toDate,
                    modifiedAfter = modifiedAfter
                )

            when (providerResult) {
                is ReleaseProviderResult.Success ->
                    fetchedReleases += providerResult.value

                else -> {
                    syncFailure = providerResult
                    break
                }
            }
        }

        val validationError =
            validateReleases(fetchedReleases)

        if (validationError != null) {
            return ReleaseProviderResult.ServiceError(
                validationError
            )
        }

        val allStoredReleases =
            comicDao.getAllScheduledReleasesForProvider(
                providerId = releaseDataProvider.providerId
            )

        val storedReleasesById =
            allStoredReleases.associateBy {
                it.externalIssueId
            }

        val releasesToPersist =
            fetchedReleases.filter { release ->
                val alreadyTracked =
                    storedReleasesById.containsKey(
                        release.externalIssueId
                    )

                alreadyTracked ||
                    release.storeReleaseDate
                        ?.let { date ->
                            runCatching {
                                LocalDate.parse(date)
                            }.getOrNull()
                        }
                        ?.let { date ->
                            !date.isBefore(fromDate) &&
                                !date.isAfter(toDate)
                        } == true
            }

        // Reconcile the previously cached calendar records, but only for
        // Metron series that are actually on the user's Pull List.
        val previouslyStoredReleases =
            comicDao.getScheduledReleasesInRange(
                providerId = releaseDataProvider.providerId,
                fromDate = fromDate.toString(),
                toDate = toDate.toString()
            ).filter {
                it.externalSeriesId in resolvedMetronSeriesIds
            }

        val releasesToReconcileById =
            linkedMapOf<String, ScheduledReleaseData>()

        previouslyStoredReleases.forEach { entity ->
            releasesToReconcileById[entity.externalIssueId] =
                entity.toScheduledReleaseData()
        }

        releasesToPersist.forEach { release ->
            releasesToReconcileById[release.externalIssueId] =
                release
        }

        val releasesToReconcile =
            releasesToReconcileById.values.toList()

        val syncedAt = currentTimeMillis()

        val dateChanges =
            releasesToPersist.mapNotNull { release ->

                val previous =
                    storedReleasesById[release.externalIssueId]
                        ?: return@mapNotNull null

                val storeDateChanged =
                    previous.storeReleaseDate !=
                        release.storeReleaseDate

                val focDateChanged =
                    previous.focDate != release.focDate

                if (!storeDateChanged && !focDateChanged) {
                    return@mapNotNull null
                }

                ScheduledReleaseChangeEntity(
                    providerId = release.providerId,
                    externalIssueId = release.externalIssueId,
                    externalSeriesId = release.externalSeriesId,
                    seriesTitleSnapshot = release.seriesTitle,
                    issueNumberSnapshot = release.issueNumber,
                    previousStoreReleaseDate =
                        previous.storeReleaseDate,
                    newStoreReleaseDate =
                        release.storeReleaseDate,
                    previousFocDate =
                        previous.focDate,
                    newFocDate =
                        release.focDate,
                    sourceModifiedAt =
                        release.sourceModifiedAt,
                    detectedAt = syncedAt
                )
            }

        var outcomes =
            emptyList<ReconciliationOutcome>()

        database.withWriteTransaction {

            if (dateChanges.isNotEmpty()) {
                comicDao.insertScheduledReleaseChanges(
                    dateChanges
                )
            }

            val currentStoredById =
                comicDao.getAllScheduledReleasesForProvider(
                    providerId = releaseDataProvider.providerId
                ).associateBy {
                    it.externalIssueId
                }

            val entitiesToUpsert =
                releasesToPersist.map { release ->
                    release.toEntity(
                        lastSyncedAt = syncedAt,
                        manuallyCanceledAt =
                            currentStoredById[release.externalIssueId]
                                ?.manuallyCanceledAt
                    )
                }

            comicDao.upsertScheduledReleases(entitiesToUpsert)

            outcomes =
                releasesToReconcile.map { release ->
                    reconcileRelease(
                        release = release,
                        seriesMetadata = null
                    )
                }

            // A partial refresh may save the releases already fetched, but
            // must not advance the cursor past series that were not checked.
            if (syncFailure == null) {
                val currentSyncState =
                    comicDao.getScheduledReleaseSyncState(
                        providerId = releaseDataProvider.providerId,
                        fromDate = fromDate.toString(),
                        toDate = toDate.toString()
                    )

                val lastSuccessfulSyncAt =
                    currentSyncState?.lastSuccessfulSyncAt
                        ?.let { maxOf(syncStartedAt, it) }
                        ?: syncStartedAt

                comicDao.upsertScheduledReleaseSyncState(
                    ScheduledReleaseSyncStateEntity(
                        providerId = releaseDataProvider.providerId,
                        fromDate = fromDate.toString(),
                        toDate = toDate.toString(),
                        lastSuccessfulSyncAt = lastSuccessfulSyncAt
                    )
                )
            }
        }

        if (syncFailure != null) {
            return syncFailure.failureAs()
        }

        return ReleaseProviderResult.Success(
            ScheduledReleaseSyncSummary(
                fetchedCount = releasesToPersist.size,
                linkedCount = outcomes.count {
                    it == ReconciliationOutcome.LINKED
                },
                alreadyLinkedCount = outcomes.count {
                    it == ReconciliationOutcome.ALREADY_LINKED
                },
                unmatchedCount = outcomes.count {
                    it == ReconciliationOutcome.UNMATCHED
                },
                ambiguousMatchCount = outcomes.count {
                    it == ReconciliationOutcome.AMBIGUOUS
                },
                conflictingExternalIdCount = outcomes.count {
                    it == ReconciliationOutcome.CONFLICT
                }
            )
        )
    }

    private fun findMatchingMetronSeries(
        trackedSeries: PullListSeriesResult,
        candidates: List<ReleaseSeriesData>,
        comicVineId: String?
    ): ReleaseSeriesData? {

        val distinctCandidates =
            candidates.distinctBy {
                it.externalSeriesId
            }

        // Comic Vine IDs are authoritative. Do not silently fall back
        // to a title match when a local Comic Vine ID exists.
        if (comicVineId != null) {
            return distinctCandidates
                .filter {
                    it.comicVineId?.trim() == comicVineId
                }
                .singleOrNull()
        }

        var matches =
            distinctCandidates.filter { candidate ->

                normalizeSeriesName(candidate.seriesTitle.orEmpty()) ==
                    normalizeSeriesName(trackedSeries.title) &&

                    (
                        trackedSeries.volume == null ||
                            candidate.seriesVolume == null ||
                            trackedSeries.volume == candidate.seriesVolume
                        ) &&

                    (
                        trackedSeries.startYear == null ||
                            candidate.seriesStartYear == null ||
                            trackedSeries.startYear ==
                            candidate.seriesStartYear
                        )
            }

        if (
            matches.size > 1 &&
            trackedSeries.publisherName.isNotBlank()
        ) {
            val publisherMatches =
                matches.filter { candidate ->
                    normalizeSeriesName(
                        candidate.publisherName.orEmpty()
                    ) == normalizeSeriesName(
                        trackedSeries.publisherName
                    )
                }

            if (publisherMatches.isNotEmpty()) {
                matches = publisherMatches
            }
        }

        // Only accept an unambiguous result.
        return matches.singleOrNull()
    }

    private fun normalizeSeriesName(value: String): String {
        return value
            .replace(
                Regex("""\s*\((?:18|19|20)\d{2}\)\s*"""),
                " "
            )
            .lowercase(Locale.ROOT)
            .replace(
                Regex("""[^\p{L}\p{N}]+"""),
                " "
            )
            .trim()
            .replace(
                Regex("""\s+"""),
                " "
            )
    }

    suspend fun getScheduledReleasesInRange(
        providerId: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<ScheduledReleaseEntity> {
        require(!fromDate.isAfter(toDate)) {
            "The start date must be on or before the end date."
        }

        return comicDao.getScheduledReleasesInRange(
            providerId = providerId,
            fromDate = fromDate.toString(),
            toDate = toDate.toString()
        )
    }

    fun getPullListScheduledReleasesInRange(
        providerId: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): Flow<List<PullListScheduledReleaseResult>> {
        require(!fromDate.isAfter(toDate)) {
            "The start date must be on or before the end date."
        }

        return comicDao.getPullListScheduledReleasesInRange(
            providerId = providerId,
            fromDate = fromDate.toString(),
            toDate = toDate.toString()
        )
    }

    suspend fun getScheduledReleasesForExternalSeries(
        providerId: String,
        externalSeriesId: String
    ): List<ScheduledReleaseEntity> {
        return comicDao.getScheduledReleasesForExternalSeries(
            providerId = providerId,
            externalSeriesId = externalSeriesId
        )
    }

    suspend fun getScheduledRelease(
        providerId: String,
        externalIssueId: String
    ): ScheduledReleaseEntity? {
        return comicDao.getScheduledRelease(
            providerId = providerId,
            externalIssueId = externalIssueId
        )
    }

    suspend fun setScheduledReleaseManuallyCanceled(
        providerId: String,
        externalIssueId: String,
        isCanceled: Boolean
    ): Boolean {

        var found = false

        database.withWriteTransaction {

            val release =
                comicDao.getScheduledRelease(
                    providerId = providerId,
                    externalIssueId = externalIssueId
                )

            if (release != null) {
                found = true

                val newManuallyCanceledAt =
                    if (isCanceled) {
                        release.manuallyCanceledAt ?: currentTimeMillis()
                    } else {
                        null
                    }

                // Do not create duplicate history entries for
                // repeated requests to set the same status.
                if (
                    newManuallyCanceledAt !=
                    release.manuallyCanceledAt
                ) {
                    val detectedAt = currentTimeMillis()

                    comicDao.insertScheduledReleaseChanges(
                        listOf(
                            ScheduledReleaseChangeEntity(
                                providerId = release.providerId,
                                externalIssueId = release.externalIssueId,
                                externalSeriesId = release.externalSeriesId,
                                seriesTitleSnapshot =
                                    release.seriesTitleSnapshot,
                                issueNumberSnapshot =
                                    release.issueNumberSnapshot,
                                previousStoreReleaseDate =
                                    release.storeReleaseDate,
                                newStoreReleaseDate =
                                    release.storeReleaseDate,
                                previousFocDate =
                                    release.focDate,
                                newFocDate =
                                    release.focDate,
                                sourceModifiedAt =
                                    release.sourceModifiedAt,
                                detectedAt = detectedAt,
                                previousManuallyCanceledAt =
                                    release.manuallyCanceledAt,
                                newManuallyCanceledAt =
                                    newManuallyCanceledAt
                            )
                        )
                    )

                    comicDao.updateScheduledReleaseManualCancellation(
                        providerId = providerId,
                        externalIssueId = externalIssueId,
                        manuallyCanceledAt = newManuallyCanceledAt
                    )
                }
            }
        }

        return found
    }

    suspend fun getScheduledReleaseCount(
        providerId: String
    ): Int {
        return comicDao.getScheduledReleaseCount(
            providerId = providerId
        )
    }

    private suspend fun loadSeriesMetadata(
        accessToken: String,
        releases: List<ScheduledReleaseData>
    ): Map<String, ReleaseSeriesMetadataEntity> {

        val metadataByExternalSeriesId =
            linkedMapOf<String, ReleaseSeriesMetadataEntity>()

        val seriesIdsToFetch = mutableListOf<String>()

        // The issue endpoint returns the broad release calendar.
// Detailed series lookups are only needed for potential matches
// to series the user is actually tracking.
        val pullListSeries =
            comicDao.getPullListSeries().first()

        val distinctSeriesIds =
            releases
                .filter { release ->
                    pullListSeries.any { trackedSeries ->

                        val titleMatches =
                            release.seriesTitle.trim().equals(
                                trackedSeries.title.trim(),
                                ignoreCase = true
                            )

                        val publisherMatches =
                            release.publisherName
                                ?.trim()
                                ?.equals(
                                    trackedSeries.publisherName.trim(),
                                    ignoreCase = true
                                ) == true

                        val volumeMatches =
                            release.seriesVolume == null ||
                                trackedSeries.volume == null ||
                                release.seriesVolume == trackedSeries.volume

                        val yearMatches =
                            release.seriesStartYear == null ||
                                trackedSeries.startYear == null ||
                                release.seriesStartYear ==
                                trackedSeries.startYear

                        titleMatches &&
                            publisherMatches &&
                            volumeMatches &&
                            yearMatches
                    }
                }
                .map { it.externalSeriesId }
                .distinct()

        for (externalSeriesId in distinctSeriesIds) {

            val cachedMetadata =
                comicDao.getReleaseSeriesMetadata(
                    providerId = releaseDataProvider.providerId,
                    externalSeriesId = externalSeriesId
                )

            if (cachedMetadata != null) {
                metadataByExternalSeriesId[externalSeriesId] =
                    cachedMetadata

                continue
            }

            val existingSeriesLink =
                comicDao.getSeriesExternalId(
                    source = releaseDataProvider.providerId,
                    externalId = externalSeriesId
                )

            if (existingSeriesLink == null) {
                seriesIdsToFetch += externalSeriesId
            }
        }

        val newlyFetchedMetadata =
            mutableListOf<ReleaseSeriesMetadataEntity>()

        for (externalSeriesId in seriesIdsToFetch) {

            val providerResult =
                releaseDataProvider.getSeriesDetails(
                    accessToken = accessToken,
                    externalSeriesId = externalSeriesId
                )

            if (providerResult is ReleaseProviderResult.RateLimited) {
                // Metadata enrichment is optional. Preserve the releases already
                // retrieved, and stop making detail requests for this sync.
                break
            }

            if (providerResult !is ReleaseProviderResult.Success) {
                continue
            }

            val details = providerResult.value

            if (
                details.providerId != releaseDataProvider.providerId ||
                details.externalSeriesId != externalSeriesId
            ) {
                continue
            }

            val entity =
                ReleaseSeriesMetadataEntity(
                    providerId = details.providerId,
                    externalSeriesId = details.externalSeriesId,
                    comicVineId = details.comicVineId
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() },
                    gcdId = details.gcdId
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() },
                    sourceUrl = details.sourceUrl
                        ?.takeIf { it.isNotBlank() },
                    sourceModifiedAt = details.sourceModifiedAt,
                    lastFetchedAt = currentTimeMillis()
                )

            newlyFetchedMetadata += entity

            metadataByExternalSeriesId[externalSeriesId] =
                entity
        }

        if (newlyFetchedMetadata.isNotEmpty()) {
            comicDao.upsertReleaseSeriesMetadata(
                newlyFetchedMetadata
            )
        }

        return metadataByExternalSeriesId
    }

    private fun validateReleases(
        releases: List<ScheduledReleaseData>
    ): String? {

        if (releases.any { it.providerId != releaseDataProvider.providerId }) {
            return "The provider returned a release with an unexpected provider ID."
        }

        if (
            releases.any {
                it.externalIssueId.isBlank() ||
                    it.externalSeriesId.isBlank() ||
                    it.seriesTitle.isBlank()
            }
        ) {
            return "The prover returned a release with missing identity information."
        }

        if (
            releases.map { it.externalIssueId }.distinct().size != releases.size
        ) {
            return "The provider returned duplicate external issue IDs."
        }

        return null
    }

    private suspend fun reconcileRelease(
        release: ScheduledReleaseData,
        seriesMetadata: ReleaseSeriesMetadataEntity?
    ): ReconciliationOutcome {

        val seriesResolution =
            resolveSeriesLink(
                release = release,
                seriesMetadata = seriesMetadata
            )

        val seriesId =
            when (seriesResolution) {
                is SeriesResolution.Mapped ->
                    seriesResolution.seriesId

                SeriesResolution.Unmatched ->
                    return ReconciliationOutcome.UNMATCHED

                SeriesResolution.Conflict ->
                    return ReconciliationOutcome.CONFLICT
            }

        val localIssues =
            comicDao.getIssuesForSeries(seriesId)

        val existingIssueLink =
            comicDao.getExternalId(
                source = release.providerId,
                externalId = release.externalIssueId
            )

        if (existingIssueLink != null) {
            val belongsToMappedSeries =
                localIssues.any { issue ->
                    issue.id == existingIssueLink.issueId
                }

            if (!belongsToMappedSeries) {
                return ReconciliationOutcome.CONFLICT
            }

            val updatedUrl =
                release.sourceUrl
                    ?.takeIf { it.isNotBlank() }

            if (
                updatedUrl != null &&
                updatedUrl != existingIssueLink.url
            ) {
                comicDao.updateExternalId(
                    existingIssueLink.copy(
                        url = updatedUrl
                    )
                )
            }

            return ReconciliationOutcome.ALREADY_LINKED
        }

        val issueNumber =
            release.issueNumber
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: return ReconciliationOutcome.UNMATCHED

        val matchingIssues =
            localIssues.filter { issue ->
                issue.issueNumber.trim().equals(
                    issueNumber,
                    ignoreCase = true
                )
            }

        if (matchingIssues.isEmpty()) {
            return ReconciliationOutcome.UNMATCHED
        }

        if (matchingIssues.size > 1) {
            return ReconciliationOutcome.AMBIGUOUS
        }

        val matchingIssue = matchingIssues.single()

        comicDao.insertExternalId(
            ExternalId(
                issueId = matchingIssue.id,
                source = release.providerId,
                externalId = release.externalIssueId,
                url = release.sourceUrl
            )
        )

        return ReconciliationOutcome.LINKED
    }

    private suspend fun resolveSeriesLink(
        release: ScheduledReleaseData,
        seriesMetadata: ReleaseSeriesMetadataEntity?
    ): SeriesResolution {

        val existingMetronLink =
            comicDao.getSeriesExternalId(
                source = release.providerId,
                externalId = release.externalSeriesId
            )

        return existingMetronLink?.let {
            SeriesResolution.Mapped(it.seriesId)
        } ?: SeriesResolution.Unmatched
    }

    private fun ScheduledReleaseData.toEntity(
        lastSyncedAt: Long,
        manuallyCanceledAt: Long? = null
    ): ScheduledReleaseEntity {
        return ScheduledReleaseEntity(
            providerId = providerId,
            externalIssueId = externalIssueId,
            externalSeriesId = externalSeriesId,
            seriesTitleSnapshot = seriesTitle,
            seriesVolumeSnapshot = seriesVolume,
            seriesStartYearSnapshot = seriesStartYear,
            publisherNameSnapshot = publisherName,
            issueNumberSnapshot = issueNumber,
            displayTitleSnapshot = displayTitle,
            coverDate = coverDate,
            storeReleaseDate = storeReleaseDate,
            focDate = focDate,
            price = price,
            priceCurrency = priceCurrency,
            distributorSku = distributorSku,
            upc = upc,
            comicVineId = comicVineId,
            gcdId = gcdId,
            sourceUrl = sourceUrl,
            sourceModifiedAt = sourceModifiedAt,
            lastSyncedAt = lastSyncedAt,
            manuallyCanceledAt = manuallyCanceledAt
        )
    }

    private fun ScheduledReleaseEntity.toScheduledReleaseData():
        ScheduledReleaseData {
        return ScheduledReleaseData(
            providerId = providerId,
            externalIssueId = externalIssueId,
            externalSeriesId = externalSeriesId,
            seriesTitle = seriesTitleSnapshot,
            seriesVolume = seriesVolumeSnapshot,
            seriesStartYear = seriesStartYearSnapshot,
            publisherName = publisherNameSnapshot,
            issueNumber = issueNumberSnapshot,
            displayTitle = displayTitleSnapshot,
            coverDate = coverDate,
            storeReleaseDate = storeReleaseDate,
            focDate = focDate,
            price = price,
            priceCurrency = priceCurrency,
            distributorSku = distributorSku,
            upc = upc,
            comicVineId = comicVineId,
            gcdId = gcdId,
            sourceUrl = sourceUrl,
            sourceModifiedAt = sourceModifiedAt
        )
    }

    private sealed interface SeriesResolution {

        data class Mapped(
            val seriesId: Long
        ) : SeriesResolution

        data object Unmatched : SeriesResolution

        data object Conflict : SeriesResolution
    }

    private enum class ReconciliationOutcome {
        LINKED,
        ALREADY_LINKED,
        UNMATCHED,
        AMBIGUOUS,
        CONFLICT
    }

    private fun <T> ReleaseProviderResult<*>.failureAs():
        ReleaseProviderResult<T> {
        return when (this) {
            is ReleaseProviderResult.Success ->
                error("Cannot convert a successful result to a failure.")

            ReleaseProviderResult.MissingCredentials ->
                ReleaseProviderResult.MissingCredentials

            ReleaseProviderResult.InvalidCredentials ->
                ReleaseProviderResult.InvalidCredentials

            ReleaseProviderResult.AccessDenied ->
                ReleaseProviderResult.AccessDenied

            is ReleaseProviderResult.RateLimited ->
                ReleaseProviderResult.RateLimited(
                    retryAfterSeconds = retryAfterSeconds
                )

            ReleaseProviderResult.NetworkError ->
                ReleaseProviderResult.NetworkError

            is ReleaseProviderResult.ServiceError ->
                ReleaseProviderResult.ServiceError(message)
        }
    }
}