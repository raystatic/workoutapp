package com.workoutapp.composeapp.ui.activeworkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.unit.dp
import com.workoutapp.composeapp.data.db.currentTimeMillis
import com.workoutapp.composeapp.ui.designsystem.components.AppCard
import com.workoutapp.composeapp.ui.designsystem.components.AppDropdownMenu
import com.workoutapp.composeapp.ui.designsystem.components.AppDropdownMenuItem
import com.workoutapp.composeapp.ui.designsystem.components.AppNumberField
import com.workoutapp.composeapp.ui.designsystem.components.AppTopBar
import com.workoutapp.composeapp.ui.designsystem.components.EmptyState
import com.workoutapp.composeapp.ui.designsystem.components.PrimaryButton
import com.workoutapp.composeapp.ui.designsystem.components.SecondaryButton
import com.workoutapp.composeapp.ui.designsystem.components.SetTypeIndicator
import com.workoutapp.composeapp.ui.designsystem.theme.LocalSpacing
import com.workoutapp.composeapp.ui.library.ExerciseLibraryPicker
import com.workoutapp.composeapp.ui.resttimer.RestTimerIntent
import com.workoutapp.composeapp.ui.resttimer.RestTimerState
import com.workoutapp.composeapp.ui.resttimer.RestTimerStore
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.workoutapp.composeapp.data.db.SetType as DbSetType
import com.workoutapp.composeapp.ui.designsystem.components.SetType as UiSetType

/**
 * The set-logging screen for an in-progress workout: a running duration
 * timer, one card per exercise with its set rows (weight/reps/duration,
 * set type, complete toggle, swipe-to-delete), reordering, superset
 * grouping via a "•••" menu, and a picker to add more exercises. Every edit
 * writes straight to SQLDelight, so the screen is fully usable offline and
 * survives process death / backgrounding.
 */
@Composable
fun ActiveWorkoutScreen(
    workoutId: Long,
    onBack: () -> Unit = {},
    onFinish: (Long) -> Unit = {},
    onOpenExerciseDetail: (Long) -> Unit = {},
    store: ActiveWorkoutStore = koinInject { parametersOf(workoutId) },
    restTimerStore: RestTimerStore = koinInject(),
) {
    val state by store.state.collectAsState()
    val restTimerState by restTimerStore.state.collectAsState()
    val spacing = LocalSpacing.current
    val elapsedSeconds = rememberElapsedSeconds(state.startedAt)

    Column(modifier = Modifier.fillMaxSize().testTag("active_workout_screen")) {
        AppTopBar(
            title = "Active Workout",
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("active_workout_back_button")) {
                    Text("←")
                }
            },
            actions = {
                SecondaryButton(
                    text = "Finish",
                    onClick = { onFinish(workoutId) },
                    modifier = Modifier.testTag("finish_workout_button"),
                )
            },
        )
        Text(
            text = formatElapsedDuration(elapsedSeconds),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(spacing.md).testTag("workout_timer"),
        )

        val runningRestTimer = restTimerState as? RestTimerState.Running
        if (runningRestTimer != null) {
            RestTimerBanner(runningRestTimer, onIntent = restTimerStore::onIntent)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (state.exercises.isEmpty()) {
                EmptyState(
                    title = "No exercises yet",
                    message = "Add an exercise below to start logging sets.",
                    modifier = Modifier.align(Alignment.Center).testTag("no_exercises_empty_state"),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                itemsIndexed(state.exercises, key = { _, item -> item.workoutExerciseId }) { index, exerciseUi ->
                    ExerciseCard(
                        exercise = exerciseUi,
                        isFirst = index == 0,
                        isLast = index == state.exercises.lastIndex,
                        canGroupWithNext = index != state.exercises.lastIndex,
                        onIntent = store::onIntent,
                        onOpenExerciseDetail = onOpenExerciseDetail,
                    )
                }
                item {
                    PrimaryButton(
                        text = "+ Add Exercise",
                        onClick = { store.onIntent(ActiveWorkoutIntent.ShowAddExercise) },
                        modifier = Modifier.fillMaxWidth()
                            .padding(vertical = spacing.md)
                            .testTag("add_exercise_button"),
                    )
                }
            }
        }
    }

    if (state.showAddExercise) {
        ExerciseLibraryPicker(
            exercises = state.availableExercises,
            recentExercises = state.recentExercises,
            onConfirm = { store.onIntent(ActiveWorkoutIntent.AddExercises(it)) },
            onDismiss = { store.onIntent(ActiveWorkoutIntent.HideAddExercise) },
            testTagPrefix = "add_exercise",
            onOpenDetail = onOpenExerciseDetail,
        )
    }
}

