package com.workoutapp.composeapp.data.library

import com.workoutapp.composeapp.data.db.SetType
import com.workoutapp.composeapp.data.workout.ExerciseSetHistoryEntry
import com.workoutapp.composeapp.db.WorkoutSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun entry(
    id: Long,
    workoutStartedAt: Long,
    weight: Double? = null,
    reps: Long? = null,
) = ExerciseSetHistoryEntry(
    set = WorkoutSet(id, 1L, 0L, reps, weight, null, SetType.NORMAL, true, null, null, workoutStartedAt, "PENDING"),
    workoutStartedAt = workoutStartedAt,
)

class ExerciseHistoryCalculationsTest {

    @Test
    fun recentSetsFor_noHistory_returnsEmpty() {
        assertTrue(recentSetsFor(emptyList(), limit = 10).isEmpty())
    }

    @Test
    fun recentSetsFor_capsAtLimitPreservingOrder() {
        val history = listOf(entry(3L, 3000L), entry(2L, 2000L), entry(1L, 1000L))

        val recent = recentSetsFor(history, limit = 2)

        assertEquals(listOf(3L, 2L), recent.map { it.set.id })
    }

    @Test
    fun bestWeightSeries_noWeightedSets_returnsEmpty() {
        val history = listOf(entry(1L, 1000L, weight = null, reps = 10L))

        assertTrue(bestWeightSeries(history, limit = 10).isEmpty())
    }

    @Test
    fun bestWeightSeries_multipleSetsSameWorkout_takesTheHeaviest() {
        val history = listOf(
            entry(1L, 1000L, weight = 80.0),
            entry(2L, 1000L, weight = 100.0),
            entry(3L, 1000L, weight = 90.0),
        )

        val series = bestWeightSeries(history, limit = 10)

        assertEquals(listOf(BestWeightPoint(1000L, 100.0)), series)
    }

    @Test
    fun bestWeightSeries_multipleWorkouts_isOrderedOldestFirst() {
        val history = listOf(
            entry(1L, 3000L, weight = 110.0),
            entry(2L, 1000L, weight = 90.0),
            entry(3L, 2000L, weight = 100.0),
        )

        val series = bestWeightSeries(history, limit = 10)

        assertEquals(
            listOf(BestWeightPoint(1000L, 90.0), BestWeightPoint(2000L, 100.0), BestWeightPoint(3000L, 110.0)),
            series,
        )
    }

    @Test
    fun bestWeightSeries_capsAtLimitKeepingMostRecentWorkouts() {
        val history = listOf(
            entry(1L, 1000L, weight = 80.0),
            entry(2L, 2000L, weight = 90.0),
            entry(3L, 3000L, weight = 100.0),
        )

        val series = bestWeightSeries(history, limit = 2)

        assertEquals(listOf(BestWeightPoint(2000L, 90.0), BestWeightPoint(3000L, 100.0)), series)
    }
}
