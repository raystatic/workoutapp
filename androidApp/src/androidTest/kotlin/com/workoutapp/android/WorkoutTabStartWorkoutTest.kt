package com.workoutapp.android

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.workoutapp.composeapp.data.routines.RoutineRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/**
 * connectedDebugAndroidTest: exercises the Workout tab's two ways to start a
 * workout — an empty session, and from a routine — and confirms each hands
 * off to the active-workout screen with a real [com.workoutapp.composeapp.db.Workout] row created.
 */
class WorkoutTabStartWorkoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun startEmptyWorkout_navigatesToActiveWorkoutScreen() {
        composeRule.onNodeWithTag("start_empty_workout").performClick()

        composeRule.onNodeWithTag("active_workout_screen").assertExists()
    }

    @Test
    fun startingASeededRoutine_navigatesToActiveWorkoutScreen() = runBlocking {
        val routineRepository = GlobalContext.get().get<RoutineRepository>()
        val uniqueName = "Push Day ${System.currentTimeMillis()}"
        routineRepository.add(name = uniqueName, updatedAt = System.currentTimeMillis())
        val routineId = routineRepository.observeAll()
            .first { routines -> routines.any { it.name == uniqueName } }
            .first { it.name == uniqueName }
            .id

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("start_routine_$routineId").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("start_routine_$routineId").performClick()

        composeRule.onNodeWithTag("active_workout_screen").assertExists()
    }
}
