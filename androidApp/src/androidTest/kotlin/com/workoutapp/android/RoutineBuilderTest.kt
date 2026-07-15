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
import androidx.compose.ui.test.performTextClearance
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
 * connectedDebugAndroidTest for the routine builder (#18): builds a routine with one
 * exercise and a target set via the "+ New Routine" flow, starts it from the Workout
 * tab, and confirms the active-workout screen is pre-filled with that exercise and set.
 * A second test exercises the routine list's "•••" duplicate/delete actions.
 */
class RoutineBuilderTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun buildingARoutineThenStartingIt_prefillsTheActiveWorkout() {
        composeRule.onNodeWithTag("create_routine_button").performClick()
        composeRule.onNodeWithTag("routine_builder_screen").assertExists()

        val routineRepository = GlobalContext.get().get<RoutineRepository>()
        val routineId = runBlocking { routineRepository.observeAll().first().maxBy { it.position }.id }
        val uniqueName = "Push Day ${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("routine_name_field").performTextClearance()
        composeRule.onNodeWithTag("routine_name_field").performTextInput(uniqueName)

        composeRule.onNodeWithTag("routine_add_exercise_button").performClick()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("Barbell Bench Press").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Barbell Bench Press").performClick()
        composeRule.onNodeWithTag("routine_add_exercise_confirm_button").performClick()

        val routineExerciseRepository = GlobalContext.get().get<RoutineExerciseRepository>()
        val routineExerciseId = runBlocking {
            routineExerciseRepository.observeByRoutineId(routineId).first { it.isNotEmpty() }.first().id
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("routine_add_set_$routineExerciseId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("routine_add_set_$routineExerciseId").performClick()

        val routineSetRepository = GlobalContext.get().get<RoutineSetRepository>()
        val routineSetId = runBlocking {
            routineSetRepository.observeByRoutineExerciseId(routineExerciseId).first { it.isNotEmpty() }.first().id
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("routine_set_reps_$routineSetId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("routine_set_reps_$routineSetId").performTextInput("10")
        composeRule.onNodeWithTag("routine_set_weight_$routineSetId").performTextInput("60")

        composeRule.onNodeWithTag("routine_builder_done_button").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("start_routine_$routineId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("start_routine_$routineId").performClick()
        composeRule.onNodeWithTag("active_workout_screen").assertExists()

        val workoutRepository = GlobalContext.get().get<WorkoutRepository>()
        val workoutId = runBlocking { workoutRepository.observeAll().first { it.isNotEmpty() }.first().id }
        val workoutExerciseRepository = GlobalContext.get().get<WorkoutExerciseRepository>()
        val workoutExerciseId = runBlocking {
            workoutExerciseRepository.observeByWorkoutId(workoutId).first { it.isNotEmpty() }.first().id
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("exercise_row_$workoutExerciseId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("exercise_name_$workoutExerciseId").assertTextContains("Barbell Bench Press")

        val workoutSetRepository = GlobalContext.get().get<WorkoutSetRepository>()
        val workoutSetId = runBlocking {
            workoutSetRepository.observeByWorkoutExerciseId(workoutExerciseId).first { it.isNotEmpty() }.first().id
        }
        composeRule.onNodeWithTag("set_reps_$workoutSetId").assertTextContains("10")
        composeRule.onNodeWithTag("set_weight_$workoutSetId").assertTextContains("60")
    }

    @Test
    fun duplicatingThenDeletingARoutine_updatesTheWorkoutTabList() {
        composeRule.onNodeWithTag("create_routine_button").performClick()
        composeRule.onNodeWithTag("routine_builder_screen").assertExists()

        val routineRepository = GlobalContext.get().get<RoutineRepository>()
        val routineId = runBlocking { routineRepository.observeAll().first().maxBy { it.position }.id }
        val uniqueName = "Leg Day ${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("routine_name_field").performTextClearance()
        composeRule.onNodeWithTag("routine_name_field").performTextInput(uniqueName)
        composeRule.onNodeWithTag("routine_builder_done_button").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("routine_menu_$routineId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("routine_menu_$routineId").performClick()
        composeRule.onNodeWithTag("duplicate_routine_$routineId").performClick()

        val duplicateId = runBlocking {
            routineRepository.observeAll().first { routines -> routines.any { it.name == "$uniqueName copy" } }
                .first { it.name == "$uniqueName copy" }
                .id
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("routine_row_$duplicateId").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("routine_menu_$duplicateId").performClick()
        composeRule.onNodeWithTag("delete_routine_$duplicateId").performClick()
        composeRule.onNodeWithText("Delete").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("routine_row_$duplicateId").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("routine_row_$duplicateId").assertDoesNotExist()
    }
}
