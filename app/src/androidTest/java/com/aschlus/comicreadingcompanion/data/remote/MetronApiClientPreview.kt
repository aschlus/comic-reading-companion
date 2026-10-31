
package com.aschlus.comicreadingcompanion.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MetronApiClientPreviewTest {

    @Test
    fun previewUsesServerTotalCountAndMakesOnlyOneRequest() =
        runBlocking {
            val transport = QueueTransport(
                responses = listOf(
                    MetronHttpResponse(
                        statusCode = 200,
                        body = issuePageJson(
                            count = 153,
                            next =
                                "https://metron.cloud/api/issue/?page=2",
                            issueIds = listOf(101L, 102L)
                        )
                    )
                )
            )

            val result = MetronApiClient(transport)
                .getScheduledReleasePreview(
                    accessToken = "test-token",
                    fromDate = LocalDate.parse("2026-10-29"),
                    toDate = LocalDate.parse("2026-11-12")
                )

            val preview = when (result) {
                is ReleaseProviderResult.Success -> result.value
                else -> throw AssertionError(
                    "Expected a successful preview, got $result"
                )
            }

            assertEquals(153, preview.totalCount)

            assertEquals(
                listOf("101", "102"),
                preview.releases.map { it.externalIssueId }
            )

            assertEquals(
                "Exactly one API request should be made for a preview.",
                1,
                transport.requestedUrls.size
            )

            assertEquals(
                "test-token",
                transport.requestedTokens.single()
            )

            val requestedUrl = transport.requestedUrls.single()

            assertTrue(
                requestedUrl.contains(
                    "store_date_range_after=2026-10-29"
                )
            )

            assertTrue(
                requestedUrl.contains(
                    "store_date_range_before=2026-11-12"
                )
            )
        }

    @Test
    fun fullReleaseDownloadStillFollowsAllPages() =
        runBlocking {
            val secondPageUrl =
                "https://metron.cloud/api/issue/?page=2"

            val transport = QueueTransport(
                responses = listOf(
                    MetronHttpResponse(
                        statusCode = 200,
                        body = issuePageJson(
                            count = 2,
                            next = secondPageUrl,
                            issueIds = listOf(101L)
                        )
                    ),
                    MetronHttpResponse(
                        statusCode = 200,
                        body = issuePageJson(
                            count = 2,
                            next = null,
                            issueIds = listOf(102L)
                        )
                    )
                )
            )

            val result = MetronApiClient(transport)
                .getScheduledReleases(
                    accessToken = "test-token",
                    fromDate = LocalDate.parse("2026-10-29"),
                    toDate = LocalDate.parse("2026-11-12")
                )

            val releases = when (result) {
                is ReleaseProviderResult.Success -> result.value
                else -> throw AssertionError(
                    "Expected a successful full download, got $result"
                )
            }

            assertEquals(
                listOf("101", "102"),
                releases.map { it.externalIssueId }
            )

            assertEquals(
                "The full download must retrieve both pages.",
                2,
                transport.requestedUrls.size
            )

            assertEquals(
                secondPageUrl,
                transport.requestedUrls[1]
            )
        }

    @Test
    fun previewWithInvalidDateRangeReturnsErrorWithoutRequest() =
        runBlocking {
            val transport = QueueTransport(
                responses = emptyList()
            )

            val result = MetronApiClient(transport)
                .getScheduledReleasePreview(
                    accessToken = "test-token",
                    fromDate = LocalDate.parse("2026-11-12"),
                    toDate = LocalDate.parse("2026-10-29")
                )

            assertTrue(
                result is ReleaseProviderResult.ServiceError
            )

            assertEquals(
                0,
                transport.requestedUrls.size
            )
        }

    @Test
    fun previewWithMalformedResponseReturnsServiceError() =
        runBlocking {
            val transport = QueueTransport(
                responses = listOf(
                    MetronHttpResponse(
                        statusCode = 200,
                        body = "not-json"
                    )
                )
            )

            val result = MetronApiClient(transport)
                .getScheduledReleasePreview(
                    accessToken = "test-token",
                    fromDate = LocalDate.parse("2026-10-29"),
                    toDate = LocalDate.parse("2026-11-12")
                )

            assertTrue(
                result is ReleaseProviderResult.ServiceError
            )

            assertEquals(
                "A malformed first page should not trigger another request.",
                1,
                transport.requestedUrls.size
            )
        }

    private class QueueTransport(
        responses: List<MetronHttpResponse>
    ) : MetronHttpTransport {

        private val queuedResponses =
            responses.toMutableList()

        val requestedUrls = mutableListOf<String>()

        val requestedTokens = mutableListOf<String>()

        override suspend fun get(
            url: String,
            accessToken: String
        ): MetronHttpResponse {
            requestedUrls += url
            requestedTokens += accessToken

            if (queuedResponses.isEmpty()) {
                throw AssertionError(
                    "MetronApiClient made an unexpected extra request: $url"
                )
            }

            return queuedResponses.removeAt(0)
        }
    }

    private fun issuePageJson(
        count: Int,
        next: String?,
        issueIds: List<Long>
    ): String {
        val nextJson = next?.let { "\"$it\"" } ?: "null"

        val resultsJson = issueIds.joinToString(",") { id ->
            issueJson(id)
        }

        return """
            {
                "count": $count,
                "next": $nextJson,
                "previous": null,
                "results": [
                    $resultsJson
                ]
            }
        """.trimIndent()
    }

    private fun issueJson(id: Long): String =
        """
            {
                "id": $id,
                "series": {
                    "id": 50,
                    "name": "Amazing Spider-Man",
                    "volume": 1,
                    "year_began": 2025,
                    "publisher": {
                        "id": 1,
                        "name": "Marvel Comics"
                    }
                },
                "number": "$id",
                "issue": "Amazing Spider-Man #$id",
                "cover_date": "2026-11-04",
                "store_date": "2026-10-29"
            }
        """.trimIndent()
}