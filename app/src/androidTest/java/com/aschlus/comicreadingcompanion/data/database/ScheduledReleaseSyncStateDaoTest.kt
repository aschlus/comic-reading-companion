package com.aschlus.comicreadingcompanion.data.database

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.entities.ScheduledReleaseSyncStateEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduledReleaseSyncStateDaoTest {

    private lateinit var database: ComicDatabase
    private lateinit var dao: ComicDao

    @Before
    fun setUp() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        database =
            Room.inMemoryDatabaseBuilder(
                context,
                ComicDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()

        dao = database.comicDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertScheduledReleaseSyncState_insertsAndUpdatesState() =
        runBlocking {

            val original = state(
                lastSuccessfulSyncAt = 1000L
            )

            dao.upsertScheduledReleaseSyncState(original)

            val updated = original.copy(
                lastSuccessfulSyncAt = 2000L
            )

            dao.upsertScheduledReleaseSyncState(updated)

            val stored =
                dao.getScheduledReleaseSyncState(
                    providerId = "METRON",
                    fromDate = "2026-11-01",
                    toDate = "2026-11-14"
                )

            assertNotNull(stored)

            assertEquals(
                2000L,
                stored?.lastSuccessfulSyncAt
            )
        }

    @Test
    fun scheduledReleaseSyncState_isScopedToProviderAndDateRange() =
        runBlocking {

            dao.upsertScheduledReleaseSyncState(
                state(
                    lastSuccessfulSyncAt = 1000L
                )
            )

            dao.upsertScheduledReleaseSyncState(
                state(
                    providerId = "OTHER",
                    lastSuccessfulSyncAt = 2000L
                )
            )

            dao.upsertScheduledReleaseSyncState(
                state(
                    toDate = "2026-11-21",
                    lastSuccessfulSyncAt = 3000L
                )
            )

            val metron =
                dao.getScheduledReleaseSyncState(
                    providerId = "METRON",
                    fromDate = "2026-11-01",
                    toDate = "2026-11-14"
                )

            val otherProvider =
                dao.getScheduledReleaseSyncState(
                    providerId = "OTHER",
                    fromDate = "2026-11-01",
                    toDate = "2026-11-14"
                )

            val differentRange =
                dao.getScheduledReleaseSyncState(
                    providerId = "METRON",
                    fromDate = "2026-11-01",
                    toDate = "2026-11-21"
                )

            assertEquals(
                1000L,
                metron?.lastSuccessfulSyncAt
            )

            assertEquals(
                2000L,
                otherProvider?.lastSuccessfulSyncAt
            )

            assertEquals(
                3000L,
                differentRange?.lastSuccessfulSyncAt
            )

            assertNull(
                dao.getScheduledReleaseSyncState(
                    providerId = "UNKNOWN",
                    fromDate = "2026-11-01",
                    toDate = "2026-11-14"
                )
            )
        }

    private fun state(
        providerId: String = "METRON",
        fromDate: String = "2026-11-01",
        toDate: String = "2026-11-14",
        lastSuccessfulSyncAt: Long
    ) = ScheduledReleaseSyncStateEntity(
        providerId = providerId,
        fromDate = fromDate,
        toDate = toDate,
        lastSuccessfulSyncAt = lastSuccessfulSyncAt
    )
}