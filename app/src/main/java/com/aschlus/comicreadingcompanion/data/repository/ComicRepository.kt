package com.aschlus.comicreadingcompanion.data.repository

import androidx.room3.withWriteTransaction
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListItem
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSection
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSource
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingProgress
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import com.aschlus.comicreadingcompanion.data.database.entities.ExternalId
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

class ComicRepository(
    private val comicDao: ComicDao,
    private val database: ComicDatabase
) {

    suspend fun addPublisher(publisher: Publisher): Long {
        return comicDao.insertPublisher(publisher)
    }

    suspend fun getPublishers(): List<Publisher> {
        return comicDao.getAllPublishers()
    }

    fun getPublishersFlow(): Flow<List<Publisher>> {
        return comicDao.getAllPublishersFlow()
    }

    fun getPublisherById(
        publisherId: Long
    ): Flow<Publisher?> {
        return comicDao.getPublisherById(
            publisherId = publisherId
        )
    }

    fun getPublisherSeries(
        publisherId: Long
    ): Flow<List<PublisherSeries>> {
        return comicDao.getPublisherSeries(
            publisherId = publisherId
        )
    }

    suspend fun addUniverse(universe: Universe): Long {
        return comicDao.insertUniverse(universe)
    }

    suspend fun getUniverseForPublisher(
        publisherId: Long
    ): List<Universe> {
        return comicDao.getUniversesForPublisher(publisherId)
    }

    suspend fun addSeries(series: Series): Long {
        return comicDao.insertSeries(series)
    }

    suspend fun getSeriesForPublisher(
        publisherId: Long
    ): List<Series> {
        return comicDao.getSeriesForPublisher(publisherId)
    }

    fun getSeriesDetail(
        seriesId: Long
    ): Flow<SeriesDetail?> {
        return comicDao.getSeriesDetail(
            seriesId = seriesId
        )
    }

    fun getSeriesIssues(
        seriesId: Long
    ): Flow<List<SeriesIssue>> {
        return comicDao.getSeriesIssues(
            seriesId = seriesId
        )
    }

    suspend fun addIssue(issue: Issue): Long {
        return comicDao.insertIssue(issue)
    }

    suspend fun getIssuesForSeries(
        seriesId: Long
    ): List<Issue> {
        return comicDao.getIssuesForSeries(seriesId)
    }

    suspend fun getIssueById(issueId: Long): Issue? {
        return comicDao.getIssueById(issueId)
    }

    fun getIssueDetail(
        issueId: Long
    ): Flow<IssueDetail?> {
        return comicDao.getIssueDetail(
            issueId = issueId
        )
    }


    // Reading lists

    suspend fun addReadingList(readingList: ReadingList): Long {
        return comicDao.insertReadingList(readingList)
    }

    suspend fun createUserReadingList(
        title: String,
        description: String?,
        publisherId: Long,
        universeId: Long?
    ): Long {
        val currentTime = System.currentTimeMillis()
        return comicDao.insertReadingList(
            ReadingList(
                title = title,
                description = description,
                publisherId = publisherId,
                universeId = universeId,
                source = ReadingListSource.USER,
                sourceKey = null,
                createdAt = currentTime,
                updatedAt = currentTime
            )
        )
    }

    fun getReadingLists(): Flow<List<ReadingList>> {
        return comicDao.getAllReadingLists()
    }

    suspend fun getReadingListById(
        readingListId: Long
    ): ReadingList? {
        return comicDao.getReadingListById(readingListId)
    }

    fun getReadingListIssues(
        readingListId: Long
    ): Flow<List<ReadingListIssue>> {
        return comicDao.getReadingListIssues(
            readingListId
        )
    }

    suspend fun updateReadingList(readingList: ReadingList) {
        comicDao.updateReadingList((readingList))
    }

    suspend fun deleteReadingList(readingList: ReadingList) {
        comicDao.deleteReadingList(readingList)
    }

    fun getReadingListSummaries(): Flow<List<ReadingListSummary>> {
        return comicDao.getReadingListSummaries()
    }


    // Reading list sections

    suspend fun addReadingListSection(
        section: ReadingListSection
    ): Long {
        return comicDao.insertReadingListSection(section)
    }

    suspend fun getSectionsForReadingList(
        readingListId: Long
    ): List<ReadingListSection> {
        return comicDao.getSectionsForReadingList(readingListId)
    }

    suspend fun updateReadingListSection(
        section: ReadingListSection
    ) {
        comicDao.updateReadingListSection(section)
    }

    suspend fun deleteReadingListSection(
        section: ReadingListSection
    ) {
        comicDao.deleteReadingListSection(section)
    }


    // Reading list items

    suspend fun addIssueToUserReadingList(
        readingListId: Long,
        issueId: Long
    ): Long {
        var resultItemId: Long? = null

        database.withWriteTransaction {
            val readingList =
                comicDao.getReadingListById(readingListId)
                    ?: throw IllegalArgumentException(
                        "Reading list " +
                        "$readingListId does not exist"
                    )

            require(
                readingList.source == ReadingListSource.USER
            ) {
                "Reading list $readingListId " +
                "is not user-owned"
            }

            val issue = comicDao.getIssueById(issueId)
                ?: throw IllegalArgumentException(
                    "Issue $issueId does not exist"
                )

            val series = comicDao.getSeriesById(issue.seriesId)
                ?: throw IllegalStateException(
                    "Series ${issue.seriesId} does not exist"
                )

            require(
                series.publisherId == readingList.publisherId
            ) {
                "Issue $issueId belongs to a " +
                "different publisher"
            }

            if (readingList.universeId != null) {
                require(
                    issue.universeId == readingList.universeId
                ) {
                    "Issue $issueId belongs to a " +
                    "different continuity"
                }
            }

            val existingItem =
                comicDao.getReadingListItem(
                    readingListId = readingListId,
                    issueId = issueId
                )

            if (existingItem != null) {
                resultItemId = existingItem.id
            } else {
                val nextPosition =
                    comicDao.getItemsForReadingList(readingListId)
                        .maxOfOrNull { item ->
                            item.position
                        }
                        ?.plus(1)
                        ?: 1

                val itemId =
                    comicDao.insertReadingListItem(
                        ReadingListItem(
                            readingListId = readingListId,
                            sectionId = null,
                            issueId = issueId,
                            position = nextPosition,
                            required = true,
                            notes = null
                        )
                    )

                resultItemId = itemId

                val updatedAt =
                    maxOf(System.currentTimeMillis(), readingList.updatedAt + 1)

                comicDao.updateReadingList(
                    readingList.copy(
                        updatedAt = updatedAt
                    )
                )
            }
        }

        return checkNotNull(resultItemId) {
            "Reading-list itemId was not set"
        }
    }

    suspend fun removeIssueFromUserReadingList(
        readingListId: Long,
        issueId: Long
    ): Boolean {
        var wasRemoved = false

        database.withWriteTransaction {
            val readingList =
                comicDao.getReadingListById(readingListId)
                    ?: throw IllegalArgumentException(
                        "Reading list " +
                        "$readingListId does not exist"
                    )

            require(
                readingList.source == ReadingListSource.USER
            ) {
                "Reading list $readingListId " +
                "is not user-owned"
            }

            val existingItem =
                comicDao.getReadingListItem(
                    readingListId = readingListId,
                    issueId = issueId
                )

            if (existingItem != null) {
                comicDao.deleteReadingListItem(existingItem)

                val laterItems =
                    comicDao.getItemsForReadingList(readingListId)
                        .filter { item ->
                            item.position > existingItem.position
                        }
                laterItems.forEach { item ->
                    comicDao.updateReadingListItem(
                        item.copy(
                            position = item.position - 1
                        )
                    )
                }

                val updatedAt =
                    maxOf(
                        System.currentTimeMillis(),
                        readingList.updatedAt + 1
                    )

                comicDao.updateReadingList(
                    readingList.copy(
                        updatedAt = updatedAt
                    )
                )

                wasRemoved = true
            }
        }

        return wasRemoved
    }

    suspend fun reorderUserReadingListItems(
        readingListId: Long,
        orderedItemIds: List<Long>
    ) {
        database.withWriteTransaction {
            val readingList = comicDao.getReadingListById(readingListId)
                ?: throw IllegalArgumentException(
                    "Reading list $readingListId does not exist"
                )

            require(readingList.source == ReadingListSource.USER) {
                "Reading list $readingListId is not user-owned"
            }

            val currentItems = comicDao.getItemsForReadingList(readingListId)

            require(orderedItemIds.size == currentItems.size) {
                "Reordered item count does not match reading list"
            }

            require(orderedItemIds.distinct().size == orderedItemIds.size) {
                "Reordered item IDs contain duplicates"
            }

            val currentItemsById = currentItems.associateBy { item -> item.id }

            require(orderedItemIds.toSet() == currentItemsById.keys) {
                "Reordered item IDs do not match reading list"
            }

            val reorderedItems =
                orderedItemIds.map { itemId -> checkNotNull(currentItemsById[itemId]) }

            val currentSectionPattern =
                currentItems.map { item -> item.sectionId }

            val reorderedSectionPattern =
                reorderedItems.map { item -> item.sectionId }

            require(reorderedSectionPattern == currentSectionPattern) {
                "Items cannot move between sections while reordering"
            }

            if (currentItems.map { item -> item.id } == orderedItemIds) {
                return@withWriteTransaction
            }

            currentItems.forEachIndexed { index, item ->
                comicDao.updateReadingListItem(
                    item.copy(
                        position = -(index + 1)
                    )
                )
            }

            reorderedItems.forEachIndexed { index, item ->
                comicDao.updateReadingListItem(
                    item.copy(
                        position = index + 1
                    )
                )
            }

            val updatedAt = maxOf(System.currentTimeMillis(), readingList.updatedAt + 1)

            comicDao.updateReadingList(
                readingList.copy(
                    updatedAt = updatedAt
                )
            )
        }
    }

    suspend fun addReadingListItem(
        item: ReadingListItem
    ): Long {
        return comicDao.insertReadingListItem(item)
    }

    suspend fun getItemsForReadingList(
        readingListId: Long
    ): List<ReadingListItem> {
        return comicDao.getItemsForReadingList(readingListId)
    }

    suspend fun getItemsForSection(
        readingListId: Long,
        sectionId: Long
    ): List<ReadingListItem> {
        return comicDao.getItemsForSection(
            readingListId,
            sectionId
        )
    }

    suspend fun getUnsectionedItemsForReadingList(
        readingListId: Long
    ): List<ReadingListItem> {
        return comicDao.getUnsectionedItemsForReadingList(
            readingListId
        )
    }

    suspend fun updateReadingListItem(
        item: ReadingListItem
    ) {
        comicDao.updateReadingListItem(item)
    }

    suspend fun deleteReadingListItem(
        item: ReadingListItem
    ) {
        comicDao.deleteReadingListItem(item)
    }

    fun getUnreadReadingListItems(): Flow<List<ReadingListContinueItem>> {
        return comicDao.getUnreadReadingListItems()
    }


    // Reading progress

    suspend fun getReadingProgressForIssue(
        issueId: Long
    ): ReadingProgress? {
        return comicDao.getReadingProgressForIssue(issueId)
    }

    suspend fun markIssueAsReading(issueId: Long) {
        val existingProgress = comicDao.getReadingProgressForIssue(issueId)

        val updatedProgress = if (existingProgress == null) {
            ReadingProgress(
                issueId = issueId,
                status = ReadingStatus.READING,
                startedAt = System.currentTimeMillis(),
                completedAt = null,
                notes = null
            )
        } else {
            existingProgress.copy(
                status = ReadingStatus.READING,
                startedAt = existingProgress.startedAt
                    ?: System.currentTimeMillis(),
                completedAt = null
            )
        }

        comicDao.upsertReadingProgress(updatedProgress)
    }

    suspend fun markIssueAsRead(issueId: Long) {
        val existingProgress = comicDao.getReadingProgressForIssue(issueId)
        val currentTime = System.currentTimeMillis()

        val updatedProgress = if (existingProgress == null) {
            ReadingProgress(
                issueId = issueId,
                status = ReadingStatus.READ,
                startedAt = currentTime,
                completedAt = currentTime,
                notes = null
            )
        } else {
            existingProgress.copy(
                status = ReadingStatus.READ,
                startedAt = existingProgress.startedAt ?: currentTime,
                completedAt = currentTime
            )
        }

        comicDao.upsertReadingProgress(updatedProgress)
    }

    suspend fun markIssuesAsRead(
        issueIds: List<Long>
    ) {
        val distinctIssueIds = issueIds.distinct()

        if(distinctIssueIds.isEmpty()) {
            return
        }

        database.withWriteTransaction {
            val existingProgress =
                comicDao.getReadingProgressForIssues(
                    distinctIssueIds
                )

            val progressByIssueId =
                existingProgress.associateBy { progress ->
                    progress.issueId
                }

            val currentTime =
                System.currentTimeMillis()

            val updateProgress =
                distinctIssueIds.map { issueId ->

                    val existing =
                        progressByIssueId[issueId]

                    if (existing == null) {
                        ReadingProgress(
                            issueId = issueId,
                            status = ReadingStatus.READ,
                            startedAt = currentTime,
                            completedAt = currentTime,
                            notes = null
                        )
                    } else {
                        existing.copy(
                            status = ReadingStatus.READ,
                            startedAt =
                                existing.startedAt
                                    ?: currentTime,
                            completedAt = currentTime
                        )
                    }
                }

            comicDao.upsertReadingProgress(
                updateProgress
            )
        }
    }

    suspend fun markIssueAsUnread(issueId: Long) {
        val existingProgress = comicDao.getReadingProgressForIssue(issueId)

        if (existingProgress != null) {
            comicDao.deleteReadingProgress(existingProgress)
        }
    }

    suspend fun markIssuesAsUnread(
        issueIds: List<Long>
    ) {
        val distinctIssueIds = issueIds.distinct()

        if (distinctIssueIds.isEmpty()) {
            return
        }

        comicDao.deleteReadingProgressForIssues(
            distinctIssueIds
        )
    }


    // Browsing

    fun searchSeries(
        query: String
    ): Flow<List<SeriesSearchResult>> {
        return comicDao.searchSeries(
            query = query
        )
    }

    fun searchIssues(
        query: String
    ): Flow<List<IssueSearchResult>> {
        return comicDao.searchIssues(
            query = query
        )
    }


    // External IDs

    suspend fun addExternalId(externalId: ExternalId): Long {
        return comicDao.insertExternalId(externalId)
    }

    suspend fun getExternalIdsForIssue(
        issueId: Long
    ): List<ExternalId> {
        return comicDao.getExternalIdsForIssue(issueId)
    }

    suspend fun getExternalId(
        source: String,
        externalId: String
    ): ExternalId? {
        return comicDao.getExternalId(
            source = source,
            externalId = externalId
        )
    }
}