package com.workoutapp.composeapp.ui.resttimer

import com.workoutapp.composeapp.data.db.currentTimeMillis
import com.workoutapp.composeapp.data.resttimer.RestTimerNotifier
import com.workoutapp.composeapp.mvi.MviEffect
import com.workoutapp.composeapp.mvi.MviIntent
import com.workoutapp.composeapp.mvi.MviState
import com.workoutapp.composeapp.mvi.StoreViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ADJUST_STEP_SECONDS = 15

sealed interface RestTimerState : MviState {
    data object Idle : RestTimerState

    data class Running(
        val exerciseId: Long,
        val totalSeconds: Int,
        val remainingSeconds: Int,
    ) : RestTimerState
}

sealed interface RestTimerIntent : MviIntent {
    data class Start(val exerciseId: Long, val seconds: Int) : RestTimerIntent
    data object Skip : RestTimerIntent
    data object AddFifteenSeconds : RestTimerIntent
    data object SubtractFifteenSeconds : RestTimerIntent
}

/** No effects currently fire; the type exists so [StoreViewModel] can be parameterized. */
sealed interface RestTimerEffect : MviEffect

/** Minimal surface other stores need to trigger the rest timer, without depending on its full MVI API. */
interface RestTimerController {
    fun start(exerciseId: Long, seconds: Int)
}

/**
 * Drives the rest-timer countdown shown on the active-workout screen. The
 * countdown itself is a UI-only reflection recomputed from a wall-clock end
 * time (so it self-corrects after any delay/suspension); the actual
 * "reliably fires while backgrounded" guarantee comes from [notifier], which
 * hands the deadline off to the OS notification scheduler.
 */
class RestTimerStore(
    private val notifier: RestTimerNotifier,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val nowMillis: () -> Long = ::currentTimeMillis,
) : StoreViewModel<RestTimerState, RestTimerIntent, RestTimerEffect>(RestTimerState.Idle, dispatcher),
    RestTimerController {

    private var endEpochMillis: Long = 0L
    private var tickJob: Job? = null

    override fun onIntent(intent: RestTimerIntent) {
        when (intent) {
            is RestTimerIntent.Start -> start(intent.exerciseId, intent.seconds)
            RestTimerIntent.Skip -> stop()
            RestTimerIntent.AddFifteenSeconds -> adjust(ADJUST_STEP_SECONDS)
            RestTimerIntent.SubtractFifteenSeconds -> adjust(-ADJUST_STEP_SECONDS)
        }
    }

    override fun start(exerciseId: Long, seconds: Int) {
        val clamped = seconds.coerceAtLeast(1)
        endEpochMillis = nowMillis() + clamped * 1000L
        setState { RestTimerState.Running(exerciseId, totalSeconds = clamped, remainingSeconds = clamped) }
        notifier.scheduleEndNotification(clamped)
        startTicking()
    }

    private fun adjust(deltaSeconds: Int) {
        val running = state.value as? RestTimerState.Running ?: return
        val newRemaining = running.remainingSeconds + deltaSeconds
        if (newRemaining <= 0) {
            stop()
            return
        }
        endEpochMillis = nowMillis() + newRemaining * 1000L
        setState { running.copy(remainingSeconds = newRemaining, totalSeconds = maxOf(running.totalSeconds, newRemaining)) }
        notifier.scheduleEndNotification(newRemaining)
    }

    private fun stop() {
        tickJob?.cancel()
        tickJob = null
        notifier.cancel()
        setState { RestTimerState.Idle }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                val remainingMillis = endEpochMillis - nowMillis()
                if (remainingMillis <= 0) {
                    setState { RestTimerState.Idle }
                    tickJob = null
                    return@launch
                }
                val running = state.value as? RestTimerState.Running ?: return@launch
                val remainingSeconds = ((remainingMillis + 999) / 1000).toInt()
                setState { running.copy(remainingSeconds = remainingSeconds) }
                delay(1000)
            }
        }
    }
}
