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
    private var workoutId: Long = 0
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
        workoutId = database.workoutQueries.selectAll().executeAsList().single().id

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
    fun update_persistsAllEditableFields() = runTest {
        repository.add(workoutExerciseId = workoutExerciseId, position = 0, reps = 10, weight = 80.0, updatedAt = 1000L)
        val id = repository.observeByWorkoutExerciseId(workoutExerciseId).first().single().id

        repository.update(
            id = id,
            reps = 12,
            weight = 82.5,
            durationSec = 45,
            setType = SetType.DROP,
            completed = true,
            updatedAt = 2000L,
        )

        val set = repository.observeByWorkoutExerciseId(workoutExerciseId).first().single()
        assertEquals(12L, set.reps)
        assertEquals(82.5, set.weight)
        assertEquals(45L, set.durationSec)
        assertEquals(SetType.DROP, set.setType)
        assertEquals(true, set.completed)
    }

    @Test
    fun updatePosition_persistsTheNewPosition() = runTest {
        repository.add(workoutExerciseId = workoutExerciseId, position = 0, updatedAt = 1000L)
        val id = repository.observeByWorkoutExerciseId(workoutExerciseId).first().single().id

        repository.updatePosition(id, 5)

        assertEquals(5L, repository.observeByWorkoutExerciseId(workoutExerciseId).first().single().position)
    }

    @Test
    fun observeByWorkoutId_returnsSetsAcrossAllExercisesInTheWorkout() = runTest {
        database.exerciseQueries.insert(
            name = "Squat",
            primaryMuscle = "Legs",
            secondaryMuscles = emptyList(),
            equipment = "Barbell",
            mediaUrl = null,
            isCustom = false,
            instructions = null,
            serverId = null,
            updatedAt = 1000L,
            syncStatus = "PENDING",
        )
        val secondExerciseId = database.exerciseQueries.selectAll().executeAsList().single { it.name == "Squat" }.id
        database.workoutExerciseQueries.insert(
            workoutId = workoutId,
            exerciseId = secondExerciseId,
            position = 1,
            supersetGroup = null,
            notes = null,
            serverId = null,
            updatedAt = 1000L,
            syncStatus = "PENDING",
        )
        val secondWorkoutExerciseId = database.workoutExerciseQueries.selectByWorkoutId(workoutId)
            .executeAsList()
            .single { it.exerciseId == secondExerciseId }
            .id

        repository.add(workoutExerciseId = workoutExerciseId, position = 0, reps = 10, updatedAt = 1000L)
        repository.add(workoutExerciseId = secondWorkoutExerciseId, position = 0, reps = 5, updatedAt = 1000L)

        val sets = repository.observeByWorkoutId(workoutId).first()

        assertEquals(2, sets.size)
        assertEquals(setOf(10L, 5L), sets.map { it.reps }.toSet())
    }

    @Test
    fun getByWorkoutExerciseId_returnsSetsOrderedByPosition() = runTest {
        repository.add(workoutExerciseId = workoutExerciseId, position = 1, reps = 5, updatedAt = 1000L)
        repository.add(workoutExerciseId = workoutExerciseId, position = 0, reps = 10, updatedAt = 1000L)

        val sets = repository.getByWorkoutExerciseId(workoutExerciseId)

        assertEquals(listOf(10L, 5L), sets.map { it.reps })
    }

    @Test
    fun getByWorkoutExerciseId_noSets_returnsEmptyList() = runTest {
        assertEquals(emptyList(), repository.getByWorkoutExerciseId(workoutExerciseId))
    }

    @Test
    fun observeHistoryByExerciseId_returnsOnlyCompletedSetsNewestWorkoutFirst() = runTest {
        val exerciseId = database.exerciseQueries.selectAll().executeAsList().single().id

        repository.add(workoutExerciseId = workoutExerciseId, position = 0, reps = 10, weight = 80.0, updatedAt = 1000L)
        val incompleteId = repository.observeByWorkoutExerciseId(workoutExerciseId).first().single().id
        repository.update(
            id = incompleteId,
            reps = 10,
            weight = 80.0,
            durationSec = null,
            setType = SetType.NORMAL,
            completed = true,
            updatedAt = 1000L,
        )
        repository.add(workoutExerciseId = workoutExerciseId, position = 1, reps = 8, weight = 60.0, updatedAt = 1000L)

        database.workoutQueries.insert(
            name = "Later Session",
            startedAt = 2000L,
            finishedAt = null,
            note = null,
            privacy = WorkoutPrivacy.PRIVATE,
            media = emptyList(),
            serverId = null,
            updatedAt = 2000L,
            syncStatus = "PENDING",
        )
        val laterWorkoutId = database.workoutQueries.selectAll().executeAsList().single { it.name == "Later Session" }.id
        database.workoutExerciseQueries.insert(
            workoutId = laterWorkoutId,
            exerciseId = exerciseId,
            position = 0,
            supersetGroup = null,
            notes = null,
            serverId = null,
            updatedAt = 2000L,
            syncStatus = "PENDING",
        )
        val laterWorkoutExerciseId = database.workoutExerciseQueries.selectByWorkoutId(laterWorkoutId).executeAsList().single().id
        repository.add(workoutExerciseId = laterWorkoutExerciseId, position = 0, reps = 5, weight = 100.0, updatedAt = 2000L)
        val laterSetId = repository.observeByWorkoutExerciseId(laterWorkoutExerciseId).first().single().id
        repository.update(
            id = laterSetId,
            reps = 5,
            weight = 100.0,
            durationSec = null,
            setType = SetType.NORMAL,
            completed = true,
            updatedAt = 2000L,
        )

        val history = repository.observeHistoryByExerciseId(exerciseId).first()

        assertEquals(listOf(100.0, 80.0), history.map { it.set.weight })
        assertEquals(listOf(2000L, 1000L), history.map { it.workoutStartedAt })
    }

    @Test
    fun delete_removesTheSet() = runTest {
        repository.add(workoutExerciseId = workoutExerciseId, position = 0, updatedAt = 1000L)
        val idToDelete = repository.observeByWorkoutExerciseId(workoutExerciseId).first().single().id

        repository.delete(idToDelete)

        assertEquals(emptyList(), repository.observeByWorkoutExerciseId(workoutExerciseId).first())
    }
}
