package com.aschlus.comicreadingcompanion.app

import android.content.Context
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.DatabaseSeeder
import com.aschlus.comicreadingcompanion.data.exporter.ReadingListExporter
import com.aschlus.comicreadingcompanion.data.importer.ComicCatalogAssetParser
import com.aschlus.comicreadingcompanion.data.importer.ComicCatalogImporter
import com.aschlus.comicreadingcompanion.data.importer.ReadingListAssetParser
import com.aschlus.comicreadingcompanion.data.importer.ReadingListImporter
import com.aschlus.comicreadingcompanion.data.preferences.ReadingListUiPreferences
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository

class AppContainer(context: Context) {

    private val database = ComicDatabase.getDatabase(context)

    val comicRepository = ComicRepository(
        comicDao = database.comicDao(),
        database = database
    )

    val readingListUiPreferences =
        ReadingListUiPreferences(context)

    val databaseSeeder = DatabaseSeeder(
        comicDao = database.comicDao()
    )

    val readingListAssetParser =
        ReadingListAssetParser(context)

    val readingListImporter = ReadingListImporter(
        comicDao = database.comicDao(),
        database = database
    )

    val readingListExporter = ReadingListExporter(
        comicDao = database.comicDao()
    )

    val comicCatalogAssetParser =
        ComicCatalogAssetParser(context)

    val comicCatalogImporter = ComicCatalogImporter(
        comicDao = database.comicDao(),
        database = database
    )
}