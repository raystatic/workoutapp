package com.workoutapp.composeapp.ui.finishworkout

import com.workoutapp.composeapp.data.db.SetType
import com.workoutapp.composeapp.data.db.WorkoutPrivacy
import com.workoutapp.composeapp.data.library.ExerciseRepository
import com.workoutapp.composeapp.data.profile.UserProfileRepository
import com.workoutapp.composeapp.data.progress.PersonalRecordRepository
import com.workoutapp.composeapp.data.workout.PersonalRecordCandidate
import com.workoutapp.composeapp.data.workout.PersonalRecordType
import com.workoutapp.composeapp.data.workout.WorkoutExerciseRepository
import com.workoutapp.composeapp.data.workout.WorkoutRepository
import com.workoutapp.composeapp.data.workout.WorkoutSetRepository
import com.workoutapp.composeapp.db.Exercise
import com.workoutapp.composeapp.db.PersonalRecord
import com.workoutapp.composeapp.db.UserProfile
import com.workoutapp.composeapp.db.Workout
import com.workoutapp.composeapp.db.WorkoutExercise
import com.workoutapp.composeapp.db.WorkoutSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun workout(
    id: Long = 1L,
    startedAt: Long = 1_000L,
    name: String = "Workout",
    note: String? = null,
    privacy: WorkoutPrivacy = WorkoutPrivacy.PRIVATE,
    media: List<String> = emptyList(),
) = Workout(id, name, startedAt, null, note, privacy, media, null, startedAt, "PENDING")

private fun exercise(id: Long, name: String) =
    Exercise(id, name, "Chest", emptyList(), "Barbell", null, false, null, null, 1_000L, "PENDING", null)

private fun workoutExercise(id: Long, workoutId: Long, exerciseId: Long, position: Long) =
    WorkoutExercise(id, workoutId, exerciseId, position, null, null, null, null, 1_000L, "PENDING")

private fun workoutSet(
    id: Long,
    workoutExerciseId: Long,
    position: Long,
    reps: Long? = null,
    weight: Double? = null,
    completed: Boolean = true,
) = WorkoutSet(id, workoutExerciseId, position, reps, weight, null, SetType.NORMAL, completed, null, null, 1_000L, "PENDING")

private class FakeWorkoutRepository(
    private val workout: Workout?,
    seedCompletedTimestamps: List<Long> = emptyList(),
) : WorkoutRepository {
    data class UpdateCall(
        val id: Long,
        val name: String,
        val finishedAt: Long?,
        val note: String?,
        val privacy: WorkoutPrivacy,
        val media: List<String>,
    )

    val updateCalls = mutableListOf<UpdateCall>()
    private val completedTimestamps = seedCompletedTimestamps.toMutableList()

    override fun observeAll(): Flow<List<Workout>> = MutableStateFlow(listOfNotNull(workout))
    override fun observeById(id: Long): Flow<Workout?> = MutableStateFlow(workout)
    override suspend fun getById(id: Long): Workout? = workout

    override suspend fun add(
        name: String,
        startedAt: Long,
        finishedAt: Long?,
        note: String?,
        privacy: WorkoutPrivacy,
        media: List<String>,
        updatedAt: Long,
    ): Long = error("not needed for these tests")

    override suspend fun update(
        id: Long,
        name: String,
        finishedAt: Long?,
        note: String?,
        privacy: WorkoutPrivacy,
        media: List<String>,
        updatedAt: Long,
    ) {
        updateCalls += UpdateCall(id, name, finishedAt, note, privacy, media)
        if (finishedAt != null) completedTimestamps += finishedAt
    }

    override suspend fun getCompletedWorkoutFinishedAtTimestamps(): List<Long> = completedTimestamps

    override suspend fun delete(id: Long) = Unit
}

private class FakeWorkoutExerciseRepository(private val seed: List<WorkoutExercise>) : WorkoutExerciseRepository {
    override fun observeByWorkoutId(workoutId: Long): Flow<List<WorkoutExercise>> =
        MutableStateFlow(seed.filter { it.workoutId == workoutId })

    override suspend fun add(
        workoutId: Long,
        exerciseId: Long,
        position: Long,
        supersetGroup: String?,
        restSeconds: Long?,
        notes: String?,
        updatedAt: Long,
    ) = error("not needed for these tests")

