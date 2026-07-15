package com.workoutapp.android

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.workoutapp.composeapp.data.library.ExerciseRepository
import com.workoutapp.composeapp.data.workout.WorkoutExerciseRepository
import com.workoutapp.composeapp.data.workout.WorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * connectedDebugAndroidTest for custom exercises (#22): creating one from the "+ Add Exercise"
 * flow makes it loggable like any seeded exercise, and the free tier's cap of 7 blocks further
 * creation with an upsell-ready message.
 */
class AddCustomExerciseFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun creatingACustomExercise_addsItToTheLibraryAndMakesItLoggable() {
        val workoutRepository = GlobalContext.get().get<WorkoutRepository>()
        val workoutExerciseRepository = GlobalContext.get().get<WorkoutExerciseRepository>()
        val exerciseRepository = GlobalContext.get().get<ExerciseRepository>()

        composeRule.onNodeWithTag("start_empty_workout").performClick()
        composeRule.onNodeWithTag("active_workout_screen").assertExists()
        val workoutId = runBlocking { workoutRepository.observeAll().first().first().id }

        composeRule.onNodeWithTag("add_exercise_button").performClick()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("Barbell Bench Press").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("add_exercise_add_custom_exercise_link").performClick()
        composeRule.onNodeWithTag("add_custom_exercise_screen").assertExists()

        composeRule.onNodeWithTag("add_custom_exercise_name_field").performTextInput("Band Pull-Apart")
        composeRule.onNodeWithTag("add_custom_exercise_primary_muscle_field").performTextInput("Shoulders")
        composeRule.onNodeWithTag("add_custom_exercise_equipment_field").performTextInput("Band")
        composeRule.onNodeWithTag("add_custom_exercise_type_field").performClick()
        composeRule.onNodeWithTag("add_custom_exercise_type_option_Strength").performClick()
        composeRule.onNodeWithTag("add_custom_exercise_save_button").performClick()

        // Saving pops back to the active workout screen; reopen the picker to find the new exercise.
        composeRule.onNodeWithTag("active_workout_screen").assertExists()
        composeRule.onNodeWithTag("add_exercise_button").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Band Pull-Apart").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Band Pull-Apart").performClick()
        composeRule.onNodeWithTag("add_exercise_confirm_button").performClick()

        val addedExercise = runBlocking {
            workoutExerciseRepository.observeByWorkoutId(workoutId).first { it.isNotEmpty() }
            val exercise = exerciseRepository.observeAll().first().first { it.name == "Band Pull-Apart" }
            exercise
        }
        assertTrue(addedExercise.isCustom)
        assertEquals("Strength", addedExercise.type)

        val workoutExerciseId = runBlocking {
            workoutExerciseRepository.observeByWorkoutId(workoutId).first().first { it.exerciseId == addedExercise.id }.id
        }
        composeRule.onNodeWithTag("add_set_$workoutExerciseId").performClick()
    }

    @Test
    fun atTheFreeCap_savingIsBlockedWithAnUpsellMessage() {
        val exerciseRepository = GlobalContext.get().get<ExerciseRepository>()
        val countBefore = runBlocking { exerciseRepository.observeCustomCount().first() }
        runBlocking {
            repeat(7) { index ->
                exerciseRepository.add(
                    name = "Custom Move $index",
                    primaryMuscle = "Full Body",
                    equipment = "None",
                    isCustom = true,
                    updatedAt = 1_000L,
                )
            }
        }

        composeRule.onNodeWithTag("start_empty_workout").performClick()
        composeRule.onNodeWithTag("active_workout_screen").assertExists()

        composeRule.onNodeWithTag("add_exercise_button").performClick()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("Barbell Bench Press").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("add_exercise_add_custom_exercise_link").performClick()
        composeRule.onNodeWithTag("add_custom_exercise_screen").assertExists()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("add_custom_exercise_cap_message").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("add_custom_exercise_name_field").performTextInput("One Too Many")
        composeRule.onNodeWithTag("add_custom_exercise_primary_muscle_field").performTextInput("Full Body")
        composeRule.onNodeWithTag("add_custom_exercise_equipment_field").performTextInput("None")
        composeRule.onNodeWithTag("add_custom_exercise_type_field").performClick()
        composeRule.onNodeWithTag("add_custom_exercise_type_option_Strength").performClick()
        composeRule.onNodeWithTag("add_custom_exercise_save_button").assertIsNotEnabled()

        val countAfter = runBlocking { exerciseRepository.observeCustomCount().first() }
        assertEquals(countBefore + 7, countAfter)
    }
}
