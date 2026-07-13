package com.workoutapp.composeapp.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.workoutapp.composeapp.isDebugBuild
import com.workoutapp.composeapp.ui.designsystem.components.SecondaryButton
import com.workoutapp.composeapp.ui.designsystem.theme.LocalSpacing

/** Placeholder — the real profile screen lands in a later issue. */
@Composable
fun ProfileScreen(onOpenComponentCatalog: () -> Unit = {}) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier.fillMaxSize().padding(spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Your Profile", style = MaterialTheme.typography.headlineSmall)
        if (isDebugBuild) {
            SecondaryButton(
                text = "Design System Catalog",
                onClick = onOpenComponentCatalog,
                modifier = Modifier
                    .padding(top = spacing.md)
                    .testTag("open_component_catalog"),
            )
        }
    }
}
