package com.workoutapp.composeapp.data.workout

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.workoutapp.composeapp.data.db.SetType
import com.workoutapp.composeapp.data.db.WorkoutPrivacy
import com.workoutapp.composeapp.data.db.testAppDatabase
import com.workoutapp.composeapp.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorkoutSetRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: AppDatabase
    private lateinit var repository: WorkoutSetRepository
    private var workoutExerciseId: Long = 0

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        database = testAppDatabase(driver)
        repository = WorkoutSetRepositoryImpl(database)

        database.workoutQueries.insert(
            name = "Morning Session",
            startedAt = 1000L,
            finishedAt = null,
            note = null,
            privacy = WorkoutPrivacy.PRIVATE,
            media = emptyList(),
            serverId = null,
            updatedAt = 1000L,
            syncStatus = "PENDING",
        )
        val workoutId = database.workoutQueries.selectAll().executeAsList().single().id

        database.exerciseQueries.insert(
            name = "Bench Press",
            primaryMuscle = "Chest",
            secondaryMuscles = emptyList(),
            equipment = "Barbell",
            mediaUrl = null,
            isCustom = false,
            instructions = null,
            serverId = null,
            updatedAt = 1000L,
            syncStatus = "PENDING",
        )
        val exerciseId = database.exerciseQueries.selectAll().executeAsList().single().id

        database.workoutExerciseQueries.insert(
            workoutId = workoutId,
            exerciseId = exerciseId,
            position = 0,
            supersetGroup = null,
            notes = null,
            serverId = null,
            updatedAt = 1000L,
            syncStatus = "PENDING",
        )
        workoutExerciseId = database.workoutExerciseQueries.selectByWorkoutId(workoutId).executeAsList().single().id
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun addThenObserve_returnsInsertedSet() = runTest {
        repository.add(workoutExerciseId = workoutExerciseId, position = 0, reps = 10, weight = 80.0, updatedAt = 1000L)

        val sets = repository.observeByWorkoutExerciseId(workoutExerciseId).first()

        assertEquals(1, sets.size)
        val set = sets.single()
        assertEquals(10L, set.reps)
        assertEquals(80.0, set.weight)
        assertEquals(SetType.NORMAL, set.setType)
        assertEquals(false, set.completed)
        assertNull(set.rpe)
    }

    @Test
    fun updateRpe_persistsTheNewValue() = runTest {
        repository.add(workoutExerciseId = workoutExerciseId, position = 0, reps = 10, weight = 80.0, updatedAt = 1000L)
        val id = repository.observeByWorkoutExerciseId(workoutExerciseId).first().single().id

        repository.updateRpe(id, 8.5)

        val set = repository.observeByWorkoutExerciseId(workoutExerciseId).first().single()
        assertEquals(8.5, set.rpe)
    }

    @Test
    fun delete_removesTheSet() = runTest {
        repository.add(workoutExerciseId = workoutExerciseId, position = 0, updatedAt = 1000L)
        val idToDelete = repository.observeByWorkoutExerciseId(workoutExerciseId).first().single().id

        repository.delete(idToDelete)

        assertEquals(emptyList(), repository.observeByWorkoutExerciseId(workoutExerciseId).first())
    }
}
