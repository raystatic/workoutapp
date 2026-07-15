package com.workoutapp.composeapp.ui.workout

import com.workoutapp.composeapp.data.db.SetType
import com.workoutapp.composeapp.data.db.WorkoutPrivacy
import com.workoutapp.composeapp.data.routines.RoutineExerciseRepository
import com.workoutapp.composeapp.data.routines.RoutineRepository
import com.workoutapp.composeapp.data.routines.RoutineSetRepository
import com.workoutapp.composeapp.data.workout.WorkoutExerciseRepository
import com.workoutapp.composeapp.data.workout.WorkoutRepository
import com.workoutapp.composeapp.data.workout.WorkoutSetRepository
import com.workoutapp.composeapp.db.Routine
import com.workoutapp.composeapp.db.RoutineExercise
import com.workoutapp.composeapp.db.RoutineSet
import com.workoutapp.composeapp.db.Workout
import com.workoutapp.composeapp.db.WorkoutExercise
import com.workoutapp.composeapp.db.WorkoutSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeRoutineRepository(seed: List<Routine> = emptyList()) : RoutineRepository {
    private val routines = MutableStateFlow(seed)
    private var nextId = (seed.maxOfOrNull { it.id } ?: 0L) + 1
    val addCalls = mutableListOf<String>()
    val updateCalls = mutableListOf<Long>()

    override fun observeAll(): Flow<List<Routine>> = routines

    override fun observeById(id: Long): Flow<Routine?> = routines.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun add(name: String, folderId: Long?, position: Long, notes: String?, updatedAt: Long): Long {
        addCalls += name
        val id = nextId++
        routines.update { it + routine(id, name, folderId) }
        return id
    }

    override suspend fun update(id: Long, name: String, folderId: Long?, notes: String?, updatedAt: Long) {
        updateCalls += id
        routines.update { list -> list.map { if (it.id == id) it.copy(name = name, folderId = folderId, notes = notes) else it } }
    }

    override suspend fun delete(id: Long) {
        routines.update { list -> list.filterNot { it.id == id } }
    }
}

private class FakeRoutineExerciseRepository(seed: List<RoutineExercise> = emptyList()) : RoutineExerciseRepository {
    private val exercisesFlow = MutableStateFlow(seed)
    private var nextId = (seed.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeByRoutineId(routineId: Long): Flow<List<RoutineExercise>> =
        exercisesFlow.map { list -> list.filter { it.routineId == routineId } }

    override suspend fun add(
        routineId: Long,
        exerciseId: Long,
        position: Long,
        supersetGroup: String?,
        restSeconds: Long?,
        notes: String?,
        updatedAt: Long,
    ): Long {
        val id = nextId++
        exercisesFlow.update {
            it + RoutineExercise(id, routineId, exerciseId, position, supersetGroup, restSeconds, notes, null, updatedAt, "PENDING")
        }
        return id
    }

    override suspend fun updatePosition(id: Long, position: Long) = error("not needed for these tests")
    override suspend fun updateSupersetGroup(id: Long, supersetGroup: String?) = error("not needed for these tests")
    override suspend fun updateRestSeconds(id: Long, restSeconds: Long?) = error("not needed for these tests")
    override suspend fun updateNotes(id: Long, notes: String?) = error("not needed for these tests")
    override suspend fun delete(id: Long) = Unit
}

private class FakeRoutineSetRepository(seed: List<RoutineSet> = emptyList()) : RoutineSetRepository {
    private val setsFlow = MutableStateFlow(seed)
    private var nextId = (seed.maxOfOrNull { it.id } ?: 0L) + 1
    val added = mutableListOf<Long>()

    override fun observeByRoutineExerciseId(routineExerciseId: Long): Flow<List<RoutineSet>> =
        setsFlow.map { list -> list.filter { it.routineExerciseId == routineExerciseId } }

    override fun observeByRoutineId(routineId: Long): Flow<List<RoutineSet>> = setsFlow

    override suspend fun add(
        routineExerciseId: Long,
        position: Long,
        targetReps: Long?,
        targetWeight: Double?,
        setType: SetType,
        updatedAt: Long,
    ) {
        added += routineExerciseId
        setsFlow.update {
            it + RoutineSet(nextId++, routineExerciseId, position, targetReps, targetWeight, setType, null, updatedAt, "PENDING")
        }
    }

    override suspend fun update(id: Long, targetReps: Long?, targetWeight: Double?, setType: SetType, updatedAt: Long) =
        error("not needed for these tests")

    override suspend fun updatePosition(id: Long, position: Long) = error("not needed for these tests")
    override suspend fun delete(id: Long) = Unit
}

private class FakeWorkoutRepository : WorkoutRepository {
    private var nextId = 1L
    val added = mutableListOf<String>()

    override fun observeAll(): Flow<List<Workout>> = MutableStateFlow(emptyList())

