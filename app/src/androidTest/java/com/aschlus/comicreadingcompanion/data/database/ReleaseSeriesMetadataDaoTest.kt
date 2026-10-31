
package com.aschlus.comicreadingcompanion.data.database

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aschlus.comicreadingcompanion.data.database.entities.ReleaseSeriesMetadataEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReleaseSeriesMetadataDaoTest {

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
    fun upsertReleaseSeriesMetadata_updatesExistingEntry() =
        runBlocking {

            val original =
                metadata(
                    externalSeriesId = "45"
                )

            dao.upsertReleaseSeriesMetadata(
                listOf(original)
            )

            val updated =
                original.copy(
                    comicVineId = "18694",
                    sourceModifiedAt = "2026-10-25T08:00:00Z",
                    lastFetchedAt = 2000L
                )

            dao.upsertReleaseSeriesMetadata(
                listOf(updated)
            )

            val stored =
                dao.getReleaseSeriesMetadata(
                    providerId = "METRON",
                    externalSeriesId = "45"
                )

            assertNotNull(stored)

            assertEquals(
                "18694",
                stored?.comicVineId
            )

            assertEquals(
                "2026-10-25T08:00:00Z",
                stored?.sourceModifiedAt
            )

            assertEquals(
                2000L,
                stored?.lastFetchedAt
            )
        }

    @Test
    fun releaseSeriesMetadataIdentityIsScopedToProvider() =
        runBlocking {

            dao.upsertReleaseSeriesMetadata(
                listOf(
                    metadata(
                        externalSeriesId = "45",
                        comicVineId = "18693"
                    ),
                    metadata(
                        providerId = "OTHER",
                        externalSeriesId = "45",
                        comicVineId = "900"
                    )
                )
            )

            val metron =
                dao.getReleaseSeriesMetadata(
                    providerId = "METRON",
                    externalSeriesId = "45"
                )

            val other =
                dao.getReleaseSeriesMetadata(
                    providerId = "OTHER",
                    externalSeriesId = "45"
                )

            assertEquals(
                "18693",
                metron?.comicVineId
            )

            assertEquals(
                "900",
                other?.comicVineId
            )

            assertNull(
                dao.getReleaseSeriesMetadata(
                    providerId = "UNKNOWN",
                    externalSeriesId = "45"
                )
            )
        }

    @Test
    fun releaseSeriesMetadata_preservesMissingExternalIdentifiers() =
        runBlocking {

            val entry =
                metadata(
                    externalSeriesId = "46",
                    comicVineId = null,
                    gcdId = null
                )

            dao.upsertReleaseSeriesMetadata(
                listOf(entry)
            )

            val stored =
                dao.getReleaseSeriesMetadata(
                    providerId = "METRON",
                    externalSeriesId = "46"
                )

            assertNotNull(stored)

            assertNull(stored?.comicVineId)
            assertNull(stored?.gcdId)

            assertEquals(
                1000L,
                stored?.lastFetchedAt
            )
        }

    private fun metadata(
        externalSeriesId: String,
        providerId: String = "METRON",
        comicVineId: String? = "18693",
        gcdId: String? = "621"
    ) = ReleaseSeriesMetadataEntity(
        providerId = providerId,
        externalSeriesId = externalSeriesId,
        comicVineId = comicVineId,
        gcdId = gcdId,
        sourceUrl = "https://metron.cloud/series/$externalSeriesId/",
        sourceModifiedAt = "2026-10-20T08:00:00Z",
        lastFetchedAt = 1000L
    )
}
