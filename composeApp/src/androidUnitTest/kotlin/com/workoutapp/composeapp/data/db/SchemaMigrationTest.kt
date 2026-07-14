package com.workoutapp.composeapp.data.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.workoutapp.composeapp.db.AppDatabase
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Seeds a driver with the pre-migration (v1) `workoutSet` table — the schema
 * before 2.sqm added `rpe` — inserts a row, then migrates to v2 and verifies
 * the row survives untouched and the new column is queryable.
 */
class SchemaMigrationTest {
    private lateinit var driver: JdbcSqliteDriver

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            null,
            """
            CREATE TABLE workoutSet (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                workoutExerciseId INTEGER NOT NULL,
                position INTEGER NOT NULL DEFAULT 0,
                reps INTEGER,
                weight REAL,
                durationSec INTEGER,
                setType TEXT NOT NULL DEFAULT 'NORMAL',
                completed INTEGER NOT NULL DEFAULT 0,
                serverId TEXT,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING'
            )
            """.trimIndent(),
            0,
        )
        driver.execute(
            null,
            """
            INSERT INTO workoutSet(workoutExerciseId, position, reps, weight, setType, completed, updatedAt, syncStatus)
            VALUES (1, 0, 10, 80.0, 'NORMAL', 0, 1000, 'PENDING')
            """.trimIndent(),
            0,
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun migrateV1ToV2_addsRpeColumn_withoutLosingExistingRows() {
        AppDatabase.Schema.migrate(driver, 1, 2)

        val database = testAppDatabase(driver)
        val rows = database.workoutSetQueries.selectByWorkoutExerciseId(1L).executeAsList()

        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals(10L, row.reps)
        assertEquals(80.0, row.weight)
        assertEquals("PENDING", row.syncStatus)
        assertNull(row.rpe)
    }
}

/**
 * Seeds a driver with the pre-migration (v2) `workoutExercise` table — the
 * schema before 3.sqm added `restSeconds` and the `appSetting` table —
 * inserts a row, then migrates to v3 and verifies the row survives untouched,
 * the new column is queryable, and the new table is usable.
 */
class RestTimerSchemaMigrationTest {
    private lateinit var driver: JdbcSqliteDriver

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            null,
            """
            CREATE TABLE workoutExercise (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                workoutId INTEGER NOT NULL,
                exerciseId INTEGER NOT NULL,
                position INTEGER NOT NULL DEFAULT 0,
                supersetGroup TEXT,
                notes TEXT,
                serverId TEXT,
                updatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING'
            )
            """.trimIndent(),
            0,
        )
        driver.execute(
            null,
            """
            INSERT INTO workoutExercise(workoutId, exerciseId, position, updatedAt, syncStatus)
            VALUES (1, 1, 0, 1000, 'PENDING')
            """.trimIndent(),
            0,
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun migrateV2ToV3_addsRestSecondsColumnAndAppSettingTable_withoutLosingExistingRows() {
        AppDatabase.Schema.migrate(driver, 2, 3)

        val database = testAppDatabase(driver)
        val rows = database.workoutExerciseQueries.selectByWorkoutId(1L).executeAsList()

        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("PENDING", row.syncStatus)
        assertNull(row.restSeconds)

        database.appSettingQueries.upsert("default_rest_seconds", "120")
        assertEquals("120", database.appSettingQueries.selectByKey("default_rest_seconds").executeAsOne())
    }
}
