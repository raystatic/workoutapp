package com.workoutapp.composeapp.ui.routinebuilder

import com.workoutapp.composeapp.data.db.SetType
import com.workoutapp.composeapp.data.library.ExerciseRepository
import com.workoutapp.composeapp.data.routines.RoutineExerciseRepository
import com.workoutapp.composeapp.data.routines.RoutineRepository
import com.workoutapp.composeapp.data.routines.RoutineSetRepository
import com.workoutapp.composeapp.db.Exercise
import com.workoutapp.composeapp.db.Routine
import com.workoutapp.composeapp.db.RoutineExercise
import com.workoutapp.composeapp.db.RoutineSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun routine(id: Long, name: String, folderId: Long? = null, notes: String? = null) =
    Routine(id, name, folderId, 0L, notes, null, 1_000L, "PENDING")

private fun exercise(id: Long, name: String) =
    Exercise(id, name, "Chest", emptyList(), "Barbell", null, false, null, null, 1_000L, "PENDING", null)

private fun routineExercise(
    id: Long,
    routineId: Long,
    exerciseId: Long,
    position: Long,
    supersetGroup: String? = null,
    restSeconds: Long? = null,
    notes: String? = null,
) = RoutineExercise(id, routineId, exerciseId, position, supersetGroup, restSeconds, notes, null, 1_000L, "PENDING")

private fun routineSet(
    id: Long,
    routineExerciseId: Long,
    position: Long,
    targetReps: Long? = null,
    targetWeight: Double? = null,
    setType: SetType = SetType.NORMAL,
) = RoutineSet(id, routineExerciseId, position, targetReps, targetWeight, setType, null, 1_000L, "PENDING")

private class FakeRoutineRepository(private val routine: Routine?) : RoutineRepository {
    data class UpdateCall(val id: Long, val name: String, val folderId: Long?, val notes: String?)

    val updateCalls = mutableListOf<UpdateCall>()

    override fun observeAll(): Flow<List<Routine>> = MutableStateFlow(listOfNotNull(routine))
    override fun observeById(id: Long): Flow<Routine?> = MutableStateFlow(routine)

    override suspend fun add(name: String, folderId: Long?, position: Long, notes: String?, updatedAt: Long): Long =
        error("not needed for these tests")

    override suspend fun update(id: Long, name: String, folderId: Long?, notes: String?, updatedAt: Long) {
        updateCalls += UpdateCall(id, name, folderId, notes)
    }

    override suspend fun delete(id: Long) = Unit
}

private class FakeRoutineExerciseRepository(seed: List<RoutineExercise>) : RoutineExerciseRepository {
    private val exercisesFlow = MutableStateFlow(seed)
    private var nextId = (seed.maxOfOrNull { it.id } ?: 0L) + 1
    val added = mutableListOf<Long>()

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
        added += exerciseId
        val id = nextId++
        exercisesFlow.update { it + routineExercise(id, routineId, exerciseId, position, supersetGroup, restSeconds, notes) }
        return id
    }

    override suspend fun updatePosition(id: Long, position: Long) {
        exercisesFlow.update { list -> list.map { if (it.id == id) it.copy(position = position) else it } }
    }

    override suspend fun updateSupersetGroup(id: Long, supersetGroup: String?) {
        exercisesFlow.update { list -> list.map { if (it.id == id) it.copy(supersetGroup = supersetGroup) else it } }
    }

    override suspend fun updateRestSeconds(id: Long, restSeconds: Long?) {
        exercisesFlow.update { list -> list.map { if (it.id == id) it.copy(restSeconds = restSeconds) else it } }
    }

    override suspend fun updateNotes(id: Long, notes: String?) {
        exercisesFlow.update { list -> list.map { if (it.id == id) it.copy(notes = notes) else it } }
    }

    override suspend fun delete(id: Long) {
        exercisesFlow.update { list -> list.filterNot { it.id == id } }
    }
}

