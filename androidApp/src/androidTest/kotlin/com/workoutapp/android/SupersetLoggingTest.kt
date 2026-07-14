package com.workoutapp.android

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.workoutapp.composeapp.data.workout.WorkoutExerciseRepository
import com.workoutapp.composeapp.data.workout.WorkoutRepository
import com.workoutapp.composeapp.data.workout.WorkoutSetRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/**
 * connectedDebugAndroidTest for superset grouping (#15): pairs two adjacent
 * exercises into a superset via the "•••" menu, confirms they render
 * together with a shared group badge, then logs a set on each and confirms
 * "up next" alternates between them — surviving activity recreation.
 */
class SupersetLoggingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun groupingTwoExercises_rendersTogetherAndAlternatesLoggingFocus() {
        val workoutRepository = GlobalContext.get().get<WorkoutRepository>()
        val workoutExerciseRepository = GlobalContext.get().get<WorkoutExerciseRepository>()
        val workoutSetRepository = GlobalContext.get().get<WorkoutSetRepository>()

        composeRule.onNodeWithTag("start_empty_workout").performClick()
        composeRule.onNodeWithTag("active_workout_screen").assertExists()
        val workoutId = runBlocking { workoutRepository.observeAll().first().first().id }

        addExercise("Barbell Bench Press")
        addExercise("Barbell Deadlift")

        val workoutExerciseIds = runBlocking {
            workoutExerciseRepository.observeByWorkoutId(workoutId).first { it.size == 2 }
        }.sortedBy { it.position }.map { it.id }
        val (firstId, secondId) = workoutExerciseIds

        composeRule.onNodeWithTag("exercise_menu_$firstId").performClick()
        composeRule.onNodeWithTag("group_with_next_$firstId").performClick()

        runBlocking {
            workoutExerciseRepository.observeByWorkoutId(workoutId)
                .first { list -> list.all { it.supersetGroup != null } }
        }
        composeRule.onNodeWithTag("superset_badge_$firstId").assertExists()
        composeRule.onNodeWithTag("superset_badge_$secondId").assertExists()
        composeRule.onNodeWithTag("superset_up_next_$firstId").assertExists()

        composeRule.onNodeWithTag("add_set_$firstId").performClick()
        composeRule.onNodeWithTag("add_set_$secondId").performClick()
        val firstSetId = runBlocking {
            workoutSetRepository.observeByWorkoutExerciseId(firstId).first { it.isNotEmpty() }.first().id
        }
        val secondSetId = runBlocking {
            workoutSetRepository.observeByWorkoutExerciseId(secondId).first { it.isNotEmpty() }.first().id
        }

        composeRule.onNodeWithTag("set_complete_$firstSetId").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("superset_up_next_$secondId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("superset_up_next_$secondId").assertExists()

        composeRule.onNodeWithTag("set_complete_$secondSetId").performClick()

        composeRule.activityRule.scenario.recreate()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("exercise_row_$firstId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("superset_badge_$firstId").assertExists()
        composeRule.onNodeWithTag("superset_badge_$secondId").assertExists()
        composeRule.onNodeWithTag("set_complete_$firstSetId").assertIsOn()
        composeRule.onNodeWithTag("set_complete_$secondSetId").assertIsOn()
    }

    private fun addExercise(name: String) {
        composeRule.onNodeWithTag("add_exercise_button").performClick()
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(name).performClick()
    }
}
