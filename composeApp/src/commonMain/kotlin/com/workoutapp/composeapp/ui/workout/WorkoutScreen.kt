package com.workoutapp.composeapp.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.workoutapp.composeapp.ui.designsystem.components.AppDialog
import com.workoutapp.composeapp.ui.designsystem.components.AppDropdownMenu
import com.workoutapp.composeapp.ui.designsystem.components.AppDropdownMenuItem
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
    onOpenRoutineBuilder: (Long) -> Unit = {},
    store: WorkoutStore = koinInject(),
) {
    val state by store.state.collectAsState()
    val spacing = LocalSpacing.current

    LaunchedEffect(store) {
        store.effects.collect { effect ->
            when (effect) {
                is WorkoutEffect.NavigateToActiveWorkout -> onWorkoutStarted(effect.workoutId)
                is WorkoutEffect.NavigateToRoutineBuilder -> onOpenRoutineBuilder(effect.routineId)
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
                .padding(horizontal = spacing.md, vertical = spacing.xs)
                .testTag("start_empty_workout"),
        )
        SecondaryButton(
            text = "+ New Routine",
            onClick = { store.onIntent(WorkoutIntent.CreateRoutine()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.xs)
                .testTag("create_routine_button"),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (!state.hasRoutines) {
                EmptyState(
                    title = "No routines yet",
                    message = "Build a routine to plan your sessions, or start an empty " +
                        "workout above to log freestyle.",
                    modifier = Modifier.align(Alignment.Center).testTag("no_routines_empty_state"),
                    action = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Or start from a template:", style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                                routineTemplateSuggestions.forEach { template ->
                                    SecondaryButton(
                                        text = template,
                                        onClick = { store.onIntent(WorkoutIntent.CreateRoutine(name = template)) },
                                        modifier = Modifier.testTag(
                                            "template_suggestion_${template.replace(" ", "_").lowercase()}",
                                        ),
                                    )
                                }
                            }
                        }
                    },
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
                            var menuExpanded by remember { mutableStateOf(false) }
                            AppListRow(
                                title = routine.name,
                                trailing = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SecondaryButton(
                                            text = "Start",
                                            onClick = {
                                                store.onIntent(WorkoutIntent.StartRoutine(routine.id))
                                            },
                                            modifier = Modifier.testTag("start_routine_${routine.id}"),
                                        )
                                        Box {
                                            IconButton(
                                                onClick = { menuExpanded = true },
                                                modifier = Modifier.testTag("routine_menu_${routine.id}"),
                                            ) { Text("•••") }
                                            AppDropdownMenu(
                                                expanded = menuExpanded,
                                                onDismissRequest = { menuExpanded = false },
                                            ) {
                                                AppDropdownMenuItem(
                                                    text = "Edit",
                                                    onClick = {
                                                        menuExpanded = false
                                                        store.onIntent(WorkoutIntent.EditRoutine(routine.id))
                                                    },
                                                    modifier = Modifier.testTag("edit_routine_${routine.id}"),
                                                )
                                                AppDropdownMenuItem(
                                                    text = "Duplicate",
                                                    onClick = {
                                                        menuExpanded = false
                                                        store.onIntent(WorkoutIntent.DuplicateRoutine(routine.id))
                                                    },
                                                    modifier = Modifier.testTag("duplicate_routine_${routine.id}"),
                                                )
                                                AppDropdownMenuItem(
                                                    text = "Delete",
                                                    onClick = {
                                                        menuExpanded = false
                                                        store.onIntent(WorkoutIntent.RequestDeleteRoutine(routine.id))
                                                    },
                                                    modifier = Modifier.testTag("delete_routine_${routine.id}"),
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("routine_row_${routine.id}"),
                            )
                        }
                    }
                }
            }
        }
    }

    val routineIdPendingDelete = state.routineIdPendingDelete
    if (routineIdPendingDelete != null) {
        AppDialog(
            title = "Delete routine?",
            message = "This removes the routine and its exercises. This can't be undone.",
            confirmText = "Delete",
            onConfirm = { store.onIntent(WorkoutIntent.ConfirmDeleteRoutine) },
            onDismiss = { store.onIntent(WorkoutIntent.CancelDeleteRoutine) },
            dismissText = "Cancel",
        )
    }
}
