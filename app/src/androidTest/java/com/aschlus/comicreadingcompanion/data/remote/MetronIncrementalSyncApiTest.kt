package com.aschlus.comicreadingcompanion.data.remote

import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class MetronIncrementalSyncApiTest {

    @Test
    fun getScheduledReleases_appliesModifiedFilterAndFollowsPagination() =
        runBlocking {

            val modifiedAfter =
                Instant.parse("2026-10-31T03:00:00Z")

            val firstUrl =
                "https://metron.cloud/api/issue/" +
                    "?modified_gt=2026-10-31T03%3A00%3A00Z"

            val secondUrl =
                "https://metron.cloud/api/issue/?page=2"

            val transport =
                FakeTransport(
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
                                                "name": "Amazing Spider-Man",
                                                "volume": 2,
                                                "year_began": 1998
                                            },
                                            "number": "1",
                                            "issue": "Amazing Spider-Man #1",
                                            "store_date": "2026-11-04",
                                            "modified": "2026-10-31T04:00:00Z"
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
                                                "id": 45,
                                                "name": "Amazing Spider-Man",
                                                "volume": 2,
                                                "year_began": 1998
                                            },
                                            "number": "2",
                                            "issue": "Amazing Spider-Man #2",
                                            "store_date": "2026-11-11",
                                            "modified": "2026-10-31T04:30:00Z"
                                        }
                                    ]
                                }
                            """.trimIndent()
                        )
                    )
                )

            val result =
                MetronApiClient(transport)
                    .getScheduledReleases(
                        accessToken = "test-token",
                        fromDate = LocalDate.parse("2026-11-01"),
                        toDate = LocalDate.parse("2026-11-30"),
                        modifiedAfter = modifiedAfter
                    )

            assertTrue(
                result is ReleaseProviderResult.Success
            )

            val releases =
                (result as ReleaseProviderResult.Success).value

            assertEquals(2, releases.size)

            assertEquals(
                "123",
                releases[0].externalIssueId
            )

            assertEquals(
                "124",
                releases[1].externalIssueId
            )

            assertEquals(
                "2026-11-04",
                releases[0].storeReleaseDate
            )

            assertEquals(
                "2026-11-11",
                releases[1].storeReleaseDate
            )

            assertEquals(
                listOf(firstUrl, secondUrl),
                transport.requests.map { it.url }
            )

            assertTrue(
                transport.requests.all {
                    it.accessToken == "test-token"
                }
            )

            assertEquals(
                2,
                transport.requests.size
            )
        }

    @Test
    fun getScheduledReleases_stopsWhenPageLimitIsExceeded() =
        runBlocking {

            val firstUrl =
                "https://metron.cloud/api/issue/" +
                    "?store_date_range_after=2026-11-01" +
                    "&store_date_range_before=2026-11-30"

            val pageUrls =
                (1..51).map { pageNumber ->
                    if (pageNumber == 1) {
                        firstUrl
                    } else {
                        "https://metron.cloud/api/issue/?page=$pageNumber"
                    }
                }

            val responses =
                pageUrls
                    .take(50)
                    .mapIndexed { index, url ->
                        val nextUrl = pageUrls[index + 1]

                        url to response(
                            body = """
                                {
                                    "count": 51,
                                    "next": "$nextUrl",
                                    "previous": null,
                                    "results": []
                                }
                            """.trimIndent()
                        )
                    }
                    .toMap()

            val transport =
                FakeTransport(responses)

            val result =
                MetronApiClient(transport)
                    .getScheduledReleases(
                        accessToken = "test-token",
                        fromDate = LocalDate.parse("2026-11-01"),
                        toDate = LocalDate.parse("2026-11-30")
                    )

            assertTrue(
                result is ReleaseProviderResult.ServiceError
            )

            val serviceError =
                result as ReleaseProviderResult.ServiceError

            assertEquals(
                "Metron returned more than 50 issue-list pages. " +
                    "Sync was stopped to prevent an unbounded request loop.",
                serviceError.message
            )

            assertEquals(
                50,
                transport.requests.size
            )

            assertEquals(
                pageUrls.take(50),
                transport.requests.map { it.url }
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