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
class MetronSeriesScopedSyncApiTest {

    @Test
    fun findSeriesByComicVineId_queriesExactSeriesAndReturnsMetronId() =
        runBlocking {

            val requestUrl =
                "https://metron.cloud/api/series/?cv_id=18693"

            val transport =
                FakeTransport(
                    responses = mapOf(
                        requestUrl to response(
                            body = """
                                {
                                    "count": 1,
                                    "next": null,
                                    "previous": null,
                                    "results": [
                                        {
                                            "id": 45,
                                            "series": "Amazing Spider-Man (1999)",
                                            "year_began": 1999,
                                            "year_end": null,
                                            "volume": 2,
                                            "cv_id": 18693,
                                            "gcd_id": 621,
                                            "modified": "2026-10-30T12:00:00Z"
                                        }
                                    ]
                                }
                            """.trimIndent()
                        )
                    )
                )

            val result =
                MetronApiClient(transport)
                    .findSeriesByComicVineId(
                        accessToken = "test-token",
                        comicVineId = "18693"
                    )

            assertTrue(
                result is ReleaseProviderResult.Success
            )

            val series =
                (result as ReleaseProviderResult.Success).value

            assertEquals(1, series.size)

            assertEquals(
                "45",
                series.single().externalSeriesId
            )

            assertEquals(
                "18693",
                series.single().comicVineId
            )

            assertEquals(
                "621",
                series.single().gcdId
            )

            assertEquals(
                "METRON",
                series.single().providerId
            )

            assertEquals(
                "Amazing Spider-Man (1999)",
                series.single().seriesTitle
            )

            assertEquals(
                listOf(requestUrl),
                transport.requests.map { it.url }
            )
        }

    @Test
    fun findSeriesByComicVineId_returnsEmptyListWhenNoMatchExists() =
        runBlocking {

            val requestUrl =
                "https://metron.cloud/api/series/?cv_id=999999"

            val transport =
                FakeTransport(
                    responses = mapOf(
                        requestUrl to response(
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
                    .findSeriesByComicVineId(
                        accessToken = "test-token",
                        comicVineId = "999999"
                    )

            assertTrue(
                result is ReleaseProviderResult.Success
            )

            val series =
                (result as ReleaseProviderResult.Success).value

            assertTrue(series.isEmpty())

            assertEquals(
                listOf(requestUrl),
                transport.requests.map { it.url }
            )
        }

    @Test
    fun findSeriesByName_queriesNameAndReturnsSeriesMetadata() =
        runBlocking {

            val requestUrl =
                "https://metron.cloud/api/series/?name=Amazing+Spider-Man"

            val transport =
                FakeTransport(
                    responses = mapOf(
                        requestUrl to response(
                            body = """
                                {
                                    "count": 1,
                                    "next": null,
                                    "previous": null,
                                    "results": [
                                        {
                                            "id": 45,
                                            "series": "Amazing Spider-Man (1999)",
                                            "year_began": 1999,
                                            "year_end": null,
                                            "volume": 2,
                                            "cv_id": null,
                                            "gcd_id": 621,
                                            "publisher": {
                                                "id": 23,
                                                "name": "Marvel Comics"
                                            },
                                            "resource_url":
                                                "https://metron.cloud/series/45/",
                                            "modified": "2026-10-30T12:00:00Z"
                                        }
                                    ]
                                }
                            """.trimIndent()
                        )
                    )
                )

            val result =
                MetronApiClient(transport)
                    .findSeriesByName(
                        accessToken = "test-token",
                        seriesName = "Amazing Spider-Man"
                    )

            assertTrue(
                result is ReleaseProviderResult.Success
            )

            val series =
                (result as ReleaseProviderResult.Success).value

            assertEquals(1, series.size)

            assertEquals(
                "45",
                series.single().externalSeriesId
            )

            assertEquals(
                "Amazing Spider-Man (1999)",
                series.single().seriesTitle
            )

            assertEquals(
                2,
                series.single().seriesVolume
            )

            assertEquals(
                1999,
                series.single().seriesStartYear
            )

            assertEquals(
                "Marvel Comics",
                series.single().publisherName
            )

            assertEquals(
                listOf(requestUrl),
                transport.requests.map { it.url }
            )
        }

    @Test
    fun getScheduledReleasesForSeries_appliesSeriesAndDateFilters() =
        runBlocking {

            val requestUrl =
                "https://metron.cloud/api/issue/" +
                    "?series_id=45" +
                    "&store_date_range_after=2026-11-01" +
                    "&store_date_range_before=2026-11-30"

            val transport =
                FakeTransport(
                    responses = mapOf(
                        requestUrl to response(
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
                                                "year_began": 1999
                                            },
                                            "number": "30",
                                            "issue": "Amazing Spider-Man #30",
                                            "store_date": "2026-11-04",
                                            "foc_date": "2026-10-12",
                                            "modified": "2026-10-31T04:00:00Z"
                                        }
                                    ]
                                }
                            """.trimIndent()
                        )
                    )
                )

            val result =
                MetronApiClient(transport)
                    .getScheduledReleasesForSeries(
                        accessToken = "test-token",
                        externalSeriesId = "45",
                        fromDate = LocalDate.parse("2026-11-01"),
                        toDate = LocalDate.parse("2026-11-30"),
                        modifiedAfter = null
                    )

            assertTrue(
                result is ReleaseProviderResult.Success
            )

            val releases =
                (result as ReleaseProviderResult.Success).value

            assertEquals(1, releases.size)

            assertEquals(
                "123",
                releases.single().externalIssueId
            )

            assertEquals(
                "45",
                releases.single().externalSeriesId
            )

            assertEquals(
                "Amazing Spider-Man",
                releases.single().seriesTitle
            )

            assertEquals(
                "2026-11-04",
                releases.single().storeReleaseDate
            )

            assertEquals(
                listOf(requestUrl),
                transport.requests.map { it.url }
            )
        }

    @Test
    fun getScheduledReleasesForSeries_appliesSeriesAndModifiedFilters() =
        runBlocking {

            val modifiedAfter =
                Instant.parse("2026-10-31T03:00:00Z")

            val requestUrl =
                "https://metron.cloud/api/issue/" +
                    "?series_id=45" +
                    "&modified_gt=2026-10-31T03%3A00%3A00Z"

            val transport =
                FakeTransport(
                    responses = mapOf(
                        requestUrl to response(
                            body = """
                                {
                                    "count": 1,
                                    "next": null,
                                    "previous": null,
                                    "results": [
                                        {
                                            "id": 124,
                                            "series": {
                                                "id": 45,
                                                "name": "Amazing Spider-Man",
                                                "volume": 2,
                                                "year_began": 1999
                                            },
                                            "number": "31",
                                            "issue": "Amazing Spider-Man #31",
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
                    .getScheduledReleasesForSeries(
                        accessToken = "test-token",
                        externalSeriesId = "45",
                        fromDate = LocalDate.parse("2026-11-01"),
                        toDate = LocalDate.parse("2026-11-30"),
                        modifiedAfter = modifiedAfter
                    )

            assertTrue(
                result is ReleaseProviderResult.Success
            )

            val releases =
                (result as ReleaseProviderResult.Success).value

            assertEquals(1, releases.size)

            assertEquals(
                "124",
                releases.single().externalIssueId
            )

            assertEquals(
                "45",
                releases.single().externalSeriesId
            )

            assertEquals(
                "2026-11-11",
                releases.single().storeReleaseDate
            )

            assertEquals(
                listOf(requestUrl),
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