package com.workoutapp.android

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.workoutapp.composeapp.data.workout.WorkoutExerciseRepository
import com.workoutapp.composeapp.data.workout.WorkoutRepository
import com.workoutapp.composeapp.data.workout.WorkoutSetRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/**
 * connectedDebugAndroidTest for #23: opens the plate/warm-up calculator from a set row, enters a
 * target weight that isn't exactly reachable with the default plates, confirms both the plate
 * breakdown and the warm-up ramp render, then applies the closest reachable weight back onto
 * the set row.
 */
class PlateCalculatorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun calculator_showsPlateBreakdownAndWarmupRamp_andAppliesReachableWeightToSet() {
        val workoutRepository = GlobalContext.get().get<WorkoutRepository>()
        val workoutExerciseRepository = GlobalContext.get().get<WorkoutExerciseRepository>()
        val workoutSetRepository = GlobalContext.get().get<WorkoutSetRepository>()

        composeRule.onNodeWithTag("start_empty_workout").performClick()
        composeRule.onNodeWithTag("active_workout_screen").assertExists()
        val workoutId = runBlocking { workoutRepository.observeAll().first().first().id }

        composeRule.onNodeWithTag("add_exercise_button").performClick()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("Barbell Bench Press").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Barbell Bench Press").performClick()
        composeRule.onNodeWithTag("add_exercise_confirm_button").performClick()

        val workoutExerciseId = runBlocking {
            workoutExerciseRepository.observeByWorkoutId(workoutId).first { it.isNotEmpty() }.first().id
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("add_set_$workoutExerciseId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("add_set_$workoutExerciseId").performClick()
        val setId = runBlocking {
            workoutSetRepository.observeByWorkoutExerciseId(workoutExerciseId).first { it.isNotEmpty() }.first().id
        }

        composeRule.onNodeWithTag("calculator_$setId").performClick()
        composeRule.onNodeWithTag("plate_calculator_dialog").assertExists()

        composeRule.onNodeWithTag("plate_calculator_target").performTextInput("101")

        composeRule.onNodeWithText("Per side: 25 + 15").assertExists()
        composeRule.onNodeWithText(
            "Closest reachable: 100 kg (101 kg isn't reachable with these plates)",
        ).assertExists()

        composeRule.onNodeWithTag("plate_calculator_tab_warmup").performClick()
        composeRule.onNodeWithText("Set 1: 40 kg × 8").assertExists()
        composeRule.onNodeWithText("Set 2: 60 kg × 5").assertExists()
        composeRule.onNodeWithText("Set 3: 80 kg × 3").assertExists()

        composeRule.onNodeWithTag("plate_calculator_tab_plates").performClick()
        composeRule.onNodeWithTag("plate_calculator_apply").performClick()

        composeRule.onNodeWithTag("plate_calculator_dialog").assertDoesNotExist()
        composeRule.onNodeWithTag("set_weight_$setId").assertTextContains("100")
    }
}
