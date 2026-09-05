package com.aschlus.comicreadingcompanion.data.importer

import android.content.Context
import com.aschlus.comicreadingcompanion.data.importer.models.ReadingListImportDto
import com.aschlus.comicreadingcompanion.data.importer.models.ReadingListItemImportDto
import kotlinx.serialization.json.Json

class ReadingListAssetParser(
    private val context: Context
) {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun parse(assetPath: String): ReadingListImportDto {
        val jsonText =
            context.assets
                .open(assetPath)
                .bufferedReader()
                .use { reader ->
                    reader.readText()
                }

        return json.decodeFromString<ReadingListImportDto>(
            jsonText
        )
    }

    fun listReadingListAssets(): List<String> {
        return context.assets
            .list("reading_lists")
            ?.filter { fileName ->
                fileName.endsWith(
                    ".json",
                    ignoreCase = true
                )
            }
            ?.sorted()
            ?.map { fileName ->
                "reading_lists/$fileName"
            }
            ?: emptyList()
    }
}