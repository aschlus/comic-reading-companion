package com.aschlus.comicreadingcompanion.data.remote

import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ComicVineApiClientTest {

    @Test
    fun validResponse_reportsValidAndBuildsRequestUrl() =
        runBlocking {
            val transport = FakeTransport(
                responseBody = """
                    {
                        "error": "OK",
                        "status_code": 1,
                        "results": []
                    }
                """.trimIndent()
            )

            val result = ComicVineApiClient(transport)
                .validateApiKey(" test key/with spaces ")

            assertEquals(
                ComicVineApiKeyValidationResult.Valid,
                result
            )

            val requestedUrl = transport.requestedUrl
            assertNotNull(requestedUrl)
            assertTrue(
                requestedUrl!!.contains(
                    "/volumes/?api_key=test+key%2Fwith+spaces" +
                            "&format=json&limit=1&field_list=id"
                )
            )
        }

    @Test
    fun invalidKeyResponse_reportsInvalidApiKey() =
        runBlocking {
            val transport = FakeTransport(
                responseBody = """
                    {
                        "error": "Invalid API Key",
                        "status_code": 100,
                        "results": []
                    }
                """.trimIndent()
            )

            val result = ComicVineApiClient(transport)
                .validateApiKey("invalid-test-key")

            assertEquals(
                ComicVineApiKeyValidationResult.InvalidApiKey,
                result
            )
        }

    @Test
    fun blankKey_returnsMissingKeyWithoutRequest() =
        runBlocking {
            val transport = FakeTransport(
                responseBody = """{"error":"OK","status_code":1}"""
            )

            val result = ComicVineApiClient(transport)
                .validateApiKey("   ")

            assertEquals(
                ComicVineApiKeyValidationResult.MissingApiKey,
                result
            )
            assertNull(transport.requestedUrl)
        }

    @Test
    fun unexpectedApiStatus_reportsServiceError() =
        runBlocking {
            val transport = FakeTransport(
                responseBody = """
                    {
                        "error": "Filter Error",
                        "status_code": 107
                    }
                """.trimIndent()
            )

            val result = ComicVineApiClient(transport)
                .validateApiKey("test-key")

            assertEquals(
                ComicVineApiKeyValidationResult.ServiceError,
                result
            )
        }

    @Test
    fun malformedResponse_reportsServiceError() =
        runBlocking {
            val transport = FakeTransport(
                responseBody = "not-json"
            )

            val result = ComicVineApiClient(transport)
                .validateApiKey("test-key")

            assertEquals(
                ComicVineApiKeyValidationResult.ServiceError,
                result
            )
        }

    @Test
    fun transportFailure_reportsNetworkError() =
        runBlocking {
            val transport = FakeTransport(
                failure = IOException("Simulated network failure")
            )

            val result = ComicVineApiClient(transport)
                .validateApiKey("test-key")

            assertEquals(
                ComicVineApiKeyValidationResult.NetworkError,
                result
            )
        }

    private class FakeTransport(
        private val responseBody: String? = null,
        private val failure: IOException? = null
    ) : ComicVineHttpTransport {

        var requestedUrl: String? = null
            private set

        override suspend fun get(url: String): String {
            requestedUrl = url

            failure?.let { throw it }

            return checkNotNull(responseBody)
        }
    }
}