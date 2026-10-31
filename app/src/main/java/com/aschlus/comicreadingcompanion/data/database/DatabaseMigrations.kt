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

val MIGRATION_3_4 =
    object : Migration(
        startVersion = 3,
        endVersion = 4
    ) {
        override suspend fun migrate(
            connection: SQLiteConnection
        ) {
            connection.execSQL(
                """
                ALTER TABLE reading_lists
                ADD COLUMN style TEXT
                NOT NULL DEFAULT 'GREEN'
                """.trimIndent()
            )
        }
    }

val MIGRATION_4_5 =
    object : Migration(
        startVersion = 4,
        endVersion = 5
    ) {
        override suspend fun migrate(
            connection: SQLiteConnection
        ) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS issue_ownership (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    issueId INTEGER NOT NULL,
                    FOREIGN KEY(issueId)
                        REFERENCES issues(id)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE
                )
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                    index_issue_ownership_issueId
                ON issue_ownership (issueId)
                """.trimIndent()
            )
        }
    }

val MIGRATION_5_6 =
    object: Migration(
        startVersion = 5,
        endVersion = 6
    ) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                    ALTER TABLE issues
                    ADD COLUMN isIssueTypeManuallyOverridden
                    INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )
        }
    }

val MIGRATION_6_7 =
    object : Migration(
        startVersion = 6,
        endVersion = 7
    ) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                    ALTER TABLE series
                    ADD COLUMN universeId INTEGER
                    REFERENCES universes(id)
                    ON DELETE SET NULL
                """.trimIndent()
            )

            connection.execSQL(
                """
                    CREATE INDEX IF NOT EXISTS
                    index_series_universeId
                    ON series(universeId)
                """.trimIndent()
            )
        }
    }


val MIGRATION_7_8 =
    object : Migration(
        startVersion = 7,
        endVersion = 8
    ) {
        override suspend fun migrate(
            connection: SQLiteConnection
        ) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS scheduled_releases (
                    providerId TEXT NOT NULL,
                    externalIssueId TEXT NOT NULL,
                    externalSeriesId TEXT NOT NULL,
                    seriesTitleSnapshot TEXT NOT NULL,
                    seriesVolumeSnapshot INTEGER,
                    seriesStartYearSnapshot INTEGER,
                    publisherNameSnapshot TEXT,
                    issueNumberSnapshot TEXT,
                    displayTitleSnapshot TEXT,
                    coverDate TEXT,
                    storeReleaseDate TEXT,
                    focDate TEXT,
                    price TEXT,
                    priceCurrency TEXT,
                    distributorSku TEXT,
                    upc TEXT,
                    comicVineId TEXT,
                    gcdId TEXT,
                    sourceUrl TEXT,
                    sourceModifiedAt TEXT,
                    lastSyncedAt INTEGER NOT NULL,
                    PRIMARY KEY(providerId, externalIssueId)
                )
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                    index_scheduled_releases_externalSeriesId
                ON scheduled_releases(externalSeriesId)
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                    index_scheduled_releases_storeReleaseDate
                ON scheduled_releases(storeReleaseDate)
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                    index_scheduled_releases_focDate
                ON scheduled_releases(focDate)
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                    index_scheduled_releases_seriesTitleSnapshot
                ON scheduled_releases(seriesTitleSnapshot)
                """.trimIndent()
            )
        }
    }

val MIGRATION_8_9 =
    object : Migration(
        startVersion = 8,
        endVersion = 9
    ) {
        override suspend fun migrate(
            connection: SQLiteConnection
        ) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS
                release_series_metadata (
                    providerId TEXT NOT NULL,
                    externalSeriesId TEXT NOT NULL,
                    comicVineId TEXT,
                    gcdId TEXT,
                    sourceUrl TEXT,
                    sourceModifiedAt TEXT,
                    lastFetchedAt INTEGER NOT NULL,
                    PRIMARY KEY (
                        providerId,
                        externalSeriesId
                    )
                )
                """.trimIndent()
            )
        }
    }

val MIGRATION_9_10 =
    object : Migration(
        startVersion = 9,
        endVersion = 10
    ) {
        override suspend fun migrate(
            connection: SQLiteConnection
        ) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS
                scheduled_release_sync_state (
                    providerId TEXT NOT NULL,
                    fromDate TEXT NOT NULL,
                    toDate TEXT NOT NULL,
                    lastSuccessfulSyncAt INTEGER NOT NULL,
                    PRIMARY KEY (
                        providerId,
                        fromDate,
                        toDate
                    )
                )
                """.trimIndent()
            )
        }
    }

val MIGRATION_10_11 =
    object : Migration(
        startVersion = 10,
        endVersion = 11
    ) {
        override suspend fun migrate(
            connection: SQLiteConnection
        ) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS
                scheduled_release_changes (
                    id INTEGER PRIMARY KEY
                        AUTOINCREMENT NOT NULL,
                    providerId TEXT NOT NULL,
                    externalIssueId TEXT NOT NULL,
                    externalSeriesId TEXT NOT NULL,
                    seriesTitleSnapshot TEXT NOT NULL,
                    issueNumberSnapshot TEXT,
                    previousStoreReleaseDate TEXT,
                    newStoreReleaseDate TEXT,
                    previousFocDate TEXT,
                    newFocDate TEXT,
                    sourceModifiedAt TEXT,
                    detectedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                index_scheduled_release_changes_providerId_externalIssueId_detectedAt
                ON scheduled_release_changes (
                    providerId,
                    externalIssueId,
                    detectedAt
                )
                """.trimIndent()
            )
        }
    }
