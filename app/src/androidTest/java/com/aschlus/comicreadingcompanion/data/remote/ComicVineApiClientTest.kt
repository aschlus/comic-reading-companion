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

    @Test
    fun searchVolumes_successParsesResultsAndBuildsRequestUrl() =
        runBlocking {
            val transport = FakeTransport(
                responseBody = """
                {
                    "error": "OK",
                    "status_code": 1,
                    "number_of_total_results": 35,
                    "number_of_page_results": 1,
                    "limit": 10,
                    "offset": 20,
                    "results": [
                        {
                            "resource_type": "volume",
                            "id": 123,
                            "name": "Amazing Spider-Man",
                            "start_year": "1963",
                            "count_of_issues": 441,
                            "publisher": {
                                "id": 31,
                                "name": "Marvel"
                            },
                            "site_detail_url":
                                "https://example.com/volume",
                            "image": {
                                "small_url":
                                    "https://example.com/small.jpg"
                            }
                        }
                    ]
                }
            """.trimIndent()
            )

            val result = ComicVineApiClient(transport)
                .searchVolumes(
                    apiKey = "search key/1",
                    query = "Amazing Spider-Man",
                    offset = 20
                )

            assertTrue(
                result is ComicVineVolumeSearchOutcome.Success
            )

            val success =
                result as ComicVineVolumeSearchOutcome.Success

            assertEquals(35, success.totalResults)
            assertEquals(20, success.offset)
            assertEquals(
                listOf(
                    ComicVineVolumeSearchHit(
                        id = 123L,
                        name = "Amazing Spider-Man",
                        startYear = 1963,
                        countOfIssues = 441,
                        publisherName = "Marvel",
                        siteDetailUrl =
                            "https://example.com/volume",
                        imageUrl = "https://example.com/small.jpg"
                    )
                ),
                success.volumes
            )

            val requestedUrl = transport.requestedUrl
            assertNotNull(requestedUrl)

            assertTrue(
                requestedUrl!!.contains(
                    "/search/?api_key=search+key%2F1" +
                            "&format=json" +
                            "&resources=volume" +
                            "&query=Amazing+Spider-Man" +
                            "&limit=50" +
                            "&offset=20" +
                            "&field_list=id,name,start_year,count_of_issues," +
                            "publisher,site_detail_url,image"
                )
            )
        }

    @Test
    fun searchVolumes_invalidKey_returnsInvalidApiKey() =
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
                .searchVolumes(
                    apiKey = "invalid-key",
                    query = "Batman"
                )

            assertEquals(
                ComicVineVolumeSearchOutcome.InvalidApiKey,
                result
            )
        }

    @Test
    fun searchVolumes_blankQuery_returnsEmptyResultWithoutRequest() =
        runBlocking {
            val transport = FakeTransport(
                responseBody = """{"error":"OK","status_code":1}"""
            )

            val result = ComicVineApiClient(transport)
                .searchVolumes(
                    apiKey = "test-key",
                    query = "   "
                )

            assertEquals(
                ComicVineVolumeSearchOutcome.Success(
                    volumes = emptyList(),
                    totalResults = 0,
                    offset = 0
                ),
                result
            )
            assertNull(transport.requestedUrl)
        }

    @Test
    fun searchVolumes_malformedResponse_returnsServiceError() =
        runBlocking {
            val transport = FakeTransport(
                responseBody = "not-json"
            )

            val result = ComicVineApiClient(transport)
                .searchVolumes(
                    apiKey = "test-key",
                    query = "Batman"
                )

            assertEquals(
                ComicVineVolumeSearchOutcome.ServiceError,
                result
            )
        }

    @Test
    fun searchVolumes_transportFailure_returnsNetworkError() =
        runBlocking {
            val transport = FakeTransport(
                failure = IOException("Simulated network failure")
            )

            val result = ComicVineApiClient(transport)
                .searchVolumes(
                    apiKey = "test-key",
                    query = "Batman"
                )

            assertEquals(
                ComicVineVolumeSearchOutcome.NetworkError,
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