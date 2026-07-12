package com.workoutapp.composeapp

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.workoutapp.composeapp.navigation.AppDestination
import com.workoutapp.composeapp.ui.profile.ProfileScreen
import com.workoutapp.composeapp.ui.workout.WorkoutScreen

/**
 * Minimal app-wide constants. Placeholder for real app metadata as the
 * project grows.
 */
object AppInfo {
    const val name: String = "Workout App"
}

/**
 * The root composable: bottom-tab navigation between the Workout and Profile
 * destinations. Both screens' state/DI wiring flows through Koin.
 */
@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        Scaffold(
            bottomBar = {
                NavigationBar {
                    AppDestination.bottomTabs.forEach { destination ->
                        NavigationBarItem(
                            modifier = Modifier.testTag("tab_${destination.route}"),
                            selected = currentRoute == destination.route,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {},
                            label = { Text(destination.label) },
                        )
                    }
                }
            },
        ) { contentPadding ->
            NavHost(
                navController = navController,
                startDestination = AppDestination.Workout.route,
                modifier = Modifier.padding(contentPadding),
            ) {
                composable(AppDestination.Workout.route) { WorkoutScreen() }
                composable(AppDestination.Profile.route) { ProfileScreen() }
            }
        }
    }
}
