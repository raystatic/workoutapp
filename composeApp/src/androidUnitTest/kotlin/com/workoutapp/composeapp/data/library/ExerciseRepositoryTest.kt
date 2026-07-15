package com.workoutapp.composeapp.data.library

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

class ExerciseRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: ExerciseRepository

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        repository = ExerciseRepositoryImpl(testAppDatabase(driver))
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun addThenObserve_returnsInsertedExercise() = runTest {
        repository.add(
            name = "Bench Press",
            primaryMuscle = "Chest",
            equipment = "Barbell",
            secondaryMuscles = listOf("Triceps", "Shoulders"),
            updatedAt = 1000L,
        )

        val exercises = repository.observeAll().first()

        assertEquals(1, exercises.size)
        val exercise = exercises.single()
        assertEquals("Bench Press", exercise.name)
        assertEquals(listOf("Triceps", "Shoulders"), exercise.secondaryMuscles)
        assertEquals("PENDING", exercise.syncStatus)
    }

    @Test
    fun add_multipleExercises_returnsAllOrderedByName() = runTest {
        repository.add(name = "Squat", primaryMuscle = "Legs", equipment = "Barbell", updatedAt = 1000L)
        repository.add(name = "Deadlift", primaryMuscle = "Back", equipment = "Barbell", updatedAt = 1000L)

        val exercises = repository.observeAll().first()

        assertEquals(listOf("Deadlift", "Squat"), exercises.map { it.name })
    }

    @Test
    fun observeById_returnsTheMatchingExercise() = runTest {
        repository.add(name = "Squat", primaryMuscle = "Legs", equipment = "Barbell", updatedAt = 1000L)
        val id = repository.observeAll().first().single().id

        val exercise = repository.observeById(id).first()

        assertEquals("Squat", exercise?.name)
    }

    @Test
    fun observeById_unknownId_returnsNull() = runTest {
        assertEquals(null, repository.observeById(999L).first())
    }

    @Test
    fun delete_removesOnlyTheMatchingExercise() = runTest {
        repository.add(name = "Squat", primaryMuscle = "Legs", equipment = "Barbell", updatedAt = 1000L)
        val idToDelete = repository.observeAll().first().single().id
        repository.add(name = "Deadlift", primaryMuscle = "Back", equipment = "Barbell", updatedAt = 1000L)

        repository.delete(idToDelete)

        val remaining = repository.observeAll().first()
        assertEquals(listOf("Deadlift"), remaining.map { it.name })
        assertTrue(remaining.none { it.id == idToDelete })
    }
}
