package com.aschlus.comicreadingcompanion.data.importer

import android.content.Context
import com.aschlus.comicreadingcompanion.data.importer.models.ReadingListImportDto
import java.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException

class ReadingListAssetParser(
    private val context: Context
) {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun parse(assetPath: String): ReadingListImportDto {
        val jsonText =
            try {
                context.assets
                    .open(assetPath)
                    .bufferedReader()
                    .use { reader ->
                        reader.readText()
                    }
            } catch (exception: IOException) {
                throw IllegalArgumentException(
                    "Could not read reading-list asset " +
                    "'$assetPath': " +
                    "${exception.message}",
                    exception
                )
            }

        return parseJson(
            jsonText = jsonText,
            sourceDescription =
                "reading-list asset '$assetPath'"
        )
    }

    fun parseJson(
        jsonText: String,
        sourceDescription: String = "reading-list JSON"
    ): ReadingListImportDto {
        return try {
            json.decodeFromString<ReadingListImportDto>(jsonText)
        } catch (
            exception: SerializationException
        ) {
            throw IllegalArgumentException(
                "Could not parse " +
                "$sourceDescription: " +
                "${exception.message}",
                exception
            )
        }
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