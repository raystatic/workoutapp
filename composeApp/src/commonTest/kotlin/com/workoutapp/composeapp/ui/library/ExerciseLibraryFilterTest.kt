package com.workoutapp.composeapp.ui.library

import com.workoutapp.composeapp.db.Exercise
import kotlin.test.Test
import kotlin.test.assertEquals

private fun exercise(
    id: Long,
    name: String,
    primaryMuscle: String,
    equipment: String,
    secondaryMuscles: List<String> = emptyList(),
) = Exercise(id, name, primaryMuscle, secondaryMuscles, equipment, null, false, null, null, 1_000L, "PENDING", null)

class ExerciseLibraryFilterTest {
    private val benchPress = exercise(1L, "Barbell Bench Press", "Chest", "Barbell", secondaryMuscles = listOf("Triceps"))
    private val squat = exercise(2L, "Barbell Squat", "Quads", "Barbell", secondaryMuscles = listOf("Glutes"))
    private val dumbbellCurl = exercise(3L, "Dumbbell Curl", "Biceps", "Dumbbell")
    private val all = listOf(benchPress, squat, dumbbellCurl)

    @Test
    fun filterExercises_blankQueryAndNoFilters_returnsEveryExercise() {
        assertEquals(all, filterExercises(all, query = "", equipment = null, muscle = null))
    }

    @Test
    fun filterExercises_byName_isCaseInsensitiveAndMatchesSubstring() {
        assertEquals(listOf(benchPress, squat), filterExercises(all, query = "barbell", equipment = null, muscle = null))
    }

    @Test
    fun filterExercises_byEquipment_matchesExactly() {
        assertEquals(listOf(dumbbellCurl), filterExercises(all, query = "", equipment = "Dumbbell", muscle = null))
    }

    @Test
    fun filterExercises_byMuscle_matchesPrimaryMuscle() {
        assertEquals(listOf(benchPress), filterExercises(all, query = "", equipment = null, muscle = "Chest"))
    }

    @Test
    fun filterExercises_byMuscle_matchesSecondaryMuscle() {
        assertEquals(listOf(squat), filterExercises(all, query = "", equipment = null, muscle = "Glutes"))
    }

    @Test
    fun filterExercises_searchAndFilters_combineWithAnd() {
        val result = filterExercises(all, query = "barbell", equipment = "Barbell", muscle = "Quads")
        assertEquals(listOf(squat), result)
    }

    @Test
    fun filterExercises_searchAndFilters_noMatch_returnsEmpty() {
        val result = filterExercises(all, query = "barbell", equipment = "Dumbbell", muscle = null)
        assertEquals(emptyList(), result)
    }

    @Test
    fun equipmentOptions_returnsDistinctSortedValues() {
        assertEquals(listOf("Barbell", "Dumbbell"), equipmentOptions(all))
    }

    @Test
    fun muscleOptions_includesPrimaryAndSecondaryMuscles() {
        assertEquals(listOf("Biceps", "Chest", "Glutes", "Quads", "Triceps"), muscleOptions(all))
    }
}
