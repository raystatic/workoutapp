package com.workoutapp.composeapp.ui.customexercise

import com.workoutapp.composeapp.data.library.ExerciseRepository
import com.workoutapp.composeapp.db.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeExerciseRepository(customCount: Long = 0L) : ExerciseRepository {
    val customCountFlow = MutableStateFlow(customCount)
    val added = mutableListOf<Exercise>()

    override fun observeAll(): Flow<List<Exercise>> = MutableStateFlow(emptyList())
    override fun observeById(id: Long): Flow<Exercise?> = MutableStateFlow(null)
    override fun observeRecentlyUsed(limit: Int): Flow<List<Exercise>> = MutableStateFlow(emptyList())
    override fun observeCustomCount(): Flow<Long> = customCountFlow

    override suspend fun add(
        name: String,
        primaryMuscle: String,
        equipment: String,
        secondaryMuscles: List<String>,
        mediaUrl: String?,
        isCustom: Boolean,
        instructions: String?,
        updatedAt: Long,
        type: String?,
    ) {
        added += Exercise(
            added.size.toLong(),
            name,
            primaryMuscle,
            secondaryMuscles,
            equipment,
            mediaUrl,
            isCustom,
            instructions,
            null,
            updatedAt,
            "PENDING",
            type,
        )
        customCountFlow.value += 1
    }

    override suspend fun delete(id: Long) = Unit
}

class AddCustomExerciseStoreTest {
    private fun newStore(customCount: Long = 0L, repository: FakeExerciseRepository = FakeExerciseRepository(customCount)) =
        repository to AddCustomExerciseStore(repository, dispatcher = UnconfinedTestDispatcher())

    @Test
    fun initialState_belowCap_capNotReached() = runTest {
        val (_, store) = newStore(customCount = 3L)

        assertEquals(3L, store.state.value.customExerciseCount)
        assertFalse(store.state.value.capReached)
    }

    @Test
    fun initialState_atCap_capReached() = runTest {
        val (_, store) = newStore(customCount = 7L)

        assertTrue(store.state.value.capReached)
    }

    @Test
    fun updatingFields_reflectsInState() = runTest {
        val (_, store) = newStore()

        store.onIntent(AddCustomExerciseIntent.UpdateName("Band Pull-Apart"))
        store.onIntent(AddCustomExerciseIntent.UpdatePrimaryMuscle("Shoulders"))
        store.onIntent(AddCustomExerciseIntent.UpdateEquipment("Band"))
        store.onIntent(AddCustomExerciseIntent.UpdateType("Strength"))

        assertEquals("Band Pull-Apart", store.state.value.name)
        assertEquals("Shoulders", store.state.value.primaryMuscle)
        assertEquals("Band", store.state.value.equipment)
        assertEquals("Strength", store.state.value.type)
        assertTrue(store.state.value.isValid)
    }

    @Test
    fun save_withValidFields_insertsCustomExerciseAndMarksSaved() = runTest {
        val (repository, store) = newStore()

        store.onIntent(AddCustomExerciseIntent.UpdateName("Band Pull-Apart"))
        store.onIntent(AddCustomExerciseIntent.UpdatePrimaryMuscle("Shoulders"))
        store.onIntent(AddCustomExerciseIntent.UpdateSecondaryMuscles("Rear Delts, Traps"))
        store.onIntent(AddCustomExerciseIntent.UpdateEquipment("Band"))
        store.onIntent(AddCustomExerciseIntent.UpdateType("Strength"))
        store.onIntent(AddCustomExerciseIntent.Save)

        assertTrue(store.state.value.saved)
        val saved = repository.added.single()
        assertEquals("Band Pull-Apart", saved.name)
        assertEquals(listOf("Rear Delts", "Traps"), saved.secondaryMuscles)
        assertTrue(saved.isCustom)
        assertEquals("Strength", saved.type)
    }

    @Test
    fun save_withMissingRequiredField_doesNotInsertAndSetsError() = runTest {
        val (repository, store) = newStore()

        store.onIntent(AddCustomExerciseIntent.UpdateName("Band Pull-Apart"))
        // primaryMuscle, equipment, and type are left blank.
        store.onIntent(AddCustomExerciseIntent.Save)

        assertTrue(repository.added.isEmpty())
        assertFalse(store.state.value.saved)
        assertEquals("Name, primary muscle, equipment, and type are required.", store.state.value.error)
    }

    @Test
    fun save_atCap_blocksInsertWithUpsellMessage() = runTest {
        val (repository, store) = newStore(customCount = 7L)

        store.onIntent(AddCustomExerciseIntent.UpdateName("Band Pull-Apart"))
        store.onIntent(AddCustomExerciseIntent.UpdatePrimaryMuscle("Shoulders"))
        store.onIntent(AddCustomExerciseIntent.UpdateEquipment("Band"))
        store.onIntent(AddCustomExerciseIntent.UpdateType("Strength"))
        store.onIntent(AddCustomExerciseIntent.Save)

        assertTrue(repository.added.isEmpty())
        assertFalse(store.state.value.saved)
        assertEquals(CUSTOM_EXERCISE_CAP_MESSAGE, store.state.value.error)
    }

    @Test
    fun emptySecondaryMusclesInput_producesEmptyList() = runTest {
        val (repository, store) = newStore()

        store.onIntent(AddCustomExerciseIntent.UpdateName("Band Pull-Apart"))
        store.onIntent(AddCustomExerciseIntent.UpdatePrimaryMuscle("Shoulders"))
        store.onIntent(AddCustomExerciseIntent.UpdateEquipment("Band"))
        store.onIntent(AddCustomExerciseIntent.UpdateType("Strength"))
        store.onIntent(AddCustomExerciseIntent.Save)

        assertTrue(repository.added.single().secondaryMuscles.isEmpty())
        assertNull(repository.added.single().mediaUrl)
    }
}
