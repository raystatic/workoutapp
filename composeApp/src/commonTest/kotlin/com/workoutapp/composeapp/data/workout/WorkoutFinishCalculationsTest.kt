package com.workoutapp.composeapp.data.workout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkoutFinishCalculationsTest {

    @Test
    fun estimateOneRepMax_appliesEpleyFormula() {
        assertEquals(140.0, estimateOneRepMax(weight = 100.0, reps = 12L))
        assertEquals(100.0, estimateOneRepMax(weight = 100.0, reps = 0L))
    }

    @Test
    fun personalRecordCandidates_noCompletedSets_returnsEmpty() {
        assertTrue(personalRecordCandidates(exerciseId = 1L, completedSets = emptyList()).isEmpty())
    }

    @Test
    fun personalRecordCandidates_repsOnlyNoWeight_returnsNoCandidates() {
        val candidates = personalRecordCandidates(1L, listOf(CompletedSetPerformance(reps = 10L, weight = null)))

        assertTrue(candidates.isEmpty())
    }

    @Test
    fun personalRecordCandidates_weightWithoutReps_returnsOnlyMaxWeight() {
        val candidates = personalRecordCandidates(1L, listOf(CompletedSetPerformance(reps = null, weight = 80.0)))

        assertEquals(listOf(PersonalRecordCandidate(1L, PersonalRecordType.MAX_WEIGHT, 80.0)), candidates)
    }

    @Test
    fun personalRecordCandidates_weightAndReps_returnsAllThreeTypes() {
        val candidates = personalRecordCandidates(1L, listOf(CompletedSetPerformance(reps = 10L, weight = 100.0)))

        val byType = candidates.associateBy { it.type }
        assertEquals(100.0, byType.getValue(PersonalRecordType.MAX_WEIGHT).value)
        assertEquals(estimateOneRepMax(100.0, 10L), byType.getValue(PersonalRecordType.ONE_REP_MAX).value)
        assertEquals(1000.0, byType.getValue(PersonalRecordType.BEST_VOLUME).value)
    }

    @Test
    fun personalRecordCandidates_multipleSets_pickHighestPerTypeAndSumsVolume() {
        val candidates = personalRecordCandidates(
            1L,
            listOf(
                CompletedSetPerformance(reps = 10L, weight = 60.0),
                CompletedSetPerformance(reps = 5L, weight = 80.0),
            ),
        )

        val byType = candidates.associateBy { it.type }
        assertEquals(80.0, byType.getValue(PersonalRecordType.MAX_WEIGHT).value)
        assertEquals(
            maxOf(estimateOneRepMax(60.0, 10L), estimateOneRepMax(80.0, 5L)),
            byType.getValue(PersonalRecordType.ONE_REP_MAX).value,
        )
        assertEquals(600.0 + 400.0, byType.getValue(PersonalRecordType.BEST_VOLUME).value)
    }

    @Test
    fun dayIdFor_bucketsTimestampsWithinTheSameUtcDayTogether() {
        val startOfDay = 10L * 86_400_000L
        assertEquals(dayIdFor(startOfDay), dayIdFor(startOfDay + 3_600_000L))
        assertEquals(dayIdFor(startOfDay) + 1, dayIdFor(startOfDay + 86_400_000L))
    }

    @Test
    fun computeStreak_emptyDays_returnsZero() {
        assertEquals(0L, computeStreak(emptySet(), latestDay = 5L))
    }

    @Test
    fun computeStreak_latestDayMissing_returnsZero() {
        assertEquals(0L, computeStreak(setOf(3L, 4L), latestDay = 5L))
    }

    @Test
    fun computeStreak_consecutiveDays_countsAllOfThem() {
        assertEquals(3L, computeStreak(setOf(3L, 4L, 5L), latestDay = 5L))
    }

    @Test
    fun computeStreak_gapBreaksTheStreak() {
        assertEquals(1L, computeStreak(setOf(1L, 3L, 5L), latestDay = 5L))
    }
}
