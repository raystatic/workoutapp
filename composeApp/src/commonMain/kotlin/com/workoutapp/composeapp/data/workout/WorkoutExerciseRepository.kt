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
        notes: String? = null,
        updatedAt: Long,
    )

    suspend fun delete(id: Long)
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
        notes: String?,
        updatedAt: Long,
    ) = withContext(ioDispatcher) {
        queries.insert(
            workoutId = workoutId,
            exerciseId = exerciseId,
            position = position,
            supersetGroup = supersetGroup,
            notes = notes,
            serverId = null,
            updatedAt = updatedAt,
            syncStatus = "PENDING",
        )
    }

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        queries.deleteById(id)
    }
}
