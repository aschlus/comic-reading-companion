package com.aschlus.comicreadingcompanion.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.homeUiDataStore:
        DataStore<Preferences> by preferencesDataStore(
            name = "home_ui_preferences"
        )

class HomeUiPreferences(
    context: Context
) {

    private val dataStore =
        context.applicationContext.homeUiDataStore

    val recentlyOpenedReadingListIds:
        Flow<List<Long>> =
        dataStore.data.map { preferences ->
            preferences[recentlyOpenedReadingListIdsKey]
                .orEmpty()
                .split(",")
                .mapNotNull { value ->
                    value
                        .takeIf { it.isNotBlank() }
                        ?.toLongOrNull()
                }
        }

    suspend fun recordReadingListOpened(
        readingListId: Long
    ) {
        dataStore.edit { preferences ->
            val existingIds =
                preferences[recentlyOpenedReadingListIdsKey]
                    .orEmpty()
                    .split(",")
                    .mapNotNull { value ->
                        value
                            .takeIf { it.isNotBlank() }
                            ?.toLongOrNull()
                    }

            val updatedIds =
                buildList {
                    add(readingListId)

                    addAll(
                        existingIds.filterNot {
                            it == readingListId
                        }
                    )
                }
                    .take(MAX_RECENT_READING_LISTS)

            preferences[recentlyOpenedReadingListIdsKey] = updatedIds.joinToString(",")
        }
    }

    suspend fun clearRecentlyOpenedReadingLists() {
        dataStore.edit { preferences ->
            preferences.remove(
                recentlyOpenedReadingListIdsKey
            )
        }
    }

    private companion object {
        const val MAX_RECENT_READING_LISTS = 5

        val recentlyOpenedReadingListIdsKey =
            stringPreferencesKey(
                "recently_opened_reading_list_ids"
            )
    }
}