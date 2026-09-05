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
import com.aschlus.comicreadingcompanion.data.database.models.IssueDetail
import com.aschlus.comicreadingcompanion.data.database.models.IssueSearchResult
import com.aschlus.comicreadingcompanion.data.database.models.PublisherSeries
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListContinueItem
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListIssue
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListSummary
import com.aschlus.comicreadingcompanion.data.database.models.SeriesDetail
import com.aschlus.comicreadingcompanion.data.database.models.SeriesIssue
import com.aschlus.comicreadingcompanion.data.database.models.SeriesSearchResult
import kotlinx.coroutines.flow.Flow

@Dao
interface ComicDao {

    // Publishers

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPublisher(publisher: Publisher): Long

    @Query("SELECT * FROM publishers ORDER BY name ASC")
    suspend fun getAllPublishers(): List<Publisher>

    @Query("SELECT * FROM publishers ORDER BY name ASC")
    fun getAllPublishersFlow(): Flow<List<Publisher>>

    @Query("""
        SELECT * FROM publishers
        WHERE name = :name
        LIMIT 1
    """)
    suspend fun getPublisherByName(
        name: String
    ): Publisher?

    @Query("""
        SELECT * FROM publishers
        WHERE id = :publisherId
        LIMIT 1
    """)
    fun getPublisherById(
        publisherId: Long
    ): Flow<Publisher?>


    // Universes

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUniverse(universe: Universe): Long

    @Query("""
        SELECT * FROM universes
        WHERE publisherId = :publisherId
        ORDER BY name ASC
    """)
    suspend fun getUniversesForPublisher(publisherId: Long): List<Universe>

