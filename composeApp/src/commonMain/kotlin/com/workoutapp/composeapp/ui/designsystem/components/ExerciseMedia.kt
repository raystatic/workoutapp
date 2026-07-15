package com.workoutapp.composeapp.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent

/**
 * An exercise's media (GIF/image), loaded from [mediaUrl] via Coil. Falls back to a placeholder
 * for a missing url, a load in progress, or a failed load — so the caller never has to special-case
 * exercises without media (the common case today; see `ExerciseSeedData`).
 */
@Composable
fun ExerciseMedia(mediaUrl: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (mediaUrl.isNullOrBlank()) {
            ExerciseMediaPlaceholder()
        } else {
            SubcomposeAsyncImage(
                model = mediaUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (painter.state.value is AsyncImagePainter.State.Success) {
                    SubcomposeAsyncImageContent()
                } else {
                    ExerciseMediaPlaceholder()
                }
            }
        }
    }
}

@Composable
private fun ExerciseMediaPlaceholder() {
    Text(text = "No media", style = MaterialTheme.typography.bodySmall)
}