private class FakeRoutineSetRepository(seed: List<RoutineSet>) : RoutineSetRepository {
    private val setsFlow = MutableStateFlow(seed)
    private var nextId = (seed.maxOfOrNull { it.id } ?: 0L) + 1

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
        setsFlow.update { it + routineSet(nextId++, routineExerciseId, position, targetReps, targetWeight, setType) }
    }

    override suspend fun update(id: Long, targetReps: Long?, targetWeight: Double?, setType: SetType, updatedAt: Long) {
        setsFlow.update { list ->
            list.map { if (it.id == id) it.copy(targetReps = targetReps, targetWeight = targetWeight, setType = setType) else it }
        }
    }

    override suspend fun updatePosition(id: Long, position: Long) {
        setsFlow.update { list -> list.map { if (it.id == id) it.copy(position = position) else it } }
    }

    override suspend fun delete(id: Long) {
        setsFlow.update { list -> list.filterNot { it.id == id } }
    }
}

private class FakeExerciseRepository(seed: List<Exercise>, private val recent: List<Exercise> = emptyList()) : ExerciseRepository {
    private val exercisesFlow = MutableStateFlow(seed)

    override fun observeAll(): Flow<List<Exercise>> = exercisesFlow

    override fun observeRecentlyUsed(limit: Int): Flow<List<Exercise>> = MutableStateFlow(recent.take(limit))

    override suspend fun add(
        name: String,
        primaryMuscle: String,
        equipment: String,
        secondaryMuscles: List<String>,
        mediaUrl: String?,
        isCustom: Boolean,
        instructions: String?,
        updatedAt: Long,
    ) = error("not needed for these tests")

    override suspend fun delete(id: Long) = Unit
}

