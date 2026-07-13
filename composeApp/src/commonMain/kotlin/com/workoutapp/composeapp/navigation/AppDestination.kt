package com.workoutapp.composeapp.navigation

/** Bottom-tab destinations. Expand this list as new tabs land. */
sealed class AppDestination(val route: String, val label: String) {
    data object Workout : AppDestination("workout", "Workout")
    data object Profile : AppDestination("profile", "Profile")

    /** Debug-only screen previewing every design-system component. */
    data object ComponentCatalog : AppDestination("component_catalog", "Component Catalog")

    /**
     * Pushed when a workout starts (empty or from a routine). Full set-logging UI
     * lands in a later issue; for now this just proves the navigation handoff.
     */
    data object ActiveWorkout : AppDestination("active_workout/{workoutId}", "Active Workout") {
        fun route(workoutId: Long): String = "active_workout/$workoutId"
    }

    companion object {
        val bottomTabs: List<AppDestination> = listOf(Workout, Profile)
    }
}
