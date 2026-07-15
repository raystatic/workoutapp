package com.workoutapp.composeapp.ui.exercisedetail

import com.workoutapp.composeapp.data.db.SetType
import com.workoutapp.composeapp.data.library.ExerciseRepository
import com.workoutapp.composeapp.data.workout.ExerciseSetHistoryEntry
import com.workoutapp.composeapp.data.workout.WorkoutSetRepository
import com.workoutapp.composeapp.db.Exercise
import com.workoutapp.composeapp.db.WorkoutSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun exercise(
    id: Long = 100L,
    name: String = "Bench Press",
    primaryMuscle: String = "Chest",
    secondaryMuscles: List<String> = emptyList(),
    equipment: String = "Barbell",
    mediaUrl: String? = null,
    instructions: String? = "Lie back and press.",
) = Exercise(id, name, primaryMuscle, secondaryMuscles, equipment, mediaUrl, false, instructions, null, 1_000L, "PENDING")

private fun historyEntry(id: Long, workoutStartedAt: Long, weight: Double? = null, reps: Long? = null) =
    ExerciseSetHistoryEntry(
        set = WorkoutSet(id, 1L, 0L, reps, weight, null, SetType.NORMAL, true, null, null, workoutStartedAt, "PENDING"),
        workoutStartedAt = workoutStartedAt,
    )

private class FakeExerciseRepository(private val seed: Exercise?) : ExerciseRepository {
    override fun observeAll(): Flow<List<Exercise>> = MutableStateFlow(listOfNotNull(seed))
    override fun observeById(id: Long): Flow<Exercise?> = MutableStateFlow(seed?.takeIf { it.id == id })
    override fun observeRecentlyUsed(limit: Int): Flow<List<Exercise>> = MutableStateFlow(emptyList())

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

private class FakeWorkoutSetRepository(private val history: List<ExerciseSetHistoryEntry>) : WorkoutSetRepository {
    override fun observeByWorkoutExerciseId(workoutExerciseId: Long): Flow<List<WorkoutSet>> = MutableStateFlow(emptyList())
    override fun observeByWorkoutId(workoutId: Long): Flow<List<WorkoutSet>> = MutableStateFlow(emptyList())
    override fun observeHistoryByExerciseId(exerciseId: Long): Flow<List<ExerciseSetHistoryEntry>> = MutableStateFlow(history)
    override suspend fun getByWorkoutExerciseId(workoutExerciseId: Long): List<WorkoutSet> = emptyList()

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
    ) = error("not needed for these tests")

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

class ExerciseDetailStoreTest {
    private fun newStore(
        exercise: Exercise? = exercise(),
        history: List<ExerciseSetHistoryEntry> = emptyList(),
        exerciseId: Long = 100L,
    ) = ExerciseDetailStore(
        exerciseId = exerciseId,
        exerciseRepository = FakeExerciseRepository(exercise),
        workoutSetRepository = FakeWorkoutSetRepository(history),
        dispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun initialState_loadsTheExercisesOwnFields() = runTest {
        val store = newStore(exercise = exercise(name = "Barbell Squat", primaryMuscle = "Quadriceps", equipment = "Barbell"))

        assertEquals("Barbell Squat", store.state.value.exercise?.name)
        assertEquals("Quadriceps", store.state.value.exercise?.primaryMuscle)
        assertEquals(false, store.state.value.isLoading)
    }

    @Test
    fun unknownExercise_stateExposesNullExercise() = runTest {
        val store = newStore(exercise = null)

        assertNull(store.state.value.exercise)
    }

    @Test
    fun recentSets_reflectsHistoryNewestFirst() = runTest {
        val history = listOf(historyEntry(3L, 3000L, weight = 100.0), historyEntry(2L, 2000L, weight = 90.0))
        val store = newStore(history = history)

        assertEquals(listOf(3L, 2L), store.state.value.recentSets.map { it.set.id })
    }

    @Test
    fun bestWeightSeries_aggregatesOnePointPerWorkoutOldestFirst() = runTest {
        val history = listOf(
            historyEntry(1L, 2000L, weight = 100.0),
            historyEntry(2L, 1000L, weight = 80.0),
            historyEntry(3L, 1000L, weight = 85.0),
        )
        val store = newStore(history = history)

        val series = store.state.value.bestWeightSeries
        assertEquals(2, series.size)
        assertEquals(1000L, series.first().workoutStartedAt)
        assertEquals(85.0, series.first().weight)
        assertEquals(2000L, series.last().workoutStartedAt)
        assertEquals(100.0, series.last().weight)
    }

    @Test
    fun noHistory_recentSetsAndBestWeightSeriesAreEmpty() = runTest {
        val store = newStore(history = emptyList())

        assertTrue(store.state.value.recentSets.isEmpty())
        assertTrue(store.state.value.bestWeightSeries.isEmpty())
    }
}
