package com.aschlus.comicreadingcompanion.data.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.ExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListItem
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSection
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingProgress
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.Universe

@Database(
    entities = [
        Publisher::class,
        Universe::class,
        Series::class,
        Issue::class,
        ExternalId::class,
        ReadingList::class,
        ReadingListSection::class,
        ReadingListItem::class,
        ReadingProgress::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ComicDatabase : RoomDatabase() {
    abstract fun comicDao(): ComicDao
}