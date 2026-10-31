package com.aschlus.comicreadingcompanion.data.remote

import java.time.LocalDate

interface ReleaseDataProvider {

    val providerId: String

    suspend fun validateConnection(
        accessToken: String
    ): ReleaseProviderResult<Unit>

    suspend fun getScheduledReleases(
        accessToken: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): ReleaseProviderResult<List<ScheduledReleaseData>>

    suspend fun getReleaseDetails(
        accessToken: String,
        externalIssueId: String
    ): ReleaseProviderResult<ScheduledReleaseData>
}

data class ScheduledReleaseData(
    val providerId: String,
    val externalIssueId: String,
    val externalSeriesId: String,
    val seriesTitle: String,
    val seriesVolume: Int?,
    val seriesStartYear: Int?,
    val publisherName: String?,
    val issueNumber: String?,
    val displayTitle: String?,
    val coverDate: String?,
    val storeReleaseDate: String?,
    val focDate: String?,
    val price: String?,
    val priceCurrency: String?,
    val distributorSku: String?,
    val upc: String?,
    val comicVineId: String?,
    val gcdId: String?,
    val sourceUrl: String?,
    val sourceModifiedAt: String?
)

data class ScheduledReleasePreview(
    val totalCount: Int,
    val releases: List<ScheduledReleaseData>
)

sealed interface ReleaseProviderResult<out T> {

    data class Success<T>(
        val value: T
    ) : ReleaseProviderResult<T>

    data object MissingCredentials : ReleaseProviderResult<Nothing>

    data object InvalidCredentials : ReleaseProviderResult<Nothing>

    data object AccessDenied : ReleaseProviderResult<Nothing>

    data class RateLimited(
        val retryAfterSeconds: Long?
    ) : ReleaseProviderResult<Nothing>

    data object NetworkError : ReleaseProviderResult<Nothing>

    data class ServiceError(
        val message: String
    ) : ReleaseProviderResult<Nothing>
}