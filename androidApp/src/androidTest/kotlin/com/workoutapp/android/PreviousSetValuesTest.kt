package com.workoutapp.android

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
 * connectedDebugAndroidTest for #14: logs a set for an exercise in one
 * workout, starts a second separate workout with the same exercise, and
 * confirms its new set row shows the first workout's values as "Previous".
 */
class PreviousSetValuesTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun secondWorkout_showsPreviousValuesFromFirstWorkout() {
        val workoutRepository = GlobalContext.get().get<WorkoutRepository>()
        val workoutExerciseRepository = GlobalContext.get().get<WorkoutExerciseRepository>()
        val workoutSetRepository = GlobalContext.get().get<WorkoutSetRepository>()

        composeRule.onNodeWithTag("start_empty_workout").performClick()
        composeRule.onNodeWithTag("active_workout_screen").assertExists()
        val firstWorkoutId = runBlocking { workoutRepository.observeAll().first().first().id }

        val firstSetId = logSet(firstWorkoutId, workoutExerciseRepository, workoutSetRepository, reps = "10", weight = "60")

        composeRule.onNodeWithTag("previous_set_$firstSetId").assertTextContains("No previous data")

        composeRule.onNodeWithTag("active_workout_back_button").performClick()
        composeRule.onNodeWithTag("start_empty_workout").performClick()
        composeRule.onNodeWithTag("active_workout_screen").assertExists()
        val secondWorkoutId = runBlocking {
            workoutRepository.observeAll().first { it.isNotEmpty() && it.first().id != firstWorkoutId }.first().id
        }

        val secondSetId = logSet(secondWorkoutId, workoutExerciseRepository, workoutSetRepository, reps = null, weight = null)

        composeRule.onNodeWithTag("previous_set_$secondSetId").assertTextContains("Previous: 60 × 10")
    }

    /** Adds "Barbell Bench Press" to [workoutId], logs one set, optionally filling reps/weight. */
    private fun logSet(
        workoutId: Long,
        workoutExerciseRepository: WorkoutExerciseRepository,
        workoutSetRepository: WorkoutSetRepository,
        reps: String?,
        weight: String?,
    ): Long {
        composeRule.onNodeWithTag("add_exercise_button").performClick()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("Barbell Bench Press").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Barbell Bench Press").performClick()

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

        if (reps != null) composeRule.onNodeWithTag("set_reps_$setId").performTextInput(reps)
        if (weight != null) composeRule.onNodeWithTag("set_weight_$setId").performTextInput(weight)

        return setId
    }
}
