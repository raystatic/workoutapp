package com.workoutapp.composeapp.data.routines

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.workoutapp.composeapp.db.AppDatabase
import com.workoutapp.composeapp.db.RoutineExercise
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface RoutineExerciseRepository {
    fun observeByRoutineId(routineId: Long): Flow<List<RoutineExercise>>

    suspend fun add(
        routineId: Long,
        exerciseId: Long,
        position: Long = 0,
        supersetGroup: String? = null,
        restSeconds: Long? = null,
        notes: String? = null,
        updatedAt: Long,
    )

    suspend fun delete(id: Long)
}

class RoutineExerciseRepositoryImpl(
    database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : RoutineExerciseRepository {
    private val queries = database.routineExerciseQueries

    override fun observeByRoutineId(routineId: Long): Flow<List<RoutineExercise>> =
        queries.selectByRoutineId(routineId).asFlow().mapToList(ioDispatcher)

    override suspend fun add(
        routineId: Long,
        exerciseId: Long,
        position: Long,
        supersetGroup: String?,
        restSeconds: Long?,
        notes: String?,
        updatedAt: Long,
    ) = withContext(ioDispatcher) {
        queries.insert(
            routineId = routineId,
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

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        queries.deleteById(id)
    }
}
