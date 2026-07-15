package com.workoutapp.composeapp.ui.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * A minimal single-series line chart — e.g. the exercise detail screen's "best weight over time".
 * [values] is expected oldest-first. Draws nothing for fewer than two points; callers should show
 * an [EmptyState] instead in that case.
 */
@Composable
fun SimpleLineChart(values: List<Float>, modifier: Modifier = Modifier, lineColor: Color = MaterialTheme.colorScheme.primary) {
    if (values.size < 2) return
    val maxValue = values.max()
    val minValue = values.min()
    val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f

    Canvas(modifier = modifier) {
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - minValue) / range) * size.height
            Offset(x, y)
        }
        for (i in 0 until points.lastIndex) {
            drawLine(color = lineColor, start = points[i], end = points[i + 1], strokeWidth = 4f)
        }
        points.forEach { point -> drawCircle(color = lineColor, radius = 5f, center = point) }
    }
}
