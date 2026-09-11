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
class ReadingListUiPreferencesTest {

    private lateinit var preferences:
            ReadingListUiPreferences

    @Before
    fun setUp() {
        val context =
            ApplicationProvider
                .getApplicationContext<Context>()

        preferences =
            ReadingListUiPreferences(
                context
            )
    }

    @Test
    fun setSectionCollapsed_persistsAndRemovesSectionId() =
        runBlocking {
            val readingListId =
                System.nanoTime()

            val sectionId = 41L

            try {
                assertEquals(
                    emptySet<Long>(),
                    preferences
                        .getCollapsedSectionIds(
                            readingListId
                        )
                        .first()
                )

                preferences
                    .setSectionCollapsed(
                        readingListId =
                            readingListId,
                        sectionId =
                            sectionId,
                        collapsed =
                            true
                    )

                assertEquals(
                    setOf(sectionId),
                    preferences
                        .getCollapsedSectionIds(
                            readingListId
                        )
                        .first()
                )

                preferences
                    .setSectionCollapsed(
                        readingListId =
                            readingListId,
                        sectionId =
                            sectionId,
                        collapsed =
                            false
                    )

                assertEquals(
                    emptySet<Long>(),
                    preferences
                        .getCollapsedSectionIds(
                            readingListId
                        )
                        .first()
                )
            } finally {
                preferences
                    .setSectionCollapsed(
                        readingListId =
                            readingListId,
                        sectionId =
                            sectionId,
                        collapsed =
                            false
                    )
            }
        }

    @Test
    fun collapsedSections_areIndependentPerReadingList() =
        runBlocking {
            val firstReadingListId =
                System.nanoTime()

            val secondReadingListId =
                firstReadingListId + 1L

            val firstSectionId = 12L
            val secondSectionId = 34L

            try {
                preferences
                    .setSectionCollapsed(
                        readingListId =
                            firstReadingListId,
                        sectionId =
                            firstSectionId,
                        collapsed =
                            true
                    )

                preferences
                    .setSectionCollapsed(
                        readingListId =
                            secondReadingListId,
                        sectionId =
                            secondSectionId,
                        collapsed =
                            true
                    )

                assertEquals(
                    setOf(firstSectionId),
                    preferences
                        .getCollapsedSectionIds(
                            firstReadingListId
                        )
                        .first()
                )

                assertEquals(
                    setOf(secondSectionId),
                    preferences
                        .getCollapsedSectionIds(
                            secondReadingListId
                        )
                        .first()
                )

                preferences
                    .setSectionCollapsed(
                        readingListId =
                            firstReadingListId,
                        sectionId =
                            firstSectionId,
                        collapsed =
                            false
                    )

                assertEquals(
                    emptySet<Long>(),
                    preferences
                        .getCollapsedSectionIds(
                            firstReadingListId
                        )
                        .first()
                )

                assertEquals(
                    setOf(secondSectionId),
                    preferences
                        .getCollapsedSectionIds(
                            secondReadingListId
                        )
                        .first()
                )
            } finally {
                preferences
                    .setSectionCollapsed(
                        readingListId =
                            firstReadingListId,
                        sectionId =
                            firstSectionId,
                        collapsed =
                            false
                    )

                preferences
                    .setSectionCollapsed(
                        readingListId =
                            secondReadingListId,
                        sectionId =
                            secondSectionId,
                        collapsed =
                            false
                    )
            }
        }
}