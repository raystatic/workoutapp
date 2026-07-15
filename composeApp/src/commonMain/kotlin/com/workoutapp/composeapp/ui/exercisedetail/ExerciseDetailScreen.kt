package com.workoutapp.composeapp.ui.exercisedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.workoutapp.composeapp.db.Exercise
import com.workoutapp.composeapp.ui.designsystem.components.AppTopBar
import com.workoutapp.composeapp.ui.designsystem.components.EmptyState
import com.workoutapp.composeapp.ui.designsystem.components.ExerciseMedia
import com.workoutapp.composeapp.ui.designsystem.components.SimpleLineChart
import com.workoutapp.composeapp.ui.designsystem.theme.LocalSpacing
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * The exercise detail screen for [exerciseId]: media (with a placeholder fallback), muscles,
 * equipment, instructions, and a basic per-exercise history — the most recent completed sets and
 * a simple best-weight-over-time line. Reached from the exercise library picker and from tapping
 * an exercise's name mid-workout.
 */
@Composable
fun ExerciseDetailScreen(
    exerciseId: Long,
    onBack: () -> Unit = {},
    store: ExerciseDetailStore = koinInject { parametersOf(exerciseId) },
) {
    val state by store.state.collectAsState()
    val exercise = state.exercise

    Column(modifier = Modifier.fillMaxSize().testTag("exercise_detail_screen")) {
        AppTopBar(
            title = exercise?.name ?: "Exercise",
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("exercise_detail_back_button")) {
                    Text("←")
                }
            },
        )

        if (exercise != null) {
            ExerciseDetailContent(exercise = exercise, state = state)
        } else if (!state.isLoading) {
            EmptyState(
                title = "Exercise not found",
                message = "This exercise may have been deleted.",
                modifier = Modifier.testTag("exercise_detail_not_found"),
            )
        }
    }
}

@Composable
private fun ExerciseDetailContent(exercise: Exercise, state: ExerciseDetailState) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        ExerciseMedia(
            mediaUrl = exercise.mediaUrl,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).testTag("exercise_detail_media"),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            AssistChip(
                onClick = {},
                label = { Text(exercise.primaryMuscle) },
                modifier = Modifier.testTag("exercise_detail_primary_muscle"),
            )
            exercise.secondaryMuscles.forEach { muscle ->
                AssistChip(
                    onClick = {},
                    label = { Text(muscle) },
                    modifier = Modifier.testTag("exercise_detail_secondary_muscle_$muscle"),
                )
            }
            AssistChip(
                onClick = {},
                label = { Text(exercise.equipment) },
                modifier = Modifier.testTag("exercise_detail_equipment"),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text("Instructions", style = MaterialTheme.typography.titleSmall)
            Text(
                text = exercise.instructions?.takeIf { it.isNotBlank() } ?: "No instructions yet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("exercise_detail_instructions"),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text("Best Weight", style = MaterialTheme.typography.titleSmall)
            if (state.bestWeightSeries.size < 2) {
                EmptyState(
                    title = "Not enough history yet",
                    message = "Log this exercise across a couple of workouts to see a trend.",
                    modifier = Modifier.testTag("exercise_detail_best_weight_empty"),
                )
            } else {
                SimpleLineChart(
                    values = state.bestWeightSeries.map { it.weight.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(120.dp).testTag("exercise_detail_best_weight_chart"),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text("Recent Sets", style = MaterialTheme.typography.titleSmall)
            if (state.recentSets.isEmpty()) {
                EmptyState(
                    title = "No history yet",
                    message = "Completed sets for this exercise will show up here.",
                    modifier = Modifier.testTag("exercise_detail_no_history"),
                )
            } else {
                state.recentSets.forEach { entry ->
                    Text(
                        text = formatHistoryEntry(entry.set.weight, entry.set.reps),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("exercise_detail_history_set_${entry.set.id}"),
                    )
                }
            }
        }
    }
}

private fun formatHistoryEntry(weight: Double?, reps: Long?): String = when {
    weight != null && reps != null -> "${formatNumber(weight)} × $reps"
    weight != null -> formatNumber(weight)
    reps != null -> "$reps reps"
    else -> "Completed set"
}

private fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
