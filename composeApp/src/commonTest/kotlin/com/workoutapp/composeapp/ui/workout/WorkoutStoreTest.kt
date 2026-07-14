package com.workoutapp.composeapp.ui.workout

import com.workoutapp.composeapp.data.db.WorkoutPrivacy
import com.workoutapp.composeapp.data.routines.RoutineRepository
import com.workoutapp.composeapp.data.workout.WorkoutRepository
import com.workoutapp.composeapp.db.Routine
import com.workoutapp.composeapp.db.Workout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeRoutineRepository(seed: List<Routine> = emptyList()) : RoutineRepository {
    private val routines = MutableStateFlow(seed)

    override fun observeAll(): Flow<List<Routine>> = routines

    override suspend fun add(name: String, folderId: Long?, position: Long, notes: String?, updatedAt: Long) {
        error("not needed for these tests")
    }

    override suspend fun delete(id: Long) {
        routines.update { list -> list.filterNot { it.id == id } }
    }
}

private class FakeWorkoutRepository : WorkoutRepository {
    private var nextId = 1L
    val added = mutableListOf<String>()

    override fun observeAll(): Flow<List<Workout>> = MutableStateFlow(emptyList())

    override fun observeById(id: Long): Flow<Workout?> = MutableStateFlow(null)

    override suspend fun add(
        name: String,
        startedAt: Long,
        finishedAt: Long?,
        note: String?,
        privacy: WorkoutPrivacy,
        media: List<String>,
        updatedAt: Long,
    ): Long {
        added += name
        return nextId++
    }

    override suspend fun delete(id: Long) = Unit
}

private fun routine(id: Long, name: String, folderId: Long? = null) =
    Routine(id, name, folderId, 0L, null, null, 0L, "PENDING")

class WorkoutStoreTest {
    private fun newStore(
        routineRepository: RoutineRepository = FakeRoutineRepository(),
        workoutRepository: WorkoutRepository = FakeWorkoutRepository(),
    ) = WorkoutStore(routineRepository, workoutRepository, dispatcher = UnconfinedTestDispatcher())

    @Test
    fun initialState_hasNoRoutinesWhenRepositoryIsEmpty() {
        val store = newStore()

        assertTrue(store.state.value.folders.isEmpty())
        assertTrue(!store.state.value.hasRoutines)
    }

    @Test
    fun routines_areGroupedByFolderId_withUnfiledLast() {
        val routines = listOf(
            routine(1, "Push Day", folderId = 2),
            routine(2, "Leg Day", folderId = null),
            routine(3, "Pull Day", folderId = 1),
        )
        val store = newStore(routineRepository = FakeRoutineRepository(routines))

        val folderIds = store.state.value.folders.map { it.folderId }

        assertEquals(listOf(1L, 2L, null), folderIds)
        assertTrue(store.state.value.hasRoutines)
    }

    @Test
    fun startEmptyWorkout_createsWorkoutAndEmitsNavigationEffect() = runTest {
        val workoutRepository = FakeWorkoutRepository()
        val store = newStore(workoutRepository = workoutRepository)

        store.onIntent(WorkoutIntent.StartEmptyWorkout)
        val effect = store.effects.first()

        assertEquals(listOf("Empty Workout"), workoutRepository.added)
        assertEquals(WorkoutEffect.NavigateToActiveWorkout(1L), effect)
    }

    @Test
    fun startRoutine_namesTheWorkoutAfterTheRoutine() = runTest {
        val workoutRepository = FakeWorkoutRepository()
        val routineRepository = FakeRoutineRepository(listOf(routine(5, "Push Day")))
        val store = newStore(routineRepository, workoutRepository)

        store.onIntent(WorkoutIntent.StartRoutine(5L))
        val effect = store.effects.first()

        assertEquals(listOf("Push Day"), workoutRepository.added)
        assertEquals(WorkoutEffect.NavigateToActiveWorkout(1L), effect)
    }

    @Test
    fun startRoutine_unknownRoutineId_doesNothing() {
        val workoutRepository = FakeWorkoutRepository()
        val store = newStore(workoutRepository = workoutRepository)

        store.onIntent(WorkoutIntent.StartRoutine(999L))

        assertTrue(workoutRepository.added.isEmpty())
    }
}
