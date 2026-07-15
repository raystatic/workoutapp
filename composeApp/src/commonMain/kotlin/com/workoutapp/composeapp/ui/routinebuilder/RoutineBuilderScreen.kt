package com.workoutapp.composeapp.ui.routinebuilder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.workoutapp.composeapp.ui.designsystem.components.AppCard
import com.workoutapp.composeapp.ui.designsystem.components.AppDropdownMenu
import com.workoutapp.composeapp.ui.designsystem.components.AppDropdownMenuItem
import com.workoutapp.composeapp.ui.designsystem.components.AppNumberField
import com.workoutapp.composeapp.ui.designsystem.components.AppTextField
import com.workoutapp.composeapp.ui.designsystem.components.AppTopBar
import com.workoutapp.composeapp.ui.designsystem.components.EmptyState
import com.workoutapp.composeapp.ui.designsystem.components.PrimaryButton
import com.workoutapp.composeapp.ui.designsystem.components.SecondaryButton
import com.workoutapp.composeapp.ui.designsystem.components.SetTypeIndicator
import com.workoutapp.composeapp.ui.designsystem.theme.LocalSpacing
import com.workoutapp.composeapp.ui.library.ExerciseLibraryPicker
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.workoutapp.composeapp.data.db.SetType as DbSetType
import com.workoutapp.composeapp.ui.designsystem.components.SetType as UiSetType

/**
 * The create/edit routine screen: name, optional folder, and a list of
 * exercises with their target sets (weight/reps, set type), per-exercise
 * notes and rest override, reordering, and superset grouping — mirroring the
 * active-workout screen's editing UX. Every edit writes straight to
 * SQLDelight, so "Done" simply navigates back.
 */
@Composable
fun RoutineBuilderScreen(
    routineId: Long,
    onDone: () -> Unit = {},
    onOpenExerciseDetail: (Long) -> Unit = {},
    store: RoutineBuilderStore = koinInject { parametersOf(routineId) },
) {
    val state by store.state.collectAsState()
    val spacing = LocalSpacing.current

    Column(modifier = Modifier.fillMaxSize().testTag("routine_builder_screen")) {
        AppTopBar(
            title = "Routine Builder",
            navigationIcon = {
                IconButton(onClick = onDone, modifier = Modifier.testTag("routine_builder_done_button")) {
                    Text("←")
                }
            },
        )

        AppTextField(
            value = state.name,
            onValueChange = { store.onIntent(RoutineBuilderIntent.NameChanged(it)) },
            label = "Routine name",
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.xs).testTag("routine_name_field"),
        )
        AppNumberField(
            value = state.folderIdText,
            onValueChange = { store.onIntent(RoutineBuilderIntent.FolderIdChanged(it)) },
            label = "Folder # (optional)",
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.xs).testTag("routine_folder_field"),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (state.exercises.isEmpty()) {
                EmptyState(
                    title = "No exercises yet",
                    message = "Add an exercise below to build this routine.",
                    modifier = Modifier.align(Alignment.Center).testTag("no_routine_exercises_empty_state"),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                itemsIndexed(state.exercises, key = { _, item -> item.routineExerciseId }) { index, exerciseUi ->
                    RoutineExerciseCard(
                        exercise = exerciseUi,
                        isFirst = index == 0,
                        isLast = index == state.exercises.lastIndex,
                        canGroupWithNext = index != state.exercises.lastIndex,
                        onIntent = store::onIntent,
                    )
                }
                item {
                    PrimaryButton(
                        text = "+ Add Exercise",
                        onClick = { store.onIntent(RoutineBuilderIntent.ShowAddExercise) },
                        modifier = Modifier.fillMaxWidth()
                            .padding(vertical = spacing.md)
                            .testTag("routine_add_exercise_button"),
                    )
                }
            }
        }
    }

    if (state.showAddExercise) {
        ExerciseLibraryPicker(
            exercises = state.availableExercises,
            recentExercises = state.recentExercises,
            onConfirm = { store.onIntent(RoutineBuilderIntent.AddExercises(it)) },
            onDismiss = { store.onIntent(RoutineBuilderIntent.HideAddExercise) },
            testTagPrefix = "routine_add_exercise",
            onOpenDetail = onOpenExerciseDetail,
        )
    }
}

@Composable
private fun RoutineExerciseCard(
    exercise: RoutineBuilderExerciseUi,
    isFirst: Boolean,
    isLast: Boolean,
    canGroupWithNext: Boolean,
    onIntent: (RoutineBuilderIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    var menuExpanded by remember { mutableStateOf(false) }
    val isGrouped = exercise.supersetGroup != null
    val cardModifier = modifier.fillMaxWidth().testTag("routine_exercise_row_${exercise.routineExerciseId}").let {
        if (isGrouped) {
            it.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(12.dp))
        } else {
            it
        }
    }
    AppCard(modifier = cardModifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                if (exercise.supersetLabel != null) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Superset ${exercise.supersetLabel}") },
                        modifier = Modifier.testTag("routine_superset_badge_${exercise.routineExerciseId}"),
                    )
                }
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("routine_exercise_name_${exercise.routineExerciseId}"),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onIntent(RoutineBuilderIntent.MoveExerciseUp(exercise.routineExerciseId)) },
                    enabled = !isFirst,
                    modifier = Modifier.testTag("routine_move_exercise_up_${exercise.routineExerciseId}"),
                ) { Text("▲") }
                IconButton(
                    onClick = { onIntent(RoutineBuilderIntent.MoveExerciseDown(exercise.routineExerciseId)) },
                    enabled = !isLast,
                    modifier = Modifier.testTag("routine_move_exercise_down_${exercise.routineExerciseId}"),
                ) { Text("▼") }
                IconButton(
                    onClick = { onIntent(RoutineBuilderIntent.RemoveExercise(exercise.routineExerciseId)) },
                    modifier = Modifier.testTag("routine_remove_exercise_${exercise.routineExerciseId}"),
                ) { Text("✕") }
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("routine_exercise_menu_${exercise.routineExerciseId}"),
                    ) { Text("•••") }
                    AppDropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        if (isGrouped) {
                            AppDropdownMenuItem(
                                text = "Remove from superset",
                                onClick = {
                                    menuExpanded = false
                                    onIntent(RoutineBuilderIntent.RemoveFromSuperset(exercise.routineExerciseId))
                                },
                                modifier = Modifier.testTag("routine_remove_from_superset_${exercise.routineExerciseId}"),
                            )
                        } else if (canGroupWithNext) {
                            AppDropdownMenuItem(
                                text = "Group with next exercise",
                                onClick = {
                                    menuExpanded = false
                                    onIntent(RoutineBuilderIntent.GroupWithNextExercise(exercise.routineExerciseId))
                                },
                                modifier = Modifier.testTag("routine_group_with_next_${exercise.routineExerciseId}"),
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            AppTextField(
                value = exercise.notes,
                onValueChange = { onIntent(RoutineBuilderIntent.UpdateExerciseNotes(exercise.routineExerciseId, it)) },
                label = "Notes",
                modifier = Modifier.weight(1f).testTag("routine_exercise_notes_${exercise.routineExerciseId}"),
            )
            AppNumberField(
                value = exercise.restSeconds?.toString().orEmpty(),
                onValueChange = { onIntent(RoutineBuilderIntent.UpdateRestSeconds(exercise.routineExerciseId, it)) },
                label = "Rest (sec)",
                modifier = Modifier.width(110.dp).testTag("routine_rest_seconds_${exercise.routineExerciseId}"),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            exercise.sets.forEach { setUi ->
                RoutineSetRow(setUi = setUi, onIntent = onIntent)
            }
        }

        SecondaryButton(
            text = "+ Add Set",
            onClick = { onIntent(RoutineBuilderIntent.AddSet(exercise.routineExerciseId)) },
            modifier = Modifier.padding(top = spacing.sm).testTag("routine_add_set_${exercise.routineExerciseId}"),
        )
    }
}

@Composable
private fun RoutineSetRow(setUi: RoutineBuilderSetUi, onIntent: (RoutineBuilderIntent) -> Unit, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    val set = setUi.set
    Row(
        modifier = modifier.fillMaxWidth().testTag("routine_set_row_${set.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        SetTypeIndicator(
            type = set.setType.toUi(),
            modifier = Modifier
                .testTag("routine_set_type_${set.id}")
                .clickable { onIntent(RoutineBuilderIntent.CycleSetType(set.id)) },
        )
        AppNumberField(
            value = setUi.targetWeightText,
            onValueChange = { onIntent(RoutineBuilderIntent.UpdateTargetWeight(set.id, it)) },
            label = "Weight",
            modifier = Modifier.width(90.dp).testTag("routine_set_weight_${set.id}"),
        )
        AppNumberField(
            value = setUi.targetRepsText,
            onValueChange = { onIntent(RoutineBuilderIntent.UpdateTargetReps(set.id, it)) },
            label = "Reps",
            modifier = Modifier.width(80.dp).testTag("routine_set_reps_${set.id}"),
        )
        IconButton(
            onClick = { onIntent(RoutineBuilderIntent.RemoveSet(set.id)) },
            modifier = Modifier.testTag("routine_remove_set_${set.id}"),
        ) { Text("✕") }
    }
}

private fun DbSetType.toUi(): UiSetType = UiSetType.valueOf(name)
