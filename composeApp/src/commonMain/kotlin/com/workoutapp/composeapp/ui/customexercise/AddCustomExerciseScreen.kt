package com.workoutapp.composeapp.ui.customexercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.workoutapp.composeapp.ui.designsystem.components.AppDropdownMenu
import com.workoutapp.composeapp.ui.designsystem.components.AppDropdownMenuItem
import com.workoutapp.composeapp.ui.designsystem.components.AppTextField
import com.workoutapp.composeapp.ui.designsystem.components.AppTopBar
import com.workoutapp.composeapp.ui.designsystem.components.PrimaryButton
import com.workoutapp.composeapp.ui.designsystem.theme.LocalSpacing
import org.koin.compose.koinInject

/**
 * "Add Custom Exercise" screen (#22). Reachable from the exercise library picker when a lifter
 * can't find a niche/rehab movement in the seeded catalog. Blocked with an upsell-ready message
 * once the free tier's [CUSTOM_EXERCISE_FREE_LIMIT] custom exercises are already created.
 */
@Composable
fun AddCustomExerciseScreen(
    onDone: () -> Unit = {},
    store: AddCustomExerciseStore = koinInject(),
) {
    val state by store.state.collectAsState()
    var typeMenuExpanded by remember { mutableStateOf(false) }
    val spacing = LocalSpacing.current

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Column(modifier = Modifier.fillMaxSize().testTag("add_custom_exercise_screen")) {
        AppTopBar(
            title = "Add Custom Exercise",
            navigationIcon = {
                IconButton(onClick = onDone, modifier = Modifier.testTag("add_custom_exercise_back_button")) {
                    Text("←")
                }
            },
        )

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            if (state.capReached) {
                Text(
                    text = CUSTOM_EXERCISE_CAP_MESSAGE,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("add_custom_exercise_cap_message"),
                )
            }

            AppTextField(
                value = state.name,
                onValueChange = { store.onIntent(AddCustomExerciseIntent.UpdateName(it)) },
                label = "Name",
                modifier = Modifier.fillMaxWidth().testTag("add_custom_exercise_name_field"),
            )
            AppTextField(
                value = state.primaryMuscle,
                onValueChange = { store.onIntent(AddCustomExerciseIntent.UpdatePrimaryMuscle(it)) },
                label = "Primary muscle",
                modifier = Modifier.fillMaxWidth().testTag("add_custom_exercise_primary_muscle_field"),
            )
            AppTextField(
                value = state.secondaryMusclesInput,
                onValueChange = { store.onIntent(AddCustomExerciseIntent.UpdateSecondaryMuscles(it)) },
                label = "Secondary muscles (comma-separated, optional)",
                modifier = Modifier.fillMaxWidth().testTag("add_custom_exercise_secondary_muscles_field"),
            )
            AppTextField(
                value = state.equipment,
                onValueChange = { store.onIntent(AddCustomExerciseIntent.UpdateEquipment(it)) },
                label = "Equipment",
                modifier = Modifier.fillMaxWidth().testTag("add_custom_exercise_equipment_field"),
            )

            Box {
                AssistChip(
                    onClick = { typeMenuExpanded = true },
                    label = { Text(if (state.type.isBlank()) "Type: Select" else "Type: ${state.type}") },
                    modifier = Modifier.testTag("add_custom_exercise_type_field"),
                )
                AppDropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                    CUSTOM_EXERCISE_TYPES.forEach { option ->
                        AppDropdownMenuItem(
                            text = option,
                            onClick = {
                                store.onIntent(AddCustomExerciseIntent.UpdateType(option))
                                typeMenuExpanded = false
                            },
                            modifier = Modifier.testTag("add_custom_exercise_type_option_$option"),
                        )
                    }
                }
            }

            AppTextField(
                value = state.mediaUrl,
                onValueChange = { store.onIntent(AddCustomExerciseIntent.UpdateMediaUrl(it)) },
                label = "Image/GIF URL (optional)",
                modifier = Modifier.fillMaxWidth().testTag("add_custom_exercise_media_url_field"),
            )
            AppTextField(
                value = state.instructions,
                onValueChange = { store.onIntent(AddCustomExerciseIntent.UpdateInstructions(it)) },
                label = "Instructions (optional)",
                modifier = Modifier.fillMaxWidth().testTag("add_custom_exercise_instructions_field"),
            )

            state.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("add_custom_exercise_error"),
                )
            }

            PrimaryButton(
                text = "Save",
                onClick = { store.onIntent(AddCustomExerciseIntent.Save) },
                enabled = state.isValid && !state.capReached,
                modifier = Modifier.testTag("add_custom_exercise_save_button"),
            )
        }
    }
}
