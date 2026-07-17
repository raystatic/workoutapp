package com.workoutapp.android

import android.app.Application
import androidx.test.runner.AndroidJUnitRunner
import com.workoutapp.composeapp.data.onboarding.OnboardingRepository
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext

/**
 * Test-only instrumentation runner. Right after [WorkoutApplication.onCreate] starts Koin,
 * this seeds the walkthrough (#24) as already-seen so every instrumentation test launches
 * `MainActivity` the way the rest of this suite already assumes: straight to the main
 * content, with no first-run overlay in front of it. [WalkthroughFlowTest] is the one test
 * that exercises the real fresh-install path — it flips the flag back itself and restores
 * it afterwards, so later tests in the same run are unaffected regardless of execution order.
 */
class WorkoutInstrumentationTestRunner : AndroidJUnitRunner() {
    override fun callApplicationOnCreate(app: Application) {
        super.callApplicationOnCreate(app)
        runBlocking {
            GlobalContext.get().get<OnboardingRepository>().setHasSeenWalkthrough(true)
        }
    }
}
