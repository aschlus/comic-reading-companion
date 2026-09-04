package com.aschlus.comicreadingcompanion

import android.app.Application
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

            val importedList =
                container.readingListAssetParser.parse(
                    "reading_lists/spider_man_test.json"
                )

            container.readingListImporter.import(
                importedList
            )
        }
    }
}