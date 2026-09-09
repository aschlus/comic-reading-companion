package com.aschlus.comicreadingcompanion.data.importer

import android.content.Context
import com.aschlus.comicreadingcompanion.data.importer.models.ComicCatalogImportDto
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ComicCatalogAssetParser(
    private val context: Context
) {

    private val json = Json {ignoreUnknownKeys = true}

    fun parse(
        assetPath: String
    ): ComicCatalogImportDto {
        val jsonText =
            try {
                context.assets
                    .open(assetPath)
                    .bufferedReader()
                    .use { reader ->
                        reader.readText()
                    }
            } catch (
                exception: IOException
            ) {
                throw IllegalArgumentException(
                    "Could not read comic-catalog asset " +
                    "'$assetPath': " +
                    "${exception.message}",
                    exception
                )
            }

        return try {
            json.decodeFromString<
                    ComicCatalogImportDto
            >(
                jsonText
            )
        } catch (
            exception: SerializationException
        ) {
            throw IllegalArgumentException(
                "Could not parse comic-catalog asset " +
                "'$assetPath': " +
                "${exception.message}",
                exception
            )
        }
    }

    fun listCatalogAssets(): List<String> {
        return context.assets
            .list("catalogs")
            ?.filter { fileName ->
                fileName.endsWith(
                    ".json",
                    ignoreCase = true
                )
            }
            ?.sorted()
            ?.map { filename ->
                "catalogs/$filename"
            }
            ?: emptyList()
    }
}