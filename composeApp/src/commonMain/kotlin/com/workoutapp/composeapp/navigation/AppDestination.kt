package com.workoutapp.composeapp.navigation

/** Bottom-tab destinations. Expand this list as new tabs land. */
sealed class AppDestination(val route: String, val label: String) {
    data object Workout : AppDestination("workout", "Workout")
    data object Profile : AppDestination("profile", "Profile")

    companion object {
        val bottomTabs: List<AppDestination> = listOf(Workout, Profile)
    }
}
