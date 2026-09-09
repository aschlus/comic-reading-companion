package com.aschlus.comicreadingcompanion.data.database

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_1_2 =
    object: Migration(
        startVersion = 1,
        endVersion = 2
    ) {
        override suspend fun migrate(
            connection: SQLiteConnection
        ) {
            connection.execSQL(
                """
                ALTER TABLE reading_lists
                ADD COLUMN source TEXT
                NOT NULL DEFAULT 'BUNDLED'
                """.trimIndent()
            )

            connection.execSQL(
                """
                ALTER TABLE reading_lists
                ADD COLUMN sourceKey TEXT
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                index_publishers_name
                ON publishers(name)
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                index_universes_publisherId_designation
                ON universes(publisherId, designation)
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                index_reading_lists_sourcekey
                ON reading_lists(sourceKey)
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                index_reading_list_sections_readingListId_position
                ON reading_list_sections(
                    readingListId,
                    position
                )
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                index_reading_list_items_readingListId_position
                ON reading_list_items(
                    readingListId,
                    position
                )
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                index_reading_list_items_readingListId_issueId
                ON reading_list_items(
                    readingListId,
                    issueId
                )
                """.trimIndent()
            )
        }
    }

val MIGRATION_2_3 =
    object: Migration(
        startVersion = 2,
        endVersion = 3
    ) {
        override suspend fun migrate(
            connection: SQLiteConnection
        ) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS
                series_external_ids (
                    id INTEGER PRIMARY KEY
                        AUTOINCREMENT NOT NULL,
                    seriesId INTEGER NOT NULL,
                    source TEXT NOT NULL,
                    externalId TEXT NOT NULL,
                    url TEXT,
                    FOREIGN KEY(seriesId)
                        REFERENCES series(id)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE
                )
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                index_series_external_ids_seriesId
                ON series_external_ids(seriesId)
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                index_series_external_ids_source_externalId
                ON series_external_ids(
                    source,
                    externalId
                )
                """.trimIndent()
            )
        }
    }