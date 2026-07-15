package com.workoutapp.composeapp.data.plates

import kotlin.math.abs
import kotlin.math.round

/** Unit a plate/warm-up calculation is expressed in, each with a sensible bar + plate-set default. */
enum class WeightUnit(val defaultBarWeight: Double, val defaultPlates: List<Double>) {
    KG(20.0, listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)),
    LB(45.0, listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)),
}

/**
 * Result of solving for the plates needed on one side of a barbell; the same plates go on the
 * other side. When [availablePlates] can't hit the requested target weight exactly, the solver
 * falls back to the closest reachable weight at or below the target and [isExactMatch] is false.
 */
data class PlateBreakdown(
    val platesPerSide: List<Double>,
    val achievedWeight: Double,
    val isExactMatch: Boolean,
)

private const val EPSILON = 1e-6

/**
 * Greedily fills each side with the largest available plates first. Returns an empty
 * [PlateBreakdown.platesPerSide] (bar only) when [targetWeight] is at or below [barWeight].
 */
fun calculatePlateBreakdown(
    targetWeight: Double,
    barWeight: Double,
    availablePlates: List<Double>,
): PlateBreakdown {
    val perSideTarget = (targetWeight - barWeight) / 2.0
    if (perSideTarget <= EPSILON) {
        return PlateBreakdown(
            platesPerSide = emptyList(),
            achievedWeight = barWeight,
            isExactMatch = abs(targetWeight - barWeight) < EPSILON,
        )
    }

    val platesPerSide = mutableListOf<Double>()
    var remaining = perSideTarget
    for (plate in availablePlates.filter { it > 0.0 }.sortedDescending()) {
        while (remaining + EPSILON >= plate) {
            platesPerSide.add(plate)
            remaining -= plate
        }
    }

    val achievedWeight = barWeight + (perSideTarget - remaining) * 2
    return PlateBreakdown(
        platesPerSide = platesPerSide,
        achievedWeight = achievedWeight,
        isExactMatch = abs(achievedWeight - targetWeight) < EPSILON,
    )
}

/** One step of a warm-up ramp: a percentage of the working weight paired with a rep target. */
data class WarmupStep(val percentageOfWorkingWeight: Double, val reps: Int)

/** A classic 3-step ramp: light for volume, then progressively heavier and lower-rep. */
val DEFAULT_WARMUP_SCHEME: List<WarmupStep> = listOf(
    WarmupStep(0.4, 8),
    WarmupStep(0.6, 5),
    WarmupStep(0.8, 3),
)

data class WarmupSet(val weight: Double, val reps: Int)

/**
 * Generates warm-up sets ramping toward [workingWeight], rounded to the nearest
 * [roundingIncrement] (e.g. 2.5 kg / 5 lb plate jumps). [scheme] is caller-adjustable so callers
 * can add/remove steps or change rep targets; defaults to [DEFAULT_WARMUP_SCHEME]. Returns an
 * empty list for a non-positive [workingWeight].
 */
fun generateWarmupSets(
    workingWeight: Double,
    scheme: List<WarmupStep> = DEFAULT_WARMUP_SCHEME,
    roundingIncrement: Double = 2.5,
): List<WarmupSet> {
    if (workingWeight <= 0.0) return emptyList()
    return scheme.map { step ->
        WarmupSet(
            weight = roundToIncrement(workingWeight * step.percentageOfWorkingWeight, roundingIncrement),
            reps = step.reps,
        )
    }
}

private fun roundToIncrement(value: Double, increment: Double): Double {
    if (increment <= 0.0) return value
    return round(value / increment) * increment
}
