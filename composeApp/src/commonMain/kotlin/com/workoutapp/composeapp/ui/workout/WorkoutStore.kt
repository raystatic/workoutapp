package com.workoutapp.composeapp.ui.workout

import com.workoutapp.composeapp.data.db.currentTimeMillis
import com.workoutapp.composeapp.data.routines.RoutineExerciseRepository
import com.workoutapp.composeapp.data.routines.RoutineRepository
import com.workoutapp.composeapp.data.routines.RoutineSetRepository
import com.workoutapp.composeapp.data.workout.WorkoutExerciseRepository
import com.workoutapp.composeapp.data.workout.WorkoutRepository
import com.workoutapp.composeapp.data.workout.WorkoutSetRepository
import com.workoutapp.composeapp.db.Routine
import com.workoutapp.composeapp.mvi.MviEffect
import com.workoutapp.composeapp.mvi.MviIntent
import com.workoutapp.composeapp.mvi.MviState
import com.workoutapp.composeapp.mvi.StoreViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Routines sharing a `folderId` (`null` = not filed under any folder). */
data class RoutineFolder(
    val folderId: Long?,
    val routines: List<Routine>,
)

data class WorkoutState(
    val folders: List<RoutineFolder> = emptyList(),
    val routineIdPendingDelete: Long? = null,
) : MviState {
    val hasRoutines: Boolean get() = folders.any { it.routines.isNotEmpty() }
}

sealed interface WorkoutIntent : MviIntent {
    data object StartEmptyWorkout : WorkoutIntent
    data class StartRoutine(val routineId: Long) : WorkoutIntent
    data object CreateRoutine : WorkoutIntent
    data class EditRoutine(val routineId: Long) : WorkoutIntent
    data class DuplicateRoutine(val routineId: Long) : WorkoutIntent
    data class RequestDeleteRoutine(val routineId: Long) : WorkoutIntent
    data object CancelDeleteRoutine : WorkoutIntent
    data object ConfirmDeleteRoutine : WorkoutIntent
}

sealed interface WorkoutEffect : MviEffect {
    data class NavigateToActiveWorkout(val workoutId: Long) : WorkoutEffect
    data class NavigateToRoutineBuilder(val routineId: Long) : WorkoutEffect
}

/**
 * Backs the Workout tab: lists routines grouped by folder, creates/duplicates/deletes
 * routines, and starts a new [com.workoutapp.composeapp.db.Workout] — empty, or
 * pre-filled from a routine's exercises and target sets — handing off to the
 * active-workout screen via [WorkoutEffect].
 */
