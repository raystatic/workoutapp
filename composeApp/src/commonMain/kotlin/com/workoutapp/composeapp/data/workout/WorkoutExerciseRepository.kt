package com.workoutapp.composeapp.data.workout

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.workoutapp.composeapp.db.AppDatabase
import com.workoutapp.composeapp.db.WorkoutExercise
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface WorkoutExerciseRepository {
    fun observeByWorkoutId(workoutId: Long): Flow<List<WorkoutExercise>>

    suspend fun add(
        workoutId: Long,
        exerciseId: Long,
        position: Long = 0,
        supersetGroup: String? = null,
        restSeconds: Long? = null,
        notes: String? = null,
        updatedAt: Long,
    )

    suspend fun updatePosition(id: Long, position: Long)

    suspend fun updateSupersetGroup(id: Long, supersetGroup: String?)

    suspend fun updateRestSeconds(id: Long, restSeconds: Long?)

    suspend fun delete(id: Long)

    /**
     * The id of the [WorkoutExercise] for [exerciseId] from the most recent
     * workout other than [excludingWorkoutId] that included it, or `null` if
     * there's no prior history for that exercise.
     */
    suspend fun findMostRecentOtherWorkoutExerciseId(exerciseId: Long, excludingWorkoutId: Long): Long?
}

class WorkoutExerciseRepositoryImpl(
    database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : WorkoutExerciseRepository {
    private val queries = database.workoutExerciseQueries

    override fun observeByWorkoutId(workoutId: Long): Flow<List<WorkoutExercise>> =
        queries.selectByWorkoutId(workoutId).asFlow().mapToList(ioDispatcher)

    override suspend fun add(
        workoutId: Long,
        exerciseId: Long,
        position: Long,
        supersetGroup: String?,
        restSeconds: Long?,
        notes: String?,
        updatedAt: Long,
    ) = withContext(ioDispatcher) {
        queries.insert(
            workoutId = workoutId,
            exerciseId = exerciseId,
            position = position,
            supersetGroup = supersetGroup,
            restSeconds = restSeconds,
            notes = notes,
            serverId = null,
            updatedAt = updatedAt,
            syncStatus = "PENDING",
        )
    }

    override suspend fun updatePosition(id: Long, position: Long) = withContext(ioDispatcher) {
        queries.updatePosition(position, id)
    }

    override suspend fun updateSupersetGroup(id: Long, supersetGroup: String?) = withContext(ioDispatcher) {
        queries.updateSupersetGroup(supersetGroup, id)
    }

    override suspend fun updateRestSeconds(id: Long, restSeconds: Long?) = withContext(ioDispatcher) {
        queries.updateRestSeconds(restSeconds, id)
    }

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        queries.deleteById(id)
    }

    override suspend fun findMostRecentOtherWorkoutExerciseId(exerciseId: Long, excludingWorkoutId: Long): Long? =
        withContext(ioDispatcher) {
            queries.selectMostRecentOtherWorkoutExerciseId(exerciseId, excludingWorkoutId).executeAsOneOrNull()
        }
}
