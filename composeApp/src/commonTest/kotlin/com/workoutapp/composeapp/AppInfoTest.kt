package com.workoutapp.composeapp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Trivial commonTest asserting the shared composeApp module wires up: the
 * app metadata is present and correct across every target that runs
 * commonTest (Android unit tests + JVM-visible common tests).
 */
class AppInfoTest {
    @Test
    fun appNameIsWorkoutApp() {
        assertEquals("Workout App", AppInfo.name)
    }

    @Test
    fun appNameIsNotBlank() {
        assertTrue(AppInfo.name.isNotBlank())
    }
}
