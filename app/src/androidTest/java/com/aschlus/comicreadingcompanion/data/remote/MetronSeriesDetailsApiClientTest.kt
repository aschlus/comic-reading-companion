
package com.aschlus.comicreadingcompanion.data.remote

import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class MetronSeriesDetailsApiClientTest {

    @Test
    fun getSeriesDetails_parsesComicVineAndGcdIdentifiers() =
        runBlocking {

            val url = "https://metron.cloud/api/series/45/"

            val transport = FakeTransport(
                responses = mapOf(
                    url to response(
                        body = """
                            {
                                "id": 45,
                                "series": "Amazing Spider-Man (1998)",
                                "year_began": 1998,
                                "volume": 2,
                                "cv_id": 18693,
                                "gcd_id": 621,
                                "resource_url": "https://metron.cloud/series/45/",
                                "modified": "2026-10-20T08:00:00Z"
                            }
                        """.trimIndent()
                    )
                )
            )

            val result =
                MetronApiClient(transport).getSeriesDetails(
                    accessToken = "test-token",
                    externalSeriesId = "45"
                )

            assertTrue(
                result is ReleaseProviderResult.Success
            )

            val series =
                (result as ReleaseProviderResult.Success).value

            assertEquals("METRON", series.providerId)
            assertEquals("45", series.externalSeriesId)
            assertEquals("18693", series.comicVineId)
            assertEquals("621", series.gcdId)

            assertEquals(
                "https://metron.cloud/series/45/",
                series.sourceUrl
            )

            assertEquals(
                "2026-10-20T08:00:00Z",
                series.sourceModifiedAt
            )

            assertEquals(1, transport.requests.size)

            assertEquals(
                url,
                transport.requests.single().url
            )

            assertEquals(
                "test-token",
                transport.requests.single().accessToken
            )
        }

    @Test
    fun getSeriesDetails_succeedsWhenComicVineIdIsMissing() =
        runBlocking {

            val url = "https://metron.cloud/api/series/45/"

            val transport = FakeTransport(
                responses = mapOf(
                    url to response(
                        body = """
                            {
                                "id": 45,
                                "series": "Example Series",
                                "gcd_id": 621
                            }
                        """.trimIndent()
                    )
                )
            )

            val result =
                MetronApiClient(transport).getSeriesDetails(
                    accessToken = "test-token",
                    externalSeriesId = "45"
                )

            assertTrue(
                result is ReleaseProviderResult.Success
            )

            val series =
                (result as ReleaseProviderResult.Success).value

            assertNull(series.comicVineId)
            assertEquals("621", series.gcdId)
        }

    @Test
    fun getSeriesDetails_rejectsDifferentReturnedSeriesId() =
        runBlocking {

            val url = "https://metron.cloud/api/series/45/"

            val transport = FakeTransport(
                responses = mapOf(
                    url to response(
                        body = """
                            {
                                "id": 46,
                                "cv_id": 18693
                            }
                        """.trimIndent()
                    )
                )
            )

            val result =
                MetronApiClient(transport).getSeriesDetails(
                    accessToken = "test-token",
                    externalSeriesId = "45"
                )

            assertTrue(
                result is ReleaseProviderResult.ServiceError
            )

            assertEquals(1, transport.requests.size)
        }

    @Test
    fun getSeriesDetails_invalidSeriesIdDoesNotMakeRequest() =
        runBlocking {

            val transport = FakeTransport(emptyMap())

            val invalidIds = listOf(
                "",
                "abc",
                "0",
                "-1"
            )

            invalidIds.forEach { invalidId ->

                val result =
                    MetronApiClient(transport).getSeriesDetails(
                        accessToken = "test-token",
                        externalSeriesId = invalidId
                    )

                assertTrue(
                    result is ReleaseProviderResult.ServiceError
                )
            }

            assertTrue(transport.requests.isEmpty())
        }

    @Test
    fun getSeriesDetails_networkFailureReturnsNetworkError() =
        runBlocking {

            val url = "https://metron.cloud/api/series/45/"

            val transport = FakeTransport(
                responses = emptyMap()
            )

            val result =
                MetronApiClient(transport).getSeriesDetails(
                    accessToken = "test-token",
                    externalSeriesId = "45"
                )

            assertEquals(
                ReleaseProviderResult.NetworkError,
                result
            )

            assertEquals(1, transport.requests.size)
            assertEquals(
                url,
                transport.requests.single().url
            )
        }

    @Test
    fun getSeriesDetails_unauthorizedResponseReturnsInvalidCredentials() =
        runBlocking {

            val url = "https://metron.cloud/api/series/45/"

            val transport = FakeTransport(
                responses = mapOf(
                    url to response(
                        statusCode = 401,
                        body = """
                            {
                                "detail": "Authentication credentials were not provided."
                            }
                        """.trimIndent()
                    )
                )
            )

            val result =
                MetronApiClient(transport).getSeriesDetails(
                    accessToken = "test-token",
                    externalSeriesId = "45"
                )

            assertEquals(
                ReleaseProviderResult.InvalidCredentials,
                result
            )
        }

    private fun response(
        statusCode: Int = 200,
        body: String
    ) = MetronHttpResponse(
        statusCode = statusCode,
        body = body
    )

    private class FakeTransport(
        private val responses: Map<String, MetronHttpResponse>
    ) : MetronHttpTransport {

        data class Request(
            val url: String,
            val accessToken: String
        )

        val requests = mutableListOf<Request>()

        override suspend fun get(
            url: String,
            accessToken: String
        ): MetronHttpResponse {

            requests += Request(
                url = url,
                accessToken = accessToken
            )

            return responses[url]
                ?: throw IOException(
                    "No fake response configured for $url"
                )
        }
    }
}
