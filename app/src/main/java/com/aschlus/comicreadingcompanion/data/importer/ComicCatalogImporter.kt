package com.aschlus.comicreadingcompanion.data.importer

import androidx.room3.withWriteTransaction
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.ExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.SeriesExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import com.aschlus.comicreadingcompanion.data.importer.models.CatalogIssueImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.CatalogSeriesImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.ComicCatalogImportDto

class ComicCatalogImporter(
    private val comicDao: ComicDao,
    private val database: ComicDatabase
) {

    suspend fun import(
        importData: ComicCatalogImportDto
    ) {
        database.withWriteTransaction {
            val publisher =
                getOrCreatePublisher(
                    importData.publisher
                )

            val universesByDesignation =
                importUniverses(
                    publisher = publisher,
                    importData = importData
                )

            importData.series.forEach { seriesData ->
                importSeries(
                    publisher = publisher,
                    universesByDesignation = universesByDesignation,
                    seriesData = seriesData
                )
            }
        }
    }

    private suspend fun getOrCreatePublisher(
        publisherName: String
    ): Publisher {
        val existing =
            comicDao.getPublisherByName(publisherName)
        if (existing != null) {
            return existing
        }

        val publisherId =
            comicDao.insertPublisher(
                Publisher(name = publisherName)
            )

        return Publisher(
            id = publisherId,
            name = publisherName
        )
    }

    private suspend fun importUniverses(
        publisher: Publisher,
        importData: ComicCatalogImportDto
    ): Map<String, Universe> {
        val universes = mutableMapOf<String, Universe>()

        importData.universes.forEach { universeData ->
            val existing =
                comicDao.getUniverseByDesignation(
                    publisherId = publisher.id,
                    designation = universeData.designation
                )

            val universe =
                if (existing != null) {
                    existing
                } else {
                    val universeId =
                        comicDao.insertUniverse(
                            Universe(
                                publisherId = publisher.id,
                                name = universeData.name,
                                designation = universeData.designation,
                                description = universeData.description
                            )
                        )

                    Universe(
                        id = universeId,
                        publisherId = publisher.id,
                        name = universeData.name,
                        designation = universeData.designation,
                        description = universeData.description
                    )
                }

            universes[universeData.designation] = universe
        }

        return universes
    }

    private suspend fun importSeries(
        publisher: Publisher,
        universesByDesignation: Map<String, Universe>,
        seriesData: CatalogSeriesImportDto
    ) {
        val matchingExternalIds =
            seriesData.externalIds
                .mapNotNull { externalId ->
                    comicDao.getSeriesExternalId(
                        source = externalId.source,
                        externalId = externalId.externalId
                    )
                }

        val matchingSeriesIds =
            matchingExternalIds.map { externalId ->
                externalId.seriesId
            }.distinct()

        if (matchingSeriesIds.size > 1) {
            throw IllegalArgumentException(
                "Series '${seriesData.title}' " +
                "has external IDs that resolve " +
                "to different series"
            )
        }

        val existingByExternalId =
            matchingSeriesIds.singleOrNull()?.let { seriesId ->
                comicDao.getSeriesById(seriesId)
            }

        val existingByMetadata =
            comicDao.getSeries(
                publisherId = publisher.id,
                title = seriesData.title,
                volume = seriesData.volume
            )

        val series =
            when {
                existingByExternalId != null -> {
                    if (
                        existingByExternalId.publisherId != publisher.id
                    ) {
                        throw IllegalArgumentException(
                            "Series external ID for " +
                            "'${seriesData.title}' " +
                            "belongs to another publisher"
                        )
                    }

                    existingByExternalId
                }

                existingByMetadata != null -> existingByMetadata

                else -> {
                    val seriesId =
                        comicDao.insertSeries(
                            Series(
                                publisherId = publisher.id,
                                title = seriesData.title,
                                volume = seriesData.volume,
                                startYear = seriesData.startYear,
                                endYear = seriesData.endYear
                            )
                        )

                    Series(
                        id = seriesId,
                        publisherId = publisher.id,
                        title = seriesData.title,
                        volume = seriesData.volume,
                        startYear = seriesData.startYear,
                        endYear = seriesData.endYear
                    )
                }
            }

        val updatedSeries =
            series.copy(
                title = seriesData.title,
                volume = seriesData.volume,
                startYear = seriesData.startYear,
                endYear = seriesData.endYear
            )

        if (updatedSeries != series) {
            comicDao.updateSeries(updatedSeries)
        }

        seriesData.externalIds.forEach { externalId ->
            val existing =
                comicDao.getSeriesExternalId(
                    source = externalId.source,
                    externalId = externalId.externalId
                )

            if (existing == null) {
                comicDao.insertSeriesExternalId(
                    SeriesExternalId(
                        seriesId = updatedSeries.id,
                        source = externalId.source,
                        externalId = externalId.externalId,
                        url = externalId.url
                    )
                )
            } else {
                if (existing.seriesId != updatedSeries.id) {
                    throw IllegalArgumentException(
                        "Series external ID " +
                        "'${externalId.source}:" +
                        "${externalId.externalId}' " +
                        "belongs to another series"
                    )
                }

                if (existing.url != externalId.url) {
                    comicDao.updateSeriesExternalId(
                        existing.copy(
                            url = externalId.url
                        )
                    )
                }
            }
        }

        seriesData.issues.forEach { issueData ->
            importIssue(
                series = updatedSeries,
                universesByDesignation = universesByDesignation,
                issueData = issueData
            )
        }
    }

    private suspend fun importIssue(
        series: Series,
        universesByDesignation: Map<String, Universe>,
        issueData: CatalogIssueImportDto
    ) {
        val matchingExternalIds =
            issueData.externalIds.mapNotNull { externalId ->
                comicDao.getExternalId(
                    source = externalId.source,
                    externalId = externalId.externalId
                )
            }

        val matchingIssueIds =
            matchingExternalIds.map { externalId ->
                externalId.issueId
            }.distinct()

        if (matchingIssueIds.size > 1) {
            throw IllegalArgumentException(
                "Issue '${series.title} " +
                "#${issueData.number}' " +
                "has external IDs that resolve " +
                "to different issues"
            )
        }

        val existingByExternalId =
            matchingIssueIds.singleOrNull()?.let { issueId ->
                comicDao.getIssueById(issueId)
            }

        val existingByNumber =
            comicDao.getIssue(
                seriesId = series.id,
                issueNumber = issueData.number
            )

        val universeId =
            issueData.universeDesignation?.let { designation ->
                universesByDesignation[designation]?.id
                    ?: throw IllegalArgumentException(
                        "Unknown universe " +
                        "'$designation' for " +
                        "${series.title} " +
                        "#${issueData.number}"
                    )
            }

        val issueType =
            IssueType.valueOf(issueData.type.uppercase())

        val issue =
            when {
                existingByExternalId != null -> {
                    if (existingByExternalId.seriesId != series.id) {
                        throw IllegalArgumentException(
                            "Issue external ID for " +
                            "${series.title} " +
                            "#${issueData.number} " +
                            "belongs to another series"
                        )
                    }

                    existingByExternalId
                }

                existingByNumber != null -> existingByNumber

                else -> {
                    val issueId =
                        comicDao.insertIssue(
                            Issue(
                                seriesId = series.id,
                                universeId = universeId,
                                issueNumber = issueData.number,
                                title = issueData.title,
                                publicationDate = issueData.publicationDate,
                                coverUrl = issueData.coverUrl,
                                description = issueData.description,
                                issueType = issueType
                            )
                        )

                    Issue(
                        id = issueId,
                        seriesId = series.id,
                        universeId = universeId,
                        issueNumber = issueData.number,
                        title = issueData.title,
                        publicationDate = issueData.publicationDate,
                        coverUrl = issueData.coverUrl,
                        description = issueData.description,
                        issueType = issueType
                    )
                }
            }

        val updatedIssue =
            issue.copy(
                universeId = universeId,
                issueNumber = issueData.number,
                title = issueData.title,
                publicationDate = issueData.publicationDate,
                coverUrl = issueData.coverUrl,
                description = issueData.description,
                issueType = issueType
            )

        if (updatedIssue != issue) {
            comicDao.updateIssue(updatedIssue)
        }

        issueData.externalIds.forEach { externalId ->
            val existing =
                comicDao.getExternalId(
                    source = externalId.source,
                    externalId = externalId.externalId
                )

            if (existing == null) {
                comicDao.insertExternalId(
                    ExternalId(
                        issueId = updatedIssue.id,
                        source = externalId.source,
                        externalId = externalId.externalId,
                        url = externalId.url
                    )
                )
            } else {
                if (existing.issueId != updatedIssue.id) {
                    throw IllegalArgumentException(
                        "Issue external ID " +
                        "'${externalId.source}:" +
                        "${externalId.externalId}' " +
                        "belongs to another issue"
                    )
                }

                if (existing.url != externalId.url) {
                    comicDao.updateExternalId(
                        existing.copy(
                            url = externalId.url
                        )
                    )
                }
            }
        }
    }
}