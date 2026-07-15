package com.workoutapp.composeapp.ui.library

import com.workoutapp.composeapp.db.Exercise

/**
 * Applies a free-text name search and optional equipment/muscle filters together (AND'd). A
 * muscle filter matches either an exercise's primary muscle or any of its secondary muscles.
 */
fun filterExercises(
    exercises: List<Exercise>,
    query: String,
    equipment: String?,
    muscle: String?,
): List<Exercise> {
    val trimmedQuery = query.trim()
    return exercises.filter { exercise ->
        val matchesQuery = trimmedQuery.isEmpty() || exercise.name.contains(trimmedQuery, ignoreCase = true)
        val matchesEquipment = equipment == null || exercise.equipment == equipment
        val matchesMuscle = muscle == null ||
            exercise.primaryMuscle == muscle ||
            exercise.secondaryMuscles.contains(muscle)
        matchesQuery && matchesEquipment && matchesMuscle
    }
}

/** Distinct equipment values present in [exercises], sorted for stable menu ordering. */
fun equipmentOptions(exercises: List<Exercise>): List<String> =
    exercises.map { it.equipment }.distinct().sorted()

/** Distinct primary + secondary muscle values present in [exercises], sorted for stable menu ordering. */
fun muscleOptions(exercises: List<Exercise>): List<String> =
    (exercises.map { it.primaryMuscle } + exercises.flatMap { it.secondaryMuscles }).distinct().sorted()
