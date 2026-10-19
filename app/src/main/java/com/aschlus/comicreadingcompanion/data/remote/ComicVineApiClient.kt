package com.aschlus.comicreadingcompanion.data.remote

import androidx.room3.Query
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

sealed interface ComicVineApiKeyValidationResult {
    data object Valid : ComicVineApiKeyValidationResult
    data object MissingApiKey : ComicVineApiKeyValidationResult
    data object InvalidApiKey : ComicVineApiKeyValidationResult
    data object NetworkError : ComicVineApiKeyValidationResult
    data object ServiceError : ComicVineApiKeyValidationResult
}

data class ComicVineVolumeSearchHit(
    val id: Long,
    val name: String,
    val startYear: Int?,
    val countOfIssues: Int?,
    val publisherName: String?,
    val siteDetailUrl: String?,
    val imageUrl: String?
)

sealed interface ComicVineVolumeSearchOutcome {
    data class Success(
        val volumes: List<ComicVineVolumeSearchHit>,
        val totalResults: Int,
        val offset: Int
    ) : ComicVineVolumeSearchOutcome

    data object MissingApiKey : ComicVineVolumeSearchOutcome
    data object InvalidApiKey : ComicVineVolumeSearchOutcome
    data object NetworkError : ComicVineVolumeSearchOutcome
    data object ServiceError : ComicVineVolumeSearchOutcome
}

data class ComicVineVolumeDetails(
    val id: Long,
    val name: String,
    val startYear: Int?,
    val countOfIssues: Int?,
    val publisherName: String?,
    val siteDetailUrl: String?,
    val description: String?,
    val imageUrl: String?
)

sealed interface ComicVineVolumeDetailsOutcome {
    data class Success(
        val volume: ComicVineVolumeDetails
    ) : ComicVineVolumeDetailsOutcome

    data object MissingApiKey : ComicVineVolumeDetailsOutcome
    data object InvalidApiKey : ComicVineVolumeDetailsOutcome
    data object NetworkError : ComicVineVolumeDetailsOutcome
    data object ServiceError : ComicVineVolumeDetailsOutcome
}

data class ComicVineIssueHit(
    val id: Long,
    val issueNumber: String?,
    val name: String?,
    val coverDate: String?,
    val storeDate: String?,
    val description: String?,
    val siteDetailUrl: String?,
    val imageUrl: String?
)

sealed interface ComicVineIssuePageOutcome {
    data class Success(
        val issues: List<ComicVineIssueHit>,
        val totalResults: Int,
        val offset: Int,
        val pageResultCount: Int
    ) : ComicVineIssuePageOutcome

    data object MissingApiKey : ComicVineIssuePageOutcome
    data object InvalidApiKey : ComicVineIssuePageOutcome
    data object NetworkError : ComicVineIssuePageOutcome
    data object ServiceError : ComicVineIssuePageOutcome
}

fun interface ComicVineHttpTransport {
    suspend fun get(url: String): String
}

