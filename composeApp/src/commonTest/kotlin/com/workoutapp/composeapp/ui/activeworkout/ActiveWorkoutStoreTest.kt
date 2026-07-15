package com.workoutapp.composeapp.ui.activeworkout

import com.workoutapp.composeapp.data.db.SetType
import com.workoutapp.composeapp.data.db.WorkoutPrivacy
import com.workoutapp.composeapp.data.library.ExerciseRepository
import com.workoutapp.composeapp.data.resttimer.RestTimerSettingsRepository
import com.workoutapp.composeapp.data.workout.PreviousSetResolver
import com.workoutapp.composeapp.data.workout.WorkoutExerciseRepository
import com.workoutapp.composeapp.data.workout.WorkoutRepository
import com.workoutapp.composeapp.data.workout.WorkoutSetRepository
import com.workoutapp.composeapp.db.Exercise
import com.workoutapp.composeapp.db.Workout
import com.workoutapp.composeapp.db.WorkoutExercise
import com.workoutapp.composeapp.db.WorkoutSet
import com.workoutapp.composeapp.ui.resttimer.RestTimerController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun workout(id: Long, startedAt: Long = 1_000L) =
    Workout(id, "Workout", startedAt, null, null, WorkoutPrivacy.PRIVATE, emptyList(), null, startedAt, "PENDING")

private fun exercise(id: Long, name: String) =
    Exercise(id, name, "Chest", emptyList(), "Barbell", null, false, null, null, 1_000L, "PENDING")

private fun workoutExercise(
    id: Long,
    workoutId: Long,
    exerciseId: Long,
    position: Long,
    supersetGroup: String? = null,
    restSeconds: Long? = null,
) = WorkoutExercise(id, workoutId, exerciseId, position, supersetGroup, restSeconds, null, null, 1_000L, "PENDING")

private fun workoutSet(
    id: Long,
    workoutExerciseId: Long,
    position: Long,
    reps: Long? = null,
    weight: Double? = null,
    durationSec: Long? = null,
    setType: SetType = SetType.NORMAL,
    completed: Boolean = false,
) = WorkoutSet(id, workoutExerciseId, position, reps, weight, durationSec, setType, completed, null, null, 1_000L, "PENDING")

private class FakeWorkoutRepository(private val workout: Workout?) : WorkoutRepository {
    override fun observeAll(): Flow<List<Workout>> = MutableStateFlow(listOfNotNull(workout))
    override fun observeById(id: Long): Flow<Workout?> = MutableStateFlow(workout)
    override suspend fun add(
        name: String,
        startedAt: Long,
        finishedAt: Long?,
        note: String?,
        privacy: WorkoutPrivacy,
        media: List<String>,
        updatedAt: Long,
    ): Long = error("not needed for these tests")

    override suspend fun delete(id: Long) = Unit
}

private class FakeWorkoutExerciseRepository(seed: List<WorkoutExercise>) : WorkoutExerciseRepository {
    private val exercisesFlow = MutableStateFlow(seed)
    private var nextId = (seed.maxOfOrNull { it.id } ?: 0L) + 1
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
        val newId = nextId++
        exercisesFlow.update { it + workoutExercise(newId, workoutId, exerciseId, position, restSeconds = restSeconds) }
        return newId
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

    override suspend fun delete(id: Long) {
        exercisesFlow.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun findMostRecentOtherWorkoutExerciseId(exerciseId: Long, excludingWorkoutId: Long): Long? =
        exercisesFlow.value
            .filter { it.exerciseId == exerciseId && it.workoutId != excludingWorkoutId }
            .maxByOrNull { it.id }
            ?.id
}

private class FakeWorkoutSetRepository(seed: List<WorkoutSet>) : WorkoutSetRepository {
    private val setsFlow = MutableStateFlow(seed)
    private var nextId = (seed.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeByWorkoutExerciseId(workoutExerciseId: Long): Flow<List<WorkoutSet>> =
        MutableStateFlow(setsFlow.value.filter { it.workoutExerciseId == workoutExerciseId })

    override fun observeByWorkoutId(workoutId: Long): Flow<List<WorkoutSet>> = setsFlow

    override suspend fun getByWorkoutExerciseId(workoutExerciseId: Long): List<WorkoutSet> =
        setsFlow.value.filter { it.workoutExerciseId == workoutExerciseId }.sortedBy { it.position }

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
        setsFlow.update {
            it + workoutSet(nextId++, workoutExerciseId, position, reps, weight, durationSec, setType, completed)
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
    ) {
        setsFlow.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(reps = reps, weight = weight, durationSec = durationSec, setType = setType, completed = completed)
                } else {
                    it
                }
            }
        }
    }

    override suspend fun updateRpe(id: Long, rpe: Double?) = Unit

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

