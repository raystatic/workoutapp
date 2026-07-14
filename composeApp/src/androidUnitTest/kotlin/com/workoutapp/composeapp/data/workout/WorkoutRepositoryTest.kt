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
import kotlin.test.assertTrue

class WorkoutRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: WorkoutRepository

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        repository = WorkoutRepositoryImpl(testAppDatabase(driver))
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun addThenObserve_returnsInsertedWorkoutWithDefaults() = runTest {
        repository.add(name = "Morning Session", startedAt = 1000L, updatedAt = 1000L)

        val workouts = repository.observeAll().first()

        assertEquals(1, workouts.size)
        val workout = workouts.single()
        assertEquals("Morning Session", workout.name)
        assertEquals(WorkoutPrivacy.PRIVATE, workout.privacy)
        assertTrue(workout.media.isEmpty())
    }

    @Test
    fun add_returnsTheGeneratedWorkoutId() = runTest {
        val firstId = repository.add(name = "Earlier", startedAt = 1000L, updatedAt = 1000L)
        val secondId = repository.add(name = "Later", startedAt = 2000L, updatedAt = 2000L)

        val workouts = repository.observeAll().first()
        assertEquals(firstId, workouts.single { it.name == "Earlier" }.id)
        assertEquals(secondId, workouts.single { it.name == "Later" }.id)
        assertTrue(secondId > firstId)
    }

    @Test
    fun add_multipleWorkouts_returnsNewestFirst() = runTest {
        repository.add(name = "Earlier", startedAt = 1000L, updatedAt = 1000L)
        repository.add(name = "Later", startedAt = 2000L, updatedAt = 2000L)

        val workouts = repository.observeAll().first()

        assertEquals(listOf("Later", "Earlier"), workouts.map { it.name })
    }

    @Test
    fun observeById_emitsTheMatchingWorkout() = runTest {
        val id = repository.add(name = "Morning Session", startedAt = 1000L, updatedAt = 1000L)

        val workout = repository.observeById(id).first()

        assertEquals(id, workout?.id)
        assertEquals("Morning Session", workout?.name)
    }

    @Test
    fun observeById_unknownId_emitsNull() = runTest {
        val workout = repository.observeById(999L).first()

        assertEquals(null, workout)
    }

    @Test
    fun delete_removesOnlyTheMatchingWorkout() = runTest {
        repository.add(name = "Earlier", startedAt = 1000L, updatedAt = 1000L)
        val idToDelete = repository.observeAll().first().single().id
        repository.add(name = "Later", startedAt = 2000L, updatedAt = 2000L)

        repository.delete(idToDelete)

        val remaining = repository.observeAll().first()
        assertEquals(listOf("Later"), remaining.map { it.name })
    }

    @Test
    fun getById_returnsTheMatchingWorkout() = runTest {
        val id = repository.add(name = "Morning Session", startedAt = 1000L, updatedAt = 1000L)

        val workout = repository.getById(id)

        assertEquals("Morning Session", workout?.name)
    }

    @Test
    fun getById_unknownId_returnsNull() = runTest {
        assertEquals(null, repository.getById(999L))
    }

    @Test
    fun update_appliesFinishSaveEditsToTheWorkout() = runTest {
        val id = repository.add(name = "Morning Session", startedAt = 1000L, updatedAt = 1000L)

        repository.update(
            id = id,
            name = "Renamed Session",
            finishedAt = 5000L,
            note = "Felt great",
            privacy = WorkoutPrivacy.PUBLIC,
            media = listOf("photo.jpg"),
            updatedAt = 5000L,
        )

        val workout = repository.observeById(id).first()
        assertEquals("Renamed Session", workout?.name)
        assertEquals(5000L, workout?.finishedAt)
        assertEquals("Felt great", workout?.note)
        assertEquals(WorkoutPrivacy.PUBLIC, workout?.privacy)
        assertEquals(listOf("photo.jpg"), workout?.media)
    }

    @Test
    fun getCompletedWorkoutFinishedAtTimestamps_excludesUnfinishedWorkouts() = runTest {
        val finishedId = repository.add(name = "Finished", startedAt = 1000L, updatedAt = 1000L)
        repository.add(name = "In Progress", startedAt = 2000L, updatedAt = 2000L)
        repository.update(
            id = finishedId,
            name = "Finished",
            finishedAt = 4000L,
            note = null,
            privacy = WorkoutPrivacy.PRIVATE,
            media = emptyList(),
            updatedAt = 4000L,
        )

        val timestamps = repository.getCompletedWorkoutFinishedAtTimestamps()

        assertEquals(listOf(4000L), timestamps)
    }
}