    @Query("""
        SELECT * FROM universes
        WHERE publisherId = :publisherId
          AND designation = :designation
        LIMIT 1
    """)
    suspend fun getUniverseByDesignation(
        publisherId: Long,
        designation: String
    ): Universe?


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
          AND (
              volume = :volume
              OR (volume IS NULL AND :volume IS NULL)
          )
        LIMIT 1
    """)
    suspend fun getSeries(
        publisherId: Long,
        title: String,
        volume: Int?
    ): Series?

    @Query("""
        SELECT
            series.id AS seriesId,
            series.title AS title,
            series.volume AS volume,
            series.startYear AS startYear,
            series.endYear AS endYear,
            publishers.id AS publisherId,
            publishers.name AS publisherName
        FROM series
        INNER JOIN publishers
            ON series.publisherId = publishers.id
        WHERE series.id = :seriesId
        LIMIT 1
    """)
    fun getSeriesDetail(
        seriesId: Long
    ): Flow<SeriesDetail?>

    @Query("""
        SELECT
            issues.id AS issueId,
            issues.issueNumber AS issueNumber,
            issues.title AS issueTitle,
            issues.publicationDate AS publicationDate,
            issues.coverUrl AS coverUrl,
            issues.issueType AS issueType,
            reading_progress.status AS readingStatus
        FROM issues
        LEFT JOIN reading_progress
            ON issues.id = reading_progress.issueId
        WHERE issues.seriesId = :seriesId
        ORDER BY
            issues.publicationDate ASC,
            issues.issueNumber ASC
    """)
    fun getSeriesIssues(
        seriesId: Long
    ): Flow<List<SeriesIssue>>

    @Query("""
        SELECT
            series.id AS seriesId,
            series.title AS title,
            series.volume AS volume,
            series.startYear AS startYear,
            series.endYear AS endYear,
            COUNT(issues.id) AS totalCount,
            SUM(
                CASE
                    WHEN reading_progress.status = 'READ'
                        THEN 1
                    ELSE 0
                END
            ) AS readCount
        FROM series
        LEFT JOIN issues
            ON series.id = issues.seriesId
        LEFT JOIN reading_progress
            ON issues.id = reading_progress.issueId
        WHERE series.publisherId = :publisherId
        GROUP By series.id
        ORDER BY
            series.startYear ASC,
            series.title ASC,
            series.volume ASC
    """)
    fun getPublisherSeries(
        publisherId: Long
    ): Flow<List<PublisherSeries>>

    @Query("""
        SELECT
            series.id AS seriesId,
            series.title AS title,
            series.volume AS volume,
            series.startYear AS startYear,
            series.endYear AS endYear,
            publishers.name AS publisherName,
            COUNT(issues.id) AS totalCount,
            SUM(
                CASE
                    WHEN reading_progress.status = 'READ'
                        THEN 1
                    ELSE 0
                END
            ) AS readCount
        FROM series
        INNER JOIN publishers
            ON series.publisherId = publishers.id
        LEFT JOIN issues
            ON series.id = issues.seriesId
        LEFT JOIN reading_progress
            ON issues.id = reading_progress.issueId
        WHERE LOWER(series.title)
            LIKE '%' || LOWER(:query) || '%'
        GROUP BY series.id
        ORDER BY
            series.title ASC,
            series.startYear ASC
        LIMIT 50
    """)
    fun searchSeries(
        query: String
    ): Flow<List<SeriesSearchResult>>


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

    @Query("""
        SELECT
            issues.id AS issueId,
            series.id AS seriesId,
            series.title AS seriesTitle,
            series.volume AS seriesVolume,
            issues.issueNumber AS issueNumber,
            issues.title AS issueTitle,
            issues.publicationDate AS publicationDate,
            issues.coverUrl AS coverUrl,
            issues.description AS description,
            issues.issueType AS issueType,
            publishers.name AS publisherName,
            universes.name AS universeName,
            universes.designation AS universeDesignation,
            reading_progress.status AS readingStatus
        FROM issues
        INNER JOIN series
            ON issues.seriesId = series.id
        INNER JOIN publishers
            ON series.publisherId = publishers.id
        LEFT JOIN universes
            ON issues.universeId = universes.id
        LEFT JOIN reading_progress
            ON issues.id = reading_progress.issueId
        WHERE issues.id = :issueId
        LIMIT 1
    """)
    fun getIssueDetail(
        issueId: Long
    ): Flow<IssueDetail>

    @Query("""
        SELECT
            issues.id AS issueId,
            series.id AS seriesId,
            series.title AS seriesTitle,
            series.volume AS seriesVolume,
            issues.issueNumber AS issueNumber,
            issues.title AS issueTitle,
            issues.publicationDate AS publicationDate,
            publishers.name AS publisherName,
            reading_progress.status AS readingStatus
        FROM issues
        INNER JOIN series
            ON issues.seriesId = series.id
        INNER JOIN publishers
            ON series.publisherId = publishers.id
        LEFT JOIN reading_progress
            ON issues.id = reading_progress.issueId
        WHERE
            LOWER(series.title)
                LIKE '%' || LOWER(:query) || '%'
            OR LOWER(COALESCE(issues.title, ''))
                LIKE '%' || LOWER(:query) || '%'
            OR LOWER(issues.issueNumber)
                LIKE '%' || LOWER(:query) || '%'
        ORDER BY
            series.title ASC,
            issues.publicationDate ASC,
            issues.issueNumber ASC
        LIMIT 75
    """)
    fun searchIssues(
        query: String
    ): Flow<List<IssueSearchResult>>


    // Reading lists

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReadingList(readingList: ReadingList): Long

    @Query("""
        SELECT * FROM reading_lists
        ORDER BY updatedAt DESC
    """)
    fun getAllReadingLists(): Flow<List<ReadingList>>

    @Query("""
        SELECT * FROM reading_lists
        WHERE id = :readingListId
        LIMIT 1
    """)
    suspend fun getReadingListById(readingListId: Long): ReadingList?

    @Query("""
        SELECT
            reading_lists.id AS readingListId,
            COUNT(reading_list_items.id) AS totalCount,
            SUM(
                CASE
                    WHEN reading_progress.status = 'READ' THEN 1
                    ELSE 0
                END
            ) AS readCount
        FROM reading_lists
        LEFT JOIN reading_list_items
            ON reading_lists.id = reading_list_items.readingListId
        LEFT JOIN reading_progress
            ON reading_list_items.issueId = reading_progress.issueId
        GROUP BY reading_lists.id
        ORDER BY reading_lists.updatedAt DESC
    """)
    fun getReadingListSummaries(): Flow<List<ReadingListSummary>>

    @Query("""
        SELECT
            reading_list_items.readingListId AS readingListId,
            issues.id AS issueId,
            reading_list_items.position AS position,
            series.title AS seriesTitle,
            issues.issueNumber AS issueNumber,
            issues.title AS issueTitle
        FROM reading_list_items
        INNER JOIN issues
            ON reading_list_items.issueId = issues.id
        INNER JOIN series
            ON issues.seriesId = series.id
        LEFT JOIN reading_progress
            ON issues.id = reading_progress.issueId
        WHERE reading_progress.status IS NULL
            OR reading_progress.status != 'READ'
        ORDER BY
            reading_list_items.readingListId ASC,
            reading_list_items.position ASC
    """)
    fun getUnreadReadingListItems():
    Flow<List<ReadingListContinueItem>>


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

    @Query("""
        SELECT * FROM reading_progress
        WHERE issueId IN (:issueIds)
    """)
    suspend fun getReadingProgressForIssues(
        issueIds: List<Long>
    ): List<ReadingProgress>


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

    @Update
    suspend fun updateSeries(series: Series)

    @Update
    suspend fun updateIssue(issue: Issue)


    // Reading progress

    @Upsert
    suspend fun upsertReadingProgress(progress: ReadingProgress)

    @Upsert
    suspend fun upsertReadingProgress(progress: List<ReadingProgress>)

    @Delete
    suspend fun deleteReadingProgress(progress: ReadingProgress)

    @Query("""
        DELETE FROM reading_progress
        WHERE issueId IN (:issueIds)
    """)
    suspend fun deleteReadingProgressForIssues(
        issueIds: List<Long>
    )
}