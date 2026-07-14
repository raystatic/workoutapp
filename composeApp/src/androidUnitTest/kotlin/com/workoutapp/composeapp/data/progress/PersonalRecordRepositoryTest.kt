package com.workoutapp.composeapp.data.progress

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

class PersonalRecordRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: AppDatabase
    private lateinit var repository: PersonalRecordRepository
    private var exerciseId: Long = 0
    private var workoutId: Long = 0

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        database = testAppDatabase(driver)
        repository = PersonalRecordRepositoryImpl(database)

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
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun addThenObserve_returnsRecordScopedToExercise() = runTest {
        repository.add(exerciseId = exerciseId, type = "1RM", value = 120.0, workoutId = workoutId, updatedAt = 1000L)

        val records = repository.observeByExerciseId(exerciseId).first()

        assertEquals(1, records.size)
        assertEquals("1RM", records.single().type)
        assertEquals(120.0, records.single().value)
    }

    @Test
    fun delete_removesOnlyTheMatchingRecord() = runTest {
        repository.add(exerciseId = exerciseId, type = "1RM", value = 120.0, workoutId = workoutId, updatedAt = 1000L)
        val idToDelete = repository.observeByExerciseId(exerciseId).first().single().id

        repository.delete(idToDelete)

        val remaining = repository.observeByExerciseId(exerciseId).first()
        assertEquals(emptyList(), remaining)
    }

    @Test
    fun getBestValue_noRecordsYet_returnsNull() = runTest {
        assertEquals(null, repository.getBestValue(exerciseId, "1RM"))
    }

    @Test
    fun getBestValue_returnsTheHighestValueForThatExerciseAndType() = runTest {
        repository.add(exerciseId = exerciseId, type = "1RM", value = 100.0, workoutId = workoutId, updatedAt = 1000L)
        repository.add(exerciseId = exerciseId, type = "1RM", value = 130.0, workoutId = workoutId, updatedAt = 2000L)
        repository.add(exerciseId = exerciseId, type = "maxWeight", value = 200.0, workoutId = workoutId, updatedAt = 2000L)

        assertEquals(130.0, repository.getBestValue(exerciseId, "1RM"))
        assertEquals(200.0, repository.getBestValue(exerciseId, "maxWeight"))
    }
}
