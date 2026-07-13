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

class ExerciseSeederTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: ExerciseRepository
    private lateinit var seeder: ExerciseSeeder

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        repository = ExerciseRepositoryImpl(testAppDatabase(driver))
        seeder = ExerciseSeeder(repository)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun seedIfNeeded_onEmptyDb_insertsTheFullCatalog() = runTest {
        seeder.seedIfNeeded()

        val exercises = repository.observeAll().first()

        assertTrue(exercises.size >= 400, "expected 400+ seeded exercises, got ${exercises.size}")
        assertEquals(ExerciseSeedData.exercises.size, exercises.size)
        assertTrue(exercises.all { it.primaryMuscle.isNotBlank() && it.equipment.isNotBlank() })
    }

    @Test
    fun seedIfNeeded_calledTwice_doesNotDuplicate() = runTest {
        seeder.seedIfNeeded()
        seeder.seedIfNeeded()

        val exercises = repository.observeAll().first()

        assertEquals(ExerciseSeedData.exercises.size, exercises.size)
        assertEquals(exercises.size, exercises.map { it.name }.toSet().size)
    }

    @Test
    fun seedIfNeeded_withPreExistingRow_onlyInsertsTheMissingOnes() = runTest {
        val alreadyThere = ExerciseSeedData.exercises.first()
        repository.add(
            name = alreadyThere.name,
            primaryMuscle = alreadyThere.primaryMuscle,
            equipment = alreadyThere.equipment,
            secondaryMuscles = alreadyThere.secondaryMuscles,
            instructions = alreadyThere.instructions,
            updatedAt = 1L,
        )

        seeder.seedIfNeeded()

        val exercises = repository.observeAll().first()
        assertEquals(ExerciseSeedData.exercises.size, exercises.size)
        assertEquals(1, exercises.count { it.name == alreadyThere.name })
    }
}
