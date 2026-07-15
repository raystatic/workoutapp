package com.workoutapp.android

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.workoutapp.composeapp.data.library.ExerciseRepository
import com.workoutapp.composeapp.data.workout.WorkoutExerciseRepository
import com.workoutapp.composeapp.data.workout.WorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import kotlin.test.assertEquals

/**
 * connectedDebugAndroidTest for the shared exercise library picker (#20): filtering the
 * active-workout's "+ Add Exercise" flow by muscle narrows the list, and multi-selecting two
 * exercises before confirming adds both to the workout in one go.
 */
class ExerciseLibraryPickerTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun filteringByMuscle_thenMultiSelecting_addsBothExercisesToTheWorkout() {
        val workoutRepository = GlobalContext.get().get<WorkoutRepository>()
        val workoutExerciseRepository = GlobalContext.get().get<WorkoutExerciseRepository>()

        composeRule.onNodeWithTag("start_empty_workout").performClick()
        composeRule.onNodeWithTag("active_workout_screen").assertExists()
        val workoutId = runBlocking { workoutRepository.observeAll().first().first().id }

        composeRule.onNodeWithTag("add_exercise_button").performClick()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("Barbell Bench Press").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("add_exercise_muscle_filter").performClick()
        composeRule.onNodeWithTag("add_exercise_muscle_option_Quadriceps").performClick()

        // The muscle filter excludes chest exercises...
        composeRule.onNodeWithText("Barbell Bench Press").assertDoesNotExist()
        // ...and includes quad exercises.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Barbell Squat").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Barbell Squat").performClick()
        composeRule.onNodeWithText("Dumbbell Squat").performClick()
        composeRule.onNodeWithTag("add_exercise_confirm_button").performClick()

        val addedExerciseNames = runBlocking {
            val exerciseRepository = GlobalContext.get().get<ExerciseRepository>()
            val allExercises = exerciseRepository.observeAll().first().associateBy { it.id }
            workoutExerciseRepository.observeByWorkoutId(workoutId)
                .first { it.size == 2 }
                .map { allExercises.getValue(it.exerciseId).name }
                .toSet()
        }
        assertEquals(setOf("Barbell Squat", "Dumbbell Squat"), addedExerciseNames)
    }
}