private class FakeRestTimerController : RestTimerController {
    val startCalls = mutableListOf<Pair<Long, Int>>()

    override fun start(exerciseId: Long, seconds: Int) {
        startCalls += exerciseId to seconds
    }
}

private class FakeRestTimerSettingsRepository(private var defaultSeconds: Int = 90) : RestTimerSettingsRepository {
    override suspend fun getDefaultRestSeconds(): Int = defaultSeconds
    override suspend fun setDefaultRestSeconds(seconds: Int) {
        defaultSeconds = seconds
    }
}

class ActiveWorkoutStoreTest {
    private fun newStore(
        workout: Workout? = workout(1L),
        workoutExercises: List<WorkoutExercise> = emptyList(),
        sets: List<WorkoutSet> = emptyList(),
        exercises: List<Exercise> = emptyList(),
        recentExercises: List<Exercise> = emptyList(),
        otherWorkoutExercises: List<WorkoutExercise> = emptyList(),
        otherSets: List<WorkoutSet> = emptyList(),
        restTimerController: FakeRestTimerController = FakeRestTimerController(),
        defaultRestSeconds: Int = 90,
    ): ActiveWorkoutStore {
        val workoutExerciseRepository = FakeWorkoutExerciseRepository(workoutExercises + otherWorkoutExercises)
        val workoutSetRepository = FakeWorkoutSetRepository(sets + otherSets)
        return ActiveWorkoutStore(
            workoutId = 1L,
            workoutRepository = FakeWorkoutRepository(workout),
            workoutExerciseRepository = workoutExerciseRepository,
            workoutSetRepository = workoutSetRepository,
            exerciseRepository = FakeExerciseRepository(exercises, recentExercises),
            previousSetResolver = PreviousSetResolver(workoutExerciseRepository, workoutSetRepository),
            restTimerController = restTimerController,
            restTimerSettingsRepository = FakeRestTimerSettingsRepository(defaultRestSeconds),
            dispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun initialState_assemblesExercisesWithNamesAndSets() {
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(workoutSet(50L, 10L, position = 0L, reps = 8L, weight = 60.0)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        val exerciseUi = store.state.value.exercises.single()
        assertEquals("Bench Press", exerciseUi.exerciseName)
        assertEquals(1_000L, store.state.value.startedAt)
        val set = exerciseUi.sets.single().set
        assertEquals(8L, set.reps)
        assertEquals(60.0, set.weight)
    }

    @Test
    fun initialState_withPriorWorkoutForSameExercise_attachesPreviousValuesByPosition() {
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(workoutSet(50L, 10L, position = 0L)),
            exercises = listOf(exercise(100L, "Bench Press")),
            otherWorkoutExercises = listOf(workoutExercise(20L, 2L, 100L, position = 0L)),
            otherSets = listOf(workoutSet(60L, 20L, position = 0L, reps = 8L, weight = 60.0)),
        )

        val setUi = store.state.value.exercises.single().sets.single()
        assertEquals(8L, setUi.previousReps)
        assertEquals(60.0, setUi.previousWeight)
    }

    @Test
    fun initialState_withNoPriorWorkoutForExercise_leavesPreviousValuesNull() {
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(workoutSet(50L, 10L, position = 0L)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        val setUi = store.state.value.exercises.single().sets.single()
        assertNull(setUi.previousReps)
        assertNull(setUi.previousWeight)
        assertNull(setUi.previousDurationSec)
    }

    @Test
    fun initialState_withMultiplePriorWorkouts_usesTheMostRecentOne() {
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(workoutSet(50L, 10L, position = 0L)),
            exercises = listOf(exercise(100L, "Bench Press")),
            otherWorkoutExercises = listOf(
                workoutExercise(20L, 2L, 100L, position = 0L),
                workoutExercise(30L, 3L, 100L, position = 0L),
            ),
            otherSets = listOf(
                workoutSet(60L, 20L, position = 0L, reps = 5L, weight = 40.0),
                workoutSet(70L, 30L, position = 0L, reps = 8L, weight = 60.0),
            ),
        )

        val setUi = store.state.value.exercises.single().sets.single()
        assertEquals(8L, setUi.previousReps)
        assertEquals(60.0, setUi.previousWeight)
    }

    @Test
    fun initialState_matchesPreviousSetsByPositionAcrossMultipleSets() {
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(
                workoutSet(50L, 10L, position = 0L),
                workoutSet(51L, 10L, position = 1L),
            ),
            exercises = listOf(exercise(100L, "Bench Press")),
            otherWorkoutExercises = listOf(workoutExercise(20L, 2L, 100L, position = 0L)),
            otherSets = listOf(
                workoutSet(60L, 20L, position = 0L, reps = 8L, weight = 60.0),
                workoutSet(61L, 20L, position = 1L, reps = 6L, weight = 65.0),
            ),
        )

        val sets = store.state.value.exercises.single().sets
        assertEquals(8L to 60.0, sets[0].previousReps to sets[0].previousWeight)
        assertEquals(6L to 65.0, sets[1].previousReps to sets[1].previousWeight)
    }

    @Test
    fun addSet_appendsAtNextPosition() = runTest {
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(workoutSet(50L, 10L, position = 0L)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        store.onIntent(ActiveWorkoutIntent.AddSet(10L))

        val sets = store.state.value.exercises.single().sets
        assertEquals(2, sets.size)
        assertEquals(1L, sets[1].set.position)
    }

    @Test
    fun toggleSetComplete_flipsCompletedFlag() = runTest {
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(workoutSet(50L, 10L, position = 0L, completed = false)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        store.onIntent(ActiveWorkoutIntent.ToggleSetComplete(50L))

        assertTrue(store.state.value.exercises.single().sets.single().set.completed)
    }

    @Test
    fun toggleSetComplete_fromIncompleteToComplete_startsRestTimerWithGlobalDefault() = runTest {
        val restTimerController = FakeRestTimerController()
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(workoutSet(50L, 10L, position = 0L, completed = false)),
            exercises = listOf(exercise(100L, "Bench Press")),
            restTimerController = restTimerController,
            defaultRestSeconds = 90,
        )

        store.onIntent(ActiveWorkoutIntent.ToggleSetComplete(50L))

        assertEquals(listOf(100L to 90), restTimerController.startCalls)
    }

    @Test
    fun toggleSetComplete_fromCompleteToIncomplete_doesNotStartRestTimer() = runTest {
        val restTimerController = FakeRestTimerController()
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(workoutSet(50L, 10L, position = 0L, completed = true)),
            exercises = listOf(exercise(100L, "Bench Press")),
            restTimerController = restTimerController,
        )

        store.onIntent(ActiveWorkoutIntent.ToggleSetComplete(50L))

        assertFalse(store.state.value.exercises.single().sets.single().set.completed)
        assertTrue(restTimerController.startCalls.isEmpty())
    }

    @Test
    fun toggleSetComplete_withPerExerciseOverride_usesOverrideInsteadOfGlobalDefault() = runTest {
        val restTimerController = FakeRestTimerController()
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L, restSeconds = 45L)),
            sets = listOf(workoutSet(50L, 10L, position = 0L, completed = false)),
            exercises = listOf(exercise(100L, "Bench Press")),
            restTimerController = restTimerController,
            defaultRestSeconds = 90,
        )

        store.onIntent(ActiveWorkoutIntent.ToggleSetComplete(50L))

        assertEquals(listOf(100L to 45), restTimerController.startCalls)
    }

    @Test
    fun cycleRestOverride_stepsThroughPresetsAndWrapsBackToDefault() = runTest {
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )
        fun currentRestSeconds() = store.state.value.exercises.single().restSeconds

        assertNull(currentRestSeconds())
        store.onIntent(ActiveWorkoutIntent.CycleRestOverride(10L))
        assertEquals(30L, currentRestSeconds())
        store.onIntent(ActiveWorkoutIntent.CycleRestOverride(10L))
        assertEquals(60L, currentRestSeconds())
        repeat(REST_OVERRIDE_PRESETS.size - 2) {
            store.onIntent(ActiveWorkoutIntent.CycleRestOverride(10L))
        }
        assertNull(currentRestSeconds())
    }

    @Test
    fun updateReps_updatesOnlyReps() = runTest {
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(workoutSet(50L, 10L, position = 0L, reps = 5L, weight = 40.0)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        store.onIntent(ActiveWorkoutIntent.UpdateReps(50L, "12"))

        val set = store.state.value.exercises.single().sets.single().set
        assertEquals(12L, set.reps)
        assertEquals(40.0, set.weight)
    }

    @Test
    fun updateWeight_blankValue_clearsWeight() = runTest {
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(workoutSet(50L, 10L, position = 0L, weight = 40.0)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        store.onIntent(ActiveWorkoutIntent.UpdateWeight(50L, ""))

        assertNull(store.state.value.exercises.single().sets.single().set.weight)
    }

    @Test
    fun cycleSetType_cyclesThroughAllTypesAndWraps() = runTest {
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(workoutSet(50L, 10L, position = 0L, setType = SetType.NORMAL)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        val seen = mutableListOf<SetType>()
        repeat(SetType.entries.size) {
            store.onIntent(ActiveWorkoutIntent.CycleSetType(50L))
            seen += store.state.value.exercises.single().sets.single().set.setType
        }

        assertEquals(SetType.entries.drop(1) + SetType.NORMAL, seen)
    }

    @Test
    fun removeSet_deletesIt() = runTest {
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L)),
            sets = listOf(workoutSet(50L, 10L, position = 0L)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        store.onIntent(ActiveWorkoutIntent.RemoveSet(50L))

        assertTrue(store.state.value.exercises.single().sets.isEmpty())
    }

    @Test
    fun showAndHideAddExercise_togglesDialogVisibility() {
        val store = newStore()

        store.onIntent(ActiveWorkoutIntent.ShowAddExercise)
        assertTrue(store.state.value.showAddExercise)

        store.onIntent(ActiveWorkoutIntent.HideAddExercise)
        assertFalse(store.state.value.showAddExercise)
    }

    @Test
    fun addExercises_appendsAtNextPositionAndHidesDialog() = runTest {
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L)),
            exercises = listOf(exercise(100L, "Bench Press"), exercise(200L, "Squat")),
        )
        store.onIntent(ActiveWorkoutIntent.ShowAddExercise)

        store.onIntent(ActiveWorkoutIntent.AddExercises(listOf(200L)))

        val exercises = store.state.value.exercises
        assertEquals(2, exercises.size)
        assertEquals(1L, exercises[1].position)
        assertEquals("Squat", exercises[1].exerciseName)
        assertFalse(store.state.value.showAddExercise)
    }

    @Test
    fun addExercises_withMultipleIds_appendsAllInOrder() = runTest {
        val store = newStore(
            exercises = listOf(exercise(100L, "Bench Press"), exercise(200L, "Squat"), exercise(300L, "Deadlift")),
        )

        store.onIntent(ActiveWorkoutIntent.AddExercises(listOf(200L, 300L)))

        val names = store.state.value.exercises.sortedBy { it.position }.map { it.exerciseName }
        assertEquals(listOf("Squat", "Deadlift"), names)
    }

    @Test
    fun addExercises_withEmptyList_doesNothing() = runTest {
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )
        store.onIntent(ActiveWorkoutIntent.ShowAddExercise)

        store.onIntent(ActiveWorkoutIntent.AddExercises(emptyList()))

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
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        store.onIntent(ActiveWorkoutIntent.RemoveExercise(10L))

        assertTrue(store.state.value.exercises.isEmpty())
    }

    @Test
    fun moveExerciseDown_swapsPositionsWithNextExercise() = runTest {
        val store = newStore(
            workoutExercises = listOf(
                workoutExercise(10L, 1L, 100L, position = 0L),
                workoutExercise(20L, 1L, 200L, position = 1L),
            ),
            exercises = listOf(exercise(100L, "Bench Press"), exercise(200L, "Squat")),
        )

        store.onIntent(ActiveWorkoutIntent.MoveExerciseDown(10L))

        val names = store.state.value.exercises.sortedBy { it.position }.map { it.exerciseName }
        assertEquals(listOf("Squat", "Bench Press"), names)
    }

    @Test
    fun moveExerciseUp_atTop_doesNothing() = runTest {
        val store = newStore(
            workoutExercises = listOf(
                workoutExercise(10L, 1L, 100L, position = 0L),
                workoutExercise(20L, 1L, 200L, position = 1L),
            ),
            exercises = listOf(exercise(100L, "Bench Press"), exercise(200L, "Squat")),
        )

        store.onIntent(ActiveWorkoutIntent.MoveExerciseUp(10L))

        val names = store.state.value.exercises.sortedBy { it.position }.map { it.exerciseName }
        assertEquals(listOf("Bench Press", "Squat"), names)
    }

    @Test
    fun groupWithNextExercise_assignsMatchingSupersetGroupToBoth() = runTest {
        val store = newStore(
            workoutExercises = listOf(
                workoutExercise(10L, 1L, 100L, position = 0L),
                workoutExercise(20L, 1L, 200L, position = 1L),
            ),
            exercises = listOf(exercise(100L, "Bench Press"), exercise(200L, "Squat")),
        )

        store.onIntent(ActiveWorkoutIntent.GroupWithNextExercise(10L))

        val exercises = store.state.value.exercises.sortedBy { it.position }
        val groupA = exercises[0].supersetGroup
        assertEquals(groupA, exercises[1].supersetGroup)
        assertEquals("A", exercises[0].supersetLabel)
        assertEquals("A", exercises[1].supersetLabel)
    }

    @Test
    fun groupWithNextExercise_atLastExercise_doesNothing() = runTest {
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, position = 0L)),
            exercises = listOf(exercise(100L, "Bench Press")),
        )

        store.onIntent(ActiveWorkoutIntent.GroupWithNextExercise(10L))

        assertNull(store.state.value.exercises.single().supersetGroup)
    }

    @Test
    fun groupWithNextExercise_extendsAnExistingGroup() = runTest {
        val store = newStore(
            workoutExercises = listOf(
                workoutExercise(10L, 1L, 100L, position = 0L, supersetGroup = "sg-10"),
                workoutExercise(20L, 1L, 200L, position = 1L, supersetGroup = "sg-10"),
                workoutExercise(30L, 1L, 300L, position = 2L),
            ),
            exercises = listOf(exercise(100L, "Bench Press"), exercise(200L, "Squat"), exercise(300L, "Row")),
        )

        store.onIntent(ActiveWorkoutIntent.GroupWithNextExercise(20L))

        val exercises = store.state.value.exercises.sortedBy { it.position }
        assertEquals("sg-10", exercises[2].supersetGroup)
        assertEquals(setOf("A"), exercises.mapNotNull { it.supersetLabel }.toSet())
    }

    @Test
    fun removeFromSuperset_clearsGroupOnBothMembersOfAPair() = runTest {
        val store = newStore(
            workoutExercises = listOf(
                workoutExercise(10L, 1L, 100L, position = 0L, supersetGroup = "sg-10"),
                workoutExercise(20L, 1L, 200L, position = 1L, supersetGroup = "sg-10"),
            ),
            exercises = listOf(exercise(100L, "Bench Press"), exercise(200L, "Squat")),
        )

        store.onIntent(ActiveWorkoutIntent.RemoveFromSuperset(10L))

        val exercises = store.state.value.exercises
        assertTrue(exercises.all { it.supersetGroup == null })
        assertTrue(exercises.all { it.supersetLabel == null })
    }

    @Test
    fun removeFromSuperset_withThreeMembers_leavesTheOtherTwoGrouped() = runTest {
        val store = newStore(
            workoutExercises = listOf(
                workoutExercise(10L, 1L, 100L, position = 0L, supersetGroup = "sg-10"),
                workoutExercise(20L, 1L, 200L, position = 1L, supersetGroup = "sg-10"),
                workoutExercise(30L, 1L, 300L, position = 2L, supersetGroup = "sg-10"),
            ),
            exercises = listOf(exercise(100L, "Bench Press"), exercise(200L, "Squat"), exercise(300L, "Row")),
        )

        store.onIntent(ActiveWorkoutIntent.RemoveFromSuperset(10L))

        val exercises = store.state.value.exercises.sortedBy { it.position }
        assertNull(exercises[0].supersetGroup)
        assertEquals("sg-10", exercises[1].supersetGroup)
        assertEquals("sg-10", exercises[2].supersetGroup)
    }

    @Test
    fun supersetLogging_upNextExerciseAlternatesAsSetsComplete() = runTest {
        val store = newStore(
            workoutExercises = listOf(
                workoutExercise(10L, 1L, 100L, position = 0L, supersetGroup = "sg-10"),
                workoutExercise(20L, 1L, 200L, position = 1L, supersetGroup = "sg-10"),
            ),
            sets = listOf(
                workoutSet(50L, 10L, position = 0L),
                workoutSet(51L, 10L, position = 1L),
                workoutSet(60L, 20L, position = 0L),
                workoutSet(61L, 20L, position = 1L),
            ),
            exercises = listOf(exercise(100L, "Bench Press"), exercise(200L, "Squat")),
        )
        fun upNextName() = store.state.value.exercises.single { it.isUpNextInSuperset }.exerciseName

        assertEquals("Bench Press", upNextName())

        store.onIntent(ActiveWorkoutIntent.ToggleSetComplete(50L))
        assertEquals("Squat", upNextName())

        store.onIntent(ActiveWorkoutIntent.ToggleSetComplete(60L))
        assertEquals("Bench Press", upNextName())

        store.onIntent(ActiveWorkoutIntent.ToggleSetComplete(51L))
        assertEquals("Squat", upNextName())

        store.onIntent(ActiveWorkoutIntent.ToggleSetComplete(61L))
        assertTrue(store.state.value.exercises.none { it.isUpNextInSuperset })
    }
}
