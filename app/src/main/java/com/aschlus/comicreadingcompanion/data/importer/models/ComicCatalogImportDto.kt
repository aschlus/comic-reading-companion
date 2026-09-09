package com.aschlus.comicreadingcompanion.data.importer.models

import kotlinx.serialization.Serializable

@Serializable
data class ComicCatalogImportDto(
    val publisher: String,
    val universes: List<CatalogUniverseImportDto> = emptyList(),
    val series: List<CatalogSeriesImportDto>
)

@Serializable
data class CatalogUniverseImportDto(
    val name: String,
    val designation: String,
    val description: String? = null
)

@Serializable
data class CatalogSeriesImportDto(
    val title: String,
    val volume: Int?,
    val startYear: Int?,
    val endYear: Int?,
    val externalIds: List<CatalogExternalIdImportDto> = emptyList(),
    val issues: List<CatalogIssueImportDto>
)

@Serializable
data class CatalogIssueImportDto(
    val number: String,
    val title: String?,
    val publicationDate: String?,
    val coverUrl: String?,
    val description: String?,
    val type: String,
    val universeDesignation: String? = null,
    val externalIds: List<CatalogExternalIdImportDto> = emptyList()
)

@Serializable
data class CatalogExternalIdImportDto(
    val source: String,
    val externalId: String,
    val url: String?
)

