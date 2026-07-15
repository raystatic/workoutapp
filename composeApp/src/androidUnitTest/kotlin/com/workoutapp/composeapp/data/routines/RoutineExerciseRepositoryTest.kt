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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoutineExerciseRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: AppDatabase
    private lateinit var repository: RoutineExerciseRepository
    private var routineId: Long = 0
    private var exerciseId: Long = 0

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        database = testAppDatabase(driver)
        repository = RoutineExerciseRepositoryImpl(database)

        database.routineQueries.insert(
            name = "Push Day",
            folderId = null,
            position = 0,
            notes = null,
            serverId = null,
            updatedAt = 1000L,
            syncStatus = "PENDING",
        )
        routineId = database.routineQueries.selectAll().executeAsList().single().id

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
    fun addThenObserve_returnsExerciseScopedToRoutine() = runTest {
        repository.add(routineId = routineId, exerciseId = exerciseId, position = 0, updatedAt = 1000L)

        val entries = repository.observeByRoutineId(routineId).first()

        assertEquals(1, entries.size)
        assertEquals(exerciseId, entries.single().exerciseId)
    }

    @Test
    fun observeByRoutineId_excludesEntriesFromOtherRoutines() = runTest {
        database.routineQueries.insert(
            name = "Pull Day",
            folderId = null,
            position = 1,
            notes = null,
            serverId = null,
            updatedAt = 1000L,
            syncStatus = "PENDING",
        )
        val otherRoutineId = database.routineQueries.selectAll().executeAsList().first { it.id != routineId }.id

        repository.add(routineId = routineId, exerciseId = exerciseId, updatedAt = 1000L)
        repository.add(routineId = otherRoutineId, exerciseId = exerciseId, updatedAt = 1000L)

        val entries = repository.observeByRoutineId(routineId).first()

        assertEquals(1, entries.size)
        assertEquals(routineId, entries.single().routineId)
    }

    @Test
    fun delete_removesOnlyTheMatchingEntry() = runTest {
        repository.add(routineId = routineId, exerciseId = exerciseId, position = 0, updatedAt = 1000L)
        val idToDelete = repository.observeByRoutineId(routineId).first().single().id

        repository.delete(idToDelete)

        val remaining = repository.observeByRoutineId(routineId).first()
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun add_returnsTheGeneratedId() = runTest {
        val id = repository.add(routineId = routineId, exerciseId = exerciseId, position = 0, updatedAt = 1000L)

        assertEquals(id, repository.observeByRoutineId(routineId).first().single().id)
    }

    @Test
    fun updatePosition_persistsTheNewPosition() = runTest {
        repository.add(routineId = routineId, exerciseId = exerciseId, position = 0, updatedAt = 1000L)
        val id = repository.observeByRoutineId(routineId).first().single().id

        repository.updatePosition(id, 3)

        assertEquals(3L, repository.observeByRoutineId(routineId).first().single().position)
    }

    @Test
    fun updateSupersetGroup_persistsTheGroupAndClearingItSetsNull() = runTest {
        repository.add(routineId = routineId, exerciseId = exerciseId, position = 0, updatedAt = 1000L)
        val id = repository.observeByRoutineId(routineId).first().single().id

        repository.updateSupersetGroup(id, "sg-$id")
        assertEquals("sg-$id", repository.observeByRoutineId(routineId).first().single().supersetGroup)

        repository.updateSupersetGroup(id, null)
        assertNull(repository.observeByRoutineId(routineId).first().single().supersetGroup)
    }

    @Test
    fun updateRestSeconds_persistsTheNewValueAndClearingItSetsNull() = runTest {
        repository.add(routineId = routineId, exerciseId = exerciseId, position = 0, updatedAt = 1000L)
        val id = repository.observeByRoutineId(routineId).first().single().id

        repository.updateRestSeconds(id, 60L)
        assertEquals(60L, repository.observeByRoutineId(routineId).first().single().restSeconds)

        repository.updateRestSeconds(id, null)
        assertNull(repository.observeByRoutineId(routineId).first().single().restSeconds)
    }

    @Test
    fun updateNotes_persistsTheNewValueAndClearingItSetsNull() = runTest {
        repository.add(routineId = routineId, exerciseId = exerciseId, position = 0, updatedAt = 1000L)
        val id = repository.observeByRoutineId(routineId).first().single().id

        repository.updateNotes(id, "go slow on the eccentric")
        assertEquals("go slow on the eccentric", repository.observeByRoutineId(routineId).first().single().notes)

        repository.updateNotes(id, null)
        assertNull(repository.observeByRoutineId(routineId).first().single().notes)
    }
}
