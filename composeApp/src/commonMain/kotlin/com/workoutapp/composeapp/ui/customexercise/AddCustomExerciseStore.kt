package com.workoutapp.composeapp.ui.customexercise

import com.workoutapp.composeapp.data.db.currentTimeMillis
import com.workoutapp.composeapp.data.library.ExerciseRepository
import com.workoutapp.composeapp.mvi.MviEffect
import com.workoutapp.composeapp.mvi.MviIntent
import com.workoutapp.composeapp.mvi.MviState
import com.workoutapp.composeapp.mvi.StoreViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Free-tier cap on user-created exercises (#22); the [CUSTOM_EXERCISE_FREE_LIMIT]+1th creation is blocked. */
const val CUSTOM_EXERCISE_FREE_LIMIT = 7

const val CUSTOM_EXERCISE_CAP_MESSAGE =
    "You've reached the free limit of $CUSTOM_EXERCISE_FREE_LIMIT custom exercises. Upgrade to Pro for unlimited custom exercises."

/** Fixed set of exercise types offered when creating a custom exercise. */
val CUSTOM_EXERCISE_TYPES = listOf("Strength", "Cardio", "Bodyweight", "Mobility")

data class AddCustomExerciseState(
    val name: String = "",
    val primaryMuscle: String = "",
    val secondaryMusclesInput: String = "",
    val equipment: String = "",
    val type: String = "",
    val mediaUrl: String = "",
    val instructions: String = "",
    val customExerciseCount: Long = 0L,
    val saved: Boolean = false,
    val error: String? = null,
) : MviState {
    val capReached: Boolean get() = customExerciseCount >= CUSTOM_EXERCISE_FREE_LIMIT
    val isValid: Boolean get() = name.isNotBlank() && primaryMuscle.isNotBlank() && equipment.isNotBlank() && type.isNotBlank()
}

sealed interface AddCustomExerciseIntent : MviIntent {
    data class UpdateName(val value: String) : AddCustomExerciseIntent
    data class UpdatePrimaryMuscle(val value: String) : AddCustomExerciseIntent
    data class UpdateSecondaryMuscles(val value: String) : AddCustomExerciseIntent
    data class UpdateEquipment(val value: String) : AddCustomExerciseIntent
    data class UpdateType(val value: String) : AddCustomExerciseIntent
    data class UpdateMediaUrl(val value: String) : AddCustomExerciseIntent
    data class UpdateInstructions(val value: String) : AddCustomExerciseIntent
    data object Save : AddCustomExerciseIntent
}

/** No effects currently fire; the type exists so [StoreViewModel] can be parameterized. */
sealed interface AddCustomExerciseEffect : MviEffect

/**
 * Backs the "Add Custom Exercise" screen (#22): name, muscles, equipment, type, and an optional
 * media URL, gated by the free-tier cap of [CUSTOM_EXERCISE_FREE_LIMIT] user-created exercises.
 * On [AddCustomExerciseIntent.Save] inserts a new [com.workoutapp.composeapp.db.Exercise] with
 * `isCustom = true`, which then appears in the shared library/search like any other exercise.
 */
class AddCustomExerciseStore(
    private val exerciseRepository: ExerciseRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : StoreViewModel<AddCustomExerciseState, AddCustomExerciseIntent, AddCustomExerciseEffect>(
    AddCustomExerciseState(),
    dispatcher,
) {
    init {
        exerciseRepository.observeCustomCount()
            .onEach { count -> setState { it.copy(customExerciseCount = count) } }
            .launchIn(scope)
    }

    override fun onIntent(intent: AddCustomExerciseIntent) {
        when (intent) {
            is AddCustomExerciseIntent.UpdateName -> setState { it.copy(name = intent.value, error = null) }
            is AddCustomExerciseIntent.UpdatePrimaryMuscle -> setState { it.copy(primaryMuscle = intent.value, error = null) }
            is AddCustomExerciseIntent.UpdateSecondaryMuscles -> setState { it.copy(secondaryMusclesInput = intent.value) }
            is AddCustomExerciseIntent.UpdateEquipment -> setState { it.copy(equipment = intent.value, error = null) }
            is AddCustomExerciseIntent.UpdateType -> setState { it.copy(type = intent.value, error = null) }
            is AddCustomExerciseIntent.UpdateMediaUrl -> setState { it.copy(mediaUrl = intent.value) }
            is AddCustomExerciseIntent.UpdateInstructions -> setState { it.copy(instructions = intent.value) }
            AddCustomExerciseIntent.Save -> save()
        }
    }

    private fun save() {
        val current = state.value
        if (current.capReached) {
            setState { it.copy(error = CUSTOM_EXERCISE_CAP_MESSAGE) }
            return
        }
        if (!current.isValid) {
            setState { it.copy(error = "Name, primary muscle, equipment, and type are required.") }
            return
        }
        scope.launch {
            val secondaryMuscles = current.secondaryMusclesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            exerciseRepository.add(
                name = current.name.trim(),
                primaryMuscle = current.primaryMuscle.trim(),
                equipment = current.equipment.trim(),
                secondaryMuscles = secondaryMuscles,
                mediaUrl = current.mediaUrl.trim().ifBlank { null },
                isCustom = true,
                instructions = current.instructions.trim().ifBlank { null },
                updatedAt = currentTimeMillis(),
                type = current.type,
            )
            setState { it.copy(saved = true) }
        }
    }
}
