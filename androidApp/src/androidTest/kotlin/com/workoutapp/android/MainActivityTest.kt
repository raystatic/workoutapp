package com.workoutapp.android

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke connectedDebugAndroidTest: launches MainActivity and asserts it
 * reaches the RESUMED lifecycle state. This screen has no backend yet, so
 * there is nothing Firebase-related to exercise here.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @Test
    fun activityLaunchesAndResumes() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }
}
