package com.workoutapp.android

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.workoutapp.composeapp.data.analytics.AnalyticsSink
import com.workoutapp.composeapp.data.workout.WorkoutExerciseRepository
import com.workoutapp.composeapp.data.workout.WorkoutRepository
import com.workoutapp.composeapp.data.workout.WorkoutSetRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/**
 * connectedDebugAndroidTest for #25: starting, logging a set in, and finishing a workout emits
 * the expected analytics event sequence — asserted against the [FakeAnalyticsSink] installed by
 * [WorkoutInstrumentationTestRunner] in place of the real sink.
 */
class AnalyticsInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun completingAWorkout_emitsTheExpectedAnalyticsEventSequence() {
        val workoutRepository = GlobalContext.get().get<WorkoutRepository>()
        val workoutExerciseRepository = GlobalContext.get().get<WorkoutExerciseRepository>()
        val workoutSetRepository = GlobalContext.get().get<WorkoutSetRepository>()
        val analyticsSink = GlobalContext.get().get<AnalyticsSink>() as FakeAnalyticsSink
        val eventsBefore = analyticsSink.eventsSnapshot().size

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

        composeRule.waitUntil(timeoutMillis = 10_000) {
            analyticsSink.eventsSnapshot().drop(eventsBefore).any { it.first == "rest_timer_used" }
        }

        composeRule.onNodeWithTag("finish_workout_button").performClick()
        composeRule.onNodeWithTag("finish_workout_screen").assertExists()
        composeRule.onNodeWithTag("save_workout_button").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            analyticsSink.eventsSnapshot().drop(eventsBefore).any { it.first == "log_duration" }
        }

        val eventNames = analyticsSink.eventsSnapshot().drop(eventsBefore).map { it.first }
        assert(eventNames == listOf("workout_started", "rest_timer_used", "workout_completed", "log_duration")) {
            "Expected [workout_started, rest_timer_used, workout_completed, log_duration] but got $eventNames"
        }
    }
}
