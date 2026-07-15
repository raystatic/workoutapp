package com.workoutapp.composeapp.ui.routinebuilder

import com.workoutapp.composeapp.data.db.SetType
import com.workoutapp.composeapp.data.db.currentTimeMillis
import com.workoutapp.composeapp.data.library.ExerciseRepository
import com.workoutapp.composeapp.data.routines.RoutineExerciseRepository
import com.workoutapp.composeapp.data.routines.RoutineRepository
import com.workoutapp.composeapp.data.routines.RoutineSetRepository
import com.workoutapp.composeapp.db.Exercise
import com.workoutapp.composeapp.db.Routine
import com.workoutapp.composeapp.db.RoutineExercise
import com.workoutapp.composeapp.db.RoutineSet
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

/** One set row's target reps/weight, formatted for editing. */
data class RoutineBuilderSetUi(val set: RoutineSet) {
    val targetRepsText: String get() = set.targetReps?.toString().orEmpty()
    val targetWeightText: String get() = set.targetWeight?.let(::formatNumber).orEmpty()
}

/** One exercise within the routine draft, with its target sets in order. */
data class RoutineBuilderExerciseUi(
    val routineExercise: RoutineExercise,
    val exerciseName: String,
    val sets: List<RoutineBuilderSetUi>,
    /** Display label ("A", "B", ...) shared by every exercise in the same superset. */
    val supersetLabel: String? = null,
) {
    val routineExerciseId: Long get() = routineExercise.id
    val position: Long get() = routineExercise.position
    val supersetGroup: String? get() = routineExercise.supersetGroup
    val restSeconds: Long? get() = routineExercise.restSeconds
    val notes: String get() = routineExercise.notes.orEmpty()
}

data class RoutineBuilderState(
    val routineId: Long,
    val name: String = "",
    val folderIdText: String = "",
    val exercises: List<RoutineBuilderExerciseUi> = emptyList(),
    val availableExercises: List<Exercise> = emptyList(),
    val recentExercises: List<Exercise> = emptyList(),
    val showAddExercise: Boolean = false,
) : MviState

sealed interface RoutineBuilderIntent : MviIntent {
    data class NameChanged(val value: String) : RoutineBuilderIntent
    data class FolderIdChanged(val value: String) : RoutineBuilderIntent
    data object ShowAddExercise : RoutineBuilderIntent
    data object HideAddExercise : RoutineBuilderIntent
    data class AddExercises(val exerciseIds: List<Long>) : RoutineBuilderIntent
    data class RemoveExercise(val routineExerciseId: Long) : RoutineBuilderIntent
    data class MoveExerciseUp(val routineExerciseId: Long) : RoutineBuilderIntent
    data class MoveExerciseDown(val routineExerciseId: Long) : RoutineBuilderIntent
    data class GroupWithNextExercise(val routineExerciseId: Long) : RoutineBuilderIntent
    data class RemoveFromSuperset(val routineExerciseId: Long) : RoutineBuilderIntent
    data class UpdateExerciseNotes(val routineExerciseId: Long, val value: String) : RoutineBuilderIntent
    data class UpdateRestSeconds(val routineExerciseId: Long, val value: String) : RoutineBuilderIntent
    data class AddSet(val routineExerciseId: Long) : RoutineBuilderIntent
    data class RemoveSet(val setId: Long) : RoutineBuilderIntent
    data class UpdateTargetReps(val setId: Long, val value: String) : RoutineBuilderIntent
    data class UpdateTargetWeight(val setId: Long, val value: String) : RoutineBuilderIntent
    data class CycleSetType(val setId: Long) : RoutineBuilderIntent
}

/** No effects currently fire; the type exists so [StoreViewModel] can be parameterized. */
sealed interface RoutineBuilderEffect : MviEffect

/** Number of exercises surfaced by the "Recent" shortcut in the exercise picker. */
private const val RECENT_EXERCISES_LIMIT = 8

private data class DerivedRoutineData(
    val routine: Routine?,
    val exercises: List<RoutineBuilderExerciseUi>,
    val availableExercises: List<Exercise>,
    val recentExercises: List<Exercise>,
)

/**
 * Backs the create/edit routine builder screen for an existing [routineId] — the
 * Workout tab always creates a placeholder [Routine] row before opening this screen,
 * so this store (like [com.workoutapp.composeapp.ui.activeworkout.ActiveWorkoutStore])
 * only ever edits an already-persisted routine. Every mutation writes straight to
 * SQLDelight, so the draft survives navigation and process death.
 */
class RoutineBuilderStore(
    private val routineId: Long,
    private val routineRepository: RoutineRepository,
    private val routineExerciseRepository: RoutineExerciseRepository,
    private val routineSetRepository: RoutineSetRepository,
    private val exerciseRepository: ExerciseRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : StoreViewModel<RoutineBuilderState, RoutineBuilderIntent, RoutineBuilderEffect>(
    RoutineBuilderState(routineId = routineId),
    dispatcher,
) {
    private var latestNotes: String? = null

    init {
        combine(
            routineRepository.observeById(routineId),
            routineExerciseRepository.observeByRoutineId(routineId),
            routineSetRepository.observeByRoutineId(routineId),
            exerciseRepository.observeAll(),
            exerciseRepository.observeRecentlyUsed(RECENT_EXERCISES_LIMIT),
        ) { routine, routineExercises, sets, exercises, recentExercises ->
            val exerciseNames = exercises.associateBy { it.id }
            val setsByExercise = sets.groupBy { it.routineExerciseId }
            val exercisesUi = routineExercises
                .sortedBy { it.position }
                .map { routineExercise ->
                    RoutineBuilderExerciseUi(
                        routineExercise = routineExercise,
                        exerciseName = exerciseNames[routineExercise.exerciseId]?.name ?: "Unknown exercise",
                        sets = setsByExercise[routineExercise.id].orEmpty().sortedBy { it.position }.map { RoutineBuilderSetUi(it) },
                    )
                }
                .withSupersetLabels()
            DerivedRoutineData(
                routine = routine,
                exercises = exercisesUi,
                availableExercises = exercises,
                recentExercises = recentExercises,
            )
        }.onEach { derived ->
            latestNotes = derived.routine?.notes
            setState {
                it.copy(
                    name = derived.routine?.name.orEmpty(),
                    folderIdText = derived.routine?.folderId?.toString().orEmpty(),
                    exercises = derived.exercises,
                    availableExercises = derived.availableExercises,
                    recentExercises = derived.recentExercises,
                )
            }
        }.launchIn(scope)
    }

    override fun onIntent(intent: RoutineBuilderIntent) {
        when (intent) {
            is RoutineBuilderIntent.NameChanged -> updateRoutine(name = intent.value)
            is RoutineBuilderIntent.FolderIdChanged -> updateRoutine(folderIdText = intent.value)
            RoutineBuilderIntent.ShowAddExercise -> setState { it.copy(showAddExercise = true) }
            RoutineBuilderIntent.HideAddExercise -> setState { it.copy(showAddExercise = false) }
            is RoutineBuilderIntent.AddExercises -> addExercises(intent.exerciseIds)
            is RoutineBuilderIntent.RemoveExercise ->
                scope.launch { routineExerciseRepository.delete(intent.routineExerciseId) }
            is RoutineBuilderIntent.MoveExerciseUp -> moveExercise(intent.routineExerciseId, offset = -1)
            is RoutineBuilderIntent.MoveExerciseDown -> moveExercise(intent.routineExerciseId, offset = 1)
            is RoutineBuilderIntent.GroupWithNextExercise -> groupWithNextExercise(intent.routineExerciseId)
            is RoutineBuilderIntent.RemoveFromSuperset -> removeFromSuperset(intent.routineExerciseId)
            is RoutineBuilderIntent.UpdateExerciseNotes ->
                scope.launch { routineExerciseRepository.updateNotes(intent.routineExerciseId, intent.value.ifBlank { null }) }
            is RoutineBuilderIntent.UpdateRestSeconds ->
                scope.launch { routineExerciseRepository.updateRestSeconds(intent.routineExerciseId, intent.value.toLongOrNull()) }
            is RoutineBuilderIntent.AddSet -> addSet(intent.routineExerciseId)
            is RoutineBuilderIntent.RemoveSet -> scope.launch { routineSetRepository.delete(intent.setId) }
            is RoutineBuilderIntent.UpdateTargetReps ->
                updateSet(intent.setId) { it.copy(targetReps = intent.value.toLongOrNull()) }
            is RoutineBuilderIntent.UpdateTargetWeight ->
                updateSet(intent.setId) { it.copy(targetWeight = intent.value.toDoubleOrNull()) }
            is RoutineBuilderIntent.CycleSetType -> updateSet(intent.setId) { it.copy(setType = it.setType.next()) }
        }
    }

    private fun updateRoutine(name: String? = null, folderIdText: String? = null) {
        val current = state.value
        val newName = name ?: current.name
        val newFolderIdText = folderIdText ?: current.folderIdText
        scope.launch {
            routineRepository.update(
                id = routineId,
                name = newName,
                folderId = newFolderIdText.toLongOrNull(),
                notes = latestNotes,
                updatedAt = currentTimeMillis(),
            )
        }
    }

    private fun addExercises(exerciseIds: List<Long>) {
        if (exerciseIds.isEmpty()) return
        val startPosition = state.value.exercises.size.toLong()
        scope.launch {
            exerciseIds.forEachIndexed { index, exerciseId ->
                routineExerciseRepository.add(
                    routineId = routineId,
                    exerciseId = exerciseId,
                    position = startPosition + index,
                    updatedAt = currentTimeMillis(),
                )
            }
            setState { it.copy(showAddExercise = false) }
        }
    }

    private fun moveExercise(routineExerciseId: Long, offset: Int) {
        val exercises = state.value.exercises
        val index = exercises.indexOfFirst { it.routineExerciseId == routineExerciseId }
        val swapWith = index + offset
        if (index < 0 || swapWith < 0 || swapWith >= exercises.size) return
        val a = exercises[index]
        val b = exercises[swapWith]
        scope.launch {
            routineExerciseRepository.updatePosition(a.routineExerciseId, b.position)
            routineExerciseRepository.updatePosition(b.routineExerciseId, a.position)
        }
    }

    /**
     * Pairs [routineExerciseId] with the exercise immediately after it into a superset. Either
     * exercise may already belong to a (different) superset, in which case the pair joins that
     * existing group — this is how a group grows past two exercises ("giant sets").
     */
    private fun groupWithNextExercise(routineExerciseId: Long) {
        val exercises = state.value.exercises
        val index = exercises.indexOfFirst { it.routineExerciseId == routineExerciseId }
        if (index == -1 || index == exercises.lastIndex) return
        val current = exercises[index]
        val next = exercises[index + 1]
        val targetGroup = current.supersetGroup ?: next.supersetGroup ?: "sg-${current.routineExerciseId}"
        scope.launch {
            if (current.supersetGroup != targetGroup) {
                routineExerciseRepository.updateSupersetGroup(current.routineExerciseId, targetGroup)
            }
            if (next.supersetGroup != targetGroup) {
                routineExerciseRepository.updateSupersetGroup(next.routineExerciseId, targetGroup)
            }
        }
    }

    /** Ungroups [routineExerciseId]. If that leaves a single member behind, ungroups it too. */
    private fun removeFromSuperset(routineExerciseId: Long) {
        val exercises = state.value.exercises
        val current = exercises.firstOrNull { it.routineExerciseId == routineExerciseId } ?: return
        val group = current.supersetGroup ?: return
        scope.launch {
            routineExerciseRepository.updateSupersetGroup(routineExerciseId, null)
            val remaining = exercises.filter { it.supersetGroup == group && it.routineExerciseId != routineExerciseId }
            if (remaining.size == 1) {
                routineExerciseRepository.updateSupersetGroup(remaining.single().routineExerciseId, null)
            }
        }
    }

    private fun addSet(routineExerciseId: Long) {
        val nextPosition = state.value.exercises
            .firstOrNull { it.routineExerciseId == routineExerciseId }
            ?.sets
            ?.size
            ?.toLong()
            ?: 0L
        scope.launch {
            routineSetRepository.add(
                routineExerciseId = routineExerciseId,
                position = nextPosition,
                updatedAt = currentTimeMillis(),
            )
        }
    }

    private fun updateSet(setId: Long, mutate: (RoutineSet) -> RoutineSet) {
        val current = state.value.exercises
            .asSequence()
            .flatMap { it.sets }
            .map { it.set }
            .firstOrNull { it.id == setId }
            ?: return
        val updated = mutate(current)
        scope.launch {
            routineSetRepository.update(
                id = setId,
                targetReps = updated.targetReps,
                targetWeight = updated.targetWeight,
                setType = updated.setType,
                updatedAt = currentTimeMillis(),
            )
        }
    }
}

/**
 * Annotates each exercise with its superset display label ("A", "B", ... assigned in order of
 * first appearance), matching the labeling scheme used on the active-workout screen.
 */
private fun List<RoutineBuilderExerciseUi>.withSupersetLabels(): List<RoutineBuilderExerciseUi> {
    val labels = mutableMapOf<String, String>()
    var nextLabelIndex = 0
    return map { exerciseUi ->
        val label = exerciseUi.supersetGroup?.let { group ->
            labels.getOrPut(group) { ('A' + nextLabelIndex++).toString() }
        }
        exerciseUi.copy(supersetLabel = label)
    }
}

private fun SetType.next(): SetType {
    val values = SetType.entries
    return values[(values.indexOf(this) + 1) % values.size]
}

private fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
