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
import com.workoutapp.composeapp.data.routines.RoutineExerciseRepository
import com.workoutapp.composeapp.data.routines.RoutineRepository
import com.workoutapp.composeapp.data.routines.RoutineSetRepository
import com.workoutapp.composeapp.data.workout.WorkoutExerciseRepository
import com.workoutapp.composeapp.data.workout.WorkoutRepository
import com.workoutapp.composeapp.data.workout.WorkoutSetRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/**
 * connectedDebugAndroidTest for the finish/save workout flow (#17): logs a completed set, taps
 * Finish, saves, and confirms the post-save summary shows the right workout count and a new PR,
 * with the edits durable in SQLDelight afterward.
 */
class FinishWorkoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun finishingAWorkoutWithACompletedSet_showsSummaryAndPersistsTheWorkout() {
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
        composeRule.onNodeWithTag("set_reps_$setId").performTextInput("10")
        composeRule.onNodeWithTag("set_weight_$setId").performTextInput("60")
        composeRule.onNodeWithTag("set_complete_$setId").performClick()

        composeRule.onNodeWithTag("finish_workout_button").performClick()
        composeRule.onNodeWithTag("finish_workout_screen").assertExists()

        composeRule.onNodeWithTag("save_workout_button").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("finish_workout_summary").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("summary_workout_count").assertTextContains("1", substring = true)
        composeRule.onNodeWithTag("pr_hit_Barbell Bench Press_maxWeight").assertExists()

        val savedWorkout = runBlocking { workoutRepository.getById(workoutId) }
        assert(savedWorkout?.finishedAt != null) { "Expected finishedAt to be set after saving" }

        composeRule.onNodeWithTag("finish_summary_done_button").performClick()
        composeRule.onNodeWithTag("start_empty_workout").assertExists()
    }

    @Test
    fun savingAWorkoutAsARoutine_prefillsTheRoutineBuilderWithItsExercisesAndSets() {
        val workoutRepository = GlobalContext.get().get<WorkoutRepository>()
        val workoutExerciseRepository = GlobalContext.get().get<WorkoutExerciseRepository>()
        val workoutSetRepository = GlobalContext.get().get<WorkoutSetRepository>()
        val routineRepository = GlobalContext.get().get<RoutineRepository>()
        val routineExerciseRepository = GlobalContext.get().get<RoutineExerciseRepository>()
        val routineSetRepository = GlobalContext.get().get<RoutineSetRepository>()

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
        composeRule.onNodeWithTag("set_reps_$setId").performTextInput("10")
        composeRule.onNodeWithTag("set_weight_$setId").performTextInput("60")
        composeRule.onNodeWithTag("set_complete_$setId").performClick()

        composeRule.onNodeWithTag("finish_workout_button").performClick()
        composeRule.onNodeWithTag("save_workout_button").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("finish_workout_summary").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("save_as_routine_button").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("routine_builder_screen").fetchSemanticsNodes().isNotEmpty()
        }

        val routineId = runBlocking {
            routineRepository.observeAll().first { routines -> routines.any { it.name == "Empty Workout" } }
                .first { it.name == "Empty Workout" }
                .id
        }
        val routineExerciseId = runBlocking {
            routineExerciseRepository.observeByRoutineId(routineId).first { it.isNotEmpty() }.first().id
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("routine_exercise_name_$routineExerciseId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("routine_exercise_name_$routineExerciseId").assertTextContains("Barbell Bench Press")

        val routineSetId = runBlocking {
            routineSetRepository.observeByRoutineExerciseId(routineExerciseId).first { it.isNotEmpty() }.first().id
        }
        composeRule.onNodeWithTag("routine_set_reps_$routineSetId").assertTextContains("10")
        composeRule.onNodeWithTag("routine_set_weight_$routineSetId").assertTextContains("60")
    }
}
