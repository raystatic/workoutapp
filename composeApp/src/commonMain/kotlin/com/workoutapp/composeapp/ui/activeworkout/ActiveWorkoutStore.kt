package com.workoutapp.composeapp.ui.activeworkout

import com.workoutapp.composeapp.data.db.SetType
import com.workoutapp.composeapp.data.db.currentTimeMillis
import com.workoutapp.composeapp.data.library.ExerciseRepository
import com.workoutapp.composeapp.data.workout.WorkoutExerciseRepository
import com.workoutapp.composeapp.data.workout.WorkoutRepository
import com.workoutapp.composeapp.data.workout.WorkoutSetRepository
import com.workoutapp.composeapp.db.Exercise
import com.workoutapp.composeapp.db.WorkoutExercise
import com.workoutapp.composeapp.db.WorkoutSet
import com.workoutapp.composeapp.mvi.MviEffect
import com.workoutapp.composeapp.mvi.MviIntent
import com.workoutapp.composeapp.mvi.MviState
import com.workoutapp.composeapp.mvi.StoreViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** One exercise within the in-progress workout, with its sets in logging order. */
data class ActiveWorkoutExerciseUi(
    val workoutExerciseId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val position: Long,
    val sets: List<WorkoutSet>,
)

data class ActiveWorkoutState(
    val workoutId: Long,
    val startedAt: Long? = null,
    val exercises: List<ActiveWorkoutExerciseUi> = emptyList(),
    val availableExercises: List<Exercise> = emptyList(),
    val showAddExercise: Boolean = false,
) : MviState

sealed interface ActiveWorkoutIntent : MviIntent {
    data class AddSet(val workoutExerciseId: Long) : ActiveWorkoutIntent
    data class RemoveSet(val setId: Long) : ActiveWorkoutIntent
    data class ToggleSetComplete(val setId: Long) : ActiveWorkoutIntent
    data class UpdateReps(val setId: Long, val value: String) : ActiveWorkoutIntent
    data class UpdateWeight(val setId: Long, val value: String) : ActiveWorkoutIntent
    data class UpdateDuration(val setId: Long, val value: String) : ActiveWorkoutIntent
    data class CycleSetType(val setId: Long) : ActiveWorkoutIntent
    data object ShowAddExercise : ActiveWorkoutIntent
    data object HideAddExercise : ActiveWorkoutIntent
    data class AddExercise(val exerciseId: Long) : ActiveWorkoutIntent
    data class RemoveExercise(val workoutExerciseId: Long) : ActiveWorkoutIntent
    data class MoveExerciseUp(val workoutExerciseId: Long) : ActiveWorkoutIntent
    data class MoveExerciseDown(val workoutExerciseId: Long) : ActiveWorkoutIntent
}

/** No effects currently fire; the type exists so [StoreViewModel] can be parameterized. */
sealed interface ActiveWorkoutEffect : MviEffect

private data class DerivedWorkoutData(
    val startedAt: Long?,
    val exercises: List<ActiveWorkoutExerciseUi>,
    val availableExercises: List<Exercise>,
)

/**
 * Backs the active-workout (set-logging) screen for a single [workoutId]. Every
 * mutation writes straight to SQLDelight — there is no separate "save" step —
 * so logging works fully offline and edits survive process death.
 */
class ActiveWorkoutStore(
    private val workoutId: Long,
    private val workoutRepository: WorkoutRepository,
    private val workoutExerciseRepository: WorkoutExerciseRepository,
    private val workoutSetRepository: WorkoutSetRepository,
    private val exerciseRepository: ExerciseRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : StoreViewModel<ActiveWorkoutState, ActiveWorkoutIntent, ActiveWorkoutEffect>(
    ActiveWorkoutState(workoutId = workoutId),
    dispatcher,
) {
    init {
        combine(
            workoutRepository.observeById(workoutId),
            workoutExerciseRepository.observeByWorkoutId(workoutId),
            workoutSetRepository.observeByWorkoutId(workoutId),
            exerciseRepository.observeAll(),
        ) { workout, workoutExercises, sets, exercises ->
            val exerciseNames = exercises.associateBy { it.id }
            val setsByExercise = sets.groupBy { it.workoutExerciseId }
            DerivedWorkoutData(
                startedAt = workout?.startedAt,
                exercises = workoutExercises
                    .sortedBy { it.position }
                    .map { workoutExercise ->
                        workoutExercise.toUi(
                            exerciseName = exerciseNames[workoutExercise.exerciseId]?.name ?: "Unknown exercise",
                            sets = setsByExercise[workoutExercise.id].orEmpty().sortedBy { it.position },
                        )
                    },
                availableExercises = exercises,
            )
        }.onEach { derived ->
            setState {
                it.copy(
                    startedAt = derived.startedAt,
                    exercises = derived.exercises,
                    availableExercises = derived.availableExercises,
                )
            }
        }.launchIn(scope)
    }

    override fun onIntent(intent: ActiveWorkoutIntent) {
        when (intent) {
            is ActiveWorkoutIntent.AddSet -> addSet(intent.workoutExerciseId)
            is ActiveWorkoutIntent.RemoveSet -> scope.launch { workoutSetRepository.delete(intent.setId) }
            is ActiveWorkoutIntent.ToggleSetComplete -> updateSet(intent.setId) { it.copy(completed = !it.completed) }
            is ActiveWorkoutIntent.UpdateReps -> updateSet(intent.setId) { it.copy(reps = intent.value.toLongOrNull()) }
            is ActiveWorkoutIntent.UpdateWeight -> updateSet(intent.setId) { it.copy(weight = intent.value.toDoubleOrNull()) }
            is ActiveWorkoutIntent.UpdateDuration ->
                updateSet(intent.setId) { it.copy(durationSec = intent.value.toLongOrNull()) }
            is ActiveWorkoutIntent.CycleSetType -> updateSet(intent.setId) { it.copy(setType = it.setType.next()) }
            ActiveWorkoutIntent.ShowAddExercise -> setState { it.copy(showAddExercise = true) }
            ActiveWorkoutIntent.HideAddExercise -> setState { it.copy(showAddExercise = false) }
            is ActiveWorkoutIntent.AddExercise -> addExercise(intent.exerciseId)
            is ActiveWorkoutIntent.RemoveExercise ->
                scope.launch { workoutExerciseRepository.delete(intent.workoutExerciseId) }
            is ActiveWorkoutIntent.MoveExerciseUp -> moveExercise(intent.workoutExerciseId, offset = -1)
            is ActiveWorkoutIntent.MoveExerciseDown -> moveExercise(intent.workoutExerciseId, offset = 1)
        }
    }

    private fun addSet(workoutExerciseId: Long) {
        val nextPosition = state.value.exercises
            .firstOrNull { it.workoutExerciseId == workoutExerciseId }
            ?.sets
            ?.size
            ?.toLong()
            ?: 0L
        scope.launch {
            workoutSetRepository.add(
                workoutExerciseId = workoutExerciseId,
                position = nextPosition,
                updatedAt = currentTimeMillis(),
            )
        }
    }

    private fun updateSet(setId: Long, mutate: (WorkoutSet) -> WorkoutSet) {
        val current = state.value.exercises
            .asSequence()
            .flatMap { it.sets }
            .firstOrNull { it.id == setId }
            ?: return
        val updated = mutate(current)
        scope.launch {
            workoutSetRepository.update(
                id = setId,
                reps = updated.reps,
                weight = updated.weight,
                durationSec = updated.durationSec,
                setType = updated.setType,
                completed = updated.completed,
                updatedAt = currentTimeMillis(),
            )
        }
    }

    private fun addExercise(exerciseId: Long) {
        val nextPosition = state.value.exercises.size.toLong()
        scope.launch {
            workoutExerciseRepository.add(
                workoutId = workoutId,
                exerciseId = exerciseId,
                position = nextPosition,
                updatedAt = currentTimeMillis(),
            )
            setState { it.copy(showAddExercise = false) }
        }
    }

    private fun moveExercise(workoutExerciseId: Long, offset: Int) {
        val exercises = state.value.exercises
        val index = exercises.indexOfFirst { it.workoutExerciseId == workoutExerciseId }
        val swapWith = index + offset
        if (index < 0 || swapWith < 0 || swapWith >= exercises.size) return
        val a = exercises[index]
        val b = exercises[swapWith]
        scope.launch {
            workoutExerciseRepository.updatePosition(a.workoutExerciseId, b.position)
            workoutExerciseRepository.updatePosition(b.workoutExerciseId, a.position)
        }
    }
}

private fun WorkoutExercise.toUi(exerciseName: String, sets: List<WorkoutSet>) = ActiveWorkoutExerciseUi(
    workoutExerciseId = id,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    position = position,
    sets = sets,
)

private fun SetType.next(): SetType {
    val values = SetType.entries
    return values[(values.indexOf(this) + 1) % values.size]
}
