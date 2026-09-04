package com.aschlus.comicreadingcompanion.data.database

import com.aschlus.comicreadingcompanion.data.database.entities.Issue
import com.aschlus.comicreadingcompanion.data.database.entities.IssueType
import com.aschlus.comicreadingcompanion.data.database.entities.Publisher
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListItem
import com.aschlus.comicreadingcompanion.data.database.entities.Series
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import kotlinx.coroutines.flow.first

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
    }
}