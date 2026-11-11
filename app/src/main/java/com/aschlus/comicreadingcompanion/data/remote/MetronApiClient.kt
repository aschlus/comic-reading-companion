package com.aschlus.comicreadingcompanion.data.remote

import android.annotation.SuppressLint
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

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

    private val requestMutex = Mutex()

    // Updated from Metron's response headers. Access is protected by requestMutex.
    private var burstRemaining: Long? = null
    private var burstResetAtEpochSeconds: Long? = null
    private var sustainedRemaining: Long? = null
    private var sustainedResetAtEpochSeconds: Long? = null

    override val providerId: String = "METRON"

    override val attribution =
        ReleaseProviderAttribution(
            providerName = "Metron",
            sourceUrl = "https://metron.cloud/",
            licenseName =
                "Creative Commons Attribution-ShareAlike 4.0 International " +
                    "(CC BY-SA 4.0)",
            licenseUrl =
                "https://creativecommons.org/licenses/by-sa/4.0/",
            changesNotice =
                "Changes: Metron release fields are normalized into the " +
                    "app's local data model. Store-date and FOC-date updates " +
                    "reported by Metron are retained in release-change history.",
            additionalNotice =
                "Metron cover images are not imported; their rights are " +
                    "separate from the CC BY-SA 4.0 catalog-data license."
        )

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

    @SuppressLint("RestrictedApi")
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
        var pageNumber = 0

        Log.d(
            LOG_TAG,
            "Starting scheduled-release sync. " +
                "Date range: $fromDate through $toDate. " +
                "Incremental sync: ${modifiedAfter != null}."
        )

        while (true) {
            val currentUrl = nextUrl ?: break

            if (pageNumber >= MAX_ISSUE_LIST_PAGES) {
                val message =
                    "Metron returned more than $MAX_ISSUE_LIST_PAGES " +
                        "issue-list pages. Sync was stopped to prevent " +
                        "an unbounded request loop."

                Log.e(LOG_TAG, message)

                return ReleaseProviderResult.ServiceError(message)
            }

            if (!visitedUrls.add(currentUrl)) {
                Log.e(
                    LOG_TAG,
                    "Metron returned a repeated pagination URL: $currentUrl"
                )

                return ReleaseProviderResult.ServiceError(
                    "Metron returned a repeated pagination URL."
                )
            }

            pageNumber++

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

            Log.d(
                LOG_TAG,
                "Issue page $pageNumber returned ${page.results.size} " +
                    "records. Provider total: ${page.count}. " +
                    "Has next page: ${page.next != null}."
            )

            for (issue in page.results) {
                val release = issue.toScheduledRelease()
                    ?: return ReleaseProviderResult.ServiceError(
                        "An issue returned by Metron was missing required identity fields."
                    )

                releases += release
            }

            Log.d(
                LOG_TAG,
                "Processed issue page $pageNumber. " +
                    "Accumulated releases: ${releases.size}."
            )

            nextUrl = page.next

            if (
                nextUrl != null &&
                !isTrustedPaginationUrl(nextUrl)
            ) {
                Log.e(
                    LOG_TAG,
                    "Metron returned an unexpected pagination URL: $nextUrl"
                )

                return ReleaseProviderResult.ServiceError(
                    "Metron returned an unexpected pagination URL."
                )
            }
        }

        Log.i(
            LOG_TAG,
            "Scheduled-release sync completed. " +
                "Pages: $pageNumber. Releases: ${releases.size}."
        )

        return ReleaseProviderResult.Success(releases)
    }

    override suspend fun getScheduledReleasesForSeries(
        accessToken: String,
        externalSeriesId: String,
        fromDate: LocalDate,
        toDate: LocalDate,
        modifiedAfter: Instant?
    ): ReleaseProviderResult<List<ScheduledReleaseData>> {

        if (fromDate.isAfter(toDate)) {
            return ReleaseProviderResult.ServiceError(
                "The start date must be on or before the end date."
            )
        }

        val seriesId =
            externalSeriesId.toLongOrNull()
                ?.takeIf { it > 0L }
                ?: return ReleaseProviderResult.ServiceError(
                    "The Metron series ID is invalid."
                )

        val query =
            modifiedAfter?.let {
                "series_id=$seriesId" +
                    "&modified_gt=" +
                    URLEncoder.encode(
                        it.toString(),
                        StandardCharsets.UTF_8.name()
                    )
            } ?: run {
                "series_id=$seriesId" +
                    "&store_date_range_after=$fromDate" +
                    "&store_date_range_before=$toDate"
            }

        return getScheduledReleasesFromUrl(
            accessToken = accessToken,
            initialUrl = "$API_BASE_URL/issue/?$query"
        )
    }

    override suspend fun findSeriesByComicVineId(
        accessToken: String,
        comicVineId: String,
    ): ReleaseProviderResult<List<ReleaseSeriesData>> {

        val id =
            comicVineId.trim().toLongOrNull()
                ?.takeIf { it > 0L }
                ?: return ReleaseProviderResult.ServiceError(
                    "The Comic Vine series ID is invalid."
                )

        val url =
            "$API_BASE_URL/series/?cv_id=$id"

        val visitedUrls = mutableSetOf<String>()
        val resolvedSeries = mutableListOf<ReleaseSeriesData>()

        var nextUrl: String? = url
        var pageNumber = 0

        while (nextUrl != null) {
            val currentUrl = nextUrl

            if (pageNumber >= MAX_SERIES_LOOKUP_PAGES) {
                return ReleaseProviderResult.ServiceError(
                    "Metron returned more than " +
                        "$MAX_SERIES_LOOKUP_PAGES series lookup pages."
                )
            }

            if (!visitedUrls.add(currentUrl)) {
                return ReleaseProviderResult.ServiceError(
                    "Metron returned a repeated series pagination URL."
                )
            }

            pageNumber++

            val body = when (
                val result = request(currentUrl, accessToken)
            ) {
                is ReleaseProviderResult.Success ->
                    result.value.body

                else ->
                    return result.failureAs()
            }

            val page = try {
                json.decodeFromString<MetronSeriesSearchPageDto>(body)
            } catch (_: SerializationException) {
                return ReleaseProviderResult.ServiceError(
                    "Metron returned an unexpected series-list response."
                )
            } catch (_: IllegalArgumentException) {
                return ReleaseProviderResult.ServiceError(
                    "Metron returned an unexpected series-list response."
                )
            }

            for (series in page.results) {
                val metronSeriesId =
                    series.id
                        ?.takeIf { it > 0L }
                        ?: return ReleaseProviderResult.ServiceError(
                            "A Metron series result was missing its ID."
                        )

                val returnedComicVineId =
                    series.comicVineId?.toString()

                if (returnedComicVineId != id.toString()) {
                    return ReleaseProviderResult.ServiceError(
                        "Metron returned a series with a different " +
                            "Comic Vine ID."
                    )
                }

                resolvedSeries += ReleaseSeriesData(
                    providerId = providerId,
                    externalSeriesId = metronSeriesId.toString(),
                    comicVineId = returnedComicVineId,
                    gcdId = series.gcdId
                        ?.takeIf { it > 0L }
                        ?.toString(),
                    sourceUrl = series.resourceUrl,
                    sourceModifiedAt = series.modified,
                    seriesTitle = series.name,
                    seriesVolume = series.volume,
                    seriesStartYear = series.yearBegan,
                    publisherName = series.publisher?.name
                )
            }

            nextUrl = page.next

            if (
                nextUrl != null &&
                !isTrustedSeriesPaginationUrl(nextUrl)
            ) {
                return ReleaseProviderResult.ServiceError(
                    "Metron returned an unexpected series pagination URL."
                )
            }
        }

        return ReleaseProviderResult.Success(resolvedSeries)
    }

    override suspend fun findSeriesByName(
        accessToken: String,
        seriesName: String
    ): ReleaseProviderResult<List<ReleaseSeriesData>> {

        if (seriesName.isBlank()) {
            return ReleaseProviderResult.ServiceError(
                "The series name is required."
            )
        }

        val encodedName =
            URLEncoder.encode(
                seriesName.trim(),
                StandardCharsets.UTF_8.name()
            )

        var nextUrl: String? =
            "$API_BASE_URL/series/?name=$encodedName"

        val visitedUrls = mutableSetOf<String>()
        val results = mutableListOf<ReleaseSeriesData>()
        var pageNumber = 0

        while (nextUrl != null) {
            val currentUrl = nextUrl

            if (pageNumber >= MAX_SERIES_LOOKUP_PAGES) {
                return ReleaseProviderResult.ServiceError(
                    "Metron returned more than " +
                        "$MAX_SERIES_LOOKUP_PAGES series lookup pages."
                )
            }

            if (!visitedUrls.add(currentUrl)) {
                return ReleaseProviderResult.ServiceError(
                    "Metron returned a repeated series pagination URL."
                )
            }

            pageNumber++

            val body = when (
                val result = request(currentUrl, accessToken)
            ) {
                is ReleaseProviderResult.Success ->
                    result.value.body

                else ->
                    return result.failureAs()
            }

            val page = try {
                json.decodeFromString<MetronSeriesSearchPageDto>(body)
            } catch (_: SerializationException) {
                return ReleaseProviderResult.ServiceError(
                    "Metron returned an unexpected series-list response."
                )
            } catch (_: IllegalArgumentException) {
                return ReleaseProviderResult.ServiceError(
                    "Metron returned an unexpected series-list response."
                )
            }

            for (series in page.results) {
                val id =
                    series.id?.takeIf { it > 0L }
                        ?: return ReleaseProviderResult.ServiceError(
                            "A Metron series result was missing its ID."
                        )

                results += ReleaseSeriesData(
                    providerId = providerId,
                    externalSeriesId = id.toString(),
                    comicVineId = series.comicVineId
                        ?.takeIf { it > 0L }
                        ?.toString(),
                    gcdId = series.gcdId
                        ?.takeIf { it > 0L }
                        ?.toString(),
                    sourceUrl = series.resourceUrl,
                    sourceModifiedAt = series.modified,
                    seriesTitle = series.name,
                    seriesVolume = series.volume,
                    seriesStartYear = series.yearBegan,
                    publisherName = series.publisher?.name
                )
            }

            nextUrl = page.next

            if (
                nextUrl != null &&
                !isTrustedSeriesPaginationUrl(nextUrl)
            ) {
                return ReleaseProviderResult.ServiceError(
                    "Metron returned an unexpected series pagination URL."
                )
            }
        }

        return ReleaseProviderResult.Success(results)
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
    ): ReleaseProviderResult<MetronHttpResponse> =
        requestMutex.withLock {

            val token = accessToken.trim()

            if (token.isEmpty()) {
                return@withLock ReleaseProviderResult.MissingCredentials
            }

            val now = Instant.now().epochSecond
            val retryAfterSeconds = rateLimitWaitSeconds(now)

            if (retryAfterSeconds != null) {
                Log.w(
                    LOG_TAG,
                    "Skipping Metron request because a rate limit is active. " +
                        "Retry after $retryAfterSeconds seconds."
                )

                return@withLock ReleaseProviderResult.RateLimited(
                    retryAfterSeconds = retryAfterSeconds
                )
            }

            val requestStartedAt = System.currentTimeMillis()

            Log.d(LOG_TAG, "GET $url")

            val response = try {
                transport.get(
                    url = url,
                    accessToken = token
                )
            } catch (exception: IOException) {
                Log.w(
                    LOG_TAG,
                    "GET $url failed with an I/O error.",
                    exception
                )

                return@withLock ReleaseProviderResult.NetworkError
            }

            val elapsedMillis =
                System.currentTimeMillis() - requestStartedAt

            updateRateLimitState(response.headers)

            Log.d(
                LOG_TAG,
                "GET $url returned HTTP ${response.statusCode} " +
                    "in ${elapsedMillis}ms."
            )

            when (response.statusCode) {
                in 200..299 ->
                    ReleaseProviderResult.Success(response)

                401 ->
                    ReleaseProviderResult.InvalidCredentials

                403 ->
                    ReleaseProviderResult.AccessDenied

                429 -> {
                    val rateLimitedAt = Instant.now().epochSecond

                    val serverRetryAfter =
                        rateLimitHeader(
                            response.headers,
                            "Retry-After"
                        )?.coerceAtLeast(1L)
                            ?: 60L

                    burstRemaining = 0L

                    val retryReset =
                        rateLimitedAt + serverRetryAfter

                    burstResetAtEpochSeconds =
                        maxOf(
                            burstResetAtEpochSeconds ?: 0L,
                            retryReset
                        )

                    val effectiveRetry =
                        rateLimitWaitSeconds(rateLimitedAt)
                            ?: serverRetryAfter

                    Log.w(
                        LOG_TAG,
                        "Metron rate limit reached. " +
                            "Retry after $effectiveRetry seconds."
                    )

                    ReleaseProviderResult.RateLimited(
                        retryAfterSeconds = effectiveRetry
                    )
                }

                else ->
                    ReleaseProviderResult.ServiceError(
                        "Metron returned HTTP ${response.statusCode}."
                    )
            }
        }

    private fun updateRateLimitState(
        headers: Map<String, String>
    ) {
        val now = Instant.now().epochSecond

        rateLimitHeader(
            headers,
            "X-RateLimit-Burst-Remaining"
        )?.let {
            burstRemaining = it
        }

        rateLimitHeader(
            headers,
            "X-RateLimit-Burst-Reset"
        )?.let {
            burstResetAtEpochSeconds = it
        }

        rateLimitHeader(
            headers,
            "X-RateLimit-Sustained-Remaining"
        )?.let {
            sustainedRemaining = it
        }

        rateLimitHeader(
            headers,
            "X-RateLimit-Sustained-Reset"
        )?.let {
            sustainedResetAtEpochSeconds = it
        }

        if (
            burstRemaining?.let { it <= 0L } == true &&
            (
                burstResetAtEpochSeconds == null ||
                    burstResetAtEpochSeconds!! <= now
                )
        ) {
            burstResetAtEpochSeconds = now + 60L
        }

        if (
            sustainedRemaining?.let { it <= 0L } == true &&
            (
                sustainedResetAtEpochSeconds == null ||
                    sustainedResetAtEpochSeconds!! <= now
                )
        ) {
            sustainedResetAtEpochSeconds =
                now + 86_400L
        }
    }

    private fun rateLimitHeader(
        headers: Map<String, String>,
        name: String
    ): Long? {
        return headers.entries
            .firstOrNull {
                it.key.equals(name, ignoreCase = true)
            }
            ?.value
            ?.trim()
            ?.toLongOrNull()
    }

    private fun rateLimitWaitSeconds(
        now: Long
    ): Long? {

        val blockedUntil = mutableListOf<Long>()

        if (burstRemaining?.let { it <= 0L } == true) {
            val resetAt =
                burstResetAtEpochSeconds
                    ?: (now + 60L).also {
                        burstResetAtEpochSeconds = it
                    }

            if (resetAt > now) {
                blockedUntil += resetAt
            } else {
                burstRemaining = null
                burstResetAtEpochSeconds = null
            }
        }

        if (sustainedRemaining?.let { it <= 0L } == true) {
            val resetAt =
                sustainedResetAtEpochSeconds
                    ?: (now + 86_400L).also {
                        sustainedResetAtEpochSeconds = it
                    }

            if (resetAt > now) {
                blockedUntil += resetAt
            } else {
                sustainedRemaining = null
                sustainedResetAtEpochSeconds = null
            }
        }

        val latestReset =
            blockedUntil.maxOrNull()
                ?: return null

        return (latestReset - now).coerceAtLeast(1L)
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

    private fun isTrustedSeriesPaginationUrl(
        value: String
    ): Boolean {
        val uri = try {
            URI(value)
        } catch (_: Exception) {
            return false
        }

        return uri.scheme == "https" &&
            uri.host.equals("metron.cloud", ignoreCase = true) &&
            uri.path?.startsWith("/api/series/") == true
    }

    private suspend fun getScheduledReleasesFromUrl(
        accessToken: String,
        initialUrl: String
    ): ReleaseProviderResult<List<ScheduledReleaseData>> {

        var nextUrl: String? = initialUrl

        val visitedUrls = mutableSetOf<String>()
        val releases = mutableListOf<ScheduledReleaseData>()

        var pageNumber = 0

        while (nextUrl != null) {
            val currentUrl = nextUrl

            if (pageNumber >= MAX_ISSUE_LIST_PAGES) {
                return ReleaseProviderResult.ServiceError(
                    "Metron returned more than " +
                        "$MAX_ISSUE_LIST_PAGES issue-list pages. " +
                        "Sync was stopped to prevent an unbounded request loop."
                )
            }

            if (!visitedUrls.add(currentUrl)) {
                return ReleaseProviderResult.ServiceError(
                    "Metron returned a repeated pagination URL."
                )
            }

            pageNumber++

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
        const val LOG_TAG = "MetronApiClient"
        const val MAX_ISSUE_LIST_PAGES = 50
        const val MAX_SERIES_LOOKUP_PAGES = 50
    }
}

@Serializable
private data class MetronIssuePageDto(
    val count: Int = 0,
    val next: String? = null,
    val results: List<MetronIssueDto> = emptyList()
)

@Serializable
private data class MetronSeriesSearchPageDto(
    val count: Int = 0,
    val next: String? = null,
    val results: List<MetronSeriesSearchDto> = emptyList()
)

@Serializable
private data class MetronSeriesSearchDto(
    val id: Long? = null,

    @SerialName("series")
    val name: String? = null,

    @SerialName("year_began")
    val yearBegan: Int? = null,

    @SerialName("year_end")
    val yearEnd: Int? = null,

    val volume: Int? = null,

    @SerialName("cv_id")
    val comicVineId: Long? = null,

    @SerialName("gcd_id")
    val gcdId: Long? = null,

    @SerialName("resource_url")
    val resourceUrl: String? = null,

    val modified: String? = null,
    val publisher: MetronPublisherDto? = null,
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