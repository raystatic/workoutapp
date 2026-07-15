package com.workoutapp.composeapp.ui.activeworkout

import com.workoutapp.composeapp.data.db.SetType
import com.workoutapp.composeapp.data.db.currentTimeMillis
import com.workoutapp.composeapp.data.library.ExerciseRepository
import com.workoutapp.composeapp.data.resttimer.RestTimerSettingsRepository
import com.workoutapp.composeapp.data.resttimer.resolveRestSeconds
import com.workoutapp.composeapp.data.workout.PreviousSetResolver
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
import com.workoutapp.composeapp.ui.resttimer.RestTimerController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** A set row paired with the matching set (by position) from the most recent prior workout, if any. */
data class ActiveWorkoutSetUi(
    val set: WorkoutSet,
    val previousReps: Long? = null,
    val previousWeight: Double? = null,
    val previousDurationSec: Long? = null,
)

/** One exercise within the in-progress workout, with its sets in logging order. */
data class ActiveWorkoutExerciseUi(
    val workoutExerciseId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val position: Long,
    val sets: List<ActiveWorkoutSetUi>,
    val supersetGroup: String? = null,
    /** Display label ("A", "B", ...) shared by every exercise in the same superset. */
    val supersetLabel: String? = null,
    /** True when this is the group member with the fewest completed sets — i.e. whose turn is next. */
    val isUpNextInSuperset: Boolean = false,
    /** Per-exercise rest-timer override; `null` means "use the global default". */
    val restSeconds: Long? = null,
)

data class ActiveWorkoutState(
    val workoutId: Long,
    val startedAt: Long? = null,
    val exercises: List<ActiveWorkoutExerciseUi> = emptyList(),
    val availableExercises: List<Exercise> = emptyList(),
    val recentExercises: List<Exercise> = emptyList(),
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
    data class AddExercises(val exerciseIds: List<Long>) : ActiveWorkoutIntent
    data class RemoveExercise(val workoutExerciseId: Long) : ActiveWorkoutIntent
    data class MoveExerciseUp(val workoutExerciseId: Long) : ActiveWorkoutIntent
    data class MoveExerciseDown(val workoutExerciseId: Long) : ActiveWorkoutIntent
    data class GroupWithNextExercise(val workoutExerciseId: Long) : ActiveWorkoutIntent
    data class RemoveFromSuperset(val workoutExerciseId: Long) : ActiveWorkoutIntent
    data class CycleRestOverride(val workoutExerciseId: Long) : ActiveWorkoutIntent
}

/** Rest-timer override presets cycled by [ActiveWorkoutIntent.CycleRestOverride]: default, then 30s..240s, then back. */
val REST_OVERRIDE_PRESETS: List<Long?> = listOf(null, 30L, 60L, 90L, 120L, 180L, 240L)

/** No effects currently fire; the type exists so [StoreViewModel] can be parameterized. */
sealed interface ActiveWorkoutEffect : MviEffect

/** Number of exercises surfaced by the "Recent" shortcut in the exercise picker. */
private const val RECENT_EXERCISES_LIMIT = 8

private data class DerivedWorkoutData(
    val startedAt: Long?,
    val exercises: List<ActiveWorkoutExerciseUi>,
    val availableExercises: List<Exercise>,
    val recentExercises: List<Exercise>,
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
    private val previousSetResolver: PreviousSetResolver,
    private val restTimerController: RestTimerController,
    private val restTimerSettingsRepository: RestTimerSettingsRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : StoreViewModel<ActiveWorkoutState, ActiveWorkoutIntent, ActiveWorkoutEffect>(
    ActiveWorkoutState(workoutId = workoutId),
    dispatcher,
) {
    // Previous-workout sets don't change during this session, so cache per exerciseId to
    // avoid re-querying on every edit to the in-progress workout's own sets.
    private val previousSetsCache = mutableMapOf<Long, List<WorkoutSet>>()

    init {
        combine(
            workoutRepository.observeById(workoutId),
            workoutExerciseRepository.observeByWorkoutId(workoutId),
            workoutSetRepository.observeByWorkoutId(workoutId),
            exerciseRepository.observeAll(),
            exerciseRepository.observeRecentlyUsed(RECENT_EXERCISES_LIMIT),
        ) { workout, workoutExercises, sets, exercises, recentExercises ->
            val exerciseNames = exercises.associateBy { it.id }
            val setsByExercise = sets.groupBy { it.workoutExerciseId }
            val baseExercises = workoutExercises
                .sortedBy { it.position }
                .map { workoutExercise ->
                    val previousSets = previousSetsCache.getOrPut(workoutExercise.exerciseId) {
                        previousSetResolver.resolve(workoutExercise.exerciseId, workoutId)
                    }
                    workoutExercise.toUi(
                        exerciseName = exerciseNames[workoutExercise.exerciseId]?.name ?: "Unknown exercise",
                        sets = setsByExercise[workoutExercise.id].orEmpty().sortedBy { it.position },
                        previousSets = previousSets,
                    )
                }
            DerivedWorkoutData(
                startedAt = workout?.startedAt,
                exercises = baseExercises.withSupersetInfo(),
                availableExercises = exercises,
                recentExercises = recentExercises,
            )
        }.onEach { derived ->
            setState {
                it.copy(
                    startedAt = derived.startedAt,
                    exercises = derived.exercises,
                    availableExercises = derived.availableExercises,
                    recentExercises = derived.recentExercises,
                )
            }
        }.launchIn(scope)
    }

    override fun onIntent(intent: ActiveWorkoutIntent) {
        when (intent) {
            is ActiveWorkoutIntent.AddSet -> addSet(intent.workoutExerciseId)
            is ActiveWorkoutIntent.RemoveSet -> scope.launch { workoutSetRepository.delete(intent.setId) }
            is ActiveWorkoutIntent.ToggleSetComplete -> toggleSetComplete(intent.setId)
            is ActiveWorkoutIntent.UpdateReps -> updateSet(intent.setId) { it.copy(reps = intent.value.toLongOrNull()) }
            is ActiveWorkoutIntent.UpdateWeight -> updateSet(intent.setId) { it.copy(weight = intent.value.toDoubleOrNull()) }
            is ActiveWorkoutIntent.UpdateDuration ->
                updateSet(intent.setId) { it.copy(durationSec = intent.value.toLongOrNull()) }
            is ActiveWorkoutIntent.CycleSetType -> updateSet(intent.setId) { it.copy(setType = it.setType.next()) }
            ActiveWorkoutIntent.ShowAddExercise -> setState { it.copy(showAddExercise = true) }
            ActiveWorkoutIntent.HideAddExercise -> setState { it.copy(showAddExercise = false) }
            is ActiveWorkoutIntent.AddExercises -> addExercises(intent.exerciseIds)
            is ActiveWorkoutIntent.RemoveExercise ->
                scope.launch { workoutExerciseRepository.delete(intent.workoutExerciseId) }
            is ActiveWorkoutIntent.MoveExerciseUp -> moveExercise(intent.workoutExerciseId, offset = -1)
            is ActiveWorkoutIntent.MoveExerciseDown -> moveExercise(intent.workoutExerciseId, offset = 1)
            is ActiveWorkoutIntent.GroupWithNextExercise -> groupWithNextExercise(intent.workoutExerciseId)
            is ActiveWorkoutIntent.RemoveFromSuperset -> removeFromSuperset(intent.workoutExerciseId)
            is ActiveWorkoutIntent.CycleRestOverride -> cycleRestOverride(intent.workoutExerciseId)
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

    /** Auto-starts the rest timer only on the incomplete → complete transition, never on undo. */
    private fun toggleSetComplete(setId: Long) {
        val exerciseUi = state.value.exercises.firstOrNull { ex -> ex.sets.any { it.set.id == setId } }
        val wasCompleted = exerciseUi?.sets?.firstOrNull { it.set.id == setId }?.set?.completed
        updateSet(setId) { it.copy(completed = !it.completed) }
        if (exerciseUi != null && wasCompleted == false) {
            scope.launch {
                val defaultSeconds = restTimerSettingsRepository.getDefaultRestSeconds()
                val seconds = resolveRestSeconds(exerciseUi.restSeconds, defaultSeconds)
                restTimerController.start(exerciseUi.exerciseId, seconds)
            }
        }
    }

    private fun cycleRestOverride(workoutExerciseId: Long) {
        val current = state.value.exercises.firstOrNull { it.workoutExerciseId == workoutExerciseId } ?: return
        val nextIndex = (REST_OVERRIDE_PRESETS.indexOf(current.restSeconds) + 1) % REST_OVERRIDE_PRESETS.size
        scope.launch {
            workoutExerciseRepository.updateRestSeconds(workoutExerciseId, REST_OVERRIDE_PRESETS[nextIndex])
        }
    }

    private fun updateSet(setId: Long, mutate: (WorkoutSet) -> WorkoutSet) {
        val current = state.value.exercises
            .asSequence()
            .flatMap { it.sets }
            .map { it.set }
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

    private fun addExercises(exerciseIds: List<Long>) {
        if (exerciseIds.isEmpty()) return
        val startPosition = state.value.exercises.size.toLong()
        scope.launch {
            exerciseIds.forEachIndexed { index, exerciseId ->
                workoutExerciseRepository.add(
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                    position = startPosition + index,
                    updatedAt = currentTimeMillis(),
                )
            }
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

    /**
     * Pairs [workoutExerciseId] with the exercise immediately after it into a superset. Either
     * exercise may already belong to a (different) superset, in which case the pair joins that
     * existing group — this is how a group grows past two exercises ("giant sets").
     */
    private fun groupWithNextExercise(workoutExerciseId: Long) {
        val exercises = state.value.exercises
        val index = exercises.indexOfFirst { it.workoutExerciseId == workoutExerciseId }
        if (index == -1 || index == exercises.lastIndex) return
        val current = exercises[index]
        val next = exercises[index + 1]
        val targetGroup = current.supersetGroup ?: next.supersetGroup ?: "sg-${current.workoutExerciseId}"
        scope.launch {
            if (current.supersetGroup != targetGroup) {
                workoutExerciseRepository.updateSupersetGroup(current.workoutExerciseId, targetGroup)
            }
            if (next.supersetGroup != targetGroup) {
                workoutExerciseRepository.updateSupersetGroup(next.workoutExerciseId, targetGroup)
            }
        }
    }

    /** Ungroups [workoutExerciseId]. If that leaves a single member behind, ungroups it too. */
    private fun removeFromSuperset(workoutExerciseId: Long) {
        val exercises = state.value.exercises
        val current = exercises.firstOrNull { it.workoutExerciseId == workoutExerciseId } ?: return
        val group = current.supersetGroup ?: return
        scope.launch {
            workoutExerciseRepository.updateSupersetGroup(workoutExerciseId, null)
            val remaining = exercises.filter { it.supersetGroup == group && it.workoutExerciseId != workoutExerciseId }
            if (remaining.size == 1) {
                workoutExerciseRepository.updateSupersetGroup(remaining.single().workoutExerciseId, null)
            }
        }
    }
}

/**
 * Annotates each exercise with its superset display label ("A", "B", ... assigned in order of
 * first appearance) and whether it's the group member currently "up next" to log — the member
 * with the fewest completed sets, ties broken by position. As sets complete, the fewest-completed
 * member changes, so this naturally alternates focus between the grouped exercises.
 */
private fun List<ActiveWorkoutExerciseUi>.withSupersetInfo(): List<ActiveWorkoutExerciseUi> {
    val members = filter { it.supersetGroup != null }.groupBy { it.supersetGroup }
    val upNextIds = members.values.mapNotNull { group ->
        if (group.size < 2) return@mapNotNull null
        fun ActiveWorkoutExerciseUi.completedCount() = sets.count { it.set.completed }
        fun ActiveWorkoutExerciseUi.hasIncompleteSet() = sets.any { !it.set.completed }
        val minCompleted = group.minOf { it.completedCount() }
        group
            .filter { it.completedCount() == minCompleted && it.hasIncompleteSet() }
            .minByOrNull { it.position }
            ?.workoutExerciseId
    }.toSet()

    val labels = mutableMapOf<String, String>()
    var nextLabelIndex = 0
    return map { exerciseUi ->
        val label = exerciseUi.supersetGroup?.let { group ->
            labels.getOrPut(group) { ('A' + nextLabelIndex++).toString() }
        }
        exerciseUi.copy(supersetLabel = label, isUpNextInSuperset = exerciseUi.workoutExerciseId in upNextIds)
    }
}

private fun WorkoutExercise.toUi(
    exerciseName: String,
    sets: List<WorkoutSet>,
    previousSets: List<WorkoutSet>,
) = ActiveWorkoutExerciseUi(
    workoutExerciseId = id,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    position = position,
    supersetGroup = supersetGroup,
    restSeconds = restSeconds,
    sets = sets.mapIndexed { index, set ->
        val previous = previousSets.getOrNull(index)
        ActiveWorkoutSetUi(
            set = set,
            previousReps = previous?.reps,
            previousWeight = previous?.weight,
            previousDurationSec = previous?.durationSec,
        )
    },
)

private fun SetType.next(): SetType {
    val values = SetType.entries
    return values[(values.indexOf(this) + 1) % values.size]
}
