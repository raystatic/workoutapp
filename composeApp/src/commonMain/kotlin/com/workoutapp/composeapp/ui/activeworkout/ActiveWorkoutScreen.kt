package com.workoutapp.composeapp.ui.activeworkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.workoutapp.composeapp.ui.designsystem.theme.LocalSpacing

/**
 * Placeholder navigation target for a freshly started workout. The real
 * set-logging screen (running timer, set rows, add exercise, ...) lands in a
 * later issue; this proves "Start Empty Workout" / "Start Routine" hand off
 * to a real workout record.
 */
@Composable
fun ActiveWorkoutScreen(workoutId: Long) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier.fillMaxSize().padding(spacing.md).testTag("active_workout_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Workout in progress", style = MaterialTheme.typography.headlineSmall)
        Text("Workout #$workoutId", style = MaterialTheme.typography.bodyMedium)
    }
}
