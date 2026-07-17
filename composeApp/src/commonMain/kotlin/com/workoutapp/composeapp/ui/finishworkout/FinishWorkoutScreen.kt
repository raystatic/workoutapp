package com.workoutapp.composeapp.ui.finishworkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.window.Dialog
import com.workoutapp.composeapp.data.db.WorkoutPrivacy
import com.workoutapp.composeapp.ui.designsystem.components.AppCard
import com.workoutapp.composeapp.ui.designsystem.components.AppTextField
import com.workoutapp.composeapp.ui.designsystem.components.AppTopBar
import com.workoutapp.composeapp.ui.designsystem.components.EmptyState
import com.workoutapp.composeapp.ui.designsystem.components.PrimaryButton
import com.workoutapp.composeapp.ui.designsystem.components.SecondaryButton
import com.workoutapp.composeapp.ui.designsystem.theme.LocalSpacing
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * The finish/save screen for [workoutId]: an editable recap (name, duration, note, privacy,
 * photos) that saves the workout and computes PRs, then swaps in a post-save summary — workout
 * count, current streak, and any new records — before handing control back via [onDone].
 */
@Composable
fun FinishWorkoutScreen(
    workoutId: Long,
    onBack: () -> Unit = {},
    onDone: () -> Unit = {},
    onSaveAsRoutine: (Long) -> Unit = {},
    store: FinishWorkoutStore = koinInject { parametersOf(workoutId) },
) {
    val state by store.state.collectAsState()

    LaunchedEffect(store) {
        store.effects.collect { effect ->
            when (effect) {
                is FinishWorkoutEffect.NavigateToRoutineBuilder -> onSaveAsRoutine(effect.routineId)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().testTag("finish_workout_screen")) {
        AppTopBar(
            title = "Finish Workout",
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("finish_workout_back_button")) {
                    Text("←")
                }
            },
        )

        val summary = state.summary
        if (summary != null) {
            WorkoutSummaryContent(
                summary = summary,
                onSaveAsRoutine = { store.onIntent(FinishWorkoutIntent.SaveAsRoutine) },
                onDone = onDone,
            )
        } else {
            FinishWorkoutForm(state = state, onIntent = store::onIntent)
        }
    }
}

@Composable
private fun FinishWorkoutForm(state: FinishWorkoutState, onIntent: (FinishWorkoutIntent) -> Unit) {
    val spacing = LocalSpacing.current
    var showAddPhoto by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AppTextField(
            value = state.name,
            onValueChange = { onIntent(FinishWorkoutIntent.UpdateName(it)) },
            label = "Workout name",
            modifier = Modifier.fillMaxWidth().testTag("finish_workout_name"),
        )
        AppTextField(
            value = state.durationMinutes.toString(),
            onValueChange = { onIntent(FinishWorkoutIntent.UpdateDurationMinutes(it)) },
            label = "Duration (minutes)",
            modifier = Modifier.fillMaxWidth().testTag("finish_workout_duration"),
        )
        AppTextField(
            value = state.note,
            onValueChange = { onIntent(FinishWorkoutIntent.UpdateNote(it)) },
            label = "Note",
            singleLine = false,
            modifier = Modifier.fillMaxWidth().testTag("finish_workout_note"),
        )

        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text("Privacy", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                WorkoutPrivacy.entries.forEach { privacy ->
                    AssistChip(
                        onClick = { onIntent(FinishWorkoutIntent.UpdatePrivacy(privacy)) },
                        label = { Text(privacy.name) },
                        colors = if (privacy == state.privacy) {
                            AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            AssistChipDefaults.assistChipColors()
                        },
                        modifier = Modifier.testTag("finish_workout_privacy_${privacy.name}"),
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text("Photos", style = MaterialTheme.typography.titleSmall)
            state.media.forEach { uri ->
                Row(
                    modifier = Modifier.fillMaxWidth().testTag("finish_workout_photo_$uri"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(uri, style = MaterialTheme.typography.bodySmall)
                    IconButton(
                        onClick = { onIntent(FinishWorkoutIntent.RemovePhoto(uri)) },
                        modifier = Modifier.testTag("finish_workout_remove_photo_$uri"),
                    ) { Text("✕") }
                }
            }
            SecondaryButton(
                text = "+ Add Photo",
                onClick = { showAddPhoto = true },
                modifier = Modifier.testTag("finish_workout_add_photo_button"),
            )
        }

        PrimaryButton(
            text = "Save Workout",
            onClick = { onIntent(FinishWorkoutIntent.Save) },
            modifier = Modifier.fillMaxWidth().testTag("save_workout_button"),
        )
    }

    if (showAddPhoto) {
        AddPhotoDialog(
            onConfirm = { uri ->
                onIntent(FinishWorkoutIntent.AddPhoto(uri))
                showAddPhoto = false
            },
            onDismiss = { showAddPhoto = false },
        )
    }
}

@Composable
private fun AddPhotoDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val spacing = LocalSpacing.current
    var uri by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        AppCard(modifier = Modifier.testTag("add_photo_dialog")) {
            Text("Add Photo", style = MaterialTheme.typography.titleMedium)
            AppTextField(
                value = uri,
                onValueChange = { uri = it },
                label = "Photo URI",
                modifier = Modifier.fillMaxWidth().padding(top = spacing.sm).testTag("add_photo_uri_field"),
            )
            Row(modifier = Modifier.padding(top = spacing.sm), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                SecondaryButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.testTag("cancel_add_photo"))
                PrimaryButton(
                    text = "Add",
                    onClick = { if (uri.isNotBlank()) onConfirm(uri) },
                    modifier = Modifier.testTag("confirm_add_photo"),
                )
            }
        }
    }
}

@Composable
private fun WorkoutSummaryContent(summary: WorkoutSummary, onSaveAsRoutine: () -> Unit, onDone: () -> Unit) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier.fillMaxSize().padding(spacing.md).testTag("finish_workout_summary"),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text("Workout Saved!", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Workouts completed: ${summary.workoutCount}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("summary_workout_count"),
        )
        Text(
            text = "Current streak: ${summary.streak} day${if (summary.streak == 1L) "" else "s"}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("summary_streak"),
        )

        Text("Personal Records", style = MaterialTheme.typography.titleSmall)
        if (summary.personalRecords.isEmpty()) {
            EmptyState(
                title = "No new PRs this time",
                message = "Keep pushing — your next record is one set away.",
                modifier = Modifier.testTag("no_prs_empty_state"),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(summary.personalRecords, key = { it.exerciseName + it.type }) { hit ->
                    Text(
                        text = "${hit.exerciseName}: new ${formatPersonalRecordType(hit.type)} — ${formatNumber(hit.value)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("pr_hit_${hit.exerciseName}_${hit.type}"),
                    )
                }
            }
        }

        SecondaryButton(
            text = "Save as Routine",
            onClick = onSaveAsRoutine,
            modifier = Modifier.fillMaxWidth().testTag("save_as_routine_button"),
        )
        PrimaryButton(
            text = "Done",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().testTag("finish_summary_done_button"),
        )
    }
}

private fun formatPersonalRecordType(type: String): String = when (type) {
    "1RM" -> "estimated 1RM"
    "maxWeight" -> "max weight"
    "bestVolume" -> "best volume"
    else -> type
}

private fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
