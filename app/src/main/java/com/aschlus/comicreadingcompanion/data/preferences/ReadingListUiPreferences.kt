package com.aschlus.comicreadingcompanion.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.readingListUiDataStore:
        DataStore<Preferences> by preferencesDataStore(
            name = "reading_list_ui_preferences"
        )

class ReadingListUiPreferences(
    context: Context
) {

    private val dataStore =
        context.applicationContext.readingListUiDataStore

    fun getCollapsedSectionIds(
        readingListId: Long
    ): Flow<Set<Long>> {
        val key =
            collapsedSectionsKey(readingListId)

        return dataStore.data.map { preferences ->
            preferences[key]
                .orEmpty()
                .mapNotNull { value ->
                    value.toLongOrNull()
                }
                .toSet()
        }
    }

    suspend fun setSectionCollapsed(
        readingListId: Long,
        sectionId: Long,
        collapsed: Boolean
    ) {
        val key =
            collapsedSectionsKey(readingListId)

        dataStore.edit { preferences ->
            val collapsedIds =
                preferences[key]
                    .orEmpty()
                    .toMutableSet()

            if (collapsed) {
                collapsedIds.add(
                    sectionId.toString()
                )
            } else {
                collapsedIds.remove(
                    sectionId.toString()
                )
            }

            if (collapsedIds.isEmpty()) {
                preferences.remove(key)
            } else {
                preferences[key] = collapsedIds
            }
        }
    }

    suspend fun toggleSectionCollapsed(
        readingListId: Long,
        sectionId: Long
    ) {
        val key = collapsedSectionsKey(readingListId)

        dataStore.edit { preferences ->
            val collapsedIds = preferences[key]
                .orEmpty()
                .toMutableSet()

            val sectionValue = sectionId.toString()

            if (collapsedIds.contains(sectionValue)) {
                collapsedIds.remove(sectionValue)
            } else {
                collapsedIds.add(sectionValue)
            }

            if (collapsedIds.isEmpty()) {
                preferences.remove(key)
            } else {
                preferences[key] = collapsedIds
            }
        }
    }

    private fun collapsedSectionsKey(
        readingListId: Long
    ): Preferences.Key<Set<String>> {
        return stringSetPreferencesKey(
            "reading_list_${readingListId}_collapsed_sections"
        )
    }
}