    override fun observeById(id: Long): Flow<Workout?> = MutableStateFlow(null)

    override suspend fun getById(id: Long): Workout? = null

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

    override suspend fun update(
        id: Long,
        name: String,
        finishedAt: Long?,
        note: String?,
        privacy: WorkoutPrivacy,
        media: List<String>,
        updatedAt: Long,
    ) = Unit

    override suspend fun getCompletedWorkoutFinishedAtTimestamps(): List<Long> = emptyList()

    override suspend fun delete(id: Long) = Unit
}

private class FakeWorkoutExerciseRepository : WorkoutExerciseRepository {
    private val exercisesFlow = MutableStateFlow(emptyList<WorkoutExercise>())
    private var nextId = 1L
    val added = mutableListOf<Pair<Long, Long>>()

    override fun observeByWorkoutId(workoutId: Long): Flow<List<WorkoutExercise>> =
        exercisesFlow.map { list -> list.filter { it.workoutId == workoutId } }

    override suspend fun add(
        workoutId: Long,
        exerciseId: Long,
        position: Long,
        supersetGroup: String?,
        restSeconds: Long?,
        notes: String?,
        updatedAt: Long,
    ): Long {
        added += workoutId to exerciseId
        val id = nextId++
        exercisesFlow.update {
            it + WorkoutExercise(id, workoutId, exerciseId, position, supersetGroup, restSeconds, notes, null, updatedAt, "PENDING")
        }
        return id
    }

    override suspend fun updatePosition(id: Long, position: Long) = error("not needed for these tests")
    override suspend fun updateSupersetGroup(id: Long, supersetGroup: String?) = error("not needed for these tests")
    override suspend fun updateRestSeconds(id: Long, restSeconds: Long?) = error("not needed for these tests")
    override suspend fun delete(id: Long) = Unit
    override suspend fun findMostRecentOtherWorkoutExerciseId(exerciseId: Long, excludingWorkoutId: Long): Long? = null
}

private class FakeWorkoutSetRepository : WorkoutSetRepository {
    private val setsFlow = MutableStateFlow(emptyList<WorkoutSet>())
    private var nextId = 1L
    val added = mutableListOf<Long>()

    override fun observeByWorkoutExerciseId(workoutExerciseId: Long): Flow<List<WorkoutSet>> =
        setsFlow.map { list -> list.filter { it.workoutExerciseId == workoutExerciseId } }

    override fun observeByWorkoutId(workoutId: Long): Flow<List<WorkoutSet>> = setsFlow

    override suspend fun getByWorkoutExerciseId(workoutExerciseId: Long): List<WorkoutSet> =
        setsFlow.value.filter { it.workoutExerciseId == workoutExerciseId }

    override suspend fun add(
        workoutExerciseId: Long,
        position: Long,
        reps: Long?,
        weight: Double?,
        durationSec: Long?,
        setType: SetType,
        completed: Boolean,
        rpe: Double?,
        updatedAt: Long,
    ) {
        added += workoutExerciseId
        setsFlow.update {
            it + WorkoutSet(nextId++, workoutExerciseId, position, reps, weight, durationSec, setType, completed, rpe, null, updatedAt, "PENDING")
        }
    }

    override suspend fun update(
        id: Long,
        reps: Long?,
        weight: Double?,
        durationSec: Long?,
        setType: SetType,
        completed: Boolean,
        updatedAt: Long,
    ) = error("not needed for these tests")

    override suspend fun updateRpe(id: Long, rpe: Double?) = error("not needed for these tests")
    override suspend fun updatePosition(id: Long, position: Long) = error("not needed for these tests")
    override suspend fun delete(id: Long) = Unit
}

private fun routine(id: Long, name: String, folderId: Long? = null) =
    Routine(id, name, folderId, 0L, null, null, 0L, "PENDING")

class WorkoutStoreTest {
    private fun newStore(
        routineRepository: RoutineRepository = FakeRoutineRepository(),
        workoutRepository: WorkoutRepository = FakeWorkoutRepository(),
        routineExerciseRepository: RoutineExerciseRepository = FakeRoutineExerciseRepository(),
        routineSetRepository: RoutineSetRepository = FakeRoutineSetRepository(),
        workoutExerciseRepository: WorkoutExerciseRepository = FakeWorkoutExerciseRepository(),
        workoutSetRepository: WorkoutSetRepository = FakeWorkoutSetRepository(),
    ) = WorkoutStore(
        routineRepository,
        workoutRepository,
        routineExerciseRepository,
        routineSetRepository,
        workoutExerciseRepository,
        workoutSetRepository,
        dispatcher = UnconfinedTestDispatcher(),
    )

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

