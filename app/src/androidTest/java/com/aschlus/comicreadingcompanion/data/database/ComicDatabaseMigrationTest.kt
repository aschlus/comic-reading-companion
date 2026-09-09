package com.aschlus.comicreadingcompanion.data.database

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComicDatabaseMigrationTest {

    private val instrumentation =
        InstrumentationRegistry.getInstrumentation()

    private val testDatabaseName = "comic-migration-test"

    @get:Rule
    val migrationHelper =
        MigrationTestHelper(
            instrumentation = instrumentation,
            file = instrumentation
                .targetContext
                .getDatabasePath(testDatabaseName),
            driver = AndroidSQLiteDriver(),
            databaseClass = ComicDatabase::class
        )

    @Before
    fun setUp() {
        instrumentation
            .targetContext
            .deleteDatabase(testDatabaseName)
    }

    @After
    fun tearDown() {
        instrumentation
            .targetContext
            .deleteDatabase(testDatabaseName)
    }

    @Test
    fun migration1To2_preservesReadingListStructure() {
        runBlocking {
            val connection = migrationHelper.createDatabase(1)
            insertVersion1Fixture(connection)
            connection.close()

            val migrated =
                migrationHelper.runMigrationsAndValidate(
                    version = 2,
                    migrations = listOf(
                        MIGRATION_1_2
                    )
                )

            assertEquals(
                1L,
                queryCount(migrated, "SELECT COUNT(*) FROM publishers")
            )

            assertEquals(
                1L,
                queryCount(migrated, "SELECT COUNT(*) FROM universes")
            )

            assertEquals(
                1L,
                queryCount(migrated, "SELECT COUNT(*) FROM series")
            )

            assertEquals(
                1L,
                queryCount(migrated, "SELECT COUNT(*) FROM issues")
            )

            assertEquals(
                1L,
                queryCount(migrated, "SELECT COUNT(*) FROM reading_lists")
            )

            assertEquals(
                1L,
                queryCount(migrated, "SELECT COUNT(*) FROM reading_list_sections")
            )

            assertEquals(
                1L,
                queryCount(migrated, "SELECT COUNT(*) FROM reading_list_items")
            )

            assertEquals(
                1L,
                queryCount(
                    migrated,
                    """
                    SELECT COUNT(*)
                    FROM reading_lists
                    WHERE id = 1
                        AND source = 'BUNDLED'
                        AND sourceKey IS NULL
                    """.trimIndent()
                )
            )

            migrated.close()
        }
    }

    @Test
    fun migration1To2_preservesReadingProgressAndExternalIds() {
        runBlocking {
            val connection = migrationHelper.createDatabase(1)
            insertVersion1Fixture(connection)
            connection.close()

            val migrated =
                migrationHelper.runMigrationsAndValidate(
                    version = 2,
                    migrations = listOf(
                        MIGRATION_1_2
                    )
                )

            migrated.prepare(
                """
                    SELECT status,
                           startedAt,
                           completedAt,
                           notes
                    FROM reading_progress
                    WHERE issueId = 1
                """.trimIndent()
            )
                .use { statement ->
                    assertTrue(statement.step())
                    assertEquals("READ", statement.getText(0))
                    assertEquals(1000L, statement.getLong(1))
                    assertEquals(2000L, statement.getLong(2))
                    assertEquals("Great issue", statement.getText(3))
                }

            migrated.prepare(
                """
                    SELECT source,
                           externalId,
                           url
                    FROM external_ids
                    WHERE issueId = 1
                """.trimIndent()
            )
                .use { statement ->
                    assertTrue(statement.step())
                    assertEquals("COMIC_VINE", statement.getText(0))
                    assertEquals("12345", statement.getText(1))
                    assertEquals(
                        "https://example.com/issue",
                        statement.getText(2)
                    )
                }

            migrated.close()
        }
    }

    @Test
    fun migration1To2_enforceUniquePublisherNames() {
        runBlocking {
            val connection = migrationHelper.createDatabase(1)
            insertVersion1Fixture(connection)
            connection.close()

            val migrated =
                migrationHelper.runMigrationsAndValidate(
                    version = 2,
                    migrations = listOf(
                        MIGRATION_1_2
                    )
                )

            migrated.execSQL(
                """
                INSERT OR IGNORE INTO publishers (
                    name
                )
                VALUES (
                    'Marvel Comics'
                )
                """.trimIndent()
            )

            assertEquals(
                1L,
                queryCount(
                    migrated,
                    """
                    SELECT COUNT(*)
                    FROM publishers
                    WHERE name = 'Marvel Comics'
                    """.trimIndent()
                )
            )

            migrated.close()
        }
    }

    @Test
    fun migration1To2_enforcesUniqueUniverseDesignationWithinPublisher() {
        runBlocking {
            val connection = migrationHelper.createDatabase(1)
            insertVersion1Fixture(connection)
            connection.close()

            val migrated =
                migrationHelper.runMigrationsAndValidate(
                    version = 2,
                    migrations = listOf(
                        MIGRATION_1_2
                    )
                )

            migrated.execSQL(
                """
                INSERT OR IGNORE INTO universes (
                    publisherId,
                    name,
                    designation,
                    description
                )
                VALUES (
                    1,
                    'Duplicate Marvel Universe',
                    'Earth-616',
                    NULL
                )
                """.trimIndent()
            )

            assertEquals(
                1L,
                queryCount(
                    migrated,
                    """
                    SELECT COUNT(*)
                    FROM universes
                    WHERE publisherId = 1
                        AND designation = 'Earth-616'
                    """.trimIndent()
                )
            )

            migrated.execSQL(
                """
                INSERT INTO publishers (
                    id,
                    name
                )
                VALUES (
                    2,
                    'Test Publisher'
                )
                """.trimIndent()
            )

            migrated.execSQL(
                """
                INSERT INTO universes (
                    publisherId,
                    name,
                    designation,
                    description
                )
                VALUES (
                    2,
                    'Another Universe',
                    'Earth-616',
                    NULL
                )
                """.trimIndent()
            )

            assertEquals(
                2L,
                queryCount(
                    migrated,
                    """
                    SELECT COUNT (*)
                    FROM universes
                    WHERE designation = 'Earth-616'
                    """.trimIndent()
                )
            )

            migrated.close()
        }
    }

    @Test
    fun migration1To2_enforcesUniqueSectionPositionsWithinReadingList() {
        runBlocking {
            val connection = migrationHelper.createDatabase(1)
            insertVersion1Fixture(connection)
            connection.close()

            val migrated =
                migrationHelper.runMigrationsAndValidate(
                    version = 2,
                    migrations = listOf(
                        MIGRATION_1_2
                    )
                )

            migrated.execSQL(
                """
                INSERT OR IGNORE INTO reading_list_sections (
                    readingListId,
                    title,
                    description,
                    position
                )
                VALUES (
                    1,
                    'Duplicate Position',
                    NULL,
                    1
                )
                """.trimIndent()
            )

            assertEquals(
                1L,
                queryCount(
                    migrated,
                    """
                    SELECT COUNT (*)
                    FROM reading_list_sections
                    WHERE readingListId = 1
                        AND position = 1
                    """.trimIndent()
                )
            )

            migrated.execSQL(
                """
                INSERT INTO reading_lists (
                    id,
                    title,
                    description,
                    publisherId,
                    universeId,
                    createdAt,
                    updatedAt
                )
                VALUES (
                    2,
                    'Second Reading List',
                    NULL,
                    1,
                    1,
                    300,
                    300
                )
                """.trimIndent()
            )

            migrated.execSQL(
                """
                INSERT INTO reading_list_sections (
                    readingListId,
                    title,
                    description,
                    position
                )
                VALUES (
                    2,
                    'Position One',
                    NULL,
                    1
                )
                """.trimIndent()
            )

            assertEquals(
                2L,
                queryCount(
                    migrated,
                    """
                    SELECT COUNT (*)
                    FROM reading_list_sections
                    WHERE position = 1
                    """.trimIndent()
                )
            )

            migrated.close()
        }
    }

    @Test
    fun migration1To2_enforcesUniqueReadingListItemPositionAndIssue() {
        runBlocking {
            val connection = migrationHelper.createDatabase(1)
            insertVersion1Fixture(connection)
            connection.close()

            val migrated =
                migrationHelper.runMigrationsAndValidate(
                    version = 2,
                    migrations = listOf(
                        MIGRATION_1_2
                    )
                )

            migrated.execSQL(
                """
                INSERT INTO issues (
                    id,
                    seriesId,
                    universeId,
                    issueNumber,
                    title,
                    publicationDate,
                    coverUrl,
                    description,
                    issueType
                )
                VALUES (
                    2,
                    1,
                    1,
                    '2',
                    'Second Test Issue',
                    '1999-01',
                    NULL,
                    NULL,
                    'REGULAR'
                )
                """.trimIndent()
            )

            migrated.execSQL(
                """
                INSERT OR IGNORE INTO reading_list_items (
                    readingListId,
                    sectionId,
                    issueId,
                    position,
                    required,
                    notes
                )
                VALUES (
                    1,
                    1,
                    2,
                    1,
                    1,
                    NULL
                )
                """.trimIndent()
            )

            assertEquals(
                1L,
                queryCount(
                    migrated,
                    """
                    SELECT COUNT (*)
                    FROM reading_list_items
                    WHERE readingListId = 1
                    """.trimIndent()
                )
            )

            migrated.execSQL(
                """
                INSERT OR IGNORE INTO reading_list_items (
                    readingListId,
                    sectionId,
                    issueId,
                    position,
                    required,
                    notes
                )
                VALUES (
                    1,
                    1,
                    1,
                    2,
                    1,
                    NULL
                )
                """.trimIndent()
            )

            assertEquals(
                1L,
                queryCount(
                    migrated,
                    """
                    SELECT COUNT (*)
                    FROM reading_list_items
                    WHERE readingListId = 1
                    """.trimIndent()
                )
            )

            migrated.execSQL(
                """
                INSERT INTO reading_lists (
                    id,
                    title,
                    description,
                    publisherId,
                    universeId,
                    createdAt,
                    updatedAt
                )
                VALUES (
                    2,
                    'Second Reading List',
                    NULL,
                    1,
                    1,
                    300,
                    300
                )
                """.trimIndent()
            )

            migrated.execSQL(
                """
                INSERT OR IGNORE INTO reading_list_items (
                    readingListId,
                    sectionId,
                    issueId,
                    position,
                    required,
                    notes
                )
                VALUES (
                    2,
                    NULL,
                    1,
                    1,
                    1,
                    NULL
                )
                """.trimIndent()
            )

            assertEquals(
                2L,
                queryCount(
                    migrated,
                    """
                    SELECT COUNT (*)
                    FROM reading_list_items
                    WHERE issueId = 1
                    """.trimIndent()
                )
            )

            migrated.close()
        }
    }

    @Test
    fun migration1To2_enforcesUniqueReadingListSourceKey() {
        runBlocking {
            val connection = migrationHelper.createDatabase(1)
            insertVersion1Fixture(connection)
            connection.close()

            val migrated =
                migrationHelper.runMigrationsAndValidate(
                    version = 2,
                    migrations = listOf(
                        MIGRATION_1_2
                    )
                )

            migrated.execSQL(
                """
                UPDATE reading_lists
                SET sourceKey =
                    'bundled:spider_man_volume_2'
                WHERE id = 1
                """.trimIndent()
            )

            migrated.execSQL(
                """
                INSERT OR IGNORE INTO reading_lists (
                    title,
                    description,
                    publisherId,
                    universeId,
                    source,
                    sourceKey,
                    createdAt,
                    updatedAt
                )
                VALUES (
                    'Duplicate Source',
                    NULL,
                    1,
                    1,
                    'BUNDLED',
                    'bundled:spider_man_volume_2',
                    300,
                    300
                )
                """.trimIndent()
            )

            assertEquals(
                1L,
                queryCount(
                    migrated,
                    """
                    SELECT COUNT (*)
                    FROM reading_lists
                    WHERE sourceKey =
                        'bundled:spider_man_volume_2'
                    """.trimIndent()
                )
            )

            migrated.execSQL(
                """
                INSERT OR IGNORE INTO reading_lists (
                    title,
                    description,
                    publisherId,
                    universeId,
                    source,
                    sourceKey,
                    createdAt,
                    updatedAt
                )
                VALUES (
                    'My Custom List',
                    NULL,
                    1,
                    1,
                    'USER',
                    NULL,
                    400,
                    400
                )
                """.trimIndent()
            )

            assertEquals(
                1L,
                queryCount(
                    migrated,
                    """
                    SELECT COUNT (*)
                    FROM reading_lists
                    WHERE source = 'USER'
                        AND sourceKey IS NULL
                    """.trimIndent()
                )
            )

            migrated.close()
        }
    }

    @Test
    fun version2_freshDatabaseUsesReadingListSourceDefault() {
        runBlocking {
            val database = migrationHelper.createDatabase(2)

            database.execSQL(
                """
                INSERT INTO publishers (
                    id,
                    name
                )
                VALUES (
                    1,
                    'Marvel Comics'
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                INSERT INTO reading_lists (
                    id,
                    title,
                    description,
                    publisherId,
                    universeId,
                    createdAt,
                    updatedAt
                )
                VALUES (
                    1,
                    'Fresh Database List',
                    NULL,
                    1,
                    NULL,
                    100,
                    100
                )
                """.trimIndent()
            )

            database.prepare(
                """
                SELECT source,
                       sourceKey
                FROM reading_lists
                WHERE id = 1
                """.trimIndent()
            )
                .use { statement ->
                    assertTrue(statement.step())
                    assertEquals("BUNDLED", statement.getText(0))
                    assertTrue(statement.isNull(1))
                }

            database.close()
        }
    }

    @Test
    fun migration2To3_addsSeriesExternalIdsAndPreservesExistingData() {
        runBlocking {
            val connection = migrationHelper.createDatabase(2)

            connection.execSQL(
                """
                INSERT INTO publishers (
                    id,
                    name
                )
                VALUES (
                    1,
                    'Marvel Comics'
                )
                """.trimIndent()
            )

            connection.execSQL(
                """
                INSERT INTO series (
                    id,
                    publisherId,
                    title,
                    volume,
                    startYear,
                    endYear
                )
                VALUES (
                    1,
                    1,
                    'Amazing Spider-Man',
                    2,
                    1999,
                    2003
                )
                """.trimIndent()
            )

            connection.close()

            val migrated =
                migrationHelper
                    .runMigrationsAndValidate(
                        version = 3,
                        migrations = listOf(
                            MIGRATION_2_3
                        )
                    )

            assertEquals(
                1L,
                queryCount(
                    migrated,
                    """
                    SELECT COUNT(*)
                    FROM publishers
                    """.trimIndent()
                )
            )

            assertEquals(
                1L,
                queryCount(
                    migrated,
                    """
                    SELECT COUNT(*)
                    FROM series
                    """.trimIndent()
                )
            )

            migrated.execSQL(
                """
                INSERT INTO series_external_ids (
                    seriesId,
                    source,
                    externalId,
                    url
                )
                VALUES (
                    1,
                    'COMIC_VINE',
                    '2127',
                    'https://example.com/series'
                )
                """.trimIndent()
            )

            assertEquals(
                1L,
                queryCount(
                    migrated,
                    """
                    SELECT COUNT(*)
                    FROM series_external_ids
                    WHERE seriesId = 1
                        AND source = 'COMIC_VINE'
                        AND externalId = '2127'
                    """.trimIndent()
                )
            )

            migrated.close()
        }
    }

    @Test
    fun version3_enforcesUniqueSeriesExternalIds() {
        runBlocking {
            val database = migrationHelper.createDatabase(3)

            database.execSQL(
                """
                INSERT INTO publishers (
                    id,
                    name
                )
                VALUES (
                    1,
                    'Marvel Comics'
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                INSERT INTO series (
                    id,
                    publisherId,
                    title,
                    volume,
                    startYear,
                    endYear
                )
                VALUES (
                    1,
                    1,
                    'Amazing Spider-Man',
                    2,
                    1999,
                    2003
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                INSERT INTO series (
                    id,
                    publisherId,
                    title,
                    volume,
                    startYear,
                    endYear
                )
                VALUES (
                    2,
                    1,
                    'Peter Parker: Spider-Man',
                    2,
                    1999,
                    2003
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                INSERT INTO series_external_ids (
                    seriesId,
                    source,
                    externalId,
                    url
                )
                VALUES (
                    1,
                    'COMIC_VINE',
                    '2127',
                    NULL
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                INSERT OR IGNORE INTO
                series_external_ids (
                    seriesId,
                    source,
                    externalId,
                    url
                )
                VALUES (
                    2,
                    'COMIC_VINE',
                    '2127',
                    NULL
                )
                """.trimIndent()
            )

            assertEquals(
                1L,
                queryCount(
                    database,
                    """
                    SELECT COUNT(*)
                    FROM series_external_ids
                    WHERE source = 'COMIC_VINE'
                        AND externalId = '2127'
                    """.trimIndent())
            )

            assertEquals(
                1L,
                queryCount(
                    database,
                    """
                    SELECT COUNT(*)
                    FROM series_external_ids
                    WHERE seriesId = 1
                    """.trimIndent()
                )
            )

            assertEquals(
                0L,
                queryCount(
                    database,
                    """
                    SELECT COUNT(*)
                    FROM series_external_ids
                    WHERE seriesId = 2
                    """.trimIndent()
                )
            )

            database.close()
        }
    }

    @Test
    fun version3_deletingSeriesCascadesSeriesExternalIds() {
        runBlocking {
            val database = migrationHelper.createDatabase(3)

            database.execSQL(
                "PRAGMA foreign_keys = ON"
            )

            database.execSQL(
                """
                INSERT INTO publishers (
                    id,
                    name
                )
                VALUES (
                    1,
                    'Marvel Comics'
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                INSERT INTO series (
                    id,
                    publisherId,
                    title,
                    volume,
                    startYear,
                    endYear
                )
                VALUES (
                    1,
                    1,
                    'Amazing Spider-Man',
                    2,
                    1999,
                    2003
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                INSERT INTO series_external_ids (
                    seriesId,
                    source,
                    externalId,
                    url
                )
                VALUES (
                    1,
                    'COMIC_VINE',
                    '2127',
                    NULL
                )
                """.trimIndent()
            )

            assertEquals(
                1L,
                queryCount(
                    database,
                    """
                    SELECT COUNT(*)
                    FROM series_external_ids
                    WHERE seriesId = 1
                    """.trimIndent()
                )
            )

            database.execSQL(
                """
                DELETE FROM series
                WHERE id = 1
                """.trimIndent()
            )

            assertEquals(
                0L,
                queryCount(
                    database,
                    """
                    SELECT COUNT(*)
                    FROM series_external_ids
                    WHERE seriesId = 1
                    """.trimIndent()
                )
            )

            database.close()
        }
    }

    private fun insertVersion1Fixture(
        connection: SQLiteConnection
    ) {
        connection.execSQL(
            """
            INSERT INTO publishers (
                id,
                name
            )
            VALUES (
                1,
                'Marvel Comics'
            )
            """.trimIndent()
        )

        connection.execSQL(
            """
            INSERT INTO universes (
                id,
                publisherId,
                name,
                designation,
                description
            )
            VALUES (
                1,
                1,
                'Marvel Universe',
                'Earth-616',
                NULL
            )
            """.trimIndent()
        )

        connection.execSQL(
            """
            INSERT INTO series (
                id,
                publisherId,
                title,
                volume,
                startYear,
                endYear
            )
            VALUES (
                1,
                1,
                'Amazing Spider-Man',
                2,
                1998,
                2003
            )
            """.trimIndent()
        )

        connection.execSQL(
            """
            INSERT INTO issues (
                id,
                seriesId,
                universeId,
                issueNumber,
                title,
                publicationDate,
                coverUrl,
                description,
                issueType
            )
            VALUES (
                1,
                1,
                1,
                '1',
                'Test Issue',
                '1998-12',
                NULL,
                NULL,
                'REGULAR'
            )
            """.trimIndent()
        )

        connection.execSQL(
            """
            INSERT INTO reading_lists (
                id,
                title,
                description,
                publisherId,
                universeId,
                createdAt,
                updatedAt
            )
            VALUES (
                1,
                'Spider-Man Volume 2',
                'Test reading list',
                1,
                1,
                100,
                200
            )
            """.trimIndent()
        )

        connection.execSQL(
            """
            INSERT INTO reading_list_sections (
                id,
                readingListId,
                title,
                description,
                position
            )
            VALUES (
                1,
                1,
                'Opening Arc',
                NULL,
                1
            )
            """.trimIndent()
        )

        connection.execSQL(
            """
            INSERT INTO reading_list_items (
                id,
                readingListId,
                sectionId,
                issueId,
                position,
                required,
                notes
            )
            VALUES (
                1,
                1,
                1,
                1,
                1,
                1,
                'First Issue'
            )
            """.trimIndent()
        )

        connection.execSQL(
            """
            INSERT INTO reading_progress (
                id,
                issueId,
                status,
                startedAt,
                completedAt,
                notes
            )
            VALUES (
                1,
                1,
                'READ',
                1000,
                2000,
                'Great issue'
            )
            """.trimIndent()
        )

        connection.execSQL(
            """
            INSERT INTO external_ids (
                id,
                issueId,
                source,
                externalId,
                url
            )
            VALUES (
                1,
                1,
                'COMIC_VINE',
                '12345',
                'https://example.com/issue'
            )
            """.trimIndent()
        )
    }

    private fun queryCount(
        connection: SQLiteConnection,
        query: String
    ): Long {
        return connection
            .prepare(query)
            .use { statement ->
                assertTrue(statement.step())
                statement.getLong(0)
            }
    }
}