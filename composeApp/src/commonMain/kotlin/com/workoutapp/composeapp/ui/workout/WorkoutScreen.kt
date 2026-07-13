package com.workoutapp.composeapp.ui.workout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.workoutapp.composeapp.ui.designsystem.components.AppListRow
import com.workoutapp.composeapp.ui.designsystem.components.AppTopBar
import com.workoutapp.composeapp.ui.designsystem.components.EmptyState
import com.workoutapp.composeapp.ui.designsystem.components.PrimaryButton
import com.workoutapp.composeapp.ui.designsystem.components.SecondaryButton
import com.workoutapp.composeapp.ui.designsystem.theme.LocalSpacing
import org.koin.compose.koinInject

@Composable
fun WorkoutScreen(
    onWorkoutStarted: (Long) -> Unit = {},
    store: WorkoutStore = koinInject(),
) {
    val state by store.state.collectAsState()
    val spacing = LocalSpacing.current

    LaunchedEffect(store) {
        store.effects.collect { effect ->
            when (effect) {
                is WorkoutEffect.NavigateToActiveWorkout -> onWorkoutStarted(effect.workoutId)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Workout")

        PrimaryButton(
            text = "Start Empty Workout",
            onClick = { store.onIntent(WorkoutIntent.StartEmptyWorkout) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
                .testTag("start_empty_workout"),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (!state.hasRoutines) {
                EmptyState(
                    title = "No routines yet",
                    message = "Build a routine to plan your sessions, or start an empty " +
                        "workout above to log freestyle.",
                    modifier = Modifier.align(Alignment.Center).testTag("no_routines_empty_state"),
                )
            } else {
                val showFolderHeaders = state.folders.size > 1 || state.folders.single().folderId != null
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    state.folders.forEach { folder ->
                        if (showFolderHeaders) {
                            item(key = "header_${folder.folderId ?: "unfiled"}") {
                                Text(
                                    text = folder.folderId?.let { "Folder #$it" } ?: "Unfiled",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(
                                        horizontal = spacing.md,
                                        vertical = spacing.sm,
                                    ),
                                )
                            }
                        }
                        items(folder.routines, key = { it.id }) { routine ->
                            AppListRow(
                                title = routine.name,
                                trailing = {
                                    SecondaryButton(
                                        text = "Start",
                                        onClick = {
                                            store.onIntent(WorkoutIntent.StartRoutine(routine.id))
                                        },
                                        modifier = Modifier.testTag("start_routine_${routine.id}"),
                                    )
                                },
                                modifier = Modifier.testTag("routine_row_${routine.id}"),
                            )
                        }
                    }
                }
            }
        }
    }
}
