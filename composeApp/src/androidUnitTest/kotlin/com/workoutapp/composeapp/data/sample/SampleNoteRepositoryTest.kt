package com.workoutapp.composeapp.data.sample

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.workoutapp.composeapp.data.db.testAppDatabase
import com.workoutapp.composeapp.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Repository CRUD against a JDBC in-memory SQLite driver — fast, no Android
 * device needed. Proves the SQLDelight schema + repository + Flow wiring.
 */
class SampleNoteRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: SampleNoteRepository

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        repository = SampleNoteRepositoryImpl(testAppDatabase(driver))
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun addThenObserve_returnsInsertedNote() = runTest {
        repository.add("Bench Press")

        val notes = repository.observeAll().first()

        assertEquals(1, notes.size)
        assertEquals("Bench Press", notes.single().text)
    }

    @Test
    fun add_multipleNotes_returnsAllOrderedNewestFirst() = runTest {
        repository.add("Squat")
        repository.add("Deadlift")

        val notes = repository.observeAll().first()

        assertEquals(listOf("Deadlift", "Squat"), notes.map { it.text })
    }

    @Test
    fun delete_removesOnlyTheMatchingNote() = runTest {
        repository.add("Squat")
        val idToDelete = repository.observeAll().first().single().id
        repository.add("Deadlift")

        repository.delete(idToDelete)

        val remaining = repository.observeAll().first()
        assertEquals(listOf("Deadlift"), remaining.map { it.text })
        assertTrue(remaining.none { it.id == idToDelete })
    }
}
