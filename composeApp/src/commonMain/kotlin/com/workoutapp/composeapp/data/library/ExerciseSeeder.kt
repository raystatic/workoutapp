package com.workoutapp.composeapp.data.library

import com.workoutapp.composeapp.data.db.currentTimeMillis
import kotlinx.coroutines.flow.first

/**
 * Populates the exercise library with [ExerciseSeedData.exercises] on first run. Diffs against
 * exercise names already in the DB so re-running (every app start, and across upgrades that grow
 * the catalog) only inserts what's missing rather than duplicating rows.
 */
class ExerciseSeeder(
    private val repository: ExerciseRepository,
) {
    suspend fun seedIfNeeded() {
        val existingNames = repository.observeAll().first().map { it.name }.toSet()
        val missing = ExerciseSeedData.exercises.filterNot { it.name in existingNames }
        if (missing.isEmpty()) return

        val updatedAt = currentTimeMillis()
        for (seed in missing) {
            repository.add(
                name = seed.name,
                primaryMuscle = seed.primaryMuscle,
                equipment = seed.equipment,
                secondaryMuscles = seed.secondaryMuscles,
                instructions = seed.instructions,
                updatedAt = updatedAt,
            )
        }
    }
}
