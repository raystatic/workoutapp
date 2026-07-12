package com.workoutapp.composeapp.mvi

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base for a screen's MVI store: holds [state], dispatches one-off [effects],
 * and reduces incoming [MviIntent]s via [onIntent]. Platform-independent —
 * not tied to any androidx ViewModel type so it works identically on both
 * Android and iOS.
 */
abstract class StoreViewModel<S : MviState, I : MviIntent, E : MviEffect>(
    initialState: S,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    protected val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = Channel<E>(Channel.BUFFERED)
    val effects: Flow<E> = _effects.receiveAsFlow()

    abstract fun onIntent(intent: I)

    protected fun setState(reducer: (S) -> S) {
        _state.update(reducer)
    }

    protected fun sendEffect(effect: E) {
        scope.launch { _effects.send(effect) }
    }

    open fun clear() {
        scope.cancel()
    }
}
