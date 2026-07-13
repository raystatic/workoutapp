package com.workoutapp.composeapp.ui.workout

import com.workoutapp.composeapp.data.db.currentTimeMillis
import com.workoutapp.composeapp.data.routines.RoutineRepository
import com.workoutapp.composeapp.data.workout.WorkoutRepository
import com.workoutapp.composeapp.db.Routine
import com.workoutapp.composeapp.mvi.MviEffect
import com.workoutapp.composeapp.mvi.MviIntent
import com.workoutapp.composeapp.mvi.MviState
import com.workoutapp.composeapp.mvi.StoreViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
) : MviState {
    val hasRoutines: Boolean get() = folders.any { it.routines.isNotEmpty() }
}

sealed interface WorkoutIntent : MviIntent {
    data object StartEmptyWorkout : WorkoutIntent
    data class StartRoutine(val routineId: Long) : WorkoutIntent
}

sealed interface WorkoutEffect : MviEffect {
    data class NavigateToActiveWorkout(val workoutId: Long) : WorkoutEffect
}

/**
 * Backs the Workout tab: lists routines grouped by folder and starts a new
 * [com.workoutapp.composeapp.db.Workout] — empty or pre-named from a routine
 * — handing off to the active-workout screen via [WorkoutEffect].
 */
class WorkoutStore(
    private val routineRepository: RoutineRepository,
    private val workoutRepository: WorkoutRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : StoreViewModel<WorkoutState, WorkoutIntent, WorkoutEffect>(WorkoutState(), dispatcher) {

    init {
        routineRepository.observeAll()
            .onEach { routines -> setState { it.copy(folders = groupByFolder(routines)) } }
            .launchIn(scope)
    }

    override fun onIntent(intent: WorkoutIntent) {
        when (intent) {
            WorkoutIntent.StartEmptyWorkout -> startWorkout(name = "Empty Workout")
            is WorkoutIntent.StartRoutine -> startRoutine(intent.routineId)
        }
    }

    private fun startRoutine(routineId: Long) {
        val routine = state.value.folders
            .asSequence()
            .flatMap { it.routines }
            .firstOrNull { it.id == routineId }
            ?: return
        startWorkout(name = routine.name)
    }

    private fun startWorkout(name: String) {
        scope.launch {
            val now = currentTimeMillis()
            val workoutId = workoutRepository.add(name = name, startedAt = now, updatedAt = now)
            sendEffect(WorkoutEffect.NavigateToActiveWorkout(workoutId))
        }
    }

    private fun groupByFolder(routines: List<Routine>): List<RoutineFolder> {
        val grouped = routines.groupBy { it.folderId }
        val (foldered, unfiled) = grouped.entries.partition { it.key != null }
        return (foldered.sortedBy { it.key } + unfiled)
            .map { (folderId, routinesInFolder) -> RoutineFolder(folderId, routinesInFolder) }
    }
}
