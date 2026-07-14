package com.workoutapp.composeapp.ui.resttimer

import com.workoutapp.composeapp.data.resttimer.RestTimerNotifier
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeRestTimerNotifier : RestTimerNotifier {
    var scheduledSeconds: Int? = null
    var cancelCount = 0

    override fun scheduleEndNotification(secondsFromNow: Int) {
        scheduledSeconds = secondsFromNow
    }

    override fun cancel() {
        cancelCount += 1
        scheduledSeconds = null
    }
}

class RestTimerStoreTest {
    @Test
    fun start_entersRunningStateAndSchedulesNotification() = runTest {
        val notifier = FakeRestTimerNotifier()
        val store = RestTimerStore(notifier, UnconfinedTestDispatcher(testScheduler)) { testScheduler.currentTime }

        store.onIntent(RestTimerIntent.Start(exerciseId = 100L, seconds = 90))

        val running = store.state.value as RestTimerState.Running
        assertEquals(100L, running.exerciseId)
        assertEquals(90, running.totalSeconds)
        assertEquals(90, running.remainingSeconds)
        assertEquals(90, notifier.scheduledSeconds)
    }

    @Test
    fun tick_decrementsRemainingSecondsAsVirtualTimeAdvances() = runTest {
        val notifier = FakeRestTimerNotifier()
        val store = RestTimerStore(notifier, UnconfinedTestDispatcher(testScheduler)) { testScheduler.currentTime }
        store.onIntent(RestTimerIntent.Start(exerciseId = 1L, seconds = 10))

        testScheduler.advanceTimeBy(3_000)
        testScheduler.runCurrent()

        val running = store.state.value as RestTimerState.Running
        assertEquals(7, running.remainingSeconds)
    }

    @Test
    fun timerReachesZero_returnsToIdleWithoutFurtherTicking() = runTest {
        val notifier = FakeRestTimerNotifier()
        val store = RestTimerStore(notifier, UnconfinedTestDispatcher(testScheduler)) { testScheduler.currentTime }
        store.onIntent(RestTimerIntent.Start(exerciseId = 1L, seconds = 5))

        testScheduler.advanceUntilIdle()

        assertEquals(RestTimerState.Idle, store.state.value)
    }

    @Test
    fun skip_cancelsNotificationAndReturnsToIdleImmediately() = runTest {
        val notifier = FakeRestTimerNotifier()
        val store = RestTimerStore(notifier, UnconfinedTestDispatcher(testScheduler)) { testScheduler.currentTime }
        store.onIntent(RestTimerIntent.Start(exerciseId = 1L, seconds = 90))

        store.onIntent(RestTimerIntent.Skip)

        assertEquals(RestTimerState.Idle, store.state.value)
        assertEquals(1, notifier.cancelCount)
        assertNull(notifier.scheduledSeconds)
    }

    @Test
    fun addFifteenSeconds_increasesRemainingAndReschedulesNotification() = runTest {
        val notifier = FakeRestTimerNotifier()
        val store = RestTimerStore(notifier, UnconfinedTestDispatcher(testScheduler)) { testScheduler.currentTime }
        store.onIntent(RestTimerIntent.Start(exerciseId = 1L, seconds = 30))

        store.onIntent(RestTimerIntent.AddFifteenSeconds)

        val running = store.state.value as RestTimerState.Running
        assertEquals(45, running.remainingSeconds)
        assertEquals(45, notifier.scheduledSeconds)
    }

    @Test
    fun subtractFifteenSeconds_decreasesRemainingAndReschedulesNotification() = runTest {
        val notifier = FakeRestTimerNotifier()
        val store = RestTimerStore(notifier, UnconfinedTestDispatcher(testScheduler)) { testScheduler.currentTime }
        store.onIntent(RestTimerIntent.Start(exerciseId = 1L, seconds = 30))

        store.onIntent(RestTimerIntent.SubtractFifteenSeconds)

        val running = store.state.value as RestTimerState.Running
        assertEquals(15, running.remainingSeconds)
        assertEquals(15, notifier.scheduledSeconds)
    }

    @Test
    fun subtractFifteenSeconds_pastZero_stopsTheTimer() = runTest {
        val notifier = FakeRestTimerNotifier()
        val store = RestTimerStore(notifier, UnconfinedTestDispatcher(testScheduler)) { testScheduler.currentTime }
        store.onIntent(RestTimerIntent.Start(exerciseId = 1L, seconds = 10))

        store.onIntent(RestTimerIntent.SubtractFifteenSeconds)

        assertEquals(RestTimerState.Idle, store.state.value)
        assertEquals(1, notifier.cancelCount)
    }

    @Test
    fun start_calledAgainWhileRunning_replacesThePreviousTimer() = runTest {
        val notifier = FakeRestTimerNotifier()
        val store = RestTimerStore(notifier, UnconfinedTestDispatcher(testScheduler)) { testScheduler.currentTime }
        store.onIntent(RestTimerIntent.Start(exerciseId = 1L, seconds = 90))

        store.onIntent(RestTimerIntent.Start(exerciseId = 2L, seconds = 60))

        val running = store.state.value as RestTimerState.Running
        assertEquals(2L, running.exerciseId)
        assertEquals(60, running.remainingSeconds)
        assertTrue(notifier.scheduledSeconds == 60)
    }
}
