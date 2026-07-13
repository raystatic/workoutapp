package com.workoutapp.composeapp.ui.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.workoutapp.composeapp.ui.designsystem.theme.LocalSpacing

/** A padded, elevated surface grouping related content. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(LocalSpacing.current.md)) {
            content()
        }
    }
}
