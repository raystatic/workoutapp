package com.workoutapp.composeapp.ui.workout

import com.workoutapp.composeapp.data.sample.SampleNoteRepository
import com.workoutapp.composeapp.db.SampleNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeSampleNoteRepository : SampleNoteRepository {
    private var nextId = 1L
    private val notes = MutableStateFlow<List<SampleNote>>(emptyList())

    override fun observeAll(): Flow<List<SampleNote>> = notes

    override suspend fun add(text: String) {
        notes.update { it + SampleNote(nextId++, text) }
    }

    override suspend fun delete(id: Long) {
        notes.update { list -> list.filterNot { it.id == id } }
    }
}

class WorkoutStoreTest {
    private fun newStore(repository: SampleNoteRepository = FakeSampleNoteRepository()) =
        WorkoutStore(repository, dispatcher = UnconfinedTestDispatcher())

    @Test
    fun draftChanged_updatesDraftText() {
        val store = newStore()

        store.onIntent(WorkoutIntent.DraftChanged("Squats"))

        assertEquals("Squats", store.state.value.draftText)
    }

    @Test
    fun addNote_appendsNoteAndClearsDraft() {
        val store = newStore()

        store.onIntent(WorkoutIntent.DraftChanged("Squats"))
        store.onIntent(WorkoutIntent.AddNote)

        assertEquals(listOf("Squats"), store.state.value.notes.map { it.text })
        assertEquals("", store.state.value.draftText)
    }

    @Test
    fun addNote_blankDraftIsIgnored() {
        val store = newStore()

        store.onIntent(WorkoutIntent.DraftChanged("   "))
        store.onIntent(WorkoutIntent.AddNote)

        assertTrue(store.state.value.notes.isEmpty())
    }

    @Test
    fun deleteNote_removesMatchingNote() {
        val store = newStore()
        store.onIntent(WorkoutIntent.DraftChanged("Squats"))
        store.onIntent(WorkoutIntent.AddNote)
        val id = store.state.value.notes.single().id

        store.onIntent(WorkoutIntent.DeleteNote(id))

        assertTrue(store.state.value.notes.isEmpty())
    }
}
