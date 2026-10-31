package com.aschlus.comicreadingcompanion.data.remote

import java.time.LocalDate
import java.time.Instant

data class ReleaseProviderAttribution(
    val providerName: String,
    val sourceUrl: String,
    val licenseName: String,
    val licenseUrl: String,
    val changesNotice: String,
    val additionalNotice: String? = null
)

interface ReleaseDataProvider {

    val providerId: String

    val attribution: ReleaseProviderAttribution?
        get() = null

    suspend fun validateConnection(
        accessToken: String
    ): ReleaseProviderResult<Unit>

    suspend fun getScheduledReleases(
        accessToken: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): ReleaseProviderResult<List<ScheduledReleaseData>>

    suspend fun getScheduledReleases(
        accessToken: String,
        fromDate: LocalDate,
        toDate: LocalDate,
        modifiedAfter: Instant?
    ): ReleaseProviderResult<List<ScheduledReleaseData>> {
        return getScheduledReleases(
            accessToken = accessToken,
            fromDate = fromDate,
            toDate = toDate
        )
    }

    suspend fun getReleaseDetails(
        accessToken: String,
        externalIssueId: String
    ): ReleaseProviderResult<ScheduledReleaseData>

    suspend fun getSeriesDetails(
        accessToken: String,
        externalSeriesId: String
    ): ReleaseProviderResult<ReleaseSeriesData> {
        return ReleaseProviderResult.ServiceError(
            "This provider does not support series metadata lookup."
        )
    }
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

data class ReleaseSeriesData(
    val providerId: String,
    val externalSeriesId: String,
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