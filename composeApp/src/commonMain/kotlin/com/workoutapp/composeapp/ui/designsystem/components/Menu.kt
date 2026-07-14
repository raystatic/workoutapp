package com.workoutapp.composeapp.ui.designsystem.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** A dropdown overflow menu anchored to the composable that toggles [expanded]. */
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, modifier = modifier, content = { content() })
}

/** One selectable action inside an [AppDropdownMenu]. */
@Composable
fun AppDropdownMenuItem(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    DropdownMenuItem(text = { Text(text) }, onClick = onClick, modifier = modifier)
}
