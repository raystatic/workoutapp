package com.workoutapp.composeapp.ui.workout

import com.workoutapp.composeapp.data.sample.SampleNoteRepository
import com.workoutapp.composeapp.db.SampleNote
import com.workoutapp.composeapp.mvi.MviEffect
import com.workoutapp.composeapp.mvi.MviIntent
import com.workoutapp.composeapp.mvi.MviState
import com.workoutapp.composeapp.mvi.StoreViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class WorkoutState(
    val notes: List<SampleNote> = emptyList(),
    val draftText: String = "",
) : MviState

sealed interface WorkoutIntent : MviIntent {
    data class DraftChanged(val text: String) : WorkoutIntent
    data object AddNote : WorkoutIntent
    data class DeleteNote(val id: Long) : WorkoutIntent
}

sealed interface WorkoutEffect : MviEffect

/**
 * Demonstrates the full plumbing this issue wires up: a Koin-provided
 * repository backed by SQLDelight, observed reactively via Flow, driven by
 * MVI intents. Real logging screens replace this content in later issues.
 */
class WorkoutStore(
    private val repository: SampleNoteRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : StoreViewModel<WorkoutState, WorkoutIntent, WorkoutEffect>(WorkoutState(), dispatcher) {

    init {
        repository.observeAll()
            .onEach { notes -> setState { it.copy(notes = notes) } }
            .launchIn(scope)
    }

    override fun onIntent(intent: WorkoutIntent) {
        when (intent) {
            is WorkoutIntent.DraftChanged -> setState { it.copy(draftText = intent.text) }
            WorkoutIntent.AddNote -> addNote()
            is WorkoutIntent.DeleteNote -> deleteNote(intent.id)
        }
    }

    private fun addNote() {
        val text = state.value.draftText.trim()
        if (text.isEmpty()) return
        scope.launch {
            repository.add(text)
            setState { it.copy(draftText = "") }
        }
    }

    private fun deleteNote(id: Long) {
        scope.launch { repository.delete(id) }
    }
}
