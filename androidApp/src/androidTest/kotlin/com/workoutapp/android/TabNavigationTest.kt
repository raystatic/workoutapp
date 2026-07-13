package com.workoutapp.android

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

/**
 * Smoke test: the bottom tab bar navigates between the Workout and Profile
 * destinations without losing the nav graph.
 */
class TabNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun switchingTabsShowsTheCorrespondingScreen() {
        composeRule.onNodeWithTag("start_empty_workout").assertExists()

        composeRule.onNodeWithTag("tab_profile").performClick()
        composeRule.onNodeWithText("Your Profile").assertExists()

        composeRule.onNodeWithTag("tab_workout").performClick()
        composeRule.onNodeWithTag("start_empty_workout").assertExists()
    }
}
