package com.aschlus.comicreadingcompanion.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeUiPreferencesTest {

    private lateinit var preferences:
            HomeUiPreferences

    @Before
    fun setUp() {
        val context =
            ApplicationProvider
                .getApplicationContext<Context>()

        preferences =
            HomeUiPreferences(
                context
            )
    }

    @Test
    fun recordReadingListOpened_persistsMostRecentFirstWithoutDuplicates() =
        runBlocking {
            try {
                preferences
                    .clearRecentlyOpenedReadingLists()

                preferences
                    .recordReadingListOpened(
                        101L
                    )

                preferences
                    .recordReadingListOpened(
                        202L
                    )

                preferences
                    .recordReadingListOpened(
                        303L
                    )

                assertEquals(
                    listOf(
                        303L,
                        202L,
                        101L
                    ),
                    preferences
                        .recentlyOpenedReadingListIds
                        .first()
                )

                preferences
                    .recordReadingListOpened(
                        101L
                    )

                assertEquals(
                    listOf(
                        101L,
                        303L,
                        202L
                    ),
                    preferences
                        .recentlyOpenedReadingListIds
                        .first()
                )
            } finally {
                preferences
                    .clearRecentlyOpenedReadingLists()
            }
        }
}