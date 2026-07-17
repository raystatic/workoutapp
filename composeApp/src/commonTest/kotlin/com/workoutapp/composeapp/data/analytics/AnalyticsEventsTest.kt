package com.workoutapp.composeapp.data.analytics

import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsEventsTest {
    @Test
    fun workoutStartedParams_withNoRoutine_reportsEmptySource() {
        assertEquals(mapOf("source" to "empty", "routine_id" to null), workoutStartedParams(routineId = null))
    }

    @Test
    fun workoutStartedParams_withRoutine_reportsRoutineSourceAndId() {
        assertEquals(mapOf("source" to "routine", "routine_id" to 7L), workoutStartedParams(routineId = 7L))
    }

    @Test
    fun workoutCompletedParams_reportsCountsVerbatim() {
        assertEquals(
            mapOf("workout_count" to 12L, "streak" to 3L, "pr_count" to 2),
            workoutCompletedParams(workoutCount = 12L, streak = 3L, personalRecordCount = 2),
        )
    }

    @Test
    fun logDurationParams_reportsDurationSeconds() {
        assertEquals(mapOf("duration_seconds" to 3_600L), logDurationParams(durationSeconds = 3_600L))
    }

    @Test
    fun workoutAbandonedParams_reportsElapsedSeconds() {
        assertEquals(mapOf("elapsed_seconds" to 45L), workoutAbandonedParams(elapsedSeconds = 45L))
    }

    @Test
    fun restTimerUsedParams_reportsAllFields() {
        assertEquals(
            mapOf("exercise_id" to 100L, "seconds" to 90, "source" to "default", "experiment_variant" to "ON"),
            restTimerUsedParams(exerciseId = 100L, seconds = 90, source = "default", experimentVariant = "ON"),
        )
    }

    @Test
    fun routineCreatedParams_reportsSource() {
        assertEquals(mapOf("source" to "duplicate"), routineCreatedParams(source = "duplicate"))
    }

    @Test
    fun routineUsedInLogParams_reportsRoutineId() {
        assertEquals(mapOf("routine_id" to 5L), routineUsedInLogParams(routineId = 5L))
    }

    @Test
    fun routineSaveFunnelParams_reportsStep() {
        assertEquals(mapOf("step" to "completed"), routineSaveFunnelParams(step = "completed"))
    }

    @Test
    fun assignVariant_belowHalf_isOn() {
        assertEquals(RestTimerDefaultVariant.ON, assignVariant(0.0))
        assertEquals(RestTimerDefaultVariant.ON, assignVariant(0.49))
    }

    @Test
    fun assignVariant_atOrAboveHalf_isOff() {
        assertEquals(RestTimerDefaultVariant.OFF, assignVariant(0.5))
        assertEquals(RestTimerDefaultVariant.OFF, assignVariant(0.99))
    }
}
