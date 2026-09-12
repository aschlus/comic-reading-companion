package com.aschlus.comicreadingcompanion.data.exporter

import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import com.aschlus.comicreadingcompanion.data.importer.models.ExternalIdImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.IssueImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.ReadingListImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.ReadingListItemImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.ReadingListSectionImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.SeriesImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.UniverseImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.UniverseOverrideImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.UniverseOverrideMode
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ReadingListExporter (
    private val comicDao: ComicDao
) {

    private val json = Json { prettyPrint = true }

    suspend fun exportJson(
        readingListId: Long
    ): String {
        val exportData = export(readingListId = readingListId)

        return json.encodeToString(exportData)
    }

    suspend fun export(
        readingListId: Long
    ) : ReadingListImportDto {
        val readingList =
            comicDao.getReadingListById(readingListId)
                ?: throw IllegalArgumentException(
                    "Reading list $readingListId does not exist"
                )

        val publisher =
            comicDao.getPublisherById(
                readingList.publisherId
            )
                .first()
                ?: throw IllegalStateException(
                    "Publisher ${readingList.publisherId} does not exist"
                )

        val universeById =
            comicDao.getUniversesForPublisher(publisher.id)
                .associateBy { universe -> universe.id }

        val defaultUniverse =
            readingList.universeId?.let { universeId ->
                universeById[universeId]
                    ?: throw IllegalStateException(
                        "Universe $universeId does not exist for publisher '${publisher.name}'"
                    )
            }

        val sections =
            comicDao.getSectionsForReadingList(readingListId)

        val sectionPositionsById =
            sections.associate { section ->
                section.id to section.position
            }

        val items =
            comicDao.getItemsForReadingList(readingListId)

        val exportedItems =
            items.map { item ->
                val issue =
                    comicDao.getIssueById(item.issueId)
                        ?: throw IllegalStateException(
                            "Issue ${item.issueId} does not exist"
                        )

                val series =
                    comicDao.getSeriesById(issue.seriesId)
                        ?: throw IllegalStateException(
                            "Series ${issue.seriesId} does not exist"
                        )

                if (series.publisherId != readingList.publisherId) {
                    throw IllegalStateException(
                        "Issue ${issue.id} belongs to a different publisher"
                    )
                }

                val issueUniverse =
                    issue.universeId?.let { universeId ->
                        universeById[universeId]
                            ?: throw IllegalStateException(
                                "Universe $universeId does not exist " +
                                "for publisher ${publisher.name}"
                            )
                    }

                val universeOverride =
                    when {
                        issue.universeId == readingList.universeId -> {
                            null
                        }

                        issueUniverse == null -> {
                            UniverseOverrideImportDto(
                                mode = UniverseOverrideMode.NONE
                            )
                        }

                        else -> {
                            UniverseOverrideImportDto(
                                mode = UniverseOverrideMode.UNIVERSE,
                                universe = issueUniverse.toImportDto()
                            )
                        }
                    }

                val externalIds =
                    comicDao
                        .getExternalIdsForIssue(issue.id)
                        .map { externalId ->
                            ExternalIdImportDto(
                                source = externalId.source,
                                externalId = externalId.externalId,
                                url = externalId.url
                            )
                        }

                val sectionPosition =
                    item.sectionId?.let { sectionId ->
                        sectionPositionsById[sectionId]
                            ?: throw IllegalStateException(
                                "Section $sectionId does not exist in reading list $readingListId"
                            )
                    }

                ReadingListItemImportDto(
                    position = item.position,
                    sectionPosition = sectionPosition,
                    series = SeriesImportDto(
                        title = series.title,
                        volume = series.volume,
                        startYear = series.startYear,
                        endYear = series.endYear
                    ),
                    issue = IssueImportDto(
                        number = issue.issueNumber,
                        title = issue.title,
                        publicationDate = issue.publicationDate,
                        coverUrl = issue.coverUrl,
                        description = issue.description,
                        type = issue.issueType.name,
                        externalIds = externalIds
                    ),
                    universeOverride = universeOverride,
                    required = item.required,
                    notes = item.notes
                )
            }

        return ReadingListImportDto(
            title = readingList.title,
            description = readingList.description,
            publisher = publisher.name,
            universe = defaultUniverse?.toImportDto(),
            sections = sections.map { section ->
                ReadingListSectionImportDto(
                    position = section.position,
                    title = section.title,
                    description = section.description
                )
            },
            items = exportedItems
        )
    }

    private fun Universe.toImportDto():
            UniverseImportDto {
        val universeDesignation =
            designation
                ?: throw IllegalStateException(
                    "Universe '$name' cannot be exported because it has no designation"
                )

        return UniverseImportDto(
            name = name,
            designation = universeDesignation
        )
    }
}