package com.workoutapp.composeapp.data.routines

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

class RoutineRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: RoutineRepository

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        repository = RoutineRepositoryImpl(testAppDatabase(driver))
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun addThenObserve_returnsInsertedRoutine() = runTest {
        repository.add(name = "Push Day", position = 0, notes = "Chest/shoulders/triceps", updatedAt = 1000L)

        val routines = repository.observeAll().first()

        assertEquals(1, routines.size)
        assertEquals("Push Day", routines.single().name)
    }

    @Test
    fun add_multipleRoutines_returnsAllOrderedByPosition() = runTest {
        repository.add(name = "Pull Day", position = 1, updatedAt = 1000L)
        repository.add(name = "Push Day", position = 0, updatedAt = 1000L)

        val routines = repository.observeAll().first()

        assertEquals(listOf("Push Day", "Pull Day"), routines.map { it.name })
    }

    @Test
    fun delete_removesOnlyTheMatchingRoutine() = runTest {
        repository.add(name = "Push Day", position = 0, updatedAt = 1000L)
        val idToDelete = repository.observeAll().first().single().id
        repository.add(name = "Pull Day", position = 1, updatedAt = 1000L)

        repository.delete(idToDelete)

        val remaining = repository.observeAll().first()
        assertEquals(listOf("Pull Day"), remaining.map { it.name })
        assertTrue(remaining.none { it.id == idToDelete })
    }
}
