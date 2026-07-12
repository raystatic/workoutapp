package com.workoutapp.composeapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Minimal app-wide constants. Placeholder for real app metadata as the
 * project grows.
 */
object AppInfo {
    const val name: String = "Workout App"
}

/**
 * The root composable for the app. This is a trivial "hello workout" screen
 * that proves the shared Compose Multiplatform UI wires up end to end across
 * Android and iOS. Real screens land in subsequent issues.
 */
@Composable
fun App() {
    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("Hello, ${AppInfo.name}!")
        }
    }
}
