package com.workoutapp.composeapp.ui.plates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import com.workoutapp.composeapp.data.plates.PlateBreakdown
import com.workoutapp.composeapp.data.plates.WeightUnit
import com.workoutapp.composeapp.data.plates.calculatePlateBreakdown
import com.workoutapp.composeapp.data.plates.generateWarmupSets
import com.workoutapp.composeapp.ui.designsystem.components.AppCard
import com.workoutapp.composeapp.ui.designsystem.components.AppNumberField
import com.workoutapp.composeapp.ui.designsystem.components.AppTextField
import com.workoutapp.composeapp.ui.designsystem.components.PrimaryButton
import com.workoutapp.composeapp.ui.designsystem.components.SecondaryButton
import com.workoutapp.composeapp.ui.designsystem.theme.LocalSpacing

private enum class CalculatorTab { PLATES, WARMUP }

private const val KG_ROUNDING_INCREMENT = 2.5
private const val LB_ROUNDING_INCREMENT = 5.0

/**
 * Plate-math + warm-up-ramp calculator for a target/working weight, opened from a set row
 * (#23). Bar weight and available plates default per [WeightUnit] but are editable so a lifter
 * with a non-standard rack still gets correct math; the warm-up ramp is generated from the same
 * target so no separate flow is needed to plan warm-ups.
 */
@Composable
fun PlateCalculatorDialog(
    initialWeight: Double?,
    onApplyWeight: (Double) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var unit by remember { mutableStateOf(WeightUnit.KG) }
    var barWeightText by remember(unit) { mutableStateOf(formatNumber(unit.defaultBarWeight)) }
    var platesText by remember(unit) { mutableStateOf(unit.defaultPlates.joinToString(", ") { formatNumber(it) }) }
    var targetText by remember { mutableStateOf(initialWeight?.let(::formatNumber).orEmpty()) }
    var tab by remember { mutableStateOf(CalculatorTab.PLATES) }
    val spacing = LocalSpacing.current

    val barWeight = barWeightText.toDoubleOrNull() ?: unit.defaultBarWeight
    val availablePlates = platesText.split(",").mapNotNull { it.trim().toDoubleOrNull() }
    val target = targetText.toDoubleOrNull()
    val roundingIncrement = if (unit == WeightUnit.KG) KG_ROUNDING_INCREMENT else LB_ROUNDING_INCREMENT
    val unitLabel = unit.name.lowercase()
    val breakdown = target?.let { calculatePlateBreakdown(it, barWeight, availablePlates) }

    Dialog(onDismissRequest = onDismiss) {
        AppCard(modifier = modifier.testTag("plate_calculator_dialog")) {
            Text("Plate & Warm-up Calculator", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                WeightUnit.entries.forEach { option ->
                    AssistChip(
                        onClick = { unit = option },
                        label = { Text(option.name) },
                        colors = if (unit == option) {
                            AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            AssistChipDefaults.assistChipColors()
                        },
                        modifier = Modifier.testTag("plate_calculator_unit_${option.name}"),
                    )
                }
            }

            AppNumberField(
                value = targetText,
                onValueChange = { targetText = it },
                label = "Target weight",
                modifier = Modifier.fillMaxWidth().padding(top = spacing.sm).testTag("plate_calculator_target"),
            )
            AppNumberField(
                value = barWeightText,
                onValueChange = { barWeightText = it },
                label = "Bar weight",
                modifier = Modifier.fillMaxWidth().padding(top = spacing.xs).testTag("plate_calculator_bar_weight"),
            )
            AppTextField(
                value = platesText,
                onValueChange = { platesText = it },
                label = "Available plates per side (comma-separated)",
                modifier = Modifier.fillMaxWidth().padding(top = spacing.xs).testTag("plate_calculator_plates"),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                SecondaryButton(
                    text = "Plates",
                    onClick = { tab = CalculatorTab.PLATES },
                    modifier = Modifier.testTag("plate_calculator_tab_plates"),
                )
                SecondaryButton(
                    text = "Warm-up",
                    onClick = { tab = CalculatorTab.WARMUP },
                    modifier = Modifier.testTag("plate_calculator_tab_warmup"),
                )
            }

            when (tab) {
                CalculatorTab.PLATES -> PlatesResult(breakdown, target, unitLabel)
                CalculatorTab.WARMUP -> WarmupResult(target, roundingIncrement, unitLabel)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SecondaryButton(text = "Close", onClick = onDismiss, modifier = Modifier.testTag("plate_calculator_close"))
                PrimaryButton(
                    text = "Use this weight",
                    onClick = { breakdown?.let { onApplyWeight(it.achievedWeight) } },
                    enabled = tab == CalculatorTab.PLATES && breakdown != null,
                    modifier = Modifier.testTag("plate_calculator_apply"),
                )
            }
        }
    }
}

@Composable
private fun PlatesResult(breakdown: PlateBreakdown?, target: Double?, unitLabel: String) {
    if (breakdown == null || target == null) {
        Text("Enter a target weight.", modifier = Modifier.testTag("plate_calculator_result"))
        return
    }
    Column(modifier = Modifier.testTag("plate_calculator_result")) {
        if (breakdown.platesPerSide.isEmpty()) {
            Text("Bar only (${formatNumber(breakdown.achievedWeight)} $unitLabel)")
        } else {
            Text("Per side: ${breakdown.platesPerSide.joinToString(" + ") { formatNumber(it) }}")
            Text(
                if (breakdown.isExactMatch) {
                    "Total: ${formatNumber(breakdown.achievedWeight)} $unitLabel"
                } else {
                    "Closest reachable: ${formatNumber(breakdown.achievedWeight)} $unitLabel " +
                        "(${formatNumber(target)} $unitLabel isn't reachable with these plates)"
                },
            )
        }
    }
}

@Composable
private fun WarmupResult(target: Double?, roundingIncrement: Double, unitLabel: String) {
    if (target == null) {
        Text("Enter a target weight.", modifier = Modifier.testTag("plate_calculator_result"))
        return
    }
    val warmupSets = generateWarmupSets(target, roundingIncrement = roundingIncrement)
    Column(modifier = Modifier.testTag("plate_calculator_result")) {
        warmupSets.forEachIndexed { index, set ->
            Text("Set ${index + 1}: ${formatNumber(set.weight)} $unitLabel × ${set.reps}")
        }
    }
}

private fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