    @Test
    fun startRoutine_copiesRoutineExercisesAndTargetSetsIntoTheNewWorkout() = runTest {
        val routineRepository = FakeRoutineRepository(listOf(routine(5, "Push Day")))
        val routineExerciseRepository = FakeRoutineExerciseRepository(
            listOf(RoutineExercise(10L, 5L, 100L, 0L, "sg-1", 45L, "warm up first", null, 0L, "PENDING")),
        )
        val routineSetRepository = FakeRoutineSetRepository(
            listOf(RoutineSet(20L, 10L, 0L, 8L, 60.0, SetType.NORMAL, null, 0L, "PENDING")),
        )
        val workoutExerciseRepository = FakeWorkoutExerciseRepository()
        val workoutSetRepository = FakeWorkoutSetRepository()
        val store = newStore(
            routineRepository = routineRepository,
            routineExerciseRepository = routineExerciseRepository,
            routineSetRepository = routineSetRepository,
            workoutExerciseRepository = workoutExerciseRepository,
            workoutSetRepository = workoutSetRepository,
        )

        store.onIntent(WorkoutIntent.StartRoutine(5L))
        val effect = store.effects.first() as WorkoutEffect.NavigateToActiveWorkout

        val copiedExercise = workoutExerciseRepository.observeByWorkoutId(effect.workoutId).first().single()
        assertEquals(100L, copiedExercise.exerciseId)
        assertEquals("sg-1", copiedExercise.supersetGroup)
        assertEquals(45L, copiedExercise.restSeconds)
        assertEquals("warm up first", copiedExercise.notes)

        val copiedSet = workoutSetRepository.observeByWorkoutExerciseId(copiedExercise.id).first().single()
        assertEquals(8L, copiedSet.reps)
        assertEquals(60.0, copiedSet.weight)
        assertEquals(SetType.NORMAL, copiedSet.setType)
    }

    @Test
    fun createRoutine_addsAPlaceholderRoutineAndEmitsBuilderNavigation() = runTest {
        val routineRepository = FakeRoutineRepository()
        val store = newStore(routineRepository = routineRepository)

        store.onIntent(WorkoutIntent.CreateRoutine)
        val effect = store.effects.first()

        assertEquals(listOf("New Routine"), routineRepository.addCalls)
        assertEquals(WorkoutEffect.NavigateToRoutineBuilder(1L), effect)
    }

    @Test
    fun editRoutine_emitsBuilderNavigationForThatRoutine() = runTest {
        val store = newStore()

        store.onIntent(WorkoutIntent.EditRoutine(42L))
        val effect = store.effects.first()

        assertEquals(WorkoutEffect.NavigateToRoutineBuilder(42L), effect)
    }

    @Test
    fun duplicateRoutine_copiesNameExercisesAndSets() = runTest {
        val routineRepository = FakeRoutineRepository(listOf(routine(5, "Push Day")))
        val routineExerciseRepository = FakeRoutineExerciseRepository(
            listOf(RoutineExercise(10L, 5L, 100L, 0L, null, null, null, null, 0L, "PENDING")),
        )
        val routineSetRepository = FakeRoutineSetRepository(
            listOf(RoutineSet(20L, 10L, 0L, 8L, 60.0, SetType.NORMAL, null, 0L, "PENDING")),
        )
        val store = newStore(
            routineRepository = routineRepository,
            routineExerciseRepository = routineExerciseRepository,
            routineSetRepository = routineSetRepository,
        )

        store.onIntent(WorkoutIntent.DuplicateRoutine(5L))
        val newRoutine = routineRepository.observeAll().first().first { it.name == "Push Day copy" }

        val copiedExercises = routineExerciseRepository.observeByRoutineId(newRoutine.id).first()
        assertEquals(1, copiedExercises.size)
        assertEquals(100L, copiedExercises.single().exerciseId)
        assertEquals(1, routineSetRepository.added.count { it == copiedExercises.single().id })
    }

    @Test
    fun requestThenConfirmDeleteRoutine_removesIt() = runTest {
        val routineRepository = FakeRoutineRepository(listOf(routine(5, "Push Day")))
        val store = newStore(routineRepository = routineRepository)

        store.onIntent(WorkoutIntent.RequestDeleteRoutine(5L))
        assertEquals(5L, store.state.value.routineIdPendingDelete)

        store.onIntent(WorkoutIntent.ConfirmDeleteRoutine)

        assertNull(store.state.value.routineIdPendingDelete)
        assertTrue(routineRepository.observeAll().first().none { it.id == 5L })
    }

    @Test
    fun requestThenCancelDeleteRoutine_leavesItIntact() = runTest {
        val routineRepository = FakeRoutineRepository(listOf(routine(5, "Push Day")))
        val store = newStore(routineRepository = routineRepository)

        store.onIntent(WorkoutIntent.RequestDeleteRoutine(5L))
        store.onIntent(WorkoutIntent.CancelDeleteRoutine)

        assertNull(store.state.value.routineIdPendingDelete)
        assertTrue(routineRepository.observeAll().first().any { it.id == 5L })
    }
}
