package com.workoutapp.composeapp.data.library

import com.workoutapp.composeapp.data.workout.ExerciseSetHistoryEntry

/** One point in the "best weight over time" line: one per workout, oldest first. */
data class BestWeightPoint(val workoutStartedAt: Long, val weight: Double)

/**
 * The most recent completed sets for an exercise, newest first, capped at [limit]. [history] is
 * expected newest-first, as returned by
 * [com.workoutapp.composeapp.data.workout.WorkoutSetRepository.observeHistoryByExerciseId].
 */
fun recentSetsFor(history: List<ExerciseSetHistoryEntry>, limit: Int): List<ExerciseSetHistoryEntry> =
    history.take(limit)

/**
 * Reduces completed-set [history] into one point per workout — the heaviest weighted set logged
 * that session — ordered oldest first (so a line chart reads left-to-right as progress over
 * time), capped to the most recent [limit] workouts. Sets with no weight (e.g. bodyweight/duration
 * exercises) don't contribute a point.
 */
fun bestWeightSeries(history: List<ExerciseSetHistoryEntry>, limit: Int): List<BestWeightPoint> =
    history
        .filter { it.set.weight != null }
        .groupBy { it.workoutStartedAt }
        .map { (startedAt, entries) -> BestWeightPoint(startedAt, entries.maxOf { it.set.weight!! }) }
        .sortedBy { it.workoutStartedAt }
        .takeLast(limit)
