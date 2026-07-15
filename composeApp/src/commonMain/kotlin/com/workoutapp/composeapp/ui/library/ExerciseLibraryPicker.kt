package com.workoutapp.composeapp.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.workoutapp.composeapp.db.Exercise
import com.workoutapp.composeapp.ui.designsystem.components.AppCard
import com.workoutapp.composeapp.ui.designsystem.components.AppDropdownMenu
import com.workoutapp.composeapp.ui.designsystem.components.AppDropdownMenuItem
import com.workoutapp.composeapp.ui.designsystem.components.AppListRow
import com.workoutapp.composeapp.ui.designsystem.components.AppTextField
import com.workoutapp.composeapp.ui.designsystem.components.EmptyState
import com.workoutapp.composeapp.ui.designsystem.components.PrimaryButton
import com.workoutapp.composeapp.ui.designsystem.components.SecondaryButton
import com.workoutapp.composeapp.ui.designsystem.theme.LocalSpacing

/**
 * Shared exercise picker used by both the active-workout screen and the routine builder's
 * "+ Add Exercise" flow (#20): searchable by name, filterable by equipment/muscle (combined
 * with AND), a "Recent" shortcut for quickly re-adding recently-logged exercises, and
 * multi-select — [onConfirm] fires once with every selected exercise id when the caller taps
 * "Add".
 */
@Composable
fun ExerciseLibraryPicker(
    exercises: List<Exercise>,
    onConfirm: (List<Long>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    recentExercises: List<Exercise> = emptyList(),
    testTagPrefix: String = "exercise_library",
) {
    var query by remember { mutableStateOf("") }
    var equipmentFilter by remember { mutableStateOf<String?>(null) }
    var muscleFilter by remember { mutableStateOf<String?>(null) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var equipmentMenuExpanded by remember { mutableStateOf(false) }
    var muscleMenuExpanded by remember { mutableStateOf(false) }

    val equipmentOptions = remember(exercises) { equipmentOptions(exercises) }
    val muscleOptions = remember(exercises) { muscleOptions(exercises) }
    val showRecent = recentExercises.isNotEmpty() && query.isBlank() && equipmentFilter == null && muscleFilter == null
    val filtered = remember(exercises, query, equipmentFilter, muscleFilter, showRecent, recentExercises) {
        val matches = filterExercises(exercises, query, equipmentFilter, muscleFilter)
        // Recent exercises are already surfaced as chips above; drop them here so a name
        // never appears twice in the dialog at once.
        if (showRecent) {
            val recentIds = recentExercises.mapTo(mutableSetOf()) { it.id }
            matches.filterNot { it.id in recentIds }
        } else {
            matches
        }
    }
    val spacing = LocalSpacing.current

    fun toggleSelection(exerciseId: Long) {
        selectedIds = if (exerciseId in selectedIds) selectedIds - exerciseId else selectedIds + exerciseId
    }

    Dialog(onDismissRequest = onDismiss) {
        AppCard(modifier = modifier.testTag("${testTagPrefix}_dialog")) {
            Text("Add Exercise", style = MaterialTheme.typography.titleMedium)

            AppTextField(
                value = query,
                onValueChange = { query = it },
                label = "Search exercises",
                modifier = Modifier.fillMaxWidth().padding(top = spacing.sm).testTag("${testTagPrefix}_search_field"),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Box {
                    AssistChip(
                        onClick = { equipmentMenuExpanded = true },
                        label = { Text("Equipment: ${equipmentFilter ?: "All"}") },
                        modifier = Modifier.testTag("${testTagPrefix}_equipment_filter"),
                    )
                    AppDropdownMenu(expanded = equipmentMenuExpanded, onDismissRequest = { equipmentMenuExpanded = false }) {
                        AppDropdownMenuItem(
                            text = "All",
                            onClick = { equipmentFilter = null; equipmentMenuExpanded = false },
                            modifier = Modifier.testTag("${testTagPrefix}_equipment_option_all"),
                        )
                        equipmentOptions.forEach { option ->
                            AppDropdownMenuItem(
                                text = option,
                                onClick = { equipmentFilter = option; equipmentMenuExpanded = false },
                                modifier = Modifier.testTag("${testTagPrefix}_equipment_option_$option"),
                            )
                        }
                    }
                }
                Box {
                    AssistChip(
                        onClick = { muscleMenuExpanded = true },
                        label = { Text("Muscle: ${muscleFilter ?: "All"}") },
                        modifier = Modifier.testTag("${testTagPrefix}_muscle_filter"),
                    )
                    AppDropdownMenu(expanded = muscleMenuExpanded, onDismissRequest = { muscleMenuExpanded = false }) {
                        AppDropdownMenuItem(
                            text = "All",
                            onClick = { muscleFilter = null; muscleMenuExpanded = false },
                            modifier = Modifier.testTag("${testTagPrefix}_muscle_option_all"),
                        )
                        muscleOptions.forEach { option ->
                            AppDropdownMenuItem(
                                text = option,
                                onClick = { muscleFilter = option; muscleMenuExpanded = false },
                                modifier = Modifier.testTag("${testTagPrefix}_muscle_option_$option"),
                            )
                        }
                    }
                }
            }

            if (showRecent) {
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = spacing.sm),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    recentExercises.forEach { exercise ->
                        AssistChip(
                            onClick = { toggleSelection(exercise.id) },
                            label = { Text(exercise.name) },
                            modifier = Modifier.testTag("${testTagPrefix}_recent_${exercise.id}"),
                        )
                    }
                }
            }

            Box(modifier = Modifier.heightIn(max = 360.dp).padding(top = spacing.xs)) {
                if (filtered.isEmpty()) {
                    EmptyState(
                        title = "No exercises found",
                        message = "Try a different search or filter.",
                        modifier = Modifier.testTag("${testTagPrefix}_empty_state"),
                    )
                } else {
                    LazyColumn {
                        items(filtered, key = { it.id }) { exercise ->
                            val isSelected = exercise.id in selectedIds
                            AppListRow(
                                title = exercise.name,
                                subtitle = exercise.primaryMuscle,
                                trailing = {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { toggleSelection(exercise.id) },
                                        modifier = Modifier.testTag("${testTagPrefix}_checkbox_${exercise.id}"),
                                    )
                                },
                                modifier = Modifier
                                    .testTag("${testTagPrefix}_option_${exercise.id}")
                                    .clickable { toggleSelection(exercise.id) },
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SecondaryButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.testTag("${testTagPrefix}_cancel_button"),
                )
                PrimaryButton(
                    text = "Add (${selectedIds.size})",
                    onClick = { onConfirm(selectedIds.toList()) },
                    enabled = selectedIds.isNotEmpty(),
                    modifier = Modifier.testTag("${testTagPrefix}_confirm_button"),
                )
            }
        }
    }
}
