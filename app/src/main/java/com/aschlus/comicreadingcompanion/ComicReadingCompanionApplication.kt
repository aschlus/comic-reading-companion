package com.aschlus.comicreadingcompanion

import android.app.Application
import com.aschlus.comicreadingcompanion.app.AppContainer

class ComicReadingCompanionApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        container = AppContainer(this)
    }
}