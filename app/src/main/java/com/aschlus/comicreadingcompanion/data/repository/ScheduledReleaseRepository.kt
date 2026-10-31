package com.aschlus.comicreadingcompanion.data.repository

import androidx.room3.withWriteTransaction
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.ExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.ScheduledReleaseEntity
import com.aschlus.comicreadingcompanion.data.remote.ReleaseDataProvider
import com.aschlus.comicreadingcompanion.data.remote.ReleaseProviderResult
import com.aschlus.comicreadingcompanion.data.remote.ScheduledReleaseData
import java.time.LocalDate
import com.aschlus.comicreadingcompanion.data.database.entities.ReleaseSeriesMetadataEntity
import com.aschlus.comicreadingcompanion.data.database.entities.SeriesExternalId

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

        val providerResult =
            releaseDataProvider.getScheduledReleases(
                accessToken = accessToken,
                fromDate = fromDate,
                toDate = toDate
            )

        if (providerResult !is ReleaseProviderResult.Success) {
            return providerResult.failureAs()
        }

        val releases = providerResult.value

        val validationError = validateReleases(releases)

        if (validationError != null) {
            return ReleaseProviderResult.ServiceError(
                validationError
            )
        }

        val seriesMetadataByExternalId =
            loadSeriesMetadata(
                accessToken = accessToken,
                releases = releases
            )

        val syncedAt = currentTimeMillis()

        val entities = releases.map { release ->
            release.toEntity(
                lastSyncedAt = syncedAt
            )
        }

        var outcomes = emptyList<ReconciliationOutcome>()

        database.withWriteTransaction {
            comicDao.upsertScheduledReleases(entities)

            outcomes = releases.map { release ->
                reconcileRelease(
                    release = release,
                    seriesMetadata = seriesMetadataByExternalId[
                        release.externalSeriesId
                    ]
                )
            }
        }

        return ReleaseProviderResult.Success(
            ScheduledReleaseSyncSummary(
                fetchedCount = releases.size,
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

        val distinctSeriesIds =
            releases
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

        val comicVineLink =
            seriesMetadata
                ?.comicVineId
                ?.takeIf { it.isNotBlank() }
                ?.let { comicVineId ->
                    comicDao.getSeriesExternalId(
                        source = "COMIC_VINE",
                        externalId = comicVineId
                    )
                }

        if (existingMetronLink != null) {

            if (
                comicVineLink != null &&
                existingMetronLink.seriesId != comicVineLink.seriesId
            ) {
                // Two authoritative identifiers disagree.
                // Never silently move an existing mapping.
                return SeriesResolution.Conflict
            }

            val updatedUrl =
                seriesMetadata
                    ?.sourceUrl
                    ?.takeIf { it.isNotBlank() }

            if (
                updatedUrl != null &&
                updatedUrl != existingMetronLink.url
            ) {
                comicDao.updateSeriesExternalId(
                    existingMetronLink.copy(
                        url = updatedUrl
                    )
                )
            }

            return SeriesResolution.Mapped(
                existingMetronLink.seriesId
            )
        }

        if (comicVineLink == null) {
            return SeriesResolution.Unmatched
        }

        comicDao.insertSeriesExternalId(
            SeriesExternalId(
                seriesId = comicVineLink.seriesId,
                source = release.providerId,
                externalId = release.externalSeriesId,
                url = seriesMetadata.sourceUrl
            )
        )

        return SeriesResolution.Mapped(
            comicVineLink.seriesId
        )
    }

    private fun ScheduledReleaseData.toEntity(
        lastSyncedAt: Long
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
            lastSyncedAt = lastSyncedAt
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