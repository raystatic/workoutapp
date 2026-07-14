package com.workoutapp.composeapp.data.workout

/** [com.workoutapp.composeapp.db.PersonalRecord.type] values this app records. */
object PersonalRecordType {
    const val ONE_REP_MAX = "1RM"
    const val MAX_WEIGHT = "maxWeight"
    const val BEST_VOLUME = "bestVolume"
}

/** The reps/weight of one completed set, as needed for PR detection. */
data class CompletedSetPerformance(val reps: Long?, val weight: Double?)

/** A PR-eligible value derived from this workout, pending comparison against the exercise's prior best. */
data class PersonalRecordCandidate(val exerciseId: Long, val type: String, val value: Double)

/** Epley formula: estimates the one-rep max implied by a [weight] x [reps] set. */
fun estimateOneRepMax(weight: Double, reps: Long): Double = weight * (1.0 + reps / 30.0)

/**
 * Derives this workout's best-of candidates for [exerciseId] from its completed sets: the
 * heaviest weight lifted, the highest estimated 1RM, and the total session volume. A candidate
 * is omitted when its inputs are missing (e.g. no weighted sets at all). Callers compare each
 * candidate against [PersonalRecordRepository.getBestValue][com.workoutapp.composeapp.data.progress.PersonalRecordRepository.getBestValue]
 * to decide whether it's actually a new record.
 */
fun personalRecordCandidates(exerciseId: Long, completedSets: List<CompletedSetPerformance>): List<PersonalRecordCandidate> {
    val maxWeight = completedSets.mapNotNull { it.weight }.maxOrNull()
    val bestOneRepMax = completedSets
        .mapNotNull { set -> if (set.weight != null && set.reps != null && set.reps > 0) estimateOneRepMax(set.weight, set.reps) else null }
        .maxOrNull()
    val totalVolume = completedSets
        .sumOf { set -> if (set.weight != null && set.reps != null) set.weight * set.reps else 0.0 }
        .takeIf { it > 0.0 }
    return listOfNotNull(
        maxWeight?.let { PersonalRecordCandidate(exerciseId, PersonalRecordType.MAX_WEIGHT, it) },
        bestOneRepMax?.let { PersonalRecordCandidate(exerciseId, PersonalRecordType.ONE_REP_MAX, it) },
        totalVolume?.let { PersonalRecordCandidate(exerciseId, PersonalRecordType.BEST_VOLUME, it) },
    )
}

private const val MILLIS_PER_DAY = 86_400_000L

/** Buckets an epoch-millis timestamp into a UTC calendar day, for streak/consecutive-day math. */
fun dayIdFor(epochMillis: Long): Long = epochMillis.floorDiv(MILLIS_PER_DAY)

/**
 * The current streak length ending on [latestDay]: the number of consecutive days, counting
 * backward from [latestDay], present in [workoutDays]. Zero when [latestDay] itself isn't in the set.
 */
fun computeStreak(workoutDays: Set<Long>, latestDay: Long): Long {
    var streak = 0L
    var day = latestDay
    while (day in workoutDays) {
        streak++
        day--
    }
    return streak
}
