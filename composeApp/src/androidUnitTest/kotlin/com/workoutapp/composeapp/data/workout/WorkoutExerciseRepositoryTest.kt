package com.workoutapp.composeapp.data.workout

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
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
import kotlin.test.assertTrue

class WorkoutExerciseRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: AppDatabase
    private lateinit var repository: WorkoutExerciseRepository
    private var workoutId: Long = 0
    private var exerciseId: Long = 0

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        database = testAppDatabase(driver)
        repository = WorkoutExerciseRepositoryImpl(database)

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
        exerciseId = database.exerciseQueries.selectAll().executeAsList().single().id
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun addThenObserve_returnsExerciseScopedToWorkout() = runTest {
        repository.add(workoutId = workoutId, exerciseId = exerciseId, position = 0, updatedAt = 1000L)

        val entries = repository.observeByWorkoutId(workoutId).first()

        assertEquals(1, entries.size)
        assertEquals(exerciseId, entries.single().exerciseId)
    }

    @Test
    fun updatePosition_persistsTheNewPosition() = runTest {
        repository.add(workoutId = workoutId, exerciseId = exerciseId, position = 0, updatedAt = 1000L)
        val id = repository.observeByWorkoutId(workoutId).first().single().id

        repository.updatePosition(id, 3)

        assertEquals(3L, repository.observeByWorkoutId(workoutId).first().single().position)
    }

    @Test
    fun updateSupersetGroup_persistsTheGroupAndClearingItSetsNull() = runTest {
        repository.add(workoutId = workoutId, exerciseId = exerciseId, position = 0, updatedAt = 1000L)
        val id = repository.observeByWorkoutId(workoutId).first().single().id

        repository.updateSupersetGroup(id, "sg-$id")
        assertEquals("sg-$id", repository.observeByWorkoutId(workoutId).first().single().supersetGroup)

        repository.updateSupersetGroup(id, null)
        assertNull(repository.observeByWorkoutId(workoutId).first().single().supersetGroup)
    }

    @Test
    fun delete_removesOnlyTheMatchingEntry() = runTest {
        repository.add(workoutId = workoutId, exerciseId = exerciseId, position = 0, updatedAt = 1000L)
        val idToDelete = repository.observeByWorkoutId(workoutId).first().single().id

        repository.delete(idToDelete)

        val remaining = repository.observeByWorkoutId(workoutId).first()
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun findMostRecentOtherWorkoutExerciseId_noPriorWorkout_returnsNull() = runTest {
        repository.add(workoutId = workoutId, exerciseId = exerciseId, position = 0, updatedAt = 1000L)

        val result = repository.findMostRecentOtherWorkoutExerciseId(exerciseId, excludingWorkoutId = workoutId)

        assertNull(result)
    }

    @Test
    fun findMostRecentOtherWorkoutExerciseId_returnsTheMostRecentOtherWorkoutsEntry() = runTest {
        database.workoutQueries.insert(
            name = "Earlier Session",
            startedAt = 500L,
            finishedAt = null,
            note = null,
            privacy = WorkoutPrivacy.PRIVATE,
            media = emptyList(),
            serverId = null,
            updatedAt = 500L,
            syncStatus = "PENDING",
        )
        val earlierWorkoutId = database.workoutQueries.selectAll().executeAsList().single { it.startedAt == 500L }.id
        database.workoutQueries.insert(
            name = "Latest Prior Session",
            startedAt = 2000L,
            finishedAt = null,
            note = null,
            privacy = WorkoutPrivacy.PRIVATE,
            media = emptyList(),
            serverId = null,
            updatedAt = 2000L,
            syncStatus = "PENDING",
        )
        val latestPriorWorkoutId =
            database.workoutQueries.selectAll().executeAsList().single { it.startedAt == 2000L }.id

        repository.add(workoutId = earlierWorkoutId, exerciseId = exerciseId, position = 0, updatedAt = 500L)
        repository.add(workoutId = latestPriorWorkoutId, exerciseId = exerciseId, position = 0, updatedAt = 2000L)
        repository.add(workoutId = workoutId, exerciseId = exerciseId, position = 0, updatedAt = 1000L)
        val expectedId = repository.observeByWorkoutId(latestPriorWorkoutId).first().single().id

        val result = repository.findMostRecentOtherWorkoutExerciseId(exerciseId, excludingWorkoutId = workoutId)

        assertEquals(expectedId, result)
    }

    @Test
    fun findMostRecentOtherWorkoutExerciseId_differentExercise_returnsNull() = runTest {
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
        val otherExerciseId = database.exerciseQueries.selectAll().executeAsList().single { it.name == "Squat" }.id
        database.workoutQueries.insert(
            name = "Prior Session",
            startedAt = 500L,
            finishedAt = null,
            note = null,
            privacy = WorkoutPrivacy.PRIVATE,
            media = emptyList(),
            serverId = null,
            updatedAt = 500L,
            syncStatus = "PENDING",
        )
        val priorWorkoutId = database.workoutQueries.selectAll().executeAsList().single { it.startedAt == 500L }.id
        repository.add(workoutId = priorWorkoutId, exerciseId = otherExerciseId, position = 0, updatedAt = 500L)

        val result = repository.findMostRecentOtherWorkoutExerciseId(exerciseId, excludingWorkoutId = workoutId)

        assertNull(result)
    }
}
