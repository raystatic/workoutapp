package com.workoutapp.composeapp.ui.walkthrough

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.workoutapp.composeapp.ui.designsystem.components.PrimaryButton
import com.workoutapp.composeapp.ui.designsystem.components.SecondaryButton
import com.workoutapp.composeapp.ui.designsystem.theme.LocalSpacing
import org.koin.compose.koinInject

private data class WalkthroughStep(val title: String, val message: String)

private val walkthroughSteps = listOf(
    WalkthroughStep(
        title = "Log a set",
        message = "Enter weight and reps, then tap the checkmark to mark a set complete — " +
            "it saves instantly, even offline.",
    ),
    WalkthroughStep(
        title = "Rest timer",
        message = "Completing a set auto-starts your rest timer, so you always know when it's time to go again.",
    ),
    WalkthroughStep(
        title = "Start a routine",
        message = "Build a routine once, then start it from the Workout tab to pre-fill your sets every time.",
    ),
)

/** Full-screen overlay shown once on first launch (over [com.workoutapp.composeapp.App]'s main content). */
@Composable
fun WalkthroughScreen(store: WalkthroughStore = koinInject()) {
    val state by store.state.collectAsState()

    LaunchedEffect(Unit) { store.onIntent(WalkthroughIntent.Reload) }

    if (state.isLoading || !state.visible) return

    val step = walkthroughSteps[state.step]
    val spacing = LocalSpacing.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("walkthrough_overlay"),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(step.title, style = MaterialTheme.typography.headlineSmall)
            Text(step.message, style = MaterialTheme.typography.bodyMedium)

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.md),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                SecondaryButton(
                    text = "Skip",
                    onClick = { store.onIntent(WalkthroughIntent.Skip) },
                    modifier = Modifier.testTag("walkthrough_skip"),
                )
                PrimaryButton(
                    text = if (state.isLastStep) "Get Started" else "Next",
                    onClick = { store.onIntent(WalkthroughIntent.Next) },
                    modifier = Modifier.testTag("walkthrough_next"),
                )
            }
        }
    }
}