    override suspend fun updatePosition(id: Long, position: Long) = error("not needed for these tests")
    override suspend fun updateSupersetGroup(id: Long, supersetGroup: String?) = error("not needed for these tests")
    override suspend fun updateRestSeconds(id: Long, restSeconds: Long?) = error("not needed for these tests")
    override suspend fun delete(id: Long) = Unit
    override suspend fun findMostRecentOtherWorkoutExerciseId(exerciseId: Long, excludingWorkoutId: Long): Long? = null
}

private class FakeWorkoutSetRepository(private val seed: List<WorkoutSet>) : WorkoutSetRepository {
    override fun observeByWorkoutExerciseId(workoutExerciseId: Long): Flow<List<WorkoutSet>> =
        MutableStateFlow(seed.filter { it.workoutExerciseId == workoutExerciseId })

    override fun observeByWorkoutId(workoutId: Long): Flow<List<WorkoutSet>> = MutableStateFlow(seed)

    override suspend fun getByWorkoutExerciseId(workoutExerciseId: Long): List<WorkoutSet> =
        seed.filter { it.workoutExerciseId == workoutExerciseId }

    override suspend fun add(
        workoutExerciseId: Long,
        position: Long,
        reps: Long?,
        weight: Double?,
        durationSec: Long?,
        setType: SetType,
        completed: Boolean,
        rpe: Double?,
        updatedAt: Long,
    ) = error("not needed for these tests")

    override suspend fun update(
        id: Long,
        reps: Long?,
        weight: Double?,
        durationSec: Long?,
        setType: SetType,
        completed: Boolean,
        updatedAt: Long,
    ) = error("not needed for these tests")

    override suspend fun updateRpe(id: Long, rpe: Double?) = error("not needed for these tests")
    override suspend fun updatePosition(id: Long, position: Long) = error("not needed for these tests")
    override suspend fun delete(id: Long) = Unit
}

private class FakeExerciseRepository(private val seed: List<Exercise>) : ExerciseRepository {
    override fun observeAll(): Flow<List<Exercise>> = MutableStateFlow(seed)

    override suspend fun add(
        name: String,
        primaryMuscle: String,
        equipment: String,
        secondaryMuscles: List<String>,
        mediaUrl: String?,
        isCustom: Boolean,
        instructions: String?,
        updatedAt: Long,
    ) = error("not needed for these tests")

    override suspend fun delete(id: Long) = Unit
}

private class FakePersonalRecordRepository(seedBests: Map<Pair<Long, String>, Double> = emptyMap()) : PersonalRecordRepository {
    private val bests = seedBests.toMutableMap()
    val added = mutableListOf<PersonalRecordCandidate>()

    override fun observeByExerciseId(exerciseId: Long): Flow<List<PersonalRecord>> = MutableStateFlow(emptyList())
    override suspend fun getBestValue(exerciseId: Long, type: String): Double? = bests[exerciseId to type]

    override suspend fun add(exerciseId: Long, type: String, value: Double, workoutId: Long, updatedAt: Long) {
        added += PersonalRecordCandidate(exerciseId, type, value)
        bests[exerciseId to type] = value
    }

    override suspend fun delete(id: Long) = Unit
}

private class FakeUserProfileRepository(private var profile: UserProfile? = null) : UserProfileRepository {
    val streakUpdates = mutableListOf<Long>()

    override fun observeAll(): Flow<List<UserProfile>> = MutableStateFlow(listOfNotNull(profile))

    override suspend fun getOrCreateLocalProfile(updatedAt: Long): UserProfile =
        profile ?: UserProfile(1L, "You", null, false, 0L, null, null, updatedAt, "PENDING").also { profile = it }

    override suspend fun updateStreak(id: Long, streak: Long, updatedAt: Long) {
        streakUpdates += streak
        profile = profile?.copy(streak = streak)
    }

    override suspend fun add(
        displayName: String,
        avatar: String?,
        isPublic: Boolean,
        streak: Long,
        proUntil: Long?,
        updatedAt: Long,
    ) = error("not needed for these tests")

    override suspend fun delete(id: Long) = Unit
}

class FinishWorkoutStoreTest {
    private fun newStore(
        workout: Workout? = workout(),
        workoutExercises: List<WorkoutExercise> = emptyList(),
        sets: List<WorkoutSet> = emptyList(),
        exercises: List<Exercise> = emptyList(),
        personalRecordRepository: FakePersonalRecordRepository = FakePersonalRecordRepository(),
        userProfileRepository: FakeUserProfileRepository = FakeUserProfileRepository(),
        workoutRepository: FakeWorkoutRepository = FakeWorkoutRepository(workout),
    ) = FinishWorkoutStore(
        workoutId = workout?.id ?: 1L,
        workoutRepository = workoutRepository,
        workoutExerciseRepository = FakeWorkoutExerciseRepository(workoutExercises),
        workoutSetRepository = FakeWorkoutSetRepository(sets),
        exerciseRepository = FakeExerciseRepository(exercises),
        personalRecordRepository = personalRecordRepository,
        userProfileRepository = userProfileRepository,
        dispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun initialState_loadsEditableFieldsFromTheWorkout() = runTest {
        val store = newStore(
            workout = workout(startedAt = 1_000L, name = "Leg Day", note = "felt strong", privacy = WorkoutPrivacy.FRIENDS, media = listOf("a.jpg")),
        )

        assertEquals(1_000L, store.state.value.startedAt)
        assertEquals("Leg Day", store.state.value.name)
        assertEquals("felt strong", store.state.value.note)
        assertEquals(WorkoutPrivacy.FRIENDS, store.state.value.privacy)
        assertEquals(listOf("a.jpg"), store.state.value.media)
    }

    @Test
    fun updateName_updatesState() = runTest {
        val store = newStore()

        store.onIntent(FinishWorkoutIntent.UpdateName("New Name"))

        assertEquals("New Name", store.state.value.name)
    }

    @Test
    fun updateNote_updatesState() = runTest {
        val store = newStore()

        store.onIntent(FinishWorkoutIntent.UpdateNote("Great session"))

        assertEquals("Great session", store.state.value.note)
    }

    @Test
    fun updatePrivacy_updatesState() = runTest {
        val store = newStore()

        store.onIntent(FinishWorkoutIntent.UpdatePrivacy(WorkoutPrivacy.PUBLIC))

        assertEquals(WorkoutPrivacy.PUBLIC, store.state.value.privacy)
    }

    @Test
    fun addThenRemovePhoto_updatesMediaList() = runTest {
        val store = newStore()

        store.onIntent(FinishWorkoutIntent.AddPhoto("photo1.jpg"))
        store.onIntent(FinishWorkoutIntent.AddPhoto("photo2.jpg"))
        assertEquals(listOf("photo1.jpg", "photo2.jpg"), store.state.value.media)

        store.onIntent(FinishWorkoutIntent.RemovePhoto("photo1.jpg"))
        assertEquals(listOf("photo2.jpg"), store.state.value.media)
    }

    @Test
    fun updateDurationMinutes_recomputesFinishedAtFromStartedAt() = runTest {
        val store = newStore(workout = workout(startedAt = 1_000L))

        store.onIntent(FinishWorkoutIntent.UpdateDurationMinutes("60"))

        assertEquals(1_000L + 60 * 60_000L, store.state.value.finishedAt)
        assertEquals(60L, store.state.value.durationMinutes)
    }

    @Test
    fun save_persistsEditedFieldsAndComputedFinishedAt() = runTest {
        val workoutRepository = FakeWorkoutRepository(workout(startedAt = 1_000L))
        val store = newStore(workout = workout(startedAt = 1_000L), workoutRepository = workoutRepository)
        store.onIntent(FinishWorkoutIntent.UpdateName("Renamed"))
        store.onIntent(FinishWorkoutIntent.UpdateNote("note"))
        store.onIntent(FinishWorkoutIntent.UpdatePrivacy(WorkoutPrivacy.PUBLIC))
        store.onIntent(FinishWorkoutIntent.UpdateDurationMinutes("30"))

        store.onIntent(FinishWorkoutIntent.Save)

        val call = workoutRepository.updateCalls.single()
        assertEquals("Renamed", call.name)
        assertEquals("note", call.note)
        assertEquals(WorkoutPrivacy.PUBLIC, call.privacy)
        assertEquals(1_000L + 30 * 60_000L, call.finishedAt)
    }

    @Test
    fun save_withNoPriorRecord_detectsAllThreePersonalRecordTypes() = runTest {
        val personalRecordRepository = FakePersonalRecordRepository()
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, 0L)),
            sets = listOf(workoutSet(50L, 10L, 0L, reps = 10L, weight = 100.0, completed = true)),
            exercises = listOf(exercise(100L, "Bench Press")),
            personalRecordRepository = personalRecordRepository,
        )

        store.onIntent(FinishWorkoutIntent.Save)

        assertEquals(3, personalRecordRepository.added.size)
        val summary = store.state.value.summary
        assertEquals(3, summary?.personalRecords?.size)
        assertTrue(summary?.personalRecords.orEmpty().all { it.exerciseName == "Bench Press" })
    }

    @Test
    fun save_whenNotBeatingPriorBest_recordsNoNewPersonalRecords() = runTest {
        val personalRecordRepository = FakePersonalRecordRepository(
            seedBests = mapOf(
                (100L to PersonalRecordType.MAX_WEIGHT) to 150.0,
                (100L to PersonalRecordType.ONE_REP_MAX) to 200.0,
                (100L to PersonalRecordType.BEST_VOLUME) to 5000.0,
            ),
        )
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, 0L)),
            sets = listOf(workoutSet(50L, 10L, 0L, reps = 10L, weight = 100.0, completed = true)),
            exercises = listOf(exercise(100L, "Bench Press")),
            personalRecordRepository = personalRecordRepository,
        )

        store.onIntent(FinishWorkoutIntent.Save)

        assertTrue(personalRecordRepository.added.isEmpty())
        assertTrue(store.state.value.summary?.personalRecords.orEmpty().isEmpty())
    }

    @Test
    fun save_whenBeatingPriorBest_recordsNewPersonalRecord() = runTest {
        val personalRecordRepository = FakePersonalRecordRepository(
            seedBests = mapOf((100L to PersonalRecordType.MAX_WEIGHT) to 10.0),
        )
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, 0L)),
            sets = listOf(workoutSet(50L, 10L, 0L, reps = 10L, weight = 100.0, completed = true)),
            exercises = listOf(exercise(100L, "Bench Press")),
            personalRecordRepository = personalRecordRepository,
        )

        store.onIntent(FinishWorkoutIntent.Save)

        assertTrue(personalRecordRepository.added.any { it.type == PersonalRecordType.MAX_WEIGHT && it.value == 100.0 })
    }

    @Test
    fun save_ignoresIncompleteSetsForPersonalRecordDetection() = runTest {
        val personalRecordRepository = FakePersonalRecordRepository()
        val store = newStore(
            workoutExercises = listOf(workoutExercise(10L, 1L, 100L, 0L)),
            sets = listOf(workoutSet(50L, 10L, 0L, reps = 10L, weight = 100.0, completed = false)),
            exercises = listOf(exercise(100L, "Bench Press")),
            personalRecordRepository = personalRecordRepository,
        )

        store.onIntent(FinishWorkoutIntent.Save)

        assertTrue(personalRecordRepository.added.isEmpty())
    }

    @Test
    fun save_computesWorkoutCountAndStreakFromCompletedHistory() = runTest {
        val day = 20L
        val dayMillis = day * 86_400_000L
        val workoutRepository = FakeWorkoutRepository(
            workout = workout(startedAt = dayMillis),
            seedCompletedTimestamps = listOf(dayMillis - 86_400_000L, dayMillis - 2 * 86_400_000L),
        )
        val store = newStore(workout = workout(startedAt = dayMillis), workoutRepository = workoutRepository)
        store.onIntent(FinishWorkoutIntent.UpdateDurationMinutes("0"))

        store.onIntent(FinishWorkoutIntent.Save)

        val summary = store.state.value.summary
        assertEquals(3L, summary?.workoutCount)
        assertEquals(3L, summary?.streak)
    }

    @Test
    fun save_withAGapInHistory_streakOnlyCountsConsecutiveDays() = runTest {
        val day = 20L
        val dayMillis = day * 86_400_000L
        val workoutRepository = FakeWorkoutRepository(
            workout = workout(startedAt = dayMillis),
            seedCompletedTimestamps = listOf(dayMillis - 2 * 86_400_000L),
        )
        val store = newStore(workout = workout(startedAt = dayMillis), workoutRepository = workoutRepository)
        store.onIntent(FinishWorkoutIntent.UpdateDurationMinutes("0"))

        store.onIntent(FinishWorkoutIntent.Save)

        val summary = store.state.value.summary
        assertEquals(2L, summary?.workoutCount)
        assertEquals(1L, summary?.streak)
    }

    @Test
    fun save_persistsTheComputedStreakToTheLocalUserProfile() = runTest {
        val day = 20L
        val dayMillis = day * 86_400_000L
        val userProfileRepository = FakeUserProfileRepository()
        val store = newStore(
            workout = workout(startedAt = dayMillis),
            workoutRepository = FakeWorkoutRepository(workout(startedAt = dayMillis)),
            userProfileRepository = userProfileRepository,
        )
        store.onIntent(FinishWorkoutIntent.UpdateDurationMinutes("0"))

        store.onIntent(FinishWorkoutIntent.Save)

        assertEquals(listOf(1L), userProfileRepository.streakUpdates)
    }
}
