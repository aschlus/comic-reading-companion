package com.aschlus.comicreadingcompanion.data.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher

@Dao
interface ComicDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPublisher(publisher: Publisher): Long

    @Query("SELECT * FROM publishers ORDER BY name ASC")
    suspend fun getAllPublishers(): List<Publisher>
}