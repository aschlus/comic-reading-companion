package com.aschlus.comicreadingcompanion.data.importer.models

import kotlinx.serialization.Serializable

@Serializable
data class ReadingListImportDto(
    val title: String,
    val description: String?,
    val publisher: String,
    val universe: UniverseImportDto,
    val sections: List<ReadingListSectionImportDto> =
        emptyList(),
    val items: List<ReadingListItemImportDto>
)

@Serializable
data class ReadingListSectionImportDto(
    val position: Int,
    val title: String,
    val description: String?
)

@Serializable
data class UniverseImportDto(
    val name: String,
    val designation: String
)

@Serializable
data class ReadingListItemImportDto(
    val position: Int,
    val sectionPosition: Int? = null,
    val series: SeriesImportDto,
    val issue: IssueImportDto,
    val required: Boolean,
    val notes: String?
)

@Serializable
data class SeriesImportDto(
    val title: String,
    val volume: Int?,
    val startYear: Int?,
    val endYear: Int?
)

@Serializable
data class IssueImportDto(
    val number: String,
    val title: String?,
    val publicationDate: String?,
    val coverUrl: String?,
    val description: String?,
    val type: String,
    val externalIds: List<ExternalIdImportDto> =
        emptyList()
)

@Serializable
data class ExternalIdImportDto(
    val source: String,
    val externalId: String,
    val url: String?
)