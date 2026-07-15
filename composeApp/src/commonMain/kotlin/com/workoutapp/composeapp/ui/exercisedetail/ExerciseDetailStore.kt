package com.workoutapp.composeapp.ui.exercisedetail

import com.workoutapp.composeapp.data.library.BestWeightPoint
import com.workoutapp.composeapp.data.library.ExerciseRepository
import com.workoutapp.composeapp.data.library.bestWeightSeries
import com.workoutapp.composeapp.data.library.recentSetsFor
import com.workoutapp.composeapp.data.workout.ExerciseSetHistoryEntry
import com.workoutapp.composeapp.data.workout.WorkoutSetRepository
import com.workoutapp.composeapp.db.Exercise
import com.workoutapp.composeapp.mvi.MviEffect
import com.workoutapp.composeapp.mvi.MviIntent
import com.workoutapp.composeapp.mvi.MviState
import com.workoutapp.composeapp.mvi.StoreViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Number of recent sets and best-weight points surfaced on the exercise detail screen. */
private const val RECENT_SETS_LIMIT = 10
private const val BEST_WEIGHT_POINTS_LIMIT = 10

data class ExerciseDetailState(
    val exerciseId: Long,
    val exercise: Exercise? = null,
    val recentSets: List<ExerciseSetHistoryEntry> = emptyList(),
    val bestWeightSeries: List<BestWeightPoint> = emptyList(),
    val isLoading: Boolean = true,
) : MviState

/** Read-only screen — no user actions to reduce, but the type exists so [StoreViewModel] can be parameterized. */
sealed interface ExerciseDetailIntent : MviIntent

/** No effects currently fire; the type exists so [StoreViewModel] can be parameterized. */
sealed interface ExerciseDetailEffect : MviEffect

/**
 * Backs the exercise detail screen for [exerciseId]: the exercise's own fields (media, muscles,
 * equipment, instructions) plus a basic history derived from every completed set ever logged
 * against it — the most recent sets, and a best-weight-per-workout line for a simple progress
 * view.
 */
class ExerciseDetailStore(
    private val exerciseId: Long,
    exerciseRepository: ExerciseRepository,
    workoutSetRepository: WorkoutSetRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : StoreViewModel<ExerciseDetailState, ExerciseDetailIntent, ExerciseDetailEffect>(
    ExerciseDetailState(exerciseId = exerciseId),
    dispatcher,
) {
    init {
        combine(
            exerciseRepository.observeById(exerciseId),
            workoutSetRepository.observeHistoryByExerciseId(exerciseId),
        ) { exercise, history ->
            ExerciseDetailState(
                exerciseId = exerciseId,
                exercise = exercise,
                recentSets = recentSetsFor(history, RECENT_SETS_LIMIT),
                bestWeightSeries = bestWeightSeries(history, BEST_WEIGHT_POINTS_LIMIT),
                isLoading = false,
            )
        }.onEach { derived -> setState { derived } }.launchIn(scope)
    }

    override fun onIntent(intent: ExerciseDetailIntent) {
    }
}
