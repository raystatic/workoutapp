package com.workoutapp.composeapp.ui.designsystem.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** One destination in [AppBottomTabBar]. */
data class BottomTabItem(val route: String, val label: String)

/** The app's bottom tab bar. Reusable across any set of top-level destinations. */
@Composable
fun AppBottomTabBar(
    items: List<BottomTabItem>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            NavigationBarItem(
                modifier = Modifier.testTag("tab_${item.route}"),
                selected = selectedRoute == item.route,
                onClick = { onSelect(item.route) },
                icon = {},
                label = { Text(item.label) },
            )
        }
    }
}
