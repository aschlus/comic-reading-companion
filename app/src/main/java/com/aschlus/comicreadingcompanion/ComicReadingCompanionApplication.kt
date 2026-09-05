package com.aschlus.comicreadingcompanion

import android.app.Application
import android.util.Log
import com.aschlus.comicreadingcompanion.app.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ComicReadingCompanionApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        container = AppContainer(this)

        applicationScope.launch {
            container.databaseSeeder.seed()

            val readingListAssets =
                container.readingListAssetParser
                    .listReadingListAssets()

            readingListAssets.forEach { assetPath ->

                try {
                    val importedList =
                        container.readingListAssetParser.parse(
                            assetPath
                        )

                    container.readingListImporter.import(
                        importedList
                    )
                } catch (exception: Exception) {
                    Log.e(
                        "ReadingListImport",
                        "Failed to import reading-list asset " +
                        "'$assetPath': " +
                        "${exception.message}",
                        exception
                    )
                }
            }
        }
    }
}