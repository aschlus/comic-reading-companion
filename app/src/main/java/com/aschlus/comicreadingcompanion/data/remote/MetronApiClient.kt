package com.aschlus.comicreadingcompanion.data.remote

import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.Instant

data class MetronHttpResponse(
    val statusCode: Int,
    val body: String,
    val headers: Map<String, String> = emptyMap()
)

fun interface MetronHttpTransport {
    suspend fun get(
        url: String,
        accessToken: String
    ): MetronHttpResponse
}

class UrlConnectionMetronHttpTransport: MetronHttpTransport {
    override suspend fun get(
        url: String,
        accessToken: String
    ): MetronHttpResponse = withContext(Dispatchers.IO) {
        val connection =
            URL(url).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000

            connection.instanceFollowRedirects = false

            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val statusCode = connection.responseCode

            val responseStream =
                if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val body =
                responseStream
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    .orEmpty()

            val headers =
                connection.headerFields.entries
                    .mapNotNull { (name, values) ->
                        name?.let {
                            it.lowercase(Locale.ROOT) to
                                values.orEmpty().joinToString(",")
                        }
                    }
                    .toMap()

            MetronHttpResponse(
                statusCode = statusCode,
                body = body,
                headers = headers
            )
        } finally {
            connection.disconnect()
        }
    }
}

class MetronApiClient(
    private val transport: MetronHttpTransport =
        UrlConnectionMetronHttpTransport(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
    }
) : ReleaseDataProvider {

    override val providerId: String = "METRON"

    override suspend fun validateConnection(
        accessToken: String
    ): ReleaseProviderResult<Unit> {
        val probeQuery = URLEncoder.encode(
            "comic-reading-companion-connection-check",
            StandardCharsets.UTF_8.name()
        )

        val url =
            "$API_BASE_URL/issue/?series_name=$probeQuery"

        return when (val result = request(url, accessToken)) {
            is ReleaseProviderResult.Success -> {
                try {
                    json.decodeFromString<MetronIssuePageDto>(
                        result.value.body
                    )

                    ReleaseProviderResult.Success(Unit)
                } catch (_: SerializationException) {
                    ReleaseProviderResult.ServiceError(
                        "Metron returned an unexpected response."
                    )
                } catch (_: IllegalArgumentException) {
                    ReleaseProviderResult.ServiceError(
                        "Metron returned an unexpected response."
                    )
                }
            }

            else -> result.failureAs()
        }
    }

    override suspend fun getScheduledReleases(
        accessToken: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): ReleaseProviderResult<List<ScheduledReleaseData>> {
        return getScheduledReleases(
            accessToken = accessToken,
            fromDate = fromDate,
            toDate = toDate,
            modifiedAfter = null
        )
    }

    override suspend fun getScheduledReleases(
        accessToken: String,
        fromDate: LocalDate,
        toDate: LocalDate,
        modifiedAfter: Instant?
    ): ReleaseProviderResult<List<ScheduledReleaseData>> {

        if (fromDate.isAfter(toDate)) {
            return ReleaseProviderResult.ServiceError(
                "The start date must be on or before the end date."
            )
        }

        val query =
            modifiedAfter?.let {
                "modified_gt=" +
                    URLEncoder.encode(
                        it.toString(),
                        StandardCharsets.UTF_8.name()
                    )
            } ?: run {
                "store_date_range_after=$fromDate" +
                    "&store_date_range_before=$toDate"
            }

        var nextUrl: String? =
            "$API_BASE_URL/issue/?$query"

        val visitedUrls = mutableSetOf<String>()
        val releases = mutableListOf<ScheduledReleaseData>()

        while (true) {
            val currentUrl = nextUrl ?: break

            if (!visitedUrls.add(currentUrl)) {
                return ReleaseProviderResult.ServiceError(
                    "Metron returned a repeated pagination URL."
                )
            }

            val body = when (
                val result = request(currentUrl, accessToken)
            ) {
                is ReleaseProviderResult.Success ->
                    result.value.body

                else ->
                    return result.failureAs()
            }

            val page = try {
                json.decodeFromString<MetronIssuePageDto>(body)
            } catch (_: SerializationException) {
                return ReleaseProviderResult.ServiceError(
                    "Metron returned an unexpected issue-list response."
                )
            } catch (_: IllegalArgumentException) {
                return ReleaseProviderResult.ServiceError(
                    "Metron returned an unexpected issue-list response."
                )
            }

            for (issue in page.results) {
                val release = issue.toScheduledRelease()
                    ?: return ReleaseProviderResult.ServiceError(
                        "An issue returned by Metron was missing required identity fields."
                    )

                releases += release
            }

            nextUrl = page.next

            if (
                nextUrl != null &&
                !isTrustedPaginationUrl(nextUrl)
            ) {
                return ReleaseProviderResult.ServiceError(
                    "Metron returned an unexpected pagination URL."
                )
            }
        }

        return ReleaseProviderResult.Success(releases)
    }

    suspend fun getScheduledReleasePreview(
        accessToken: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): ReleaseProviderResult<ScheduledReleasePreview> {
        if (fromDate.isAfter(toDate)) {
            return ReleaseProviderResult.ServiceError(
                "The start date must be on or before the end date."
            )
        }

        val url =
            "$API_BASE_URL/issue/" +
            "?store_date_range_after=$fromDate" +
            "&store_date_range_before=$toDate"

        return when (
            val result = request(url, accessToken)
        ) {
            is ReleaseProviderResult.Success -> {
                val page = try {
                    json.decodeFromString<MetronIssuePageDto>(
                        result.value.body
                    )
                } catch (_: SerializationException) {
                    return ReleaseProviderResult.ServiceError(
                        "Metron returned an unexpected issue-list response."
                    )
                } catch (_: IllegalArgumentException) {
                    return ReleaseProviderResult.ServiceError(
                        "Metron returned an unexpected issue-list response."
                    )
                }

                val releases = mutableListOf<ScheduledReleaseData>()

                for (issue in page.results) {
                    val release = issue.toScheduledRelease()
                        ?: return ReleaseProviderResult.ServiceError(
                            "An issue returned by Metron was missing required identity fields."
                        )

                    releases += release
                }

                ReleaseProviderResult.Success(
                    ScheduledReleasePreview(
                        totalCount = page.count,
                        releases = releases
                    )
                )
            }

            else -> result.failureAs()
        }
    }

    override suspend fun getReleaseDetails(
        accessToken: String,
        externalIssueId: String
    ): ReleaseProviderResult<ScheduledReleaseData> {
        val id = externalIssueId.toLongOrNull()

        if (id == null || id <= 0L) {
            return ReleaseProviderResult.ServiceError(
                "The Metron issue ID is invalid."
            )
        }

        val url = "$API_BASE_URL/issue/$id/"

        return when (val result = request(url, accessToken)) {
            is ReleaseProviderResult.Success -> {
                val issue = try {
                    json.decodeFromString<MetronIssueDto>(
                        result.value.body
                    )
                } catch (_: SerializationException) {
                    return ReleaseProviderResult.ServiceError(
                        "Metron returned an unexpected issue-detail response."
                    )
                } catch (_: IllegalArgumentException) {
                    return ReleaseProviderResult.ServiceError(
                        "Metron returned an unexpected issue-detail response."
                    )
                }

                if (issue.id != id) {
                    return ReleaseProviderResult.ServiceError(
                        "Metron returned a different issue than requested."
                    )
                }

                val release = issue.toScheduledRelease()
                    ?: return ReleaseProviderResult.ServiceError(
                        "The Metron issue was missing required identity fields."
                    )

                ReleaseProviderResult.Success(release)
            }

            else -> result.failureAs()
        }
    }

    override suspend fun getSeriesDetails(
        accessToken: String,
        externalSeriesId: String
    ): ReleaseProviderResult<ReleaseSeriesData> {

        val id = externalSeriesId.toLongOrNull()

        if (id == null || id <= 0L) {
            return ReleaseProviderResult.ServiceError(
                "The Metron series ID is invalid."
            )
        }

        val url = "$API_BASE_URL/series/$id/"

        return when (val result = request(url, accessToken)) {

            is ReleaseProviderResult.Success -> {

                val series = try {
                    json.decodeFromString<MetronSeriesDetailDto>(
                        result.value.body
                    )
                } catch (_: SerializationException) {
                    return ReleaseProviderResult.ServiceError(
                        "Metron returned an unexpected series-detail response."
                    )
                } catch (_: IllegalArgumentException) {
                    return ReleaseProviderResult.ServiceError(
                        "Metron returned an unexpected series-detail response."
                    )
                }

                if (series.id != id) {
                    return ReleaseProviderResult.ServiceError(
                        "Metron returned a different series than requested."
                    )
                }

                ReleaseProviderResult.Success(
                    ReleaseSeriesData(
                        providerId = providerId,
                        externalSeriesId = id.toString(),
                        comicVineId = series.comicVineId
                            ?.takeIf { it > 0L }
                            ?.toString(),
                        gcdId = series.gcdId
                            ?.takeIf { it > 0L }
                            ?.toString(),
                        sourceUrl = series.resourceUrl,
                        sourceModifiedAt = series.modified
                    )
                )
            }

            else -> result.failureAs()
        }
    }

    private suspend fun request(
        url: String,
        accessToken: String
    ): ReleaseProviderResult<MetronHttpResponse> {
        val token = accessToken.trim()

        if (token.isEmpty()) {
            return ReleaseProviderResult.MissingCredentials
        }

        val response = try {
            transport.get(
                url = url,
                accessToken = token
            )
        } catch (_: IOException) {
            return ReleaseProviderResult.NetworkError
        }

        return when (response.statusCode) {
            in 200..299 ->
                ReleaseProviderResult.Success(response)

            401 ->
                ReleaseProviderResult.InvalidCredentials

            403 ->
                ReleaseProviderResult.AccessDenied

            429 -> {
                val retryAfter =
                    response.headers.entries
                        .firstOrNull {
                            it.key.equals(
                                "Retry-After",
                                ignoreCase = true
                            )
                        }
                        ?.value
                        ?.toLongOrNull()

                ReleaseProviderResult.RateLimited(
                    retryAfterSeconds = retryAfter
                )
            }

            else ->
                ReleaseProviderResult.ServiceError(
                    "Metron returned HTTP ${response.statusCode}."
                )
        }
    }

    private fun isTrustedPaginationUrl(value: String): Boolean {
        val uri = try {
            URI(value)
        } catch (_: Exception) {
            return false
        }

        return uri.scheme == "https" &&
            uri.host.equals("metron.cloud", ignoreCase = true) &&
            uri.path?.startsWith("/api/issue/") == true
    }

    private fun MetronIssueDto.toScheduledRelease():
        ScheduledReleaseData? {
        val id = id ?: return null
        val seriesInfo = series ?: return null
        val seriesId = seriesInfo.id ?: return null

        val title = seriesInfo.name
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val number = number
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return ScheduledReleaseData(
            providerId = providerId,
            externalIssueId = id.toString(),
            externalSeriesId = seriesId.toString(),
            seriesTitle = title,
            seriesVolume = seriesInfo.volume,
            seriesStartYear = seriesInfo.yearBegan,
            publisherName = seriesInfo.publisher?.name,
            issueNumber = number,
            displayTitle = issue,
            coverDate = coverDate,
            storeReleaseDate = storeDate,
            focDate = focDate,
            price = price,
            priceCurrency = priceCurrency,
            distributorSku = sku,
            upc = upc,
            comicVineId = comicVineId?.toString(),
            gcdId = gcdId?.toString(),
            sourceUrl = resourceUrl,
            sourceModifiedAt = modified
        )
    }

    private fun <T> ReleaseProviderResult<*>.failureAs():
        ReleaseProviderResult<T> =
        when (this) {
            is ReleaseProviderResult.Success<*> ->
                error("Cannot convert a successful result to a failure.")

            ReleaseProviderResult.MissingCredentials ->
                ReleaseProviderResult.MissingCredentials

            ReleaseProviderResult.InvalidCredentials ->
                ReleaseProviderResult.InvalidCredentials

            ReleaseProviderResult.AccessDenied ->
                ReleaseProviderResult.AccessDenied

            is ReleaseProviderResult.RateLimited ->
                ReleaseProviderResult.RateLimited(
                    retryAfterSeconds = this.retryAfterSeconds
                )

            ReleaseProviderResult.NetworkError ->
                ReleaseProviderResult.NetworkError

            is ReleaseProviderResult.ServiceError ->
                ReleaseProviderResult.ServiceError(message)
        }

    private companion object {
        const val API_BASE_URL = "https://metron.cloud/api"
    }
}

@Serializable
private data class MetronIssuePageDto(
    val count: Int = 0,
    val next: String? = null,
    val results: List<MetronIssueDto> = emptyList()
)

@Serializable
private data class MetronIssueDto(
    val id: Long? = null,
    val series: MetronSeriesDto? = null,
    val number: String? = null,
    val issue: String? = null,

    @SerialName("cover_date")
    val coverDate: String? = null,

    @SerialName("store_date")
    val storeDate: String? = null,

    @SerialName("foc_date")
    val focDate: String? = null,

    val price: String? = null,

    @SerialName("price_currency")
    val priceCurrency: String? = null,

    val sku: String? = null,
    val upc: String? = null,

    @SerialName("cv_id")
    val comicVineId: Long? = null,

    @SerialName("gcd_id")
    val gcdId: Long? = null,

    @SerialName("resource_url")
    val resourceUrl: String? = null,

    val modified: String? = null
)

@Serializable
private data class MetronSeriesDto(
    val id: Long? = null,
    val name: String? = null,
    val volume: Int? = null,

    @SerialName("year_began")
    val yearBegan: Int? = null,

    val publisher: MetronPublisherDto? = null
)

@Serializable
private data class MetronSeriesDetailDto(
    val id: Long? = null,

    @SerialName("cv_id")
    val comicVineId: Long? = null,

    @SerialName("gcd_id")
    val gcdId: Long? = null,

    @SerialName("resource_url")
    val resourceUrl: String? = null,

    val modified: String? = null
)

@Serializable
private data class MetronPublisherDto(
    val id: Long? = null,
    val name: String? = null
)