class WorkoutStore(
    private val routineRepository: RoutineRepository,
    private val workoutRepository: WorkoutRepository,
    private val routineExerciseRepository: RoutineExerciseRepository,
    private val routineSetRepository: RoutineSetRepository,
    private val workoutExerciseRepository: WorkoutExerciseRepository,
    private val workoutSetRepository: WorkoutSetRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : StoreViewModel<WorkoutState, WorkoutIntent, WorkoutEffect>(WorkoutState(), dispatcher) {

    init {
        routineRepository.observeAll()
            .onEach { routines -> setState { it.copy(folders = groupByFolder(routines)) } }
            .launchIn(scope)
    }

    override fun onIntent(intent: WorkoutIntent) {
        when (intent) {
            WorkoutIntent.StartEmptyWorkout -> startEmptyWorkout()
            is WorkoutIntent.StartRoutine -> startRoutine(intent.routineId)
            WorkoutIntent.CreateRoutine -> createRoutine()
            is WorkoutIntent.EditRoutine -> sendEffect(WorkoutEffect.NavigateToRoutineBuilder(intent.routineId))
            is WorkoutIntent.DuplicateRoutine -> duplicateRoutine(intent.routineId)
            is WorkoutIntent.RequestDeleteRoutine -> setState { it.copy(routineIdPendingDelete = intent.routineId) }
            WorkoutIntent.CancelDeleteRoutine -> setState { it.copy(routineIdPendingDelete = null) }
            WorkoutIntent.ConfirmDeleteRoutine -> confirmDeleteRoutine()
        }
    }

    private fun startEmptyWorkout() {
        scope.launch {
            val now = currentTimeMillis()
            val workoutId = workoutRepository.add(name = "Empty Workout", startedAt = now, updatedAt = now)
            sendEffect(WorkoutEffect.NavigateToActiveWorkout(workoutId))
        }
    }

    /** Starts a new workout named after the routine, copying its exercises and target sets in as a pre-fill. */
    private fun startRoutine(routineId: Long) {
        val routine = findRoutine(routineId) ?: return
        scope.launch {
            val now = currentTimeMillis()
            val workoutId = workoutRepository.add(name = routine.name, startedAt = now, updatedAt = now)
            val routineExercises = routineExerciseRepository.observeByRoutineId(routineId).first().sortedBy { it.position }
            for (routineExercise in routineExercises) {
                val workoutExerciseId = workoutExerciseRepository.add(
                    workoutId = workoutId,
                    exerciseId = routineExercise.exerciseId,
                    position = routineExercise.position,
                    supersetGroup = routineExercise.supersetGroup,
                    restSeconds = routineExercise.restSeconds,
                    notes = routineExercise.notes,
                    updatedAt = now,
                )
                val routineSets = routineSetRepository.observeByRoutineExerciseId(routineExercise.id).first().sortedBy { it.position }
                for (routineSet in routineSets) {
                    workoutSetRepository.add(
                        workoutExerciseId = workoutExerciseId,
                        position = routineSet.position,
                        reps = routineSet.targetReps,
                        weight = routineSet.targetWeight,
                        setType = routineSet.setType,
                        updatedAt = now,
                    )
                }
            }
            sendEffect(WorkoutEffect.NavigateToActiveWorkout(workoutId))
        }
    }

    private fun createRoutine() {
        scope.launch {
            val now = currentTimeMillis()
            val routineId = routineRepository.add(name = "New Routine", position = nextRoutinePosition(), updatedAt = now)
            sendEffect(WorkoutEffect.NavigateToRoutineBuilder(routineId))
        }
    }

    /** Deep-copies a routine's exercises and target sets into a new routine named "<name> copy". */
    private fun duplicateRoutine(routineId: Long) {
        val source = findRoutine(routineId) ?: return
        scope.launch {
            val now = currentTimeMillis()
            val sourceExercises = routineExerciseRepository.observeByRoutineId(routineId).first().sortedBy { it.position }
            val newRoutineId = routineRepository.add(
                name = "${source.name} copy",
                folderId = source.folderId,
                position = nextRoutinePosition(),
                notes = source.notes,
                updatedAt = now,
            )
            for (sourceExercise in sourceExercises) {
                val newExerciseId = routineExerciseRepository.add(
                    routineId = newRoutineId,
                    exerciseId = sourceExercise.exerciseId,
                    position = sourceExercise.position,
                    supersetGroup = sourceExercise.supersetGroup,
                    restSeconds = sourceExercise.restSeconds,
                    notes = sourceExercise.notes,
                    updatedAt = now,
                )
                val sourceSets = routineSetRepository.observeByRoutineExerciseId(sourceExercise.id).first().sortedBy { it.position }
                for (sourceSet in sourceSets) {
                    routineSetRepository.add(
                        routineExerciseId = newExerciseId,
                        position = sourceSet.position,
                        targetReps = sourceSet.targetReps,
                        targetWeight = sourceSet.targetWeight,
                        setType = sourceSet.setType,
                        updatedAt = now,
                    )
                }
            }
        }
    }

    private fun confirmDeleteRoutine() {
        val routineId = state.value.routineIdPendingDelete ?: return
        scope.launch { routineRepository.delete(routineId) }
        setState { it.copy(routineIdPendingDelete = null) }
    }

    private fun findRoutine(routineId: Long): Routine? =
        state.value.folders.asSequence().flatMap { it.routines }.firstOrNull { it.id == routineId }

    private fun nextRoutinePosition(): Long =
        (state.value.folders.flatMap { it.routines }.maxOfOrNull { it.position } ?: -1L) + 1L

    private fun groupByFolder(routines: List<Routine>): List<RoutineFolder> {
        val grouped = routines.groupBy { it.folderId }
        val (foldered, unfiled) = grouped.entries.partition { it.key != null }
        return (foldered.sortedBy { it.key } + unfiled)
            .map { (folderId, routinesInFolder) -> RoutineFolder(folderId, routinesInFolder) }
    }
}
