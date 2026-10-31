package com.aschlus.comicreadingcompanion.data.repository

import androidx.room3.withWriteTransaction
import com.aschlus.comicreadingcompanion.data.database.ComicDao
import com.aschlus.comicreadingcompanion.data.database.ComicDatabase
import com.aschlus.comicreadingcompanion.data.database.entities.ExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.IssueOwnership
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListItem
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSection
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListSource
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListStyle
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingProgress
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.SeriesExternalId
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import com.aschlus.comicreadingcompanion.data.database.models.IssueDetail
import com.aschlus.comicreadingcompanion.data.database.models.IssueReadingListResult
import com.aschlus.comicreadingcompanion.data.database.models.IssueSearchResult
import com.aschlus.comicreadingcompanion.data.database.models.OwnedIssueResult
import com.aschlus.comicreadingcompanion.data.database.models.PublisherBrowseResult
import com.aschlus.comicreadingcompanion.data.database.models.PublisherSeries
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListArtworkCover
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListContinueItem
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListIssue
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListSummary
import com.aschlus.comicreadingcompanion.data.database.models.RecentReadIssue
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

    fun getPublisherBrowseResults():
            Flow<List<PublisherBrowseResult>> {
        return comicDao.getPublisherBrowseResults()
    }

    suspend fun addUniverse(universe: Universe): Long {
        return comicDao.insertUniverse(universe)
    }

    suspend fun getUniverseForPublisher(
        publisherId: Long
    ): List<Universe> {
        return comicDao.getUniversesForPublisher(publisherId)
    }

    fun getAllUniverses(): Flow<List<Universe>> {
        return comicDao.getAllUniverses()
    }

    fun getAvailablePublicationYears(): Flow<List<Int>> {
        return comicDao.getAvailablePublicationYears()
    }

    suspend fun addSeries(series: Series): Long {
        return comicDao.insertSeries(series)
    }

    suspend fun updateSeriesDetails(
        seriesId: Long,
        title: String,
        volume: Int?,
        startYear: Int?,
        endYear: Int?,
        publisherId: Long,
        universeId: Long?
    ) {
        val existingSeries =
            comicDao.getSeriesById(seriesId)
                ?: throw IllegalArgumentException(
                    "Series $seriesId does not exist"
                )

        val trimmedTitle = title.trim()

        require(trimmedTitle.isNotEmpty()) {
            "Series title cannot be blank"
        }

        require(volume == null || volume > 0) {
            "Volume must be a positive number"
        }

        require(startYear == null || startYear > 0) {
            "Start year must be a positive number"
        }

        require(endYear == null || endYear > 0) {
            "End year must be a positive number"
        }

        require(
            startYear == null ||
            endYear == null ||
            endYear >= startYear
        ) {
            "End year cannot be before start year"
        }

        require(
            comicDao.getAllPublishers().any {
                it.id == publisherId
            }
        ) {
            "Publisher $publisherId does not exist"
        }

        if (universeId != null) {
            require(
                getUniverseForPublisher(publisherId)
                    .any { it.id == universeId }
            ) {
                "Universe $universeId does not belong to publisher $publisherId"
            }
        }

        comicDao.updateSeries(
            existingSeries.copy(
                publisherId = publisherId,
                title = trimmedTitle,
                volume = volume,
                startYear = startYear,
                endYear = endYear,
                universeId = universeId
            )
        )
    }

    suspend fun getSeriesForPublisher(
        publisherId: Long
    ): List<Series> {
        return comicDao.getSeriesForPublisher(publisherId)
    }

    suspend fun getSeriesById(
        seriesId: Long
    ): Series? {
        return comicDao.getSeriesById(seriesId)
    }

    suspend fun getComicVineVolumeIdForSeries(
        seriesId: Long
    ): Long? {
        return comicDao
            .getSeriesExternalIdsForSeries(seriesId)
            .firstOrNull { externalId ->
                externalId.source == "COMIC_VINE"
            }
            ?.externalId
            ?.toLongOrNull()
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

    suspend fun previewComicVineSeriesRefresh(
        seriesId: Long,
        comicVineVolumeId: Long,
        refreshedTitle: String,
        refreshedStartYear: Int?,
        sourceIssues: List<ComicVineRefreshIssue>
    ): ComicVineSeriesRefreshPreview {
        val series = comicDao.getSeriesById(seriesId)
            ?: throw IllegalArgumentException(
                "Series $seriesId does not exist"
            )

        val localIssues = comicDao.getIssuesForSeries(seriesId)

        val localIssuesByComicVineId =
            localIssues.mapNotNull { issue ->
                comicDao.getExternalIdsForIssue(issue.id)
                    .firstOrNull { it.source == "COMIC_VINE" }
                    ?.externalId
                    ?.toLongOrNull()
                    ?.let { comicVineId -> comicVineId to issue }
            }.toMap()

        val matchedLocalIssueIds = mutableSetOf<Long>()

        val changes = sourceIssues.map { sourceIssue ->
            val localIssue =
                localIssuesByComicVineId[sourceIssue.comicVineIssueId]
                    ?: localIssues.firstOrNull { local ->
                        local.id !in matchedLocalIssueIds &&
                            local.issueNumber.trim()
                                .equals(
                                    sourceIssue.issueNumber.trim(),
                                    ignoreCase = true
                                )
                    }

            if (localIssue == null) {
                ComicVineRefreshIssueChange(
                    issueNumber = sourceIssue.issueNumber,
                    title = sourceIssue.title,
                    changeType = ComicVineRefreshIssueChange.ChangeType.ADDED
                )
            } else {
                matchedLocalIssueIds += localIssue.id

                val hasChanges =
                    localIssue.issueNumber != sourceIssue.issueNumber ||
                        (
                            sourceIssue.title != null &&
                                localIssue.title != sourceIssue.title
                        ) ||
                        (
                            sourceIssue.publicationDate != null &&
                                localIssue.publicationDate != sourceIssue.publicationDate
                        ) ||
                        (
                            sourceIssue.coverUrl != null &&
                            localIssue.coverUrl != sourceIssue.coverUrl
                        ) ||
                        (
                            !localIssue.isIssueTypeManuallyOverridden &&
                                localIssue.issueType != sourceIssue.issueType
                        ) ||
                        (
                            series.universeId != null &&
                                localIssue.universeId != series.universeId
                        )

                ComicVineRefreshIssueChange(
                    issueNumber = sourceIssue.issueNumber,
                    title = sourceIssue.title ?: localIssue.title,
                    changeType = if (hasChanges) {
                        ComicVineRefreshIssueChange.ChangeType.UPDATED
                    } else {
                        ComicVineRefreshIssueChange.ChangeType.UNCHANGED
                    },
                    localIssueId = localIssue.id
                )
            }
        }

        val missingFromSource = localIssues
            .filter { it.id !in matchedLocalIssueIds }
            .map { issue ->
                ComicVineRefreshIssueChange(
                    issueNumber = issue.issueNumber,
                    title = issue.title,
                    changeType = ComicVineRefreshIssueChange.ChangeType.MISSING_FROM_SOURCE,
                    localIssueId = issue.id
                )
            }

        return ComicVineSeriesRefreshPreview(
            seriesId = seriesId,
            comicVineVolumeId = comicVineVolumeId,
            refreshedTitle = refreshedTitle,
            refreshedStartYear = refreshedStartYear,
            issues = changes + missingFromSource
        )
    }

    suspend fun applyComicVineSeriesRefresh(
        seriesId: Long,
        comicVineVolumeId: Long,
        refreshedTitle: String,
        refreshedStartYear: Int?,
        volumeUrl: String?,
        sourceIssues: List<ComicVineRefreshIssue>
    ) {
        require(comicVineVolumeId > 0L) {
            "Comic Vine volume ID must be positive"
        }

        require(refreshedTitle.isNotBlank()) {
            "Series title cannot be blank"
        }

        require(sourceIssues.all { it.comicVineIssueId > 0L }) {
            "Comic Vine issue IDS must be positive"
        }

        require(sourceIssues.all { it.issueNumber.isNotBlank() }) {
            "Comic Vine issue numbers cannot be blank"
        }

        require(
            sourceIssues.map { it.comicVineIssueId }.distinct().size == sourceIssues.size
        ) {
            "Comic Vine issue IDs contain duplicates"
        }

        database.withWriteTransaction {
            val series = comicDao.getSeriesById(seriesId)
                ?: throw IllegalArgumentException(
                    "Series $seriesId does not exist"
                )

            val preservedSeriesUniverseId = series.universeId

            val source = "COMIC_VINE"
            val volumeId = comicVineVolumeId.toString()

            val existingVolumeLink =
                comicDao.getSeriesExternalId(source, volumeId)

            require(
                existingVolumeLink == null || existingVolumeLink.seriesId == seriesId
            ) {
                "This Comic Vine volume is linked to another series"
            }

            val existingSeriesLink =
                comicDao.getSeriesExternalIdsForSeries(seriesId)
                    .firstOrNull { it.source == source }

            require(
                existingSeriesLink == null || existingSeriesLink.externalId == volumeId
            ) {
                "This series is linked to a different Comic Vine volume"
            }
            val normalizedTitle = refreshedTitle.trim()

            comicDao.updateSeries(
                series.copy(
                    title = normalizedTitle,
                    startYear = refreshedStartYear ?: series.startYear,
                    universeId = preservedSeriesUniverseId
                )
            )

            val updatedVolumeLink =
                existingSeriesLink?.copy(
                    url = volumeUrl ?: existingSeriesLink.url
                )

            if (updatedVolumeLink != null) {
                if (updatedVolumeLink != existingSeriesLink) {
                    comicDao.updateSeriesExternalId(updatedVolumeLink)
                }
            } else {
                comicDao.insertSeriesExternalId(
                    SeriesExternalId(
                        seriesId = seriesId,
                        source = source,
                        externalId = volumeId,
                        url = volumeUrl
                    )
                )
            }

            val localIssues = comicDao.getIssuesForSeries(seriesId)
            val localIssueIds = localIssues.map { it.id }.toSet()

            val localIssuesByComicVineId = localIssues.mapNotNull { issue ->
                comicDao.getExternalIdsForIssue(issue.id)
                    .firstOrNull { it.source == source }
                    ?.externalId
                    ?.let { it to issue }
            }.toMap()

            val matchedLocalIssueIds = mutableSetOf<Long>()

            sourceIssues.forEach { sourceIssue ->
                val externalId = sourceIssue.comicVineIssueId.toString()

                val globallyLinkedId =
                    comicDao.getExternalId(source, externalId)

                require(
                    globallyLinkedId == null || globallyLinkedId.issueId in localIssueIds
                ) {
                    "Comic Vine issue $externalId is linked to another local issue"
                }

                val linkedLocalIssue =
                    localIssuesByComicVineId[externalId]

                if (linkedLocalIssue != null &&
                    linkedLocalIssue.id in matchedLocalIssueIds
                ) {
                   throw IllegalStateException(
                       "A local issue matched more than one Comic Vine issue"
                   )
                }

                val localIssue = linkedLocalIssue
                    ?: localIssues.firstOrNull { issue ->
                        issue.id !in matchedLocalIssueIds &&
                            issue.issueNumber.trim().equals(
                                sourceIssue.issueNumber.trim(),
                                ignoreCase = true
                            )
                    }

                val normalizedIssueNumber = sourceIssue.issueNumber.trim()
                val normalizedIssueTitle = sourceIssue.title?.trim()?.takeIf { it.isNotEmpty() }
                val normalizedPublicationDate = sourceIssue.publicationDate
                    ?.trim()?.takeIf { it.isNotEmpty() }
                val normalizedCoverUrl = sourceIssue.coverUrl?.trim()?.takeIf { it.isNotEmpty() }

                val issueId = if (localIssue == null) {
                    comicDao.insertIssue(
                        Issue(
                            seriesId = seriesId,
                            universeId = preservedSeriesUniverseId,
                            issueNumber = normalizedIssueNumber,
                            title = normalizedIssueTitle,
                            publicationDate = normalizedPublicationDate,
                            coverUrl = normalizedCoverUrl,
                            description = null,
                            issueType = sourceIssue.issueType,
                            isIssueTypeManuallyOverridden = false
                        )
                    )
                } else {
                    matchedLocalIssueIds += localIssue.id

                    comicDao.updateIssue(
                        localIssue.copy(
                            universeId = preservedSeriesUniverseId
                                ?: localIssue.universeId,
                            issueNumber = normalizedIssueNumber,
                            title = normalizedIssueTitle ?: localIssue.title,
                            publicationDate = normalizedPublicationDate
                                ?: localIssue.publicationDate,
                            coverUrl = normalizedCoverUrl ?: localIssue.coverUrl,
                            issueType = if (
                                localIssue.isIssueTypeManuallyOverridden
                            ) {
                                localIssue.issueType
                            } else {
                                sourceIssue.issueType
                            }
                        )
                    )

                    localIssue.id
                }

                if (globallyLinkedId == null) {
                    comicDao.insertExternalId(
                        ExternalId(
                            issueId = issueId,
                            source = source,
                            externalId = externalId,
                            url = sourceIssue.siteDetailsUrl
                        )
                    )
                } else if (
                    sourceIssue.siteDetailsUrl != null &&
                    globallyLinkedId.url != sourceIssue.siteDetailsUrl
                ) {
                    comicDao.updateExternalId(
                        globallyLinkedId.copy(
                            url = sourceIssue.siteDetailsUrl
                        )
                    )
                }
            }

            if (preservedSeriesUniverseId != null) {
                localIssues
                    .filter { it.id !in matchedLocalIssueIds }
                    .filter {
                        it.universeId != preservedSeriesUniverseId
                    }
                    .forEach { issue ->
                        comicDao.updateIssue(
                            issue.copy(
                                universeId = preservedSeriesUniverseId
                            )
                        )
                    }
            }

            reconcileScheduledReleasesForSeries(
                seriesId = seriesId
            )
        }
    }

    private suspend fun reconcileScheduledReleasesForSeries(
        seriesId: Long
    ) {
        val metronSeriesLink =
            comicDao.getSeriesExternalIdsForSeries(seriesId)
                .firstOrNull { it.source == "METRON" }
                ?: return

        val scheduledReleases =
            comicDao.getScheduledReleasesForExternalSeries(
                providerId = metronSeriesLink.source,
                externalSeriesId = metronSeriesLink.externalId
            )

        if (scheduledReleases.isEmpty()) {
            return
        }

        val localIssues =
            comicDao.getIssuesForSeries(seriesId)

        val localIssueIds =
            localIssues.map { it.id }.toSet()

        scheduledReleases.forEach { release ->

            val existingExternalId =
                comicDao.getExternalId(
                    source = metronSeriesLink.source,
                    externalId = release.externalIssueId
                )

            if (existingExternalId != null) {

                if (existingExternalId.issueId in localIssueIds) {
                    val updatedUrl =
                        release.sourceUrl
                            ?.takeIf { it.isNotBlank() }

                    if (
                        updatedUrl != null &&
                        updatedUrl != existingExternalId.url
                    ) {
                        comicDao.updateExternalId(
                            existingExternalId.copy(
                                url = updatedUrl
                            )
                        )
                    }
                }

                return@forEach
            }

            val releaseIssueNumber =
                release.issueNumberSnapshot
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: return@forEach

            val matchingIssues =
                localIssues.filter { issue ->
                    issue.issueNumber.trim().equals(
                        releaseIssueNumber,
                        ignoreCase = true
                    )
                }

            if (matchingIssues.size != 1) {
                return@forEach
            }

            comicDao.insertExternalId(
                ExternalId(
                    issueId = matchingIssues.single().id,
                    source = metronSeriesLink.source,
                    externalId = release.externalIssueId,
                    url = release.sourceUrl
                )
            )
        }
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
        universeId: Long?,
        style: ReadingListStyle = ReadingListStyle.GREEN
    ): Long {
        val currentTime = System.currentTimeMillis()
        return comicDao.insertReadingList(
            ReadingList(
                title = title,
                description = description,
                publisherId = publisherId,
                universeId = universeId,
                style = style,
                source = ReadingListSource.USER,
                sourceKey = null,
                createdAt = currentTime,
                updatedAt = currentTime
            )
        )
    }

    suspend fun createUserReadingListWithIssues(
        title: String,
        description: String?,
        publisherId: Long,
        universeId: Long?,
        style: ReadingListStyle = ReadingListStyle.GREEN,
        issueIds: List<Long>
    ): Long {
        var resultReadingListId: Long? = null

        database.withWriteTransaction {
            val trimmedTitle = title.trim()

            require(
                trimmedTitle.isNotEmpty()
            ) {
                "Reading-list title cannot be blank"
            }

            require(
                issueIds.distinct().size == issueIds.size
            ) {
                "Reading-list issues IDs contain duplicates"
            }

            val currentTime = System.currentTimeMillis()

            val readingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = trimmedTitle,
                        description = description
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() },
                        publisherId = publisherId,
                        universeId = universeId,
                        style = style,
                        source = ReadingListSource.USER,
                        sourceKey = null,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    )
                )

            issueIds.forEachIndexed { index, issueId ->
                val issue =
                    comicDao.getIssueById(issueId)
                        ?: throw IllegalArgumentException(
                            "Issue $issueId does not exist"
                        )

                val series =
                    comicDao.getSeriesById(issue.seriesId)
                        ?: throw IllegalArgumentException(
                            "Series ${issue.seriesId} does not exist"
                        )

                require(
                    series.publisherId == publisherId
                ) {
                    "Issue $issueId belongs to a different publisher"
                }

                comicDao.insertReadingListItem(
                    ReadingListItem(
                        readingListId = readingListId,
                        sectionId = null,
                        issueId = issueId,
                        position = index + 1,
                        required = true,
                        notes = null
                    )
                )
            }

            val resolvedUniverseId =
                resolveReadingListUniverseId(
                    readingListId = readingListId,
                    fallbackUniverseId = universeId
                )

            if (resolvedUniverseId != universeId) {
                val createdReadingList =
                    comicDao.getReadingListById(readingListId)
                        ?: throw IllegalArgumentException(
                            "Created reading list $readingListId does not exist"
                        )

                comicDao.updateReadingList(
                    createdReadingList.copy(
                        universeId = resolvedUniverseId
                    )
                )
            }

            resultReadingListId = readingListId
        }

        return checkNotNull(resultReadingListId) {
            "Created reading-list ID was not set"
        }
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

    suspend fun getUniverseIdsForReadingList(
        readingListId: Long
    ): List<Long?> {
        return comicDao
            .getUniverseIdsForReadingList(readingListId)
    }

    suspend fun updateReadingList(readingList: ReadingList) {
        comicDao.updateReadingList((readingList))
    }

    suspend fun deleteReadingList(readingList: ReadingList) {
        comicDao.deleteReadingList(readingList)
    }

    suspend fun deleteUserReadingList(
        readingListId: Long
    ) {
        val readingList =
            comicDao.getReadingListById(readingListId)
                ?: throw IllegalArgumentException(
                    "Reading list $readingListId does not exist"
                )

        require(readingList.source == ReadingListSource.USER) {
            "Reading list $readingListId is not user-owned"
        }

        comicDao.deleteReadingList(readingList)
    }

    fun getReadingListSummaries(): Flow<List<ReadingListSummary>> {
        return comicDao.getReadingListSummaries()
    }

    suspend fun updateUserReadingListDetails(
        readingListId: Long,
        title: String,
        description: String?,
        style: ReadingListStyle
    ) {
        val readingList =
            comicDao.getReadingListById(readingListId)
                ?: throw IllegalArgumentException(
                    "Reading list $readingListId does not exist"
                )

        require(readingList.source == ReadingListSource.USER) {
            "Reading list $readingListId is not user-owned"
        }

        val trimmedTitle = title.trim()

        require(trimmedTitle.isNotEmpty()) {
            "Reading-list title cannot be blank"
        }

        val trimmedDescription = description?.trim()?.takeIf { it.isNotEmpty() }
        val updatedAt = maxOf(System.currentTimeMillis(), readingList.updatedAt + 1)

        comicDao.updateReadingList(
            readingList.copy(
                title = trimmedTitle,
                description = trimmedDescription,
                style = style,
                updatedAt = updatedAt
            )
        )
    }

    suspend fun updateUserReadingListWithIssues(
        readingListId: Long,
        title: String,
        description: String?,
        style: ReadingListStyle,
        issueIds: List<Long>
    ) {
        database.withWriteTransaction {
            val readingList =
                comicDao.getReadingListById(readingListId)
                    ?: throw IllegalArgumentException(
                        "Reading list $readingListId does not exist"
                    )

            require(
                readingList.source == ReadingListSource.USER
            ) {
                "Reading list $readingListId is not user-owned"
            }

            val trimmedTitle = title.trim()

            require(
                trimmedTitle.isNotEmpty()
            ) {
                "Reading-list title cannot be blank"
            }

            require(
                issueIds.distinct().size == issueIds.size
            ) {
                "Reading-list issue IDs contain duplicates"
            }

            val currentItems =
                comicDao.getItemsForReadingList(readingListId)

            val currentItemsByIssueId =
                currentItems.associateBy { item ->
                    item.issueId
                }

            issueIds.forEach { issueId ->
                val issue =
                    comicDao.getIssueById(issueId)
                        ?: throw IllegalArgumentException(
                            "Issue $issueId does not exist"
                        )

                val series =
                    comicDao.getSeriesById(issue.seriesId)
                        ?: throw IllegalArgumentException(
                            "Series ${issue.seriesId} does not exist"
                        )

                require(
                    series.publisherId == readingList.publisherId
                ) {
                    "Issues $issueId belongs to a different publisher"
                }
            }

            val sections =
                comicDao.getSectionsForReadingList(readingListId)

            val sectionOrder =
                sections
                    .mapIndexed { index, section ->
                        section.id to index
                    }
                    .toMap()

            val unsectionedOrder = sections.size

            val desiredSectionOrder =
                issueIds.map { issueId ->
                    val sectionId =
                        currentItemsByIssueId[issueId]?.sectionId

                    if (sectionId == null) {
                        unsectionedOrder
                    } else {
                        sectionOrder[sectionId]
                            ?: throw IllegalArgumentException(
                                "Section $sectionId does not belong to reading list $readingListId"
                            )
                    }
                }

            require(
                desiredSectionOrder.zipWithNext().all { (first, second) -> first <= second }
            ) {
                "Items cannot move between sections while editing reading list"
            }

            val desiredIssueIds =
                issueIds.toSet()

            val removedItems =
                currentItems.filter { item ->
                    item.issueId !in desiredIssueIds
                }

            removedItems.forEach { item ->
                comicDao.deleteReadingListItem(item)
            }

            val retainedItems =
                currentItems.filter { item ->
                    item.issueId in desiredIssueIds
                }

            retainedItems.forEachIndexed { index, item ->
                comicDao.updateReadingListItem(
                    item.copy(
                        position = -(index + 1)
                    )
                )
            }

            issueIds.forEachIndexed { index, issueId ->
                val existingItem =
                    currentItemsByIssueId[issueId]

                if (existingItem != null) {
                    comicDao.updateReadingListItem(
                        existingItem.copy(
                            position = index + 1
                        )
                    )
                } else {
                    comicDao.insertReadingListItem(
                        ReadingListItem(
                            readingListId = readingListId,
                            sectionId = null,
                            issueId = issueId,
                            position = index + 1,
                            required = true,
                            notes = null
                        )
                    )
                }
            }

            val trimmedDescription =
                description
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

            val updatedAt =
                maxOf(
                    System.currentTimeMillis(),
                    readingList.updatedAt + 1
                )

            val resolvedUniverseId =
                resolveReadingListUniverseId(
                    readingListId = readingListId,
                    fallbackUniverseId = readingList.universeId
                )

            comicDao.updateReadingList(
                readingList.copy(
                    title = trimmedTitle,
                    description = trimmedDescription,
                    style = style,
                    universeId = resolvedUniverseId,
                    updatedAt = updatedAt
                )
            )
        }
    }

    suspend fun duplicateReadingList(
        readingListId: Long
    ): Long {
        var duplicatedReadingListId: Long? = null

        database.withWriteTransaction {
            val original =
                comicDao.getReadingListById(readingListId)
                    ?: throw IllegalArgumentException(
                        "Reading list $readingListId does not exist"
                    )

            val originalSections =
                comicDao.getSectionsForReadingList(readingListId)

            val originalItems=
                comicDao.getItemsForReadingList(readingListId)

            val currentTime = System.currentTimeMillis()

            val newReadingListId =
                comicDao.insertReadingList(
                    ReadingList(
                        title = "${original.title} Copy",
                        description = original.description,
                        publisherId = original.publisherId,
                        universeId = original.universeId,
                        source = ReadingListSource.USER,
                        sourceKey = null,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    )
                )

            val duplicatedSectionIds = mutableMapOf<Long, Long>()

            originalSections.forEach { section ->
                val newSectionId =
                    comicDao.insertReadingListSection(
                        ReadingListSection(
                            readingListId = newReadingListId,
                            title = section.title,
                            description = section.description,
                            position = section.position
                        )
                    )

                duplicatedSectionIds[section.id] = newSectionId
            }

            originalItems.forEach { item ->
                val newSectionId =
                    item.sectionId?.let { originalSectionId ->
                        duplicatedSectionIds[originalSectionId]
                            ?: throw IllegalStateException(
                                "Section $originalSectionId was not duplicated"
                            )
                    }

                comicDao.insertReadingListItem(
                    ReadingListItem(
                        readingListId = newReadingListId,
                        sectionId = newSectionId,
                        issueId = item.issueId,
                        position = item.position,
                        required = item.required,
                        notes = item.notes
                    )
                )
            }

            duplicatedReadingListId = newReadingListId
        }

        return checkNotNull(
            duplicatedReadingListId
        ) {
            "Duplicated reading-list ID was not set"
        }
    }

    fun getReadingListsContainingIssue(
        issueId: Long
    ): Flow<List<IssueReadingListResult>> {
        return comicDao.getReadingListsContainingIssue(
            issueId = issueId
        )
    }

    fun getReadingListArtworkCovers():
    Flow<List<ReadingListArtworkCover>> {
        return comicDao.getReadingListArtworkCovers()
    }


    // Reading list sections

    suspend fun addReadingListSection(
        section: ReadingListSection
    ): Long {
        return comicDao.insertReadingListSection(section)
    }

    suspend fun createUserReadingListSection(
        readingListId: Long,
        title: String,
        description: String?
    ): Long {
        var resultSectionId: Long? = null

        database.withWriteTransaction {
            val readingList = comicDao.getReadingListById(readingListId)
                ?: throw IllegalArgumentException(
                    "Reading list $readingListId does not exist"
                )

            require(readingList.source == ReadingListSource.USER) {
                "Reading list $readingListId is not user-owned"
            }

            val trimmedTitle = title.trim()

            require(trimmedTitle.isNotEmpty()) {
                "Section title cannot be blank"
            }

            val sections = comicDao.getSectionsForReadingList(readingListId)
            val nextPosition = sections.maxOfOrNull { section ->
                section.position
            }
                ?.plus(1)
                ?: 1

            resultSectionId =
                comicDao.insertReadingListSection(
                    ReadingListSection(
                        readingListId = readingListId,
                        title = trimmedTitle,
                        description = description?.trim()
                            ?.takeIf { it.isNotEmpty() },
                        position = nextPosition
                    )
                )

            val updatedAt = maxOf(System.currentTimeMillis(), readingList.updatedAt + 1)

            comicDao.updateReadingList(
                readingList.copy(
                    updatedAt = updatedAt
                )
            )
        }

        return checkNotNull(resultSectionId) {
            "Reading-list section ID was not set"
        }
    }

    suspend fun getSectionsForReadingList(
        readingListId: Long
    ): List<ReadingListSection> {
        return comicDao.getSectionsForReadingList(readingListId)
    }

    private suspend fun normalizeReadingListSectionPositions(
        readingListId: Long,
        orderedItems: List<ReadingListItem>
    ) {
        val sections = comicDao.getSectionsForReadingList(readingListId)

        if (sections.isEmpty()) {
            return
        }

        val sectionsWithIssues = orderedItems
            .mapNotNull { item -> item.sectionId }
            .distinct()

        val emptySectionIds = sections
            .filter { section -> section.id !in sectionsWithIssues }
            .sortedBy { section -> section.position }
            .map { section -> section.id }

        val orderedSectionIds = sectionsWithIssues + emptySectionIds

        val sectionsById = sections.associateBy { section -> section.id }

        orderedSectionIds.forEachIndexed { index, sectionId ->
            val section = checkNotNull(sectionsById[sectionId])

            comicDao.updateReadingListSection(
                section.copy(position = -(index + 1))
            )
        }

        orderedSectionIds.forEachIndexed { index, sectionId ->
            val section = checkNotNull(sectionsById[sectionId])

            comicDao.updateReadingListSection(
                section.copy(position = index + 1)
            )
        }
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

                val resolvedUniverseId =
                    resolveReadingListUniverseId(
                        readingListId = readingListId,
                        fallbackUniverseId = readingList.universeId
                    )

                val updatedAt =
                    maxOf(System.currentTimeMillis(), readingList.updatedAt + 1)

                comicDao.updateReadingList(
                    readingList.copy(
                        universeId = resolvedUniverseId,
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

                val resolvedUniverseId =
                    resolveReadingListUniverseId(
                        readingListId = readingListId,
                        fallbackUniverseId = readingList.universeId
                    )

                comicDao.updateReadingList(
                    readingList.copy(
                        universeId = resolvedUniverseId,
                        updatedAt = updatedAt
                    )
                )

                wasRemoved = true
            }
        }

        return wasRemoved
    }

    suspend fun moveUserReadingListItemToSection(
        readingListId: Long,
        readingListItemId: Long,
        targetSectionId: Long?
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
            val currentIndex = currentItems.indexOfFirst { item ->
                item.id == readingListItemId
            }

            require(currentIndex >= 0) {
                "Reading-list item $readingListItemId does " +
                "not exist in reading list $readingListId"
            }

            val movedItem = currentItems[currentIndex]
            val sections = comicDao.getSectionsForReadingList(readingListId)

            val targetSection =
                if (targetSectionId == null) {
                    null
                } else {
                    sections.firstOrNull {section ->
                        section.id == targetSectionId
                    }
                        ?: throw IllegalArgumentException(
                            "Section $targetSectionId does not " +
                            "belong to reading list " +
                            "$readingListId"
                        )
                }

            if (movedItem.sectionId == targetSectionId) {
                return@withWriteTransaction
            }

            val remainingItems = currentItems.filter { item -> item.id != readingListItemId }
            val insertionIndex =
                when {
                    targetSection == null -> remainingItems.size

                    else -> {
                        val lastTargetIndex = remainingItems.indexOfLast { item ->
                            item.sectionId == targetSection.id
                        }

                        if (lastTargetIndex >= 0) {
                            lastTargetIndex + 1
                        } else {
                            currentIndex.coerceAtMost(remainingItems.size)
                        }
                    }
                }

            val reorderedItems = remainingItems.toMutableList().apply {
                add(
                    insertionIndex,
                    movedItem.copy(sectionId = targetSectionId)
                )
            }

            currentItems.forEachIndexed { index, item ->
                comicDao.updateReadingListItem(
                    item.copy(position = -(index + 1))
                )
            }

            reorderedItems.forEachIndexed { index, item ->
                comicDao.updateReadingListItem(
                    item.copy(position = index + 1)
                )
            }

            normalizeReadingListSectionPositions(
                readingListId = readingListId,
                orderedItems = reorderedItems
            )

            val updatedAt = maxOf(System.currentTimeMillis(), readingList.updatedAt + 1)
            comicDao.updateReadingList(readingList.copy(updatedAt = updatedAt))
        }
    }

    suspend fun moveUserReadingListSection(
        readingListId: Long,
        sectionId: Long,
        offset: Int
    ) {
        require(offset == -1 || offset == 1) {
            "Section move offset must be -1 or 1"
        }

        database.withWriteTransaction {
            val readingList = comicDao.getReadingListById(readingListId)
                ?: throw IllegalArgumentException(
                    "Reading list $readingListId does not exist"
                )

            require(readingList.source == ReadingListSource.USER) {
                "Reading list $readingListId is not user-owned"
            }

            val sections = comicDao.getSectionsForReadingList(readingListId)

            require(sections.any { it.id == sectionId }) {
                "Section $sectionId does not belong to reading list $readingListId"
            }

            val currentItems = comicDao.getItemsForReadingList(readingListId)

            val groups = mutableListOf<List<ReadingListItem>>()

            var index = 0

            while (index < currentItems.size) {
                val item = currentItems[index]

                if (item.sectionId == null) {
                    groups.add(listOf(item))
                    index++
                } else {
                    val groupSectionId = item.sectionId
                    val groupStart = index

                    while (
                        index < currentItems.size &&
                        currentItems[index].sectionId == groupSectionId
                    ) {
                        index++
                    }

                    groups.add(
                        currentItems.subList(groupStart, index)
                    )
                }
            }

            val currentGroupIndex =
                groups.indexOfFirst { group ->
                    group.any { item -> item.sectionId == sectionId }
                }

            if (currentGroupIndex < 0) {
                return@withWriteTransaction
            }

            val targetGroupIndex = currentGroupIndex + offset

            if (targetGroupIndex !in groups.indices) {
                return@withWriteTransaction
            }

            val reorderedGroups = groups.toMutableList()
            val movedGroup = reorderedGroups.removeAt(currentGroupIndex)

            reorderedGroups.add(targetGroupIndex, movedGroup)

            val reorderedItems = reorderedGroups.flatten()

            currentItems.forEachIndexed { itemIndex, item ->
                comicDao.updateReadingListItem(
                    item.copy(position = -(itemIndex + 1))
                )
            }

            reorderedItems.forEachIndexed { itemIndex, item ->
                comicDao.updateReadingListItem(
                    item.copy(position = itemIndex + 1)
                )
            }

            normalizeReadingListSectionPositions(
                readingListId = readingListId,
                orderedItems = reorderedItems
            )

            val updatedAt = maxOf(System.currentTimeMillis(), readingList.updatedAt + 1)

            comicDao.updateReadingList(
                readingList.copy(updatedAt = updatedAt)
            )
        }
    }

    suspend fun reorderUserReadingListItems(
        readingListId: Long,
        orderedItemIds: List<Long>,
        detachedItemIds: Set<Long> = emptySet()
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

            require(detachedItemIds.all { it in currentItemsById.keys }) {
                "Detached item IDs do not match reading list"
            }

            val reorderedItems =
                orderedItemIds.map { itemId ->
                    val item = checkNotNull(currentItemsById[itemId])

                    if (itemId in detachedItemIds) {
                        item.copy(sectionId = null)
                    } else {
                        item
                    }
                }

            val sectionIds = reorderedItems.mapNotNull { item -> item.sectionId }.distinct()

            sectionIds.forEach { sectionId ->
                val indices =
                    reorderedItems.indices.filter { index ->
                        reorderedItems[index].sectionId == sectionId
                    }

                require(indices.last() - indices.first() + 1 == indices.size) {
                    "Items from a section must remain contiguous"
                }
            }

            if (
                currentItems.map { it.id } == orderedItemIds &&
                detachedItemIds.isEmpty()
            ) {
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

            normalizeReadingListSectionPositions(
                readingListId = readingListId,
                orderedItems = reorderedItems
            )

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


    // Issue ownership

    fun isIssueOwned(
        issueId: Long
    ): Flow<Boolean> {
        return comicDao.isIssueOwned(issueId)
    }

    suspend fun markIssueOwned(
        issueId: Long
    ) {
        comicDao.markIssueOwned(
            IssueOwnership(issueId = issueId)
        )
    }

    suspend fun markIssueUnowned(
        issueId: Long
    ) {
        comicDao.markIssueUnowned(issueId)
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

            val latestCompletionTime = System.currentTimeMillis()

            val earliestCompletionTime =
                latestCompletionTime - distinctIssueIds.lastIndex.toLong()

            val updateProgress =
                distinctIssueIds.mapIndexed { index, issueId ->

                    val completionTime =
                        earliestCompletionTime + index

                    val existing =
                        progressByIssueId[issueId]

                    if (existing == null) {
                        ReadingProgress(
                            issueId = issueId,
                            status = ReadingStatus.READ,
                            startedAt = completionTime,
                            completedAt = completionTime,
                            notes = null
                        )
                    } else {
                        existing.copy(
                            status = ReadingStatus.READ,
                            startedAt =
                                existing.startedAt
                                    ?: completionTime,
                            completedAt = completionTime
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

    fun getRecentlyReadIssues():
        Flow<List<RecentReadIssue>> {
        return comicDao.getRecentlyReadIssues()
    }


    // Browsing

    fun searchSeries(
        query: String,
        publisherId: Long? = null,
        universeId: Long? = null,
        publicationYear: Int? = null
    ): Flow<List<SeriesSearchResult>> {
        return comicDao.searchSeries(
            query = query,
            publisherId = publisherId,
            universeId = universeId,
            publicationYear = publicationYear
        )
    }

    fun searchIssues(
        query: String,
        publisherId: Long? = null,
        universeId: Long? = null,
        publicationYear: Int? = null
    ): Flow<List<IssueSearchResult>> {
        return comicDao.searchIssues(
            query = query,
            publisherId = publisherId,
            universeId = universeId,
            publicationYear = publicationYear
        )
    }

    fun getOwnedIssues(): Flow<List<OwnedIssueResult>> {
        return comicDao.getOwnedIssues()
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

    suspend fun getManualIssueTypeOverridesForComicVineIssues(
        comicVineIssueIds: List<Long>
    ): Map<Long, IssueType> {
        val requestedIds = comicVineIssueIds.toSet()

        if (requestedIds.isEmpty()) {
            return emptyMap()
        }

        return comicDao.getManualIssueTypeOverrides(
            source = "COMIC_VINE"
        ).mapNotNull { savedOverride ->
            savedOverride.externalId
                .toLongOrNull()
                ?.takeIf { it in requestedIds }
                ?.let { comicVineIssueId ->
                    comicVineIssueId to savedOverride.issueType
                }
        }.toMap()
    }

    private suspend fun resolveReadingListUniverseId(
        readingListId: Long,
        fallbackUniverseId: Long?
    ): Long? {
        val universeIds =
            comicDao
                .getUniverseIdsForReadingList(
                    readingListId
                )
                .distinct()

        return when {
            universeIds.isEmpty() ->
                fallbackUniverseId

            universeIds.size == 1 ->
                universeIds.single()

            else ->
                null
        }
    }
}