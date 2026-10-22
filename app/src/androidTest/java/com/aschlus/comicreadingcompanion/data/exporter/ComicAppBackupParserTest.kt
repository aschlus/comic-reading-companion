
package com.aschlus.comicreadingcompanion.data.exporter

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComicAppBackupParserTest {

    private val parser = ComicAppBackupParser()

    private val json = Json {
        encodeDefaults = true
    }

    @Test
    fun parseValidBackup_returnsValidatedBackup() {
        val backup = validBackup()

        val result = parser.parseJson(
            jsonText = json.encodeToString(backup)
        )

        assertEquals(backup, result)
    }

    @Test
    fun parseMalformedJson_reportsParsingError() {
        assertInvalidBackupContaining("Could not parse test backup") {
            parser.parseJson(
                jsonText = """{"formatVersion":""",
                sourceDescription = "test backup"
            )
        }
    }

    @Test
    fun parseUnsupportedVersion_rejectsBackup() {
        val backup = validBackup().copy(formatVersion = 2)

        assertInvalidBackupContaining("unsupported format version 2") {
            parser.parseJson(json.encodeToString(backup))
        }
    }

    @Test
    fun parseUnknownPublisherReference_rejectsBackup() {
        val backup = validBackup().let { original ->
            original.copy(
                series = original.series.map { series ->
                    series.copy(publisherId = 999L)
                }
            )
        }

        assertInvalidBackupContaining("references unknown ID 999") {
            parser.parseJson(json.encodeToString(backup))
        }
    }

    @Test
    fun parseUnsupportedIssueType_rejectsBackup() {
        val backup = validBackup().copy(
            issues = validBackup().issues.map { issue ->
                issue.copy(issueType = "NOT_A_REAL_TYPE")
            }
        )

        assertInvalidBackupContaining("unsupported issue type") {
            parser.parseJson(json.encodeToString(backup))
        }
    }

    @Test
    fun parseDuplicateExternalIds_rejectsBackup() {
        val backup = validBackup().let { original ->
            original.copy(
                seriesExternalIds = original.seriesExternalIds + listOf(
                    original.seriesExternalIds.first().copy(id = 12L)
                )
            )
        }

        assertInvalidBackupContaining(
            "series external source/ID pairs contains duplicates"
        ) {
            parser.parseJson(json.encodeToString(backup))
        }
    }


    @Test
    fun parseSeriesUniverseFromDifferentPublisher_rejectsBackup() {
        val original = validBackup()

        val backup =
            original.copy(
                publishers = original.publishers +
                        BackupPublisherDto(
                            12L,
                            "Other Publisher"
                        ),
                series = original.series.map { series ->
                    series.copy(
                        publisherId = 12L
                    )
                }
            )

        assertInvalidBackupContaining(
            "belongs to a different publisher"
        ) {
            parser.parseJson(
                json.encodeToString(backup)
            )
        }
    }


    private fun validBackup() = ComicAppBackupDto(
        exportedAtEpochMillis = 1L,
        publishers = listOf(
            BackupPublisherDto(1L, "Test Publisher")
        ),
        universes = listOf(
            BackupUniverseDto(
                id = 2L,
                publisherId = 1L,
                name = "Test Universe",
                designation = "Earth-Test",
                description = null
            )
        ),
        series = listOf(
            BackupSeriesDto(
                id = 3L,
                publisherId = 1L,
                title = "Test Series",
                volume = 1,
                startYear = 2000,
                endYear = 2001,
                universeId = 2L
            )
        ),
        seriesExternalIds = listOf(
            BackupSeriesExternalIdDto(
                id = 4L,
                seriesId = 3L,
                source = "COMIC_VINE",
                externalId = "series-101",
                url = null
            )
        ),
        issues = listOf(
            BackupIssueDto(
                id = 5L,
                seriesId = 3L,
                universeId = 2L,
                issueNumber = "1",
                title = "First Issue",
                publicationDate = null,
                coverUrl = null,
                description = null,
                issueType = "REGULAR"
            )
        ),
        issueExternalIds = listOf(
            BackupIssueExternalIdDto(
                id = 6L,
                issueId = 5L,
                source = "COMIC_VINE",
                externalId = "issue-201",
                url = null
            )
        ),
        readingLists = listOf(
            BackupReadingListDto(
                id = 7L,
                title = "Test Reading List",
                description = null,
                publisherId = 1L,
                universeId = 2L,
                style = "BLUE",
                source = "USER",
                sourceKey = null,
                createdAt = 10L,
                updatedAt = 20L
            )
        ),
        readingListSections = listOf(
            BackupReadingListSectionDto(
                id = 8L,
                readingListId = 7L,
                title = "Beginning",
                description = null,
                position = 1
            )
        ),
        readingListItems = listOf(
            BackupReadingListItemDto(
                id = 9L,
                readingListId = 7L,
                sectionId = 8L,
                issueId = 5L,
                position = 1,
                required = true,
                notes = null
            )
        ),
        readingProgress = listOf(
            BackupReadingProgressDto(
                id = 10L,
                issueId = 5L,
                status = "READ",
                startedAt = 10L,
                completedAt = 20L,
                notes = null
            )
        ),
        issueOwnership = listOf(
            BackupIssueOwnershipDto(
                id = 11L,
                issueId = 5L
            )
        )
    )

    private inline fun assertInvalidBackupContaining(
        expectedMessage: String,
        block: () -> Unit
    ) {
        try {
            block()
            throw AssertionError(
                "Expected invalid backup containing '$expectedMessage'"
            )
        } catch (exception: IllegalArgumentException) {
            assertTrue(
                "Unexpected error: ${exception.message}",
                exception.message?.contains(expectedMessage) == true
            )
        }
    }
}