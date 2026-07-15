package com.workoutapp.android

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsOn
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
 * connectedDebugAndroidTest for the active-workout screen (#13): adds an
 * exercise from the seeded library, logs 3 sets, marks one complete, and
 * confirms every edit is already durable in SQLDelight by recreating the
 * activity and re-reading the same values back from the UI.
 */
class ActiveWorkoutLoggingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loggingThreeSetsAndMarkingOneComplete_persistsAcrossRecreation() {
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

        repeat(3) { index ->
            composeRule.onNodeWithTag("add_set_$workoutExerciseId").performClick()
            runBlocking {
                workoutSetRepository.observeByWorkoutExerciseId(workoutExerciseId).first { it.size == index + 1 }
            }
        }
        val setIds = runBlocking {
            workoutSetRepository.observeByWorkoutExerciseId(workoutExerciseId).first { it.size == 3 }.map { it.id }
        }
        val firstSetId = setIds.first()

        composeRule.onNodeWithTag("set_reps_$firstSetId").performTextInput("10")
        composeRule.onNodeWithTag("set_weight_$firstSetId").performTextInput("60")
        composeRule.onNodeWithTag("set_complete_$firstSetId").performClick()

        composeRule.activityRule.scenario.recreate()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("set_row_$firstSetId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("set_reps_$firstSetId").assertTextContains("10")
        composeRule.onNodeWithTag("set_weight_$firstSetId").assertTextContains("60")
        composeRule.onNodeWithTag("set_complete_$firstSetId").assertIsOn()
        setIds.forEach { id -> composeRule.onNodeWithTag("set_row_$id").assertExists() }
    }
}
