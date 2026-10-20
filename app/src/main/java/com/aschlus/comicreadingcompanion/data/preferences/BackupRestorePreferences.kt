package com.aschlus.comicreadingcompanion.data.preferences

import android.content.Context
import kotlinx.coroutines.sync.Mutex

class BackupRestorePreferences(
    context: Context
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            "comic_app_backup_restore",
            Context.MODE_PRIVATE
        )

    private val initializationMutex = Mutex()

    fun hasCompletedRestore(): Boolean {
        return preferences.getBoolean(
            "has_completed_restore",
            false
        )
    }

    fun markRestoreCompleted() {
        preferences.edit()
            .putBoolean("has_completed_restore", true)
            .apply()
    }

    suspend fun <T> withInitializationLock(
        block: suspend () -> T
    ): T {
        initializationMutex.lock()

        return try {
            block()
        } finally {
            initializationMutex.unlock()
        }
    }
}