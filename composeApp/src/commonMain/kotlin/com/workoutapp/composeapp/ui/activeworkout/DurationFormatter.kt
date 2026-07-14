package com.workoutapp.composeapp.ui.activeworkout

/** Formats a non-negative second count as `MM:SS`, or `H:MM:SS` once it reaches an hour. */
fun formatElapsedDuration(totalSeconds: Long): String {
    val clamped = totalSeconds.coerceAtLeast(0)
    val hours = clamped / 3600
    val minutes = (clamped % 3600) / 60
    val seconds = clamped % 60
    return if (hours > 0) {
        "$hours:${minutes.padded()}:${seconds.padded()}"
    } else {
        "${minutes.padded()}:${seconds.padded()}"
    }
}

private fun Long.padded(): String = if (this < 10) "0$this" else toString()
