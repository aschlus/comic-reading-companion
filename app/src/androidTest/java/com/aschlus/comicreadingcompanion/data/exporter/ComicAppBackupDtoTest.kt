package com.aschlus.comicreadingcompanion.data.exporter

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComicAppBackupDtoTest {

    @Test
    fun backupDto_roundTripsEveryRecordCategory() {
        val backup = ComicAppBackupDto(
            exportedAtEpochMillis = 123456789L,
            publishers = listOf(
                BackupPublisherDto(1L, "Marvel Comics")
            ),
            universes = listOf(
                BackupUniverseDto(
                    id = 2L,
                    publisherId = 1L,
                    name = "Marvel Universe",
                    designation = "Earth-616",
                    description = null
                )
            ),
            series = listOf(
                BackupSeriesDto(
                    id = 3L,
                    publisherId = 1L,
                    title = "Amazing Spider-Man",
                    volume = 2,
                    startYear = 1999,
                    endYear = 2003
                )
            ),
            seriesExternalIds = listOf(
                BackupSeriesExternalIdDto(
                    id = 4L,
                    seriesId = 3L,
                    source = "COMIC_VINE",
                    externalId = "12345",
                    url = "https://example.com/series"
                )
            ),
            issues = listOf(
                BackupIssueDto(
                    id = 5L,
                    seriesId = 3L,
                    universeId = 2L,
                    issueNumber = "1",
                    title = "Origin",
                    publicationDate = "1999-01",
                    coverUrl = "https://example.com/cover.jpg",
                    description = "Existing description",
                    issueType = "REGULAR",
                    isIssueTypeManuallyOverridden = true
                )
            ),
            issueExternalIds = listOf(
                BackupIssueExternalIdDto(
                    id = 6L,
                    issueId = 5L,
                    source = "COMIC_VINE",
                    externalId = "67890",
                    url = "https://example.com/issue"
                )
            ),
            readingLists = listOf(
                BackupReadingListDto(
                    id = 7L,
                    title = "My Reading Order",
                    description = "Personal list",
                    publisherId = 1L,
                    universeId = 2L,
                    style = "BLUE",
                    source = "USER",
                    sourceKey = null,
                    createdAt = 100L,
                    updatedAt = 200L
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
                    notes = "Start here"
                )
            ),
            readingProgress = listOf(
                BackupReadingProgressDto(
                    id = 10L,
                    issueId = 5L,
                    status = "READ",
                    startedAt = 300L,
                    completedAt = 400L,
                    notes = "Finished"
                )
            ),
            issueOwnership = listOf(
                BackupIssueOwnershipDto(
                    id = 11L,
                    issueId = 5L
                )
            )
        )

        val json = Json {
            prettyPrint = true
            encodeDefaults = true
        }

        val serialized = json.encodeToString(backup)
        val restored = json.decodeFromString<ComicAppBackupDto>(serialized)

        assertEquals(backup, restored)
        assertEquals(
            ComicAppBackupDto.CURRENT_FORMAT_VERSION,
            restored.formatVersion
        )
    }
}