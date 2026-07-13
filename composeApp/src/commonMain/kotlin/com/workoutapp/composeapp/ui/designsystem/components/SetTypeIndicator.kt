package com.workoutapp.composeapp.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.workoutapp.composeapp.ui.designsystem.theme.LocalSpacing

/** The set types a lifter can log per set (EXECUTION_PLAN.md §4/CUJ 1-2). */
enum class SetType(val label: String, val symbol: String) {
    NORMAL(label = "Normal", symbol = "•"),
    WARMUP(label = "Warm-up", symbol = "W"),
    DROP(label = "Drop", symbol = "D"),
    FAILURE(label = "Failure", symbol = "F"),
}

/**
 * Badge distinguishing a [SetType] by symbol + label as well as color, so it
 * reads correctly for color-blind users rather than relying on hue alone.
 */
@Composable
fun SetTypeIndicator(type: SetType, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    Row(
        modifier = modifier.testTag("set_type_indicator_${type.name}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(setTypeColor(type), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = type.symbol,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
        Text(
            text = type.label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = spacing.sm),
        )
    }
}

@Composable
private fun setTypeColor(type: SetType): Color = when (type) {
    SetType.NORMAL -> MaterialTheme.colorScheme.primary
    SetType.WARMUP -> MaterialTheme.colorScheme.tertiary
    SetType.DROP -> MaterialTheme.colorScheme.secondary
    SetType.FAILURE -> MaterialTheme.colorScheme.error
}
