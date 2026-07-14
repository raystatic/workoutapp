package com.workoutapp.composeapp.data.workout

import com.workoutapp.composeapp.db.WorkoutSet

/**
 * Finds the sets logged for an exercise in the most recent other workout, so
 * the active-workout screen can show "last time" values alongside each set
 * row (matched by position/index in the caller).
 */
class PreviousSetResolver(
    private val workoutExerciseRepository: WorkoutExerciseRepository,
    private val workoutSetRepository: WorkoutSetRepository,
) {
    suspend fun resolve(exerciseId: Long, excludingWorkoutId: Long): List<WorkoutSet> {
        val previousWorkoutExerciseId = workoutExerciseRepository
            .findMostRecentOtherWorkoutExerciseId(exerciseId, excludingWorkoutId)
            ?: return emptyList()
        return workoutSetRepository.getByWorkoutExerciseId(previousWorkoutExerciseId)
    }
}