@Composable
private fun RestTimerBanner(
    running: RestTimerState.Running,
    onIntent: (RestTimerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    AppCard(modifier = modifier.fillMaxWidth().padding(horizontal = spacing.md).testTag("rest_timer_banner")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Rest: ${formatElapsedDuration(running.remainingSeconds.toLong())}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("rest_timer_remaining"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                SecondaryButton(
                    text = "-15s",
                    onClick = { onIntent(RestTimerIntent.SubtractFifteenSeconds) },
                    modifier = Modifier.testTag("rest_timer_minus_15"),
                )
                SecondaryButton(
                    text = "+15s",
                    onClick = { onIntent(RestTimerIntent.AddFifteenSeconds) },
                    modifier = Modifier.testTag("rest_timer_plus_15"),
                )
                SecondaryButton(
                    text = "Skip",
                    onClick = { onIntent(RestTimerIntent.Skip) },
                    modifier = Modifier.testTag("rest_timer_skip"),
                )
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: ActiveWorkoutExerciseUi,
    isFirst: Boolean,
    isLast: Boolean,
    canGroupWithNext: Boolean,
    onIntent: (ActiveWorkoutIntent) -> Unit,
    onOpenExerciseDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    var menuExpanded by remember { mutableStateOf(false) }
    val isGrouped = exercise.supersetGroup != null
    val cardModifier = modifier.fillMaxWidth().testTag("exercise_row_${exercise.workoutExerciseId}").let {
        if (isGrouped) {
            it.border(
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
            )
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Superset ${exercise.supersetLabel}") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                            modifier = Modifier.testTag("superset_badge_${exercise.workoutExerciseId}"),
                        )
                        if (exercise.isUpNextInSuperset) {
                            Text(
                                text = "Up next",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("superset_up_next_${exercise.workoutExerciseId}"),
                            )
                        }
                    }
                }
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .testTag("exercise_name_${exercise.workoutExerciseId}")
                        .clickable { onOpenExerciseDetail(exercise.exerciseId) },
                )
                AssistChip(
                    onClick = { onIntent(ActiveWorkoutIntent.CycleRestOverride(exercise.workoutExerciseId)) },
                    label = { Text(formatRestOverride(exercise.restSeconds)) },
                    modifier = Modifier.testTag("rest_override_chip_${exercise.workoutExerciseId}"),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onIntent(ActiveWorkoutIntent.MoveExerciseUp(exercise.workoutExerciseId)) },
                    enabled = !isFirst,
                    modifier = Modifier.testTag("move_exercise_up_${exercise.workoutExerciseId}"),
                ) { Text("▲") }
                IconButton(
                    onClick = { onIntent(ActiveWorkoutIntent.MoveExerciseDown(exercise.workoutExerciseId)) },
                    enabled = !isLast,
                    modifier = Modifier.testTag("move_exercise_down_${exercise.workoutExerciseId}"),
                ) { Text("▼") }
                IconButton(
                    onClick = { onIntent(ActiveWorkoutIntent.RemoveExercise(exercise.workoutExerciseId)) },
                    modifier = Modifier.testTag("remove_exercise_${exercise.workoutExerciseId}"),
                ) { Text("✕") }
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("exercise_menu_${exercise.workoutExerciseId}"),
                    ) { Text("•••") }
                    AppDropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        if (isGrouped) {
                            AppDropdownMenuItem(
                                text = "Remove from superset",
                                onClick = {
                                    menuExpanded = false
                                    onIntent(ActiveWorkoutIntent.RemoveFromSuperset(exercise.workoutExerciseId))
                                },
                                modifier = Modifier.testTag("remove_from_superset_${exercise.workoutExerciseId}"),
                            )
                        } else if (canGroupWithNext) {
                            AppDropdownMenuItem(
                                text = "Group with next exercise",
                                onClick = {
                                    menuExpanded = false
                                    onIntent(ActiveWorkoutIntent.GroupWithNextExercise(exercise.workoutExerciseId))
                                },
                                modifier = Modifier.testTag("group_with_next_${exercise.workoutExerciseId}"),
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            exercise.sets.forEach { setUi ->
                SetRow(setUi = setUi, onIntent = onIntent)
            }
        }

        SecondaryButton(
            text = "+ Add Set",
            onClick = { onIntent(ActiveWorkoutIntent.AddSet(exercise.workoutExerciseId)) },
            modifier = Modifier.padding(top = spacing.sm).testTag("add_set_${exercise.workoutExerciseId}"),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetRow(setUi: ActiveWorkoutSetUi, onIntent: (ActiveWorkoutIntent) -> Unit, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    val set = setUi.set
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onIntent(ActiveWorkoutIntent.RemoveSet(set.id))
            }
            true
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.testTag("set_row_${set.id}"),
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    "Delete",
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(horizontal = spacing.md),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = spacing.xs),
        ) {
            Text(
                text = formatPrevious(setUi),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("previous_set_${set.id}"),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                SetTypeIndicator(
                    type = set.setType.toUi(),
                    modifier = Modifier
                        .testTag("set_type_${set.id}")
                        .clickable { onIntent(ActiveWorkoutIntent.CycleSetType(set.id)) },
                )
                AppNumberField(
                    value = set.weight?.let(::formatNumber) ?: "",
                    onValueChange = { onIntent(ActiveWorkoutIntent.UpdateWeight(set.id, it)) },
                    label = "Weight",
                    modifier = Modifier.width(90.dp).testTag("set_weight_${set.id}"),
                )
                AppNumberField(
                    value = set.reps?.toString() ?: "",
                    onValueChange = { onIntent(ActiveWorkoutIntent.UpdateReps(set.id, it)) },
                    label = "Reps",
                    modifier = Modifier.width(80.dp).testTag("set_reps_${set.id}"),
                )
                AppNumberField(
                    value = set.durationSec?.toString() ?: "",
                    onValueChange = { onIntent(ActiveWorkoutIntent.UpdateDuration(set.id, it)) },
                    label = "Sec",
                    modifier = Modifier.width(80.dp).testTag("set_duration_${set.id}"),
                )
                Checkbox(
                    checked = set.completed,
                    onCheckedChange = { onIntent(ActiveWorkoutIntent.ToggleSetComplete(set.id)) },
                    modifier = Modifier.testTag("set_complete_${set.id}"),
                )
                IconButton(
                    onClick = { onIntent(ActiveWorkoutIntent.RemoveSet(set.id)) },
                    modifier = Modifier.testTag("remove_set_${set.id}"),
                ) { Text("✕") }
            }
        }
    }
}

