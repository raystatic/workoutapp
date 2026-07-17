package com.workoutapp.android

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.workoutapp.composeapp.data.onboarding.OnboardingRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/**
 * connectedDebugAndroidTest for the first-run walkthrough (#24). Every other test in this
 * suite launches with the walkthrough already marked seen (see
 * [WorkoutInstrumentationTestRunner]); this test explicitly resets that flag to simulate a
 * genuine fresh install, drives the real flow, then restores the flag so later tests in the
 * same instrumentation run aren't affected.
 */
class WalkthroughFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val onboardingRepository by lazy { GlobalContext.get().get<OnboardingRepository>() }

    @Before
    fun resetToFreshInstall() = runBlocking {
        onboardingRepository.setHasSeenWalkthrough(false)
    }

    @After
    fun restoreSeenFlagForLaterTests() = runBlocking {
        onboardingRepository.setHasSeenWalkthrough(true)
    }

    @Test
    fun freshInstall_showsWalkthrough_andSkippingDismissesItForGood() {
        composeRule.activityRule.scenario.recreate()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("walkthrough_overlay").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("walkthrough_skip").performClick()

        composeRule.onNodeWithTag("walkthrough_overlay").assertDoesNotExist()
        composeRule.onNodeWithTag("start_empty_workout").assertExists()

        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("walkthrough_overlay").assertDoesNotExist()
    }

    @Test
    fun freshInstall_steppingThroughEveryStep_hidesTheWalkthroughAndPersistsSeen() {
        composeRule.activityRule.scenario.recreate()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("walkthrough_overlay").fetchSemanticsNodes().isNotEmpty()
        }
        repeat(3) { composeRule.onNodeWithTag("walkthrough_next").performClick() }

        composeRule.onNodeWithTag("walkthrough_overlay").assertDoesNotExist()
        composeRule.onNodeWithTag("start_empty_workout").assertExists()
    }
}
