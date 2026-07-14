package com.workoutapp.composeapp.data.workout

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.workoutapp.composeapp.data.db.SetType
import com.workoutapp.composeapp.db.AppDatabase
import com.workoutapp.composeapp.db.WorkoutSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface WorkoutSetRepository {
    fun observeByWorkoutExerciseId(workoutExerciseId: Long): Flow<List<WorkoutSet>>

    fun observeByWorkoutId(workoutId: Long): Flow<List<WorkoutSet>>

    /** One-shot fetch of a workout exercise's sets, ordered by position. */
    suspend fun getByWorkoutExerciseId(workoutExerciseId: Long): List<WorkoutSet>

    suspend fun add(
        workoutExerciseId: Long,
        position: Long = 0,
        reps: Long? = null,
        weight: Double? = null,
        durationSec: Long? = null,
        setType: SetType = SetType.NORMAL,
        completed: Boolean = false,
        rpe: Double? = null,
        updatedAt: Long,
    )

    suspend fun update(
        id: Long,
        reps: Long?,
        weight: Double?,
        durationSec: Long?,
        setType: SetType,
        completed: Boolean,
        updatedAt: Long,
    )

    suspend fun updateRpe(id: Long, rpe: Double?)

    suspend fun updatePosition(id: Long, position: Long)

    suspend fun delete(id: Long)
}

class WorkoutSetRepositoryImpl(
    database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : WorkoutSetRepository {
    private val queries = database.workoutSetQueries

    override fun observeByWorkoutExerciseId(workoutExerciseId: Long): Flow<List<WorkoutSet>> =
        queries.selectByWorkoutExerciseId(workoutExerciseId).asFlow().mapToList(ioDispatcher)

    override fun observeByWorkoutId(workoutId: Long): Flow<List<WorkoutSet>> =
        queries.selectByWorkoutId(workoutId).asFlow().mapToList(ioDispatcher)

    override suspend fun getByWorkoutExerciseId(workoutExerciseId: Long): List<WorkoutSet> =
        withContext(ioDispatcher) {
            queries.selectByWorkoutExerciseId(workoutExerciseId).executeAsList()
        }

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
    ) = withContext(ioDispatcher) {
        queries.insert(
            workoutExerciseId = workoutExerciseId,
            position = position,
            reps = reps,
            weight = weight,
            durationSec = durationSec,
            setType = setType,
            completed = completed,
            rpe = rpe,
            serverId = null,
            updatedAt = updatedAt,
            syncStatus = "PENDING",
        )
    }

    override suspend fun update(
        id: Long,
        reps: Long?,
        weight: Double?,
        durationSec: Long?,
        setType: SetType,
        completed: Boolean,
        updatedAt: Long,
    ) = withContext(ioDispatcher) {
        queries.update(
            reps = reps,
            weight = weight,
            durationSec = durationSec,
            setType = setType,
            completed = completed,
            updatedAt = updatedAt,
            id = id,
        )
    }

    override suspend fun updateRpe(id: Long, rpe: Double?) = withContext(ioDispatcher) {
        queries.updateRpe(rpe, id)
    }

    override suspend fun updatePosition(id: Long, position: Long) = withContext(ioDispatcher) {
        queries.updatePosition(position, id)
    }

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        queries.deleteById(id)
    }
}
