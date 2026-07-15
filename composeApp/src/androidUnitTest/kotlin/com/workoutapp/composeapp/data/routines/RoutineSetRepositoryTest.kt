package com.workoutapp.composeapp.data.routines

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.workoutapp.composeapp.data.db.SetType
import com.workoutapp.composeapp.data.db.testAppDatabase
import com.workoutapp.composeapp.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class RoutineSetRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: AppDatabase
    private lateinit var repository: RoutineSetRepository
    private var routineExerciseId: Long = 0

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        database = testAppDatabase(driver)
        repository = RoutineSetRepositoryImpl(database)

        database.routineQueries.insert(
            name = "Push Day",
            folderId = null,
            position = 0,
            notes = null,
            serverId = null,
            updatedAt = 1000L,
            syncStatus = "PENDING",
        )
        val routineId = database.routineQueries.selectAll().executeAsList().single().id

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

        database.routineExerciseQueries.insert(
            routineId = routineId,
            exerciseId = exerciseId,
            position = 0,
            supersetGroup = null,
            restSeconds = null,
            notes = null,
            serverId = null,
            updatedAt = 1000L,
            syncStatus = "PENDING",
        )
        routineExerciseId = database.routineExerciseQueries.selectByRoutineId(routineId).executeAsList().single().id
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun addThenObserve_returnsInsertedSetWithDefaultType() = runTest {
        repository.add(routineExerciseId = routineExerciseId, position = 0, targetReps = 8, targetWeight = 60.0, updatedAt = 1000L)

        val sets = repository.observeByRoutineExerciseId(routineExerciseId).first()

        assertEquals(1, sets.size)
        assertEquals(SetType.NORMAL, sets.single().setType)
        assertEquals(8L, sets.single().targetReps)
    }

    @Test
    fun add_withExplicitSetType_persistsThatType() = runTest {
        repository.add(
            routineExerciseId = routineExerciseId,
            position = 0,
            setType = SetType.WARMUP,
            updatedAt = 1000L,
        )

        val sets = repository.observeByRoutineExerciseId(routineExerciseId).first()

        assertEquals(SetType.WARMUP, sets.single().setType)
    }

    @Test
    fun delete_removesTheSet() = runTest {
        repository.add(routineExerciseId = routineExerciseId, position = 0, updatedAt = 1000L)
        val idToDelete = repository.observeByRoutineExerciseId(routineExerciseId).first().single().id

        repository.delete(idToDelete)

        assertEquals(emptyList(), repository.observeByRoutineExerciseId(routineExerciseId).first())
    }

    @Test
    fun update_persistsTargetRepsWeightAndSetType() = runTest {
        repository.add(routineExerciseId = routineExerciseId, position = 0, updatedAt = 1000L)
        val id = repository.observeByRoutineExerciseId(routineExerciseId).first().single().id

        repository.update(id = id, targetReps = 12, targetWeight = 70.0, setType = SetType.DROP, updatedAt = 2000L)

        val updated = repository.observeByRoutineExerciseId(routineExerciseId).first().single()
        assertEquals(12L, updated.targetReps)
        assertEquals(70.0, updated.targetWeight)
        assertEquals(SetType.DROP, updated.setType)
    }

    @Test
    fun updatePosition_persistsTheNewPosition() = runTest {
        repository.add(routineExerciseId = routineExerciseId, position = 0, updatedAt = 1000L)
        val id = repository.observeByRoutineExerciseId(routineExerciseId).first().single().id

        repository.updatePosition(id, 5)

        assertEquals(5L, repository.observeByRoutineExerciseId(routineExerciseId).first().single().position)
    }

    @Test
    fun observeByRoutineId_returnsSetsAcrossAllOfARoutinesExercises() = runTest {
        val routineId = database.routineQueries.selectAll().executeAsList().single().id
        val exerciseId = database.exerciseQueries.selectAll().executeAsList().single().id
        database.routineExerciseQueries.insert(
            routineId = routineId,
            exerciseId = exerciseId,
            position = 1,
            supersetGroup = null,
            restSeconds = null,
            notes = null,
            serverId = null,
            updatedAt = 1000L,
            syncStatus = "PENDING",
        )
        val secondRoutineExerciseId = database.routineExerciseQueries.selectByRoutineId(routineId)
            .executeAsList()
            .first { it.id != routineExerciseId }
            .id

        repository.add(routineExerciseId = routineExerciseId, position = 0, targetReps = 8, updatedAt = 1000L)
        repository.add(routineExerciseId = secondRoutineExerciseId, position = 0, targetReps = 10, updatedAt = 1000L)

        val sets = repository.observeByRoutineId(routineId).first()

        assertEquals(setOf(8L, 10L), sets.map { it.targetReps }.toSet())
    }
}
