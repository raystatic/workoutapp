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

    /** Pushed from [ActiveWorkout] to finish/save the workout and show the post-save summary. */
    data object FinishWorkout : AppDestination("finish_workout/{workoutId}", "Finish Workout") {
        fun route(workoutId: Long): String = "finish_workout/$workoutId"
    }

    /**
     * Pushed from the Workout tab to create or edit a routine. The Workout tab always
     * creates the [com.workoutapp.composeapp.db.Routine] row first, so this destination
     * only ever edits an existing one — there's no separate "new" route.
     */
    data object RoutineBuilder : AppDestination("routine_builder/{routineId}", "Routine Builder") {
        fun route(routineId: Long): String = "routine_builder/$routineId"
    }

    /** Pushed from the exercise library picker or from an exercise's name mid-workout (#21). */
    data object ExerciseDetail : AppDestination("exercise_detail/{exerciseId}", "Exercise Detail") {
        fun route(exerciseId: Long): String = "exercise_detail/$exerciseId"
    }

    companion object {
        val bottomTabs: List<AppDestination> = listOf(Workout, Profile)
    }
}
