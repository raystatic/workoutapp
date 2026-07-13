package com.workoutapp.composeapp.ui.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.workoutapp.composeapp.ui.designsystem.components.AppCard
import com.workoutapp.composeapp.ui.designsystem.components.AppDialog
import com.workoutapp.composeapp.ui.designsystem.components.AppListRow
import com.workoutapp.composeapp.ui.designsystem.components.AppNumberField
import com.workoutapp.composeapp.ui.designsystem.components.AppTextField
import com.workoutapp.composeapp.ui.designsystem.components.AppTopBar
import com.workoutapp.composeapp.ui.designsystem.components.EmptyState
import com.workoutapp.composeapp.ui.designsystem.components.PrimaryButton
import com.workoutapp.composeapp.ui.designsystem.components.SecondaryButton
import com.workoutapp.composeapp.ui.designsystem.components.SetType
import com.workoutapp.composeapp.ui.designsystem.components.SetTypeIndicator
import com.workoutapp.composeapp.ui.designsystem.theme.LocalSpacing

/**
 * Renders every core design-system component on one screen so visual
 * regressions are easy to spot in either theme. Debug-only — see
 * [com.workoutapp.composeapp.isDebugBuild].
 */
@Composable
fun ComponentCatalogScreen() {
    val spacing = LocalSpacing.current
    var textValue by remember { mutableStateOf("") }
    var numberValue by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "Component Catalog")
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            item {
                CatalogSection("Buttons") {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        PrimaryButton(
                            text = "Primary",
                            onClick = {},
                            modifier = Modifier.testTag("catalog_primary_button"),
                        )
                        SecondaryButton(
                            text = "Secondary",
                            onClick = {},
                            modifier = Modifier.testTag("catalog_secondary_button"),
                        )
                    }
                }
            }
            item {
                CatalogSection("Inputs") {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        AppTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            label = "Note",
                            modifier = Modifier.testTag("catalog_text_field"),
                        )
                        AppNumberField(
                            value = numberValue,
                            onValueChange = { numberValue = it },
                            label = "Weight (kg)",
                            modifier = Modifier.testTag("catalog_number_field"),
                        )
                    }
                }
            }
            item {
                CatalogSection("List row") {
                    AppListRow(title = "Bench Press", subtitle = "3 sets · 60 kg")
                }
            }
            item {
                CatalogSection("Card") {
                    AppCard { Text("Card content", style = MaterialTheme.typography.bodyMedium) }
                }
            }
            item {
                CatalogSection("Set type indicators") {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        SetType.entries.forEach { SetTypeIndicator(it) }
                    }
                }
            }
            item {
                CatalogSection("Empty state") {
                    EmptyState(
                        title = "No routines yet",
                        message = "Create your first routine to get started.",
                    )
                }
            }
            item {
                CatalogSection("Dialog") {
                    PrimaryButton(
                        text = "Show dialog",
                        onClick = { showDialog = true },
                        modifier = Modifier.testTag("catalog_show_dialog_button"),
                    )
                }
            }
        }
    }

    if (showDialog) {
        AppDialog(
            title = "Delete set?",
            message = "This can't be undone.",
            confirmText = "Delete",
            dismissText = "Cancel",
            onConfirm = { showDialog = false },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun CatalogSection(title: String, content: @Composable () -> Unit) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}