class ComicVineApiClient(
    private val transport: ComicVineHttpTransport =
        UrlConnectionComicVineHttpTransport(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
    }
) {
    suspend fun validateApiKey(
        apikey: String
    ): ComicVineApiKeyValidationResult {
        val normalizedKey = apikey.trim()

        if (normalizedKey.isEmpty()) {
            return ComicVineApiKeyValidationResult.MissingApiKey
        }

        val encodedKey = URLEncoder.encode(
            normalizedKey,
            StandardCharsets.UTF_8.name()
        )

        val requestUrl =
            "$API_BASE_URL/volumes/" +
                "?api_key=$encodedKey" +
                "&format=json" +
                "&limit=1" +
                "&field_list=id"

        val responseBody = try {
            transport.get(requestUrl)
        } catch (exception: IOException) {
            return ComicVineApiKeyValidationResult.NetworkError
        }

        val response = try {
            json.parseToJsonElement(responseBody) as? JsonObject
                ?: return ComicVineApiKeyValidationResult.ServiceError
        } catch (exception: SerializationException) {
            return ComicVineApiKeyValidationResult.ServiceError
        }

        val statusCode =
            (response["status_code"] as? JsonPrimitive)?.intOrNull
                ?: return ComicVineApiKeyValidationResult.ServiceError

        val apiError =
            (response["error"] as? JsonPrimitive)?.contentOrNull

        return when {
            statusCode == 1 &&
                apiError.equals("OK", ignoreCase = true) ->
                ComicVineApiKeyValidationResult.Valid

            statusCode == 100 ||
                apiError?.contains(
                    "invalid api key",
                    ignoreCase = true
                ) == true ->
                ComicVineApiKeyValidationResult.InvalidApiKey

            else ->
                ComicVineApiKeyValidationResult.ServiceError
        }
    }

    suspend fun searchVolumes(
        apiKey: String,
        query: String,
        offset: Int = 0
    ): ComicVineVolumeSearchOutcome {
        val normalizedKey = apiKey.trim()
        val normalizedQuery = query.trim()

        if (normalizedQuery.isEmpty()) {
            return ComicVineVolumeSearchOutcome.Success(
                volumes = emptyList(),
                totalResults = 0,
                offset = 0
            )
        }

        val safeOffset = offset.coerceAtLeast(0)

        val encodedKey = URLEncoder.encode(
            normalizedKey,
            StandardCharsets.UTF_8.name()
        )
        val encodedQuery = URLEncoder.encode(
            normalizedQuery,
            StandardCharsets.UTF_8.name()
        )

        val requestUrl =
            "$API_BASE_URL/search/" +
                "?api_key=$encodedKey" +
                "&format=json" +
                "&resources=volume" +
                "&query=$encodedQuery" +
                "&limit=50" +
                "&offset=$safeOffset" +
                "&field_list=id,name,start_year,count_of_issues," +
                "publisher,site_detail_url,image"

        val responseBody = try {
            transport.get(requestUrl)
        } catch (exception: IOException) {
            return ComicVineVolumeSearchOutcome.NetworkError
        }

        val response = try {
            json.parseToJsonElement(responseBody) as? JsonObject
                ?: return ComicVineVolumeSearchOutcome.ServiceError
        } catch (exception: SerializationException) {
            return ComicVineVolumeSearchOutcome.ServiceError
        }

        val statusCode = response.intValue("status_code")
            ?: return ComicVineVolumeSearchOutcome.ServiceError

        val apiError = response.stringValue("error")

        if (
            statusCode == 100 ||
            apiError?.contains(
                "invalid api key",
                ignoreCase = true
            ) == true
        ) {
            return ComicVineVolumeSearchOutcome.InvalidApiKey
        }

        if (
            statusCode != 1 ||
            !apiError.equals("OK", ignoreCase = true)
        ) {
            return ComicVineVolumeSearchOutcome.ServiceError
        }

        val rawResults = response["results"] as? JsonArray
            ?: return ComicVineVolumeSearchOutcome.ServiceError

        val volumes = rawResults.mapNotNull { element ->
            val volume = element as? JsonObject
                ?: return@mapNotNull null

            val id = volume.longValue("id")
                ?: return@mapNotNull null

            val name = volume.stringValue("name")
                ?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            val image = volume["image"] as? JsonObject
            val publisher = volume["publisher"] as? JsonObject

            ComicVineVolumeSearchHit(
                id = id,
                name = name,
                startYear = volume.stringValue("start_year")
                    ?.toIntOrNull(),
                countOfIssues = volume.intValue("count_of_issues"),
                publisherName = publisher?.stringValue("name"),
                siteDetailUrl = volume.stringValue("site_detail_url"),
                imageUrl = image?.stringValue("small_url")
                    ?: image?.stringValue("thumb_url")
                    ?: image?.stringValue("medium_url")
            )
        }

        return ComicVineVolumeSearchOutcome.Success(
            volumes = volumes,
            totalResults = response.intValue(
                "number_of_total_results"
            ) ?: (safeOffset + volumes.size),
            offset = safeOffset
        )
    }

    suspend fun getVolumeDetails(
        apiKey: String,
        volumeId: Long
    ): ComicVineVolumeDetailsOutcome {
        val normalizedKey = apiKey.trim()

        if (normalizedKey.isEmpty()) {
            return ComicVineVolumeDetailsOutcome.MissingApiKey
        }

        if (volumeId <= 0L) {
            return ComicVineVolumeDetailsOutcome.ServiceError
        }

        val encodedKey = URLEncoder.encode(
            normalizedKey,
            StandardCharsets.UTF_8.name()
        )

        val requestUrl =
            "$API_BASE_URL/volume/4050-$volumeId/" +
                "?api_key=$encodedKey" +
                "&format=json" +
                "&limit=1" +
                "&field_list=id,name,start_year,count_of_issues," +
                "publisher,site_detail_url,description,image"

        val responseBody = try {
            transport.get(requestUrl)
        } catch (exception: okio.IOException) {
            return ComicVineVolumeDetailsOutcome.NetworkError
        }

        val response = try {
            json.parseToJsonElement(responseBody) as? JsonObject
                ?: return ComicVineVolumeDetailsOutcome.ServiceError
        } catch (exception: SerializationException) {
            return ComicVineVolumeDetailsOutcome.ServiceError
        }

        val statusCode = response.intValue("status_code")
            ?: return ComicVineVolumeDetailsOutcome.ServiceError
        val apiError = response.stringValue("error")

        if (
            statusCode == 100 ||
            apiError?.contains("invalid api key", ignoreCase = true) == true
        ) {
            return ComicVineVolumeDetailsOutcome.InvalidApiKey
        }

        if (statusCode != 1 || !apiError.equals("OK", ignoreCase = true)) {
            return ComicVineVolumeDetailsOutcome.ServiceError
        }

        val result = response["results"] as? JsonObject
            ?: return ComicVineVolumeDetailsOutcome.ServiceError

        val id = result.longValue("id")
            ?: return ComicVineVolumeDetailsOutcome.ServiceError

        if (id != volumeId) {
            return ComicVineVolumeDetailsOutcome.ServiceError
        }

        val name = result.stringValue("name")
            ?.takeIf { it.isNotBlank() }
            ?: return ComicVineVolumeDetailsOutcome.ServiceError

        val publisher = result["publisher"] as? JsonObject
        val image = result["image"] as? JsonObject

        return ComicVineVolumeDetailsOutcome.Success(
            volume = ComicVineVolumeDetails(
                id = id,
                name = name,
                startYear = result.stringValue("start_year")?.toIntOrNull(),
                countOfIssues = result.intValue("count_of_issues"),
                publisherName = publisher?.stringValue("name"),
                siteDetailUrl = result.stringValue("site_detail_url"),
                description = result.stringValue("description"),
                imageUrl = image?.stringValue("super_url")
                    ?: image?.stringValue("original_url")
                    ?: image?.stringValue("medium_url")
                    ?: image?.stringValue("small_url")
            )
        )
    }

    suspend fun getVolumeIssues(
        apiKey: String,
        volumeId: Long,
        offset: Int = 0
    ): ComicVineIssuePageOutcome {
        val normalizedKey = apiKey.trim()

        if (normalizedKey.isEmpty()) {
            return ComicVineIssuePageOutcome.MissingApiKey
        }

        if (volumeId <= 0L) {
            return ComicVineIssuePageOutcome.ServiceError
        }

        val safeOffset = offset.coerceAtLeast(0)

        val encodedKey = URLEncoder.encode(
            normalizedKey,
            StandardCharsets.UTF_8.name()
        )
        val encodedFilter = URLEncoder.encode(
            "volume:$volumeId",
            StandardCharsets.UTF_8.name()
        )

        val requestUrl =
            "$API_BASE_URL/issues/" +
                "?api_key=$encodedKey" +
                "&format=json" +
                "&filter=$encodedFilter" +
                "&limit=100" +
                "&offset=$safeOffset" +
                "&field_list=id,issue_number,name,cover_date,store_date," +
                "description,image,site_detail_url"

        val responseBody = try {
            transport.get(requestUrl)
        } catch (exception: IOException) {
            return ComicVineIssuePageOutcome.NetworkError
        }

        val response = try {
            json.parseToJsonElement(responseBody) as? JsonObject
                ?: return ComicVineIssuePageOutcome.ServiceError
        } catch (exception: SerializationException) {
            return ComicVineIssuePageOutcome.ServiceError
        }

        val statusCode = response.intValue("status_code")
            ?: return ComicVineIssuePageOutcome.ServiceError
        val apiError = response.stringValue("error")

        if (
            statusCode == 100 ||
            apiError?.contains("invalid api key", ignoreCase = true) == true
        ) {
            return ComicVineIssuePageOutcome.InvalidApiKey
        }

        if (statusCode != 1 || !apiError.equals("OK", ignoreCase = true)) {
            return ComicVineIssuePageOutcome.ServiceError
        }

        val rawResults = response["results"] as? JsonArray
            ?: return ComicVineIssuePageOutcome.ServiceError

        val issues = rawResults.mapNotNull { element ->
            val issue = element as? JsonObject
                ?: return@mapNotNull null

            val id = issue.longValue("id")
                ?: return@mapNotNull null

            val image = issue["image"] as? JsonObject

            ComicVineIssueHit(
                id = id,
                issueNumber = issue.stringValue("issue_number")
                    ?.takeIf { it.isNotBlank() },
                name = issue.stringValue("name")
                    ?.takeIf { it.isNotBlank() },
                coverDate = issue.stringValue("cover_date"),
                storeDate = issue.stringValue("store_date"),
                description = issue.stringValue("description"),
                siteDetailUrl = issue.stringValue("site_detail_url"),
                imageUrl = image?.stringValue("super_url")
                    ?: image?.stringValue("original_url")
                    ?: image?.stringValue("medium_url")
                    ?: image?.stringValue("small_url")
            )
        }

        return ComicVineIssuePageOutcome.Success(
            issues = issues,
            totalResults = response.intValue("number_of_total_results")
                ?: rawResults.size,
            offset = safeOffset,
            pageResultCount = rawResults.size
        )
    }

    private companion object {
        const val API_BASE_URL =
            "https://comicvine.gamespot.com/api"
    }
}

private class UrlConnectionComicVineHttpTransport :
    ComicVineHttpTransport {

    override suspend fun get(url: String): String {
        return withContext(Dispatchers.IO) {
            val connection =
                URL(url).openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.useCaches = false

            try {
                val responseCode = connection.responseCode

                val responseStream =
                    if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val responseBody =
                    responseStream
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        .orEmpty()

                if (responseCode !in 200..299) {
                    throw IOException(
                        "Comic Vine returned HTTP $responseCode"
                    )
                }

                responseBody
            } finally {
                connection.disconnect()
            }
        }
    }
}

private fun JsonObject.stringValue(
    name: String
): String? {
    return (this[name] as? JsonPrimitive)?.contentOrNull
}

private fun JsonObject.intValue(
    name: String
): Int? {
    return (this[name] as? JsonPrimitive)?.intOrNull
}

private fun JsonObject.longValue(
    name: String
): Long? {
    return (this[name] as? JsonPrimitive)?.longOrNull
}