/** Formats the "last time" hint shown above a set row; graceful when there's no history. */
private fun formatPrevious(setUi: ActiveWorkoutSetUi): String {
    val weight = setUi.previousWeight
    val reps = setUi.previousReps
    val duration = setUi.previousDurationSec
    return when {
        weight != null && reps != null -> "Previous: ${formatNumber(weight)} × $reps"
        weight != null -> "Previous: ${formatNumber(weight)}"
        reps != null -> "Previous: $reps reps"
        duration != null -> "Previous: ${duration}s"
        else -> "No previous data"
    }
}

@Composable
private fun rememberElapsedSeconds(startedAt: Long?): Long {
    var elapsed by remember(startedAt) { mutableStateOf(0L) }
    LaunchedEffect(startedAt) {
        if (startedAt == null) return@LaunchedEffect
        while (true) {
            elapsed = ((currentTimeMillis() - startedAt) / 1000).coerceAtLeast(0)
            delay(1000)
        }
    }
    return elapsed
}

/** Label for the per-exercise rest-timer override chip; `null` means "use the global default". */
private fun formatRestOverride(restSeconds: Long?): String =
    if (restSeconds == null) "Rest: Default" else "Rest: ${restSeconds}s"

private fun DbSetType.toUi(): UiSetType = UiSetType.valueOf(name)

private fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
