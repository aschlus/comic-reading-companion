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
                reconcileRelease(release)
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
        release: ScheduledReleaseData
    ): ReconciliationOutcome {

        val seriesLink =
            comicDao.getSeriesExternalId(
                source = release.providerId,
                externalId = release.externalSeriesId
            ) ?: return ReconciliationOutcome.UNMATCHED

        val localIssues =
            comicDao.getIssuesForSeries(
                seriesLink.seriesId
            )

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