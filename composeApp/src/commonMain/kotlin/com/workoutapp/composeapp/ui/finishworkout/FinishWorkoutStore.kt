package com.workoutapp.composeapp.ui.finishworkout

import com.workoutapp.composeapp.data.db.WorkoutPrivacy
import com.workoutapp.composeapp.data.db.currentTimeMillis
import com.workoutapp.composeapp.data.library.ExerciseRepository
import com.workoutapp.composeapp.data.profile.UserProfileRepository
import com.workoutapp.composeapp.data.progress.PersonalRecordRepository
import com.workoutapp.composeapp.data.workout.CompletedSetPerformance
import com.workoutapp.composeapp.data.workout.WorkoutExerciseRepository
import com.workoutapp.composeapp.data.workout.WorkoutRepository
import com.workoutapp.composeapp.data.workout.WorkoutSetRepository
import com.workoutapp.composeapp.data.workout.computeStreak
import com.workoutapp.composeapp.data.workout.dayIdFor
import com.workoutapp.composeapp.data.workout.personalRecordCandidates
import com.workoutapp.composeapp.mvi.MviEffect
import com.workoutapp.composeapp.mvi.MviIntent
import com.workoutapp.composeapp.mvi.MviState
import com.workoutapp.composeapp.mvi.StoreViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** One new personal record surfaced in the post-save summary. */
data class PersonalRecordHit(val exerciseName: String, val type: String, val value: Double)

/** Shown once [FinishWorkoutIntent.Save] completes; its presence switches the screen to the summary view. */
data class WorkoutSummary(
    val workoutCount: Long,
    val streak: Long,
    val personalRecords: List<PersonalRecordHit>,
)

data class FinishWorkoutState(
    val workoutId: Long,
    val startedAt: Long = 0L,
    val finishedAt: Long = 0L,
    val name: String = "",
    val note: String = "",
    val privacy: WorkoutPrivacy = WorkoutPrivacy.PRIVATE,
    val media: List<String> = emptyList(),
    val summary: WorkoutSummary? = null,
) : MviState {
    val durationMinutes: Long get() = ((finishedAt - startedAt) / 1000 / 60).coerceAtLeast(0)
}

sealed interface FinishWorkoutIntent : MviIntent {
    data class UpdateName(val value: String) : FinishWorkoutIntent
    data class UpdateNote(val value: String) : FinishWorkoutIntent
    data class UpdatePrivacy(val value: WorkoutPrivacy) : FinishWorkoutIntent
    data class UpdateDurationMinutes(val value: String) : FinishWorkoutIntent
    data class AddPhoto(val uri: String) : FinishWorkoutIntent
    data class RemovePhoto(val uri: String) : FinishWorkoutIntent
    data object Save : FinishWorkoutIntent
}

/** No effects currently fire; the type exists so [StoreViewModel] can be parameterized. */
sealed interface FinishWorkoutEffect : MviEffect

/**
 * Backs the finish/save screen for [workoutId]: an editable recap (name, duration, note, privacy,
 * photos) that on [FinishWorkoutIntent.Save] persists the workout, detects new personal records
 * (best weight / estimated 1RM / session volume per exercise), and computes the updated workout
 * count and day streak — surfaced afterward as [FinishWorkoutState.summary].
 */
class FinishWorkoutStore(
    private val workoutId: Long,
    private val workoutRepository: WorkoutRepository,
    private val workoutExerciseRepository: WorkoutExerciseRepository,
    private val workoutSetRepository: WorkoutSetRepository,
    private val exerciseRepository: ExerciseRepository,
    private val personalRecordRepository: PersonalRecordRepository,
    private val userProfileRepository: UserProfileRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : StoreViewModel<FinishWorkoutState, FinishWorkoutIntent, FinishWorkoutEffect>(
    FinishWorkoutState(workoutId = workoutId),
    dispatcher,
) {
    init {
        scope.launch {
            val workout = workoutRepository.getById(workoutId) ?: return@launch
            setState {
                it.copy(
                    startedAt = workout.startedAt,
                    finishedAt = currentTimeMillis().coerceAtLeast(workout.startedAt),
                    name = workout.name,
                    note = workout.note.orEmpty(),
                    privacy = workout.privacy,
                    media = workout.media,
                )
            }
        }
    }

    override fun onIntent(intent: FinishWorkoutIntent) {
        when (intent) {
            is FinishWorkoutIntent.UpdateName -> setState { it.copy(name = intent.value) }
            is FinishWorkoutIntent.UpdateNote -> setState { it.copy(note = intent.value) }
            is FinishWorkoutIntent.UpdatePrivacy -> setState { it.copy(privacy = intent.value) }
            is FinishWorkoutIntent.UpdateDurationMinutes -> updateDurationMinutes(intent.value)
            is FinishWorkoutIntent.AddPhoto -> setState { it.copy(media = it.media + intent.uri) }
            is FinishWorkoutIntent.RemovePhoto -> setState { it.copy(media = it.media - intent.uri) }
            FinishWorkoutIntent.Save -> save()
        }
    }

    private fun updateDurationMinutes(value: String) {
        val minutes = value.toLongOrNull() ?: return
        setState { it.copy(finishedAt = it.startedAt + minutes * 60_000) }
    }

    private fun save() {
        scope.launch {
            val now = currentTimeMillis()
            val current = state.value
            val finishedAt = current.finishedAt.coerceAtLeast(current.startedAt)

            val workoutExercises = workoutExerciseRepository.observeByWorkoutId(workoutId).first()
            val sets = workoutSetRepository.observeByWorkoutId(workoutId).first()
            val exerciseNames = exerciseRepository.observeAll().first().associateBy { it.id }
            val completedSetsByWorkoutExercise = sets.filter { it.completed }.groupBy { it.workoutExerciseId }

            val prHits = mutableListOf<PersonalRecordHit>()
            for (workoutExercise in workoutExercises) {
                val completedSets = completedSetsByWorkoutExercise[workoutExercise.id].orEmpty()
                    .map { CompletedSetPerformance(reps = it.reps, weight = it.weight) }
                for (candidate in personalRecordCandidates(workoutExercise.exerciseId, completedSets)) {
                    val existingBest = personalRecordRepository.getBestValue(candidate.exerciseId, candidate.type)
                    if (existingBest == null || candidate.value > existingBest) {
                        personalRecordRepository.add(
                            exerciseId = candidate.exerciseId,
                            type = candidate.type,
                            value = candidate.value,
                            workoutId = workoutId,
                            updatedAt = now,
                        )
                        prHits += PersonalRecordHit(
                            exerciseName = exerciseNames[candidate.exerciseId]?.name ?: "Unknown exercise",
                            type = candidate.type,
                            value = candidate.value,
                        )
                    }
                }
            }

            workoutRepository.update(
                id = workoutId,
                name = current.name,
                finishedAt = finishedAt,
                note = current.note.ifBlank { null },
                privacy = current.privacy,
                media = current.media,
                updatedAt = now,
            )

            val completedTimestamps = workoutRepository.getCompletedWorkoutFinishedAtTimestamps()
            val completedDays = completedTimestamps.map(::dayIdFor).toSet()
            val streak = computeStreak(completedDays, dayIdFor(finishedAt))
            val profile = userProfileRepository.getOrCreateLocalProfile(now)
            userProfileRepository.updateStreak(profile.id, streak, now)

            setState {
                it.copy(
                    summary = WorkoutSummary(
                        workoutCount = completedTimestamps.size.toLong(),
                        streak = streak,
                        personalRecords = prHits,
                    ),
                )
            }
        }
    }
}
