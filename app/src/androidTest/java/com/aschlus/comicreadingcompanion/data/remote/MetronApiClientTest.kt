package com.aschlus.comicreadingcompanion.data.remote

import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class MetronApiClientTest {

    @Test
    fun validateConnection_successUsesTokenAndParsesResponse() =
        runBlocking {
            val url =
                "https://metron.cloud/api/issue/" +
                        "?series_name=comic-reading-companion-connection-check"

            val transport = FakeTransport(
                responses = mapOf(
                    url to response(
                        body = """
                            {
                                "count": 0,
                                "next": null,
                                "previous": null,
                                "results": []
                            }
                        """.trimIndent()
                    )
                )
            )

            val result =
                MetronApiClient(transport)
                    .validateConnection("test-metron-token")

            assertEquals(
                ReleaseProviderResult.Success(Unit),
                result
            )
            assertEquals(1, transport.requests.size)
            assertEquals(
                "test-metron-token",
                transport.requests.single().accessToken
            )
        }

    @Test
    fun validateConnection_blankTokenDoesNotMakeRequest() =
        runBlocking {
            val transport = FakeTransport(emptyMap())

            val result =
                MetronApiClient(transport)
                    .validateConnection("   ")

            assertEquals(
                ReleaseProviderResult.MissingCredentials,
                result
            )
            assertTrue(transport.requests.isEmpty())
        }

    @Test
    fun validateConnection_unauthorizedResponseReportsInvalidToken() =
        runBlocking {
            val url =
                "https://metron.cloud/api/issue/" +
                        "?series_name=comic-reading-companion-connection-check"

            val transport = FakeTransport(
                responses = mapOf(
                    url to response(
                        statusCode = 401,
                        body = """{"detail":"Authentication credentials were not provided."}"""
                    )
                )
            )

            val result =
                MetronApiClient(transport)
                    .validateConnection("invalid-token")

            assertEquals(
                ReleaseProviderResult.InvalidCredentials,
                result
            )
        }

    @Test
    fun getScheduledReleases_parsesListResponse() =
        runBlocking {
            val url =
                "https://metron.cloud/api/issue/" +
                        "?store_date_range_after=2026-11-01" +
                        "&store_date_range_before=2026-11-30"

            val transport = FakeTransport(
                responses = mapOf(
                    url to response(
                        body = """
                            {
                                "count": 1,
                                "next": null,
                                "previous": null,
                                "results": [
                                    {
                                        "id": 123,
                                        "series": {
                                            "id": 45,
                                            "name": "Amazing Spider-Man",
                                            "volume": 2,
                                            "year_began": 1998
                                        },
                                        "number": "1",
                                        "issue": "Amazing Spider-Man (1998) #1",
                                        "cover_date": "2026-12-01",
                                        "store_date": "2026-11-04",
                                        "modified": "2026-10-20T08:00:00Z"
                                    }
                                ]
                            }
                        """.trimIndent()
                    )
                )
            )

            val result =
                MetronApiClient(transport).getScheduledReleases(
                    accessToken = "test-token",
                    fromDate = LocalDate.parse("2026-11-01"),
                    toDate = LocalDate.parse("2026-11-30")
                )

            assertTrue(result is ReleaseProviderResult.Success)

            val release =
                (result as ReleaseProviderResult.Success)
                    .value
                    .single()

            assertEquals("METRON", release.providerId)
            assertEquals("123", release.externalIssueId)
            assertEquals("45", release.externalSeriesId)
            assertEquals("Amazing Spider-Man", release.seriesTitle)
            assertEquals(1998, release.seriesStartYear)
            assertEquals("1", release.issueNumber)
            assertEquals("2026-11-04", release.storeReleaseDate)

            // List responses don't guarantee detail-only fields.
            assertNull(release.focDate)
            assertNull(release.upc)
            assertNull(release.comicVineId)
        }

    @Test
    fun getScheduledReleases_followsPaginationSequentially() =
        runBlocking {
            val firstUrl =
                "https://metron.cloud/api/issue/" +
                        "?store_date_range_after=2026-11-01" +
                        "&store_date_range_before=2026-11-30"

            val secondUrl =
                "https://metron.cloud/api/issue/?page=2"

            val transport = FakeTransport(
                responses = mapOf(
                    firstUrl to response(
                        body = """
                            {
                                "count": 2,
                                "next": "$secondUrl",
                                "previous": null,
                                "results": [
                                    {
                                        "id": 123,
                                        "series": {
                                            "id": 45,
                                            "name": "Series One",
                                            "volume": 1,
                                            "year_began": 2024
                                        },
                                        "number": "1",
                                        "issue": "Series One #1",
                                        "store_date": "2026-11-04"
                                    }
                                ]
                            }
                        """.trimIndent()
                    ),
                    secondUrl to response(
                        body = """
                            {
                                "count": 2,
                                "next": null,
                                "previous": "$firstUrl",
                                "results": [
                                    {
                                        "id": 124,
                                        "series": {
                                            "id": 46,
                                            "name": "Series Two",
                                            "volume": 1,
                                            "year_began": 2025
                                        },
                                        "number": "2",
                                        "issue": "Series Two #2",
                                        "store_date": "2026-11-11"
                                    }
                                ]
                            }
                        """.trimIndent()
                    )
                )
            )

            val result =
                MetronApiClient(transport).getScheduledReleases(
                    accessToken = "test-token",
                    fromDate = LocalDate.parse("2026-11-01"),
                    toDate = LocalDate.parse("2026-11-30")
                )

            assertTrue(result is ReleaseProviderResult.Success)

            val releases =
                (result as ReleaseProviderResult.Success).value

            assertEquals(2, releases.size)
            assertEquals("123", releases[0].externalIssueId)
            assertEquals("124", releases[1].externalIssueId)

            assertEquals(
                listOf(firstUrl, secondUrl),
                transport.requests.map { it.url }
            )
        }

    @Test
    fun getScheduledReleases_rejectsPaginationOutsideMetron() =
        runBlocking {
            val firstUrl =
                "https://metron.cloud/api/issue/" +
                        "?store_date_range_after=2026-11-01" +
                        "&store_date_range_before=2026-11-30"

            val transport = FakeTransport(
                responses = mapOf(
                    firstUrl to response(
                        body = """
                            {
                                "count": 1,
                                "next": "https://example.com/steal-token",
                                "previous": null,
                                "results": []
                            }
                        """.trimIndent()
                    )
                )
            )

            val result =
                MetronApiClient(transport).getScheduledReleases(
                    accessToken = "test-token",
                    fromDate = LocalDate.parse("2026-11-01"),
                    toDate = LocalDate.parse("2026-11-30")
                )

            assertTrue(result is ReleaseProviderResult.ServiceError)
            assertEquals(1, transport.requests.size)
            assertFalse(
                transport.requests.any {
                    it.url.startsWith("https://example.com")
                }
            )
        }

    @Test
    fun getReleaseDetails_parsesFocPriceAndExternalIdentifiers() =
        runBlocking {
            val url = "https://metron.cloud/api/issue/123/"

            val transport = FakeTransport(
                responses = mapOf(
                    url to response(
                        body = """
                            {
                                "id": 123,
                                "series": {
                                    "id": 45,
                                    "name": "Amazing Spider-Man",
                                    "volume": 2,
                                    "year_began": 1998,
                                    "publisher": {
                                        "id": 7,
                                        "name": "Marvel Comics"
                                    }
                                },
                                "number": "1",
                                "issue": "Amazing Spider-Man (1998) #1",
                                "cover_date": "2026-12-01",
                                "store_date": "2026-11-04",
                                "foc_date": "2026-10-12",
                                "price": "4.99",
                                "price_currency": "USD",
                                "sku": "MAR260001",
                                "upc": "75960609558200111",
                                "cv_id": 9999,
                                "gcd_id": 555,
                                "resource_url": "https://metron.cloud/issue/123/",
                                "modified": "2026-10-20T08:00:00Z"
                            }
                        """.trimIndent()
                    )
                )
            )

            val result =
                MetronApiClient(transport).getReleaseDetails(
                    accessToken = "test-token",
                    externalIssueId = "123"
                )

            assertTrue(result is ReleaseProviderResult.Success)

            val release =
                (result as ReleaseProviderResult.Success).value

            assertEquals("2026-10-12", release.focDate)
            assertEquals("4.99", release.price)
            assertEquals("USD", release.priceCurrency)
            assertEquals("MAR260001", release.distributorSku)
            assertEquals("75960609558200111", release.upc)
            assertEquals("9999", release.comicVineId)
            assertEquals("555", release.gcdId)
            assertEquals("Marvel Comics", release.publisherName)
        }

    @Test
    fun request_rateLimitedResponsePreservesRetryAfter() =
        runBlocking {
            val url =
                "https://metron.cloud/api/issue/" +
                        "?series_name=comic-reading-companion-connection-check"

            val transport = FakeTransport(
                responses = mapOf(
                    url to MetronHttpResponse(
                        statusCode = 429,
                        body = """{"detail":"Request was throttled."}""",
                        headers = mapOf("Retry-After" to "30")
                    )
                )
            )

            val result =
                MetronApiClient(transport)
                    .validateConnection("test-token")

            assertEquals(
                ReleaseProviderResult.RateLimited(30L),
                result
            )
        }

    @Test
    fun getScheduledReleases_rejectsReversedDateRange() =
        runBlocking {
            val transport = FakeTransport(emptyMap())

            val result =
                MetronApiClient(transport).getScheduledReleases(
                    accessToken = "test-token",
                    fromDate = LocalDate.parse("2026-11-30"),
                    toDate = LocalDate.parse("2026-11-01")
                )

            assertTrue(result is ReleaseProviderResult.ServiceError)
            assertTrue(transport.requests.isEmpty())
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
            requests += Request(url, accessToken)

            return responses[url]
                ?: throw IOException("No fake response configured for $url")
        }
    }
}