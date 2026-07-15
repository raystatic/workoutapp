package com.workoutapp.composeapp.data.plates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlateCalculationsTest {

    @Test
    fun calculatePlateBreakdown_exactMatch_kg() {
        val breakdown = calculatePlateBreakdown(
            targetWeight = 100.0,
            barWeight = WeightUnit.KG.defaultBarWeight,
            availablePlates = WeightUnit.KG.defaultPlates,
        )

        assertEquals(listOf(25.0, 15.0), breakdown.platesPerSide)
        assertEquals(100.0, breakdown.achievedWeight)
        assertTrue(breakdown.isExactMatch)
    }

    @Test
    fun calculatePlateBreakdown_exactMatch_lb() {
        val breakdown = calculatePlateBreakdown(
            targetWeight = 225.0,
            barWeight = WeightUnit.LB.defaultBarWeight,
            availablePlates = WeightUnit.LB.defaultPlates,
        )

        // Each denomination is assumed to be available in unlimited pairs (as real plate
        // calculators do), so two 45s wins over 45+35+10 for a 90 lb-per-side target.
        assertEquals(listOf(45.0, 45.0), breakdown.platesPerSide)
        assertEquals(225.0, breakdown.achievedWeight)
        assertTrue(breakdown.isExactMatch)
    }

    @Test
    fun calculatePlateBreakdown_unreachableTarget_fallsBackToClosestReachableWeight() {
        val breakdown = calculatePlateBreakdown(
            targetWeight = 101.0,
            barWeight = WeightUnit.KG.defaultBarWeight,
            availablePlates = WeightUnit.KG.defaultPlates,
        )

        assertEquals(listOf(25.0, 15.0), breakdown.platesPerSide)
        assertEquals(100.0, breakdown.achievedWeight)
        assertTrue(!breakdown.isExactMatch)
    }

    @Test
    fun calculatePlateBreakdown_targetAtOrBelowBar_returnsBarOnly() {
        val exact = calculatePlateBreakdown(targetWeight = 20.0, barWeight = 20.0, availablePlates = listOf(25.0, 10.0))
        assertEquals(emptyList(), exact.platesPerSide)
        assertEquals(20.0, exact.achievedWeight)
        assertTrue(exact.isExactMatch)

        val below = calculatePlateBreakdown(targetWeight = 15.0, barWeight = 20.0, availablePlates = listOf(25.0, 10.0))
        assertEquals(emptyList(), below.platesPerSide)
        assertEquals(20.0, below.achievedWeight)
        assertTrue(!below.isExactMatch)
    }

    @Test
    fun calculatePlateBreakdown_noAvailablePlates_returnsBarOnlyAndNotExact() {
        val breakdown = calculatePlateBreakdown(targetWeight = 100.0, barWeight = 20.0, availablePlates = emptyList())

        assertEquals(emptyList(), breakdown.platesPerSide)
        assertEquals(20.0, breakdown.achievedWeight)
        assertTrue(!breakdown.isExactMatch)
    }

    @Test
    fun calculatePlateBreakdown_ignoresNonPositivePlateWeights() {
        val breakdown = calculatePlateBreakdown(
            targetWeight = 40.0,
            barWeight = 20.0,
            availablePlates = listOf(10.0, 0.0, -5.0),
        )

        assertEquals(listOf(10.0), breakdown.platesPerSide)
        assertEquals(40.0, breakdown.achievedWeight)
        assertTrue(breakdown.isExactMatch)
    }

    @Test
    fun generateWarmupSets_scalesPercentagesAndRoundsToNearestIncrement() {
        val sets = generateWarmupSets(workingWeight = 97.0, roundingIncrement = 2.5)

        assertEquals(
            listOf(WarmupSet(40.0, 8), WarmupSet(57.5, 5), WarmupSet(77.5, 3)),
            sets,
        )
    }

    @Test
    fun generateWarmupSets_nonPositiveWorkingWeight_returnsEmpty() {
        assertTrue(generateWarmupSets(0.0).isEmpty())
        assertTrue(generateWarmupSets(-10.0).isEmpty())
    }

    @Test
    fun generateWarmupSets_customScheme_usesProvidedSteps() {
        val sets = generateWarmupSets(
            workingWeight = 100.0,
            scheme = listOf(WarmupStep(0.5, 10)),
            roundingIncrement = 5.0,
        )

        assertEquals(listOf(WarmupSet(50.0, 10)), sets)
    }

    @Test
    fun generateWarmupSets_nonPositiveRoundingIncrement_returnsRawPercentageWeight() {
        val sets = generateWarmupSets(
            workingWeight = 97.0,
            scheme = listOf(WarmupStep(0.4, 8)),
            roundingIncrement = 0.0,
        )

        assertEquals(1, sets.size)
        assertEquals(38.8, sets.single().weight, 1e-9)
        assertEquals(8, sets.single().reps)
    }
}
