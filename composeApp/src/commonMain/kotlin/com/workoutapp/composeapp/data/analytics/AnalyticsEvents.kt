package com.workoutapp.composeapp.data.analytics

/** Event names for the Logging + Routine KPIs (EXECUTION_PLAN.md §6). */
object AnalyticsEvent {
    const val WORKOUT_STARTED = "workout_started"
    const val WORKOUT_COMPLETED = "workout_completed"
    const val WORKOUT_ABANDONED = "workout_abandoned"
    const val LOG_DURATION = "log_duration"
    const val REST_TIMER_USED = "rest_timer_used"
    const val ROUTINE_CREATED = "routine_created"
    const val ROUTINE_USED_IN_LOG = "routine_used_in_log"
    const val ROUTINE_SAVE_FUNNEL = "routine_save_funnel"
}

/** [routineId] is `null` for an empty workout, set when the workout was pre-filled from a routine. */
fun workoutStartedParams(routineId: Long?): Map<String, Any?> =
    mapOf("source" to if (routineId != null) "routine" else "empty", "routine_id" to routineId)

fun workoutCompletedParams(workoutCount: Long, streak: Long, personalRecordCount: Int): Map<String, Any?> =
    mapOf("workout_count" to workoutCount, "streak" to streak, "pr_count" to personalRecordCount)

/** Total elapsed time (start to finish) for the workout just saved — feeds the "avg log time" KPI. */
fun logDurationParams(durationSeconds: Long): Map<String, Any?> =
    mapOf("duration_seconds" to durationSeconds)

fun workoutAbandonedParams(elapsedSeconds: Long): Map<String, Any?> =
    mapOf("elapsed_seconds" to elapsedSeconds)

/** [source] is `"default"` when the exercise has no rest-time override, `"override"` otherwise. */
fun restTimerUsedParams(exerciseId: Long, seconds: Int, source: String, experimentVariant: String): Map<String, Any?> =
    mapOf(
        "exercise_id" to exerciseId,
        "seconds" to seconds,
        "source" to source,
        "experiment_variant" to experimentVariant,
    )

/** [source] is one of `"blank"`, `"duplicate"`, `"save_as_routine"`. */
fun routineCreatedParams(source: String): Map<String, Any?> = mapOf("source" to source)

fun routineUsedInLogParams(routineId: Long): Map<String, Any?> = mapOf("routine_id" to routineId)

/** [step] marks progress through the save-as-routine funnel, e.g. `"completed"`. */
fun routineSaveFunnelParams(step: String): Map<String, Any?> = mapOf("step" to step)
