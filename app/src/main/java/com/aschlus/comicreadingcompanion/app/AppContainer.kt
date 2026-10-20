package com.aschlus.comicreadingcompanion.app

import android.content.Context
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.DatabaseSeeder
import com.aschlus.comicreadingcompanion.data.exporter.ComicAppBackupExporter
import com.aschlus.comicreadingcompanion.data.exporter.ComicAppBackupParser
import com.aschlus.comicreadingcompanion.data.exporter.ComicAppBackupRestorer
import com.aschlus.comicreadingcompanion.data.exporter.ReadingListExporter
import com.aschlus.comicreadingcompanion.data.importer.ComicCatalogAssetParser
import com.aschlus.comicreadingcompanion.data.importer.ComicCatalogImporter
import com.aschlus.comicreadingcompanion.data.importer.ReadingListAssetParser
import com.aschlus.comicreadingcompanion.data.importer.ReadingListImporter
import com.aschlus.comicreadingcompanion.data.preferences.BackupRestorePreferences
import com.aschlus.comicreadingcompanion.data.preferences.ComicVineApiKeyStore
import com.aschlus.comicreadingcompanion.data.preferences.HomeUiPreferences
import com.aschlus.comicreadingcompanion.data.preferences.ReadingListUiPreferences
import com.aschlus.comicreadingcompanion.data.remote.ComicVineApiClient
import com.aschlus.comicreadingcompanion.data.repository.ComicRepository

class AppContainer(context: Context) {

    private val database = ComicDatabase.getDatabase(context)

    val comicRepository = ComicRepository(
        comicDao = database.comicDao(),
        database = database
    )

    val readingListUiPreferences =
        ReadingListUiPreferences(context)

    val homeUiPreferences =
        HomeUiPreferences(context)

    val backupRestorePreferences =
        BackupRestorePreferences(context)

    val comicVineApiKeyStore =
        ComicVineApiKeyStore(context)

    val comicVineApiClient =
        ComicVineApiClient()

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

    val comicAppBackupExporter = ComicAppBackupExporter(
        comicDao = database.comicDao(),
        database = database
    )

    val comicAppBackupParser = ComicAppBackupParser()

    val comicAppBackupRestorer = ComicAppBackupRestorer(
        comicDao = database.comicDao(),
        database = database,
        parser = comicAppBackupParser,
        backupRestorePreferences = backupRestorePreferences
    )

    val comicCatalogAssetParser =
        ComicCatalogAssetParser(context)

    val comicCatalogImporter = ComicCatalogImporter(
        comicDao = database.comicDao(),
        database = database
    )
}