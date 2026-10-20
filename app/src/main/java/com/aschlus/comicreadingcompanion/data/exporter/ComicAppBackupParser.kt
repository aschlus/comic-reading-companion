package com.aschlus.comicreadingcompanion.data.exporter

import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSource
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListStyle
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ComicAppBackupParser {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun parseJson(
        jsonText: String,
        sourceDescription: String = "comic-app backup JSON"
    ): ComicAppBackupDto {
        val backup = try {
            json.decodeFromString<ComicAppBackupDto>(jsonText)
        } catch (exception: SerializationException) {
            throw IllegalArgumentException(
                "Could not parse $sourceDescription: " +
                    exception.message,
                exception
            )
        }

        validate(backup, sourceDescription)
        return backup
    }

    private fun validate(
        backup: ComicAppBackupDto,
        source: String
    ) {
        checkValid(
            backup.formatVersion ==
                    ComicAppBackupDto.CURRENT_FORMAT_VERSION,
            source,
            "unsupported format version ${backup.formatVersion}"
        )
        checkValid(
            backup.exportedAtEpochMillis > 0,
            source,
            "export timestamp must be positive"
        )

        validateIds(source, "publishers", backup.publishers.map { it.id })
        validateIds(source, "universes", backup.universes.map { it.id })
        validateIds(source, "series", backup.series.map { it.id })
        validateIds(source, "series external IDs", backup.seriesExternalIds.map { it.id })
        validateIds(source, "issues", backup.issues.map { it.id })
        validateIds(source, "issue external IDs", backup.issueExternalIds.map { it.id })
        validateIds(source, "reading lists", backup.readingLists.map { it.id })
        validateIds(source, "reading-list sections", backup.readingListSections.map { it.id })
        validateIds(source, "reading-list items", backup.readingListItems.map { it.id })
        validateIds(source, "reading progress", backup.readingProgress.map { it.id })
        validateIds(source, "issue ownership", backup.readingProgress.map { it.id })

        validateUniqueValues(
            source,
            "publisher names",
            backup.publishers.map { it.name }
        )
        validateUniqueValues(
            source,
            "universe publisher/designation pairs",
            backup.universes
                .filter { it.designation != null }
                .map { it.publisherId to it.designation }
        )
        validateUniqueValues(
            source,
            "series external source/ID pairs",
            backup.seriesExternalIds.map {
                it.source to it.externalId
            }
        )
        validateUniqueValues(
            source,
            "issue external source/ID pairs",
            backup.issueExternalIds.map {
                it.source to it.externalId
            }
        )
        validateUniqueValues(
            source,
            "reading-list source keys",
            backup.readingLists.mapNotNull { it.sourceKey }
        )
        validateUniqueValues(
            source,
            "reading-list section positions",
            backup.readingListSections.map {
                it.readingListId to it.position
            }
        )
        validateUniqueValues(
            source,
            "reading-list item positions",
            backup.readingListItems.map {
                it.readingListId to it.position
            }
        )
        validateUniqueValues(
            source,
            "reading-list item issue references",
            backup.readingListItems.map {
                it.readingListId to it.issueId
            }
        )
        validateUniqueValues(
            source,
            "reading-progress issue references",
            backup.readingProgress.map { it.issueId }
        )
        validateUniqueValues(
            source,
            "ownership issue references",
            backup.issueOwnership.map { it.issueId }
        )

        val publisherIds = backup.publishers.map { it.id }.toSet()
        val universeIds = backup.universes.map { it.id }.toSet()
        val seriesIds = backup.series.map { it.id }.toSet()
        val issueIds = backup.issues.map { it.id }.toSet()
        val readingListIds = backup.readingLists.map { it.id }.toSet()
        val sectionById = backup.readingListSections.associateBy { it.id }

        backup.universes.forEach { universe ->
            requireReference(
                source,
                "universe ${universe.id} publisher",
                universe.publisherId,
                publisherIds
            )
        }

        backup.series.forEach { series ->
            requireReference(
                source,
                "series ${series.id} publisher",
                series.publisherId,
                publisherIds
            )
        }

        backup.seriesExternalIds.forEach { externalId ->
            requireReference(
                source,
                "series external ID ${externalId.id} series",
                externalId.seriesId,
                seriesIds
            )
        }

        backup.issues.forEach { issue ->
            requireReference(
                source,
                "issue ${issue.id} series",
                issue.seriesId,
                seriesIds
            )
            issue.universeId?.let { universeId ->
                requireReference(
                    source,
                    "issue ${issue.id} universe",
                    universeId,
                    universeIds
                )
            }
        }

        backup.issueExternalIds.forEach { externalId ->
            requireReference(
                source,
                "issue external ID ${externalId.id} issue",
                externalId.issueId,
                issueIds
            )
        }

        backup.readingLists.forEach { list ->
            requireReference(
                source,
                "reading list ${list.id} publisher",
                list.publisherId,
                publisherIds
            )
            list.universeId?.let { universeId ->
                requireReference(
                    source,
                    "reading list ${list.id} universe",
                    universeId,
                    universeIds
                )
            }
        }

        backup.readingListSections.forEach { section ->
            requireReference(
                source,
                "section ${section.id} reading list",
                section.readingListId,
                readingListIds
            )
        }

        backup.readingListItems.forEach { item ->
            requireReference(
                source,
                "reading-list item ${item.id} reading list",
                item.readingListId,
                readingListIds
            )
            requireReference(
                source,
                "reading-list item ${item.id} issue",
                item.issueId,
                issueIds
            )

            item.sectionId?.let { sectionId ->
                requireReference(
                    source,
                    "reading-list item ${item.id} section",
                    sectionId,
                    sectionById.keys
                )
                checkValid(
                    sectionById[sectionId]?.readingListId ==
                        item.readingListId,
                    source,
                    "reading-list item ${item.id} references a section " +
                        "belonging to a different reading list"
                )
            }
        }

        backup.readingProgress.forEach { progress ->
            requireReference(
                source,
                "reading progress ${progress.id} issue",
                progress.issueId,
                issueIds
            )
        }

        backup.issueOwnership.forEach { ownership ->
            requireReference(
                source,
                "ownership ${ownership.id} issue",
                ownership.issueId,
                issueIds
            )
        }

        val issueTypes = IssueType.entries.map { it.name }.toSet()
        val listStyles = ReadingListStyle.entries.map { it.name }.toSet()
        val listSources = ReadingListSource.entries.map { it.name }.toSet()
        val readingStatuses = ReadingStatus.entries.map { it.name }.toSet()

        checkValid(
            backup.issues.all { it.issueType in issueTypes },
            source,
            "an issue has an unsupported issue type"
        )
        checkValid(
            backup.readingLists.all { it.style in listStyles },
            source,
            "a reading list has an unsupported style"
        )
        checkValid(
            backup.readingLists.all { it.source in listSources },
            source,
            "a reading list has an unsupported source"
        )
        checkValid(
            backup.readingProgress.all { it.status in readingStatuses },
            source,
            "a reading-progress has an unsupported status"
        )
    }

    private fun validateIds(
        source: String,
        label: String,
        ids: List<Long>
    ) {
        checkValid(
            ids.all { it > 0 },
            source,
            "$label contains a non-positive ID"
        )
        validateUniqueValues(source, "$label IDs", ids)
    }

    private fun <T> validateUniqueValues(
        source: String,
        label: String,
        values: List<T>
    ) {
        val duplicates = values
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys

        checkValid(
            duplicates.isEmpty(),
            source,
            "$label contains duplicates: ${duplicates.joinToString()}"
        )
    }

    private fun requireReference(
        source: String,
        label: String,
        id: Long,
        validIds: Set<Long>
    ) {
        checkValid(
            id in validIds,
            source,
            "$label references unknown ID $id"
        )
    }

    private fun checkValid(
        condition: Boolean,
        source: String,
        message: String
    ) {
        if (!condition) {
            throw IllegalArgumentException(
                "Invalid $source: $message"
            )
        }
    }
}