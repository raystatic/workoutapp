package com.workoutapp.android

import androidx.compose.ui.test.assertExists
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
import com.workoutapp.composeapp.data.workout.WorkoutSetRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/**
 * connectedDebugAndroidTest for the exercise detail screen (#21): reachable from the exercise
 * library picker and from tapping an exercise's name mid-workout, and its "recent sets" history
 * reflects a set actually logged in this session.
 */
class ExerciseDetailNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun openingDetailFromTheLibraryPicker_showsTheExercisesOwnInfo() {
        val exerciseRepository = GlobalContext.get().get<ExerciseRepository>()

        composeRule.onNodeWithTag("start_empty_workout").performClick()
        composeRule.onNodeWithTag("active_workout_screen").assertExists()

        composeRule.onNodeWithTag("add_exercise_button").performClick()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("Barbell Bench Press").fetchSemanticsNodes().isNotEmpty()
        }
        val exerciseId = runBlocking {
            exerciseRepository.observeAll().first().first { it.name == "Barbell Bench Press" }.id
        }

        composeRule.onNodeWithTag("add_exercise_info_$exerciseId").performClick()

        composeRule.onNodeWithTag("exercise_detail_screen").assertExists()
        composeRule.onNodeWithText("Barbell Bench Press").assertExists()
        composeRule.onNodeWithTag("exercise_detail_primary_muscle").assertExists()
    }

    @Test
    fun tappingAnExerciseNameInActiveWorkout_opensItsDetailScreen() {
        val workoutRepository = GlobalContext.get().get<WorkoutRepository>()
        val workoutExerciseRepository = GlobalContext.get().get<WorkoutExerciseRepository>()

        val workoutExerciseId = addBenchPressToANewWorkout(workoutRepository, workoutExerciseRepository)

        composeRule.onNodeWithTag("exercise_name_$workoutExerciseId").performClick()

        composeRule.onNodeWithTag("exercise_detail_screen").assertExists()
        composeRule.onNodeWithText("Barbell Bench Press").assertExists()

        composeRule.onNodeWithTag("exercise_detail_back_button").performClick()
        composeRule.onNodeWithTag("active_workout_screen").assertExists()
    }

    @Test
    fun completingASet_thenOpeningDetail_showsItInRecentHistory() {
        val workoutRepository = GlobalContext.get().get<WorkoutRepository>()
        val workoutExerciseRepository = GlobalContext.get().get<WorkoutExerciseRepository>()
        val workoutSetRepository = GlobalContext.get().get<WorkoutSetRepository>()

        val workoutExerciseId = addBenchPressToANewWorkout(workoutRepository, workoutExerciseRepository)

        composeRule.onNodeWithTag("add_set_$workoutExerciseId").performClick()
        val setId = runBlocking {
            workoutSetRepository.observeByWorkoutExerciseId(workoutExerciseId).first { it.isNotEmpty() }.first().id
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("set_reps_$setId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("set_reps_$setId").performTextInput("10")
        composeRule.onNodeWithTag("set_weight_$setId").performTextInput("60")
        composeRule.onNodeWithTag("set_complete_$setId").performClick()
        runBlocking {
            workoutSetRepository.observeByWorkoutExerciseId(workoutExerciseId).first { it.first().completed }
        }

        composeRule.onNodeWithTag("exercise_name_$workoutExerciseId").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("exercise_detail_history_set_$setId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("exercise_detail_history_set_$setId").assertExists()
    }

    private fun addBenchPressToANewWorkout(
        workoutRepository: WorkoutRepository,
        workoutExerciseRepository: WorkoutExerciseRepository,
    ): Long {
        composeRule.onNodeWithTag("start_empty_workout").performClick()
        composeRule.onNodeWithTag("active_workout_screen").assertExists()
        val workoutId = runBlocking { workoutRepository.observeAll().first().first().id }

        composeRule.onNodeWithTag("add_exercise_button").performClick()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText("Barbell Bench Press").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Barbell Bench Press").performClick()
        composeRule.onNodeWithTag("add_exercise_confirm_button").performClick()

        return runBlocking {
            workoutExerciseRepository.observeByWorkoutId(workoutId).first { it.isNotEmpty() }.first().id
        }
    }
}
