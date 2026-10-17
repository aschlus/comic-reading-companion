package com.aschlus.comicreadingcompanion.data.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

sealed interface ComicVineApiKeyValidationResult {
    data object Valid : ComicVineApiKeyValidationResult
    data object MissingApiKey : ComicVineApiKeyValidationResult
    data object InvalidApiKey : ComicVineApiKeyValidationResult
    data object NetworkError : ComicVineApiKeyValidationResult
    data object ServiceError : ComicVineApiKeyValidationResult
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