package com.aschlus.comicreadingcompanion.data.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Delete
import androidx.room3.Update
import androidx.room3.Upsert
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListItem
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSection
import com.aschlus.comicreadingcompanion.data.database.entities.ExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingProgress
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListIssue
import kotlinx.coroutines.flow.Flow

@Dao
interface ComicDao {

    // Publishers

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPublisher(publisher: Publisher): Long

    @Query("SELECT * FROM publishers ORDER BY name ASC")
    suspend fun getAllPublishers(): List<Publisher>


    // Universes

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUniverse(universe: Universe): Long

    @Query("""
        SELECT * FROM universes
        WHERE publisherId = :publisherId
        ORDER BY name ASC
    """)
    suspend fun getUniversesForPublisher(publisherId: Long): List<Universe>


    // Series

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSeries(series: Series): Long

    @Query("""
        SELECT * FROM series
        WHERE publisherId = :publisherId
        ORDER BY startYear ASC, title ASC
    """)
    suspend fun getSeriesForPublisher(publisherId: Long): List<Series>

    @Query("""
        SELECT * FROM series
        WHERE publisherId = :publisherId
          AND title = :title
          AND volume = :volume
        LIMIT 1
    """)
    suspend fun getSeries(
        publisherId: Long,
        title: String,
        volume: Int
    ): Series?


    // Issues

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertIssue(issue: Issue): Long

    @Query("""
        SELECT * FROM issues
        WHERE seriesId = :seriesId
        ORDER BY publicationDate ASC, issueNumber ASC
    """)
    suspend fun getIssuesForSeries(seriesId: Long): List<Issue>

    @Query("""
        SELECT * FROM issues
        WHERE id = :issueId
        LIMIT 1
    """)
    suspend fun getIssueById(issueId: Long): Issue?

    @Query("""
        SELECT * FROM issues
        WHERE seriesId = :seriesId
          AND issueNumber = :issueNumber
        LIMIT 1
    """)
    suspend fun getIssue(
        seriesId: Long,
        issueNumber: String
    ): Issue?


    // Reading lists

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReadingList(readingList: ReadingList): Long

    @Query("""
        SELECT * FROM reading_lists
        ORDER BY updatedAt DESC
    """)
    suspend fun getAllReadingLists(): List<ReadingList>

    @Query("""
        SELECT * FROM reading_lists
        WHERE id = :readingListId
        LIMIT 1
    """)
    suspend fun getReadingListById(readingListId: Long): ReadingList?


    // Reading list sections

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReadingListSection(section: ReadingListSection): Long

    @Query("""
        SELECT * FROM reading_list_sections
        WHERE readingListId = :readingListId
        ORDER BY position ASC
    """)
    suspend fun getSectionsForReadingList(
        readingListId: Long
    ): List<ReadingListSection>


    // Reading list items

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReadingListItem(item: ReadingListItem): Long

    @Query("""
        SELECT * FROM reading_list_items
        WHERE readingListId = :readingListId
        ORDER BY position ASC
    """)
    suspend fun getItemsForReadingList(
        readingListId: Long
    ): List<ReadingListItem>

    @Query("""
        SELECT * FROM reading_list_items
        WHERE readingListId = :readingListId
          AND sectionId = :sectionId
        ORDER BY position ASC
    """)
    suspend fun getItemsForSection(
        readingListId: Long,
        sectionId: Long
    ): List<ReadingListItem>

    @Query("""
        SELECT * FROM reading_list_items
        WHERE readingListId = :readingListId
          AND sectionId IS NULL
        ORDER BY position ASC
    """)
    suspend fun getUnsectionedItemsForReadingList(
        readingListId: Long
    ): List<ReadingListItem>

    @Query("""
        SELECT * FROM reading_list_items
        WHERE readingListId = :readingListId
          AND issueId = :issueId
        LIMIT 1
    """)
    suspend fun getReadingListItem(
        readingListId: Long,
        issueId: Long
    ): ReadingListItem?

    @Query("""
        SELECT
            reading_list_items.id AS readingListItemId,
            issues.id AS issueId,
            reading_list_items.position AS position,
            reading_list_items.required AS required,
            reading_list_items.notes AS notes,
            issues.issueNumber AS issueNumber,
            issues.title AS issueTitle,
            issues.publicationDate AS publicationDate,
            issues.coverUrl AS coverUrl,
            series.title AS seriesTitle,
            series.volume AS seriesVolume,
            reading_progress.status AS readingStatus
        FROM reading_list_items
        INNER JOIN issues
            ON reading_list_items.issueId = issues.id
        INNER JOIN series
            ON issues.seriesId = series.id
        LEFT JOIN reading_progress
            ON issues.id = reading_progress.issueId
        WHERE reading_list_items.readingListId = :readingListId
        ORDER BY reading_list_items.position ASC
    """)
    fun getReadingListIssues(
        readingListId: Long
    ): Flow<List<ReadingListIssue>>


    // External IDs

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExternalId(externalId: ExternalId): Long

    @Query("""
        SELECT * FROM external_ids
        WHERE issueId = :issueId
        ORDER BY source ASC
    """)
    suspend fun getExternalIdsForIssue(issueId: Long): List<ExternalId>

    @Query("""
        SELECT * FROM external_ids
        WHERE source = :source
          AND externalId = :externalId
        LIMIT 1
    """)
    suspend fun getExternalId(
        source: String,
        externalId: String
    ): ExternalId?


    // Reading progress

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReadingProgress(progress: ReadingProgress): Long

    @Query("""
        SELECT * FROM reading_progress
        WHERE issueId = :issueId
        LIMIT 1
    """)
    suspend fun getReadingProgressForIssue(issueId: Long): ReadingProgress?


    // Updates and deletes

    @Update
    suspend fun updateReadingList(readingList: ReadingList)

    @Delete
    suspend fun deleteReadingList(readingList: ReadingList)

    @Update
    suspend fun updateReadingListSection(section: ReadingListSection)

    @Delete
    suspend fun deleteReadingListSection(section: ReadingListSection)

    @Update
    suspend fun updateReadingListItem(item: ReadingListItem)

    @Delete
    suspend fun deleteReadingListItem(item: ReadingListItem)


    // Reading progress

    @Upsert
    suspend fun upsertReadingProgress(progress: ReadingProgress)

    @Delete
    suspend fun deleteReadingProgress(progress: ReadingProgress)
}