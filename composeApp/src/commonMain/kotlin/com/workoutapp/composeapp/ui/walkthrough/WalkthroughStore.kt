package com.workoutapp.composeapp.ui.walkthrough

import com.workoutapp.composeapp.data.onboarding.OnboardingRepository
import com.workoutapp.composeapp.mvi.MviEffect
import com.workoutapp.composeapp.mvi.MviIntent
import com.workoutapp.composeapp.mvi.MviState
import com.workoutapp.composeapp.mvi.StoreViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class WalkthroughState(
    val isLoading: Boolean = true,
    val visible: Boolean = false,
    val step: Int = 0,
) : MviState {
    val isLastStep: Boolean get() = step == STEP_COUNT - 1

    companion object {
        const val STEP_COUNT = 3
    }
}

sealed interface WalkthroughIntent : MviIntent {
    /** Re-checks [OnboardingRepository] — sent whenever [WalkthroughScreen] enters composition. */
    data object Reload : WalkthroughIntent
    data object Next : WalkthroughIntent
    data object Skip : WalkthroughIntent
}

sealed interface WalkthroughEffect : MviEffect

/**
 * Drives the first-run walkthrough overlay (#24): shown once, over the main
 * content, until it's stepped through or skipped, after which
 * [OnboardingRepository] remembers it's been seen so it never shows again.
 */
class WalkthroughStore(
    private val onboardingRepository: OnboardingRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : StoreViewModel<WalkthroughState, WalkthroughIntent, WalkthroughEffect>(WalkthroughState(), dispatcher) {

    init {
        checkStatus()
    }

    override fun onIntent(intent: WalkthroughIntent) {
        when (intent) {
            WalkthroughIntent.Reload -> checkStatus()
            WalkthroughIntent.Next -> advance()
            WalkthroughIntent.Skip -> finish()
        }
    }

    private fun checkStatus() {
        scope.launch {
            val hasSeen = onboardingRepository.hasSeenWalkthrough()
            setState { it.copy(isLoading = false, visible = !hasSeen, step = 0) }
        }
    }

    private fun advance() {
        if (state.value.isLastStep) {
            finish()
        } else {
            setState { it.copy(step = it.step + 1) }
        }
    }

    private fun finish() {
        setState { it.copy(visible = false) }
        scope.launch { onboardingRepository.setHasSeenWalkthrough(true) }
    }
}
