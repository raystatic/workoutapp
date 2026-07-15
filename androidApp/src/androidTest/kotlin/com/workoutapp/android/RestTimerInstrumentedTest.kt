package com.workoutapp.android

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.workoutapp.composeapp.data.workout.WorkoutExerciseRepository
import com.workoutapp.composeapp.data.workout.WorkoutRepository
import com.workoutapp.composeapp.data.workout.WorkoutSetRepository
import com.workoutapp.composeapp.ui.resttimer.RestTimerState
import com.workoutapp.composeapp.ui.resttimer.RestTimerStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/**
 * connectedDebugAndroidTest for the rest timer (#16): completing a set
 * auto-starts the timer (which schedules the platform notification as part
 * of [RestTimerStore.start]), the countdown banner reflects that state, and
 * skipping returns everything to idle.
 */
class RestTimerInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun completingASet_startsTheRestTimerAndSkipDismissesIt() {
        val workoutRepository = GlobalContext.get().get<WorkoutRepository>()
        val workoutExerciseRepository = GlobalContext.get().get<WorkoutExerciseRepository>()
        val workoutSetRepository = GlobalContext.get().get<WorkoutSetRepository>()
        val restTimerStore = GlobalContext.get().get<RestTimerStore>()

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
            composeRule.onNodeWithTag("add_set_$workoutExerciseId").fetchSemanticsNode() != null
        }
        composeRule.onNodeWithTag("add_set_$workoutExerciseId").performClick()
        val setId = runBlocking {
            workoutSetRepository.observeByWorkoutExerciseId(workoutExerciseId).first { it.isNotEmpty() }.first().id
        }

        // Set a low per-exercise override so the assertion below doesn't depend on the mutable global default.
        composeRule.onNodeWithTag("rest_override_chip_$workoutExerciseId").performClick()
        composeRule.onNodeWithTag("rest_override_chip_$workoutExerciseId").assertTextEquals("Rest: 30s")

        composeRule.onNodeWithTag("set_complete_$setId").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            (restTimerStore.state.value as? RestTimerState.Running)?.exerciseId != null
        }
        composeRule.onNodeWithTag("rest_timer_banner").assertExists()
        composeRule.onNodeWithTag("rest_timer_remaining").assertTextEquals("Rest: 00:30")

        composeRule.onNodeWithTag("rest_timer_skip").performClick()

        composeRule.onNodeWithTag("rest_timer_banner").assertDoesNotExist()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            restTimerStore.state.value == RestTimerState.Idle
        }
    }
}