class RoutineBuilderStoreTest {
    private fun newStore(
        routine: Routine? = routine(1L, "Push Day"),
        routineExercises: List<RoutineExercise> = emptyList(),
        sets: List<RoutineSet> = emptyList(),
        exercises: List<Exercise> = emptyList(),
        recentExercises: List<Exercise> = emptyList(),
        routineRepository: FakeRoutineRepository = FakeRoutineRepository(routine),
    ): RoutineBuilderStore = RoutineBuilderStore(
        routineId = 1L,
        routineRepository = routineRepository,
        routineExerciseRepository = FakeRoutineExerciseRepository(routineExercises),
        routineSetRepository = FakeRoutineSetRepository(sets),
        exerciseRepository = FakeExerciseRepository(exercises, recentExercises),
        dispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun initialState_loadsNameFolderAndExercisesFromTheRoutine() {
        val store = newStore(
            routine = routine(1L, "Push Day", folderId = 3L),
            routineExercises = listOf(routineExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(routineSet(20L, 10L, position = 0L, targetReps = 8L, targetWeight = 60.0)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        assertEquals("Push Day", store.state.value.name)
        assertEquals("3", store.state.value.folderIdText)
        val exerciseUi = store.state.value.exercises.single()
        assertEquals("Bench Press", exerciseUi.exerciseName)
        assertEquals("8", exerciseUi.sets.single().targetRepsText)
        assertEquals("60", exerciseUi.sets.single().targetWeightText)
    }

    @Test
    fun nameChanged_persistsTheNewNameAndPreservesFolderAndNotes() = runTest {
        val routineRepository = FakeRoutineRepository(routine(1L, "Push Day", folderId = 3L, notes = "chest focus"))
        val store = newStore(routineRepository = routineRepository)

        store.onIntent(RoutineBuilderIntent.NameChanged("Push Day v2"))

        val call = routineRepository.updateCalls.single()
        assertEquals("Push Day v2", call.name)
        assertEquals(3L, call.folderId)
        assertEquals("chest focus", call.notes)
    }

    @Test
    fun folderIdChanged_blank_clearsFolder() = runTest {
        val routineRepository = FakeRoutineRepository(routine(1L, "Push Day", folderId = 3L))
        val store = newStore(routineRepository = routineRepository)

        store.onIntent(RoutineBuilderIntent.FolderIdChanged(""))

        assertNull(routineRepository.updateCalls.single().folderId)
    }

    @Test
    fun addExercises_appendsAtNextPositionAndHidesDialog() = runTest {
        val store = newStore(
            routineExercises = listOf(routineExercise(10L, 1L, 100L, position = 0L)),
            exercises = listOf(exercise(100L, "Bench Press"), exercise(200L, "Squat")),
        )
        store.onIntent(RoutineBuilderIntent.ShowAddExercise)

        store.onIntent(RoutineBuilderIntent.AddExercises(listOf(200L)))

        val exercises = store.state.value.exercises
        assertEquals(2, exercises.size)
        assertEquals(1L, exercises[1].position)
        assertEquals("Squat", exercises[1].exerciseName)
        assertTrue(!store.state.value.showAddExercise)
    }

    @Test
    fun addExercises_withMultipleIds_appendsAllInOrder() = runTest {
        val store = newStore(
            exercises = listOf(exercise(100L, "Bench Press"), exercise(200L, "Squat"), exercise(300L, "Deadlift")),
        )

        store.onIntent(RoutineBuilderIntent.AddExercises(listOf(200L, 300L)))

        val names = store.state.value.exercises.sortedBy { it.position }.map { it.exerciseName }
        assertEquals(listOf("Squat", "Deadlift"), names)
    }

    @Test
    fun addExercises_withEmptyList_doesNothing() = runTest {
        val store = newStore(
            routineExercises = listOf(routineExercise(10L, 1L, 100L, position = 0L)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )
        store.onIntent(RoutineBuilderIntent.ShowAddExercise)

        store.onIntent(RoutineBuilderIntent.AddExercises(emptyList()))

        assertEquals(1, store.state.value.exercises.size)
        assertTrue(store.state.value.showAddExercise)
    }

    @Test
    fun initialState_exposesRecentlyUsedExercises() {
        val store = newStore(
            exercises = listOf(exercise(100L, "Bench Press"), exercise(200L, "Squat")),
            recentExercises = listOf(exercise(200L, "Squat")),
        )

        assertEquals(listOf("Squat"), store.state.value.recentExercises.map { it.name })
    }

    @Test
    fun removeExercise_removesIt() = runTest {
        val store = newStore(
            routineExercises = listOf(routineExercise(10L, 1L, 100L, position = 0L)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        store.onIntent(RoutineBuilderIntent.RemoveExercise(10L))

        assertTrue(store.state.value.exercises.isEmpty())
    }

    @Test
    fun moveExerciseDown_swapsPositionsWithNextExercise() = runTest {
        val store = newStore(
            routineExercises = listOf(
                routineExercise(10L, 1L, 100L, position = 0L),
                routineExercise(20L, 1L, 200L, position = 1L),
            ),
            exercises = listOf(exercise(100L, "Bench Press"), exercise(200L, "Squat")),
        )

        store.onIntent(RoutineBuilderIntent.MoveExerciseDown(10L))

        val names = store.state.value.exercises.sortedBy { it.position }.map { it.exerciseName }
        assertEquals(listOf("Squat", "Bench Press"), names)
    }

    @Test
    fun moveExerciseUp_atTop_doesNothing() = runTest {
        val store = newStore(
            routineExercises = listOf(
                routineExercise(10L, 1L, 100L, position = 0L),
                routineExercise(20L, 1L, 200L, position = 1L),
            ),
            exercises = listOf(exercise(100L, "Bench Press"), exercise(200L, "Squat")),
        )

        store.onIntent(RoutineBuilderIntent.MoveExerciseUp(10L))

        val names = store.state.value.exercises.sortedBy { it.position }.map { it.exerciseName }
        assertEquals(listOf("Bench Press", "Squat"), names)
    }

    @Test
    fun groupWithNextExercise_assignsMatchingSupersetGroupToBoth() = runTest {
        val store = newStore(
            routineExercises = listOf(
                routineExercise(10L, 1L, 100L, position = 0L),
                routineExercise(20L, 1L, 200L, position = 1L),
            ),
            exercises = listOf(exercise(100L, "Bench Press"), exercise(200L, "Squat")),
        )

        store.onIntent(RoutineBuilderIntent.GroupWithNextExercise(10L))

        val exercises = store.state.value.exercises.sortedBy { it.position }
        assertEquals(exercises[0].supersetGroup, exercises[1].supersetGroup)
        assertEquals("A", exercises[0].supersetLabel)
        assertEquals("A", exercises[1].supersetLabel)
    }

    @Test
    fun removeFromSuperset_clearsGroupOnBothMembersOfAPair() = runTest {
        val store = newStore(
            routineExercises = listOf(
                routineExercise(10L, 1L, 100L, position = 0L, supersetGroup = "sg-10"),
                routineExercise(20L, 1L, 200L, position = 1L, supersetGroup = "sg-10"),
            ),
            exercises = listOf(exercise(100L, "Bench Press"), exercise(200L, "Squat")),
        )

        store.onIntent(RoutineBuilderIntent.RemoveFromSuperset(10L))

        val exercises = store.state.value.exercises
        assertTrue(exercises.all { it.supersetGroup == null })
        assertTrue(exercises.all { it.supersetLabel == null })
    }

    @Test
    fun updateExerciseNotes_persistsTheNotes() = runTest {
        val store = newStore(
            routineExercises = listOf(routineExercise(10L, 1L, 100L, position = 0L)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        store.onIntent(RoutineBuilderIntent.UpdateExerciseNotes(10L, "pause at the bottom"))

        assertEquals("pause at the bottom", store.state.value.exercises.single().notes)
    }

    @Test
    fun updateRestSeconds_blankValue_clearsIt() = runTest {
        val store = newStore(
            routineExercises = listOf(routineExercise(10L, 1L, 100L, position = 0L, restSeconds = 90L)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        store.onIntent(RoutineBuilderIntent.UpdateRestSeconds(10L, ""))

        assertNull(store.state.value.exercises.single().restSeconds)
    }

    @Test
    fun addSet_appendsAtNextPosition() = runTest {
        val store = newStore(
            routineExercises = listOf(routineExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(routineSet(20L, 10L, position = 0L)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        store.onIntent(RoutineBuilderIntent.AddSet(10L))

        val sets = store.state.value.exercises.single().sets
        assertEquals(2, sets.size)
        assertEquals(1L, sets[1].set.position)
    }

    @Test
    fun removeSet_deletesIt() = runTest {
        val store = newStore(
            routineExercises = listOf(routineExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(routineSet(20L, 10L, position = 0L)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        store.onIntent(RoutineBuilderIntent.RemoveSet(20L))

        assertTrue(store.state.value.exercises.single().sets.isEmpty())
    }

    @Test
    fun updateTargetReps_updatesOnlyReps() = runTest {
        val store = newStore(
            routineExercises = listOf(routineExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(routineSet(20L, 10L, position = 0L, targetReps = 5L, targetWeight = 40.0)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        store.onIntent(RoutineBuilderIntent.UpdateTargetReps(20L, "12"))

        val set = store.state.value.exercises.single().sets.single()
        assertEquals("12", set.targetRepsText)
        assertEquals("40", set.targetWeightText)
    }

    @Test
    fun updateTargetWeight_blankValue_clearsWeight() = runTest {
        val store = newStore(
            routineExercises = listOf(routineExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(routineSet(20L, 10L, position = 0L, targetWeight = 40.0)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        store.onIntent(RoutineBuilderIntent.UpdateTargetWeight(20L, ""))

        assertEquals("", store.state.value.exercises.single().sets.single().targetWeightText)
    }

    @Test
    fun cycleSetType_cyclesThroughAllTypesAndWraps() = runTest {
        val store = newStore(
            routineExercises = listOf(routineExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(routineSet(20L, 10L, position = 0L, setType = SetType.NORMAL)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        val seen = mutableListOf<SetType>()
        repeat(SetType.entries.size) {
            store.onIntent(RoutineBuilderIntent.CycleSetType(20L))
            seen += store.state.value.exercises.single().sets.single().set.setType
        }

        assertEquals(SetType.entries.drop(1) + SetType.NORMAL, seen)
    }

    @Test
    fun showAndHideAddExercise_togglesDialogVisibility() {
        val store = newStore()

        store.onIntent(RoutineBuilderIntent.ShowAddExercise)
        assertTrue(store.state.value.showAddExercise)

        store.onIntent(RoutineBuilderIntent.HideAddExercise)
        assertTrue(!store.state.value.showAddExercise)
    }
}
