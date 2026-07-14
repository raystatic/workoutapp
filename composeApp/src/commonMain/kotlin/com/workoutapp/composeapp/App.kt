package com.workoutapp.composeapp

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.workoutapp.composeapp.navigation.AppDestination
import com.workoutapp.composeapp.ui.activeworkout.ActiveWorkoutScreen
import com.workoutapp.composeapp.ui.designsystem.catalog.ComponentCatalogScreen
import com.workoutapp.composeapp.ui.designsystem.components.AppBottomTabBar
import com.workoutapp.composeapp.ui.designsystem.components.BottomTabItem
import com.workoutapp.composeapp.ui.designsystem.theme.WorkoutAppTheme
import com.workoutapp.composeapp.ui.finishworkout.FinishWorkoutScreen
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
    WorkoutAppTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        Scaffold(
            bottomBar = {
                AppBottomTabBar(
                    items = AppDestination.bottomTabs.map { BottomTabItem(it.route, it.label) },
                    selectedRoute = currentRoute,
                    onSelect = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            },
        ) { contentPadding ->
            NavHost(
                navController = navController,
                startDestination = AppDestination.Workout.route,
                modifier = Modifier.padding(contentPadding),
            ) {
                composable(AppDestination.Workout.route) {
                    WorkoutScreen(
                        onWorkoutStarted = { workoutId ->
                            navController.navigate(AppDestination.ActiveWorkout.route(workoutId))
                        },
                    )
                }
                composable(AppDestination.Profile.route) {
                    ProfileScreen(
                        onOpenComponentCatalog = {
                            navController.navigate(AppDestination.ComponentCatalog.route)
                        },
                    )
                }
                composable(AppDestination.ComponentCatalog.route) { ComponentCatalogScreen() }
                composable(
                    route = AppDestination.ActiveWorkout.route,
                    arguments = listOf(navArgument("workoutId") { type = NavType.LongType }),
                ) { backStackEntry ->
                    val workoutId = backStackEntry.arguments?.read { getLong("workoutId") } ?: 0L
                    ActiveWorkoutScreen(
                        workoutId = workoutId,
                        onBack = { navController.popBackStack() },
                        onFinish = { navController.navigate(AppDestination.FinishWorkout.route(it)) },
                    )
                }
                composable(
                    route = AppDestination.FinishWorkout.route,
                    arguments = listOf(navArgument("workoutId") { type = NavType.LongType }),
                ) { backStackEntry ->
                    val workoutId = backStackEntry.arguments?.read { getLong("workoutId") } ?: 0L
                    FinishWorkoutScreen(
                        workoutId = workoutId,
                        onBack = { navController.popBackStack() },
                        onDone = { navController.popBackStack(AppDestination.Workout.route, false) },
                    )
                }
            }
        }
    }
}
