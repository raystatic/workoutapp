package com.workoutapp.composeapp.ui.walkthrough

import com.workoutapp.composeapp.data.onboarding.OnboardingRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeOnboardingRepository(private var seen: Boolean = false) : OnboardingRepository {
    val setCalls = mutableListOf<Boolean>()

    override suspend fun hasSeenWalkthrough(): Boolean = seen

    override suspend fun setHasSeenWalkthrough(seen: Boolean) {
        this.seen = seen
        setCalls += seen
    }
}

class WalkthroughStoreTest {
    private fun newStore(repository: OnboardingRepository) =
        WalkthroughStore(repository, dispatcher = UnconfinedTestDispatcher())

    @Test
    fun freshInstall_hasNotSeenWalkthrough_becomesVisibleAtStepZero() = runTest {
        val store = newStore(FakeOnboardingRepository(seen = false))

        assertFalse(store.state.value.isLoading)
        assertTrue(store.state.value.visible)
        assertEquals(0, store.state.value.step)
    }

    @Test
    fun alreadySeen_staysHidden() = runTest {
        val store = newStore(FakeOnboardingRepository(seen = true))

        assertFalse(store.state.value.isLoading)
        assertFalse(store.state.value.visible)
    }

    @Test
    fun next_advancesThroughEveryStepThenHidesAndPersistsSeen() = runTest {
        val repository = FakeOnboardingRepository(seen = false)
        val store = newStore(repository)

        store.onIntent(WalkthroughIntent.Next)
        assertEquals(1, store.state.value.step)
        assertTrue(store.state.value.visible)
        assertTrue(repository.setCalls.isEmpty())

        store.onIntent(WalkthroughIntent.Next)
        assertEquals(2, store.state.value.step)
        assertTrue(store.state.value.isLastStep)

        store.onIntent(WalkthroughIntent.Next)
        assertFalse(store.state.value.visible)
        assertEquals(listOf(true), repository.setCalls)
    }

    @Test
    fun skip_hidesImmediatelyAndPersistsSeen_regardlessOfStep() = runTest {
        val repository = FakeOnboardingRepository(seen = false)
        val store = newStore(repository)

        store.onIntent(WalkthroughIntent.Next)
        store.onIntent(WalkthroughIntent.Skip)

        assertFalse(store.state.value.visible)
        assertEquals(listOf(true), repository.setCalls)
    }

    @Test
    fun reload_reflectsUpdatedRepositoryState() = runTest {
        val repository = FakeOnboardingRepository(seen = true)
        val store = newStore(repository)
        assertFalse(store.state.value.visible)

        repository.setHasSeenWalkthrough(false)
        store.onIntent(WalkthroughIntent.Reload)

        assertTrue(store.state.value.visible)
        assertEquals(0, store.state.value.step)
    }
}
