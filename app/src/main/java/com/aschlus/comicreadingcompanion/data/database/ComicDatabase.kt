package com.aschlus.comicreadingcompanion.data.database

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.ExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.IssueOwnership
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.PullListSeriesEntity
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListItem
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSection
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingProgress
import com.aschlus.comicreadingcompanion.data.database.entities.ReleaseSeriesMetadataEntity
import com.aschlus.comicreadingcompanion.data.database.entities.ScheduledReleaseChangeEntity
import com.aschlus.comicreadingcompanion.data.database.entities.ScheduledReleaseEntity
import com.aschlus.comicreadingcompanion.data.database.entities.ScheduledReleaseSyncStateEntity
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.SeriesExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.Universe

@Database(
    entities = [
        Publisher::class,
        Universe::class,
        Series::class,
        SeriesExternalId::class,
        Issue::class,
        ExternalId::class,
        ReadingList::class,
        ReadingListSection::class,
        ReadingListItem::class,
        ReadingProgress::class,
        IssueOwnership::class,
        ScheduledReleaseEntity::class,
        ReleaseSeriesMetadataEntity::class,
        ScheduledReleaseSyncStateEntity::class,
        ScheduledReleaseChangeEntity::class,
        PullListSeriesEntity::class
    ],
    version = 13,
    exportSchema = true
)
abstract class ComicDatabase : RoomDatabase() {
    abstract fun comicDao(): ComicDao

    companion object {

        @Volatile
        private var INSTANCE: ComicDatabase? = null

        fun getDatabase(context: Context): ComicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ComicDatabase::class.java,
                    "comic_reading_companion_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}