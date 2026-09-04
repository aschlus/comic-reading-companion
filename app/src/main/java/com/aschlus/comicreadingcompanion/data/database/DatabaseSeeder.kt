package com.aschlus.comicreadingcompanion.data.database

import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListItem
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.Universe

class DatabaseSeeder(
    private val comicDao: ComicDao
) {

    suspend fun seed() {
        val publishers = comicDao.getAllPublishers()

        var marvel = publishers.find {
            it.name == "Marvel Comics"
        }

        if (marvel == null) {
            var marvelId = comicDao.insertPublisher(
                Publisher(
                    name = "Marvel Comics"
                )
            )

            marvel = Publisher(
                id = marvelId,
                name = "Marvel Comics"
            )
        }

        val universes = comicDao.getUniversesForPublisher(marvel.id)

        val earth616Exists = universes.any {
            it.designation == "Earth-616"
        }

        if (!earth616Exists) {
            comicDao.insertUniverse(
                Universe(
                    publisherId = marvel.id,
                    name = "Marvel Universe",
                    designation = "Earth-616",
                    description = "The primary Marvel Comics universe."
                )
            )
        }

        val updatedUniverses =
            comicDao.getUniversesForPublisher(marvel.id)

        val earth616 = updatedUniverses.find {
            it.designation == "Earth-616"
        }

        val readingLists = comicDao.getAllReadingLists()

        val spiderManListExists = readingLists.any {
            it.title == "Spider-Man Reading Order"
        }

        if (!spiderManListExists && earth616 != null) {
            val currentTime = System.currentTimeMillis()

            comicDao.insertReadingList(
                ReadingList(
                    title = "Spider-Man Reading Order",
                    description = "A development reading list for Spider-Man comics.",
                    publisherId = marvel.id,
                    universeId = earth616.id,
                    createdAt = currentTime,
                    updatedAt = currentTime
                )
            )
        }

        var amazingSpiderMan = comicDao.getSeries(
            publisherId = marvel.id,
            title = "Amazing Spider-Man",
            volume = 2
        )

        if (amazingSpiderMan == null) {
            val seriesId = comicDao.insertSeries(
                Series(
                    publisherId = marvel.id,
                    title = "Amazing Spider-Man",
                    volume = 2,
                    startYear = 1999,
                    endYear = 2013
                )
            )

            amazingSpiderMan = Series(
                id = seriesId,
                publisherId = marvel.id,
                title = "Amazing Spider-Man",
                volume = 2,
                startYear = 1999,
                endYear = 2013
            )
        }

        if (earth616 != null) {

            for (issueNumber in 1..5) {

                val issueNumberString = issueNumber.toString()

                val existingIssue = comicDao.getIssue(
                    seriesId = amazingSpiderMan.id,
                    issueNumber = issueNumberString
                )

                if (existingIssue == null) {
                    comicDao.insertIssue(
                        Issue(
                            seriesId = amazingSpiderMan.id,
                            universeId = earth616.id,
                            issueNumber = issueNumberString,
                            title = null,
                            publicationDate = null,
                            coverUrl = null,
                            description = null,
                            issueType = IssueType.REGULAR
                        )
                    )
                }
            }
        }

        val updatedReadingLists =
            comicDao.getAllReadingLists()

        val spiderManReadingList =
            updatedReadingLists.find {
                it.title == "Spider-Man Reading Order"
            }

        if (
            spiderManReadingList != null &&
            earth616 != null
        ) {
            for (issueNumber in 1..5) {

                val issue = comicDao.getIssue(
                    seriesId = amazingSpiderMan.id,
                    issueNumber = issueNumber.toString()
                )

                if (issue != null) {

                    val existingItem =
                        comicDao.getReadingListItem(
                            readingListId = spiderManReadingList.id,
                            issueId = issue.id
                        )

                    if (existingItem == null) {
                        comicDao.insertReadingListItem(
                            ReadingListItem(
                                readingListId = spiderManReadingList.id,
                                sectionId = null,
                                issueId = issue.id,
                                position = issueNumber,
                                required = true,
                                notes = null
                            )
                        )
                    }
                }
            }
        }
    }
}