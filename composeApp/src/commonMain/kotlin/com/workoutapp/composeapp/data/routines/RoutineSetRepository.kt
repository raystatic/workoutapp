package com.workoutapp.composeapp.data.routines

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.workoutapp.composeapp.data.db.SetType
import com.workoutapp.composeapp.db.AppDatabase
import com.workoutapp.composeapp.db.RoutineSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface RoutineSetRepository {
    fun observeByRoutineExerciseId(routineExerciseId: Long): Flow<List<RoutineSet>>

    suspend fun add(
        routineExerciseId: Long,
        position: Long = 0,
        targetReps: Long? = null,
        targetWeight: Double? = null,
        setType: SetType = SetType.NORMAL,
        updatedAt: Long,
    )

    suspend fun delete(id: Long)
}

class RoutineSetRepositoryImpl(
    database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : RoutineSetRepository {
    private val queries = database.routineSetQueries

    override fun observeByRoutineExerciseId(routineExerciseId: Long): Flow<List<RoutineSet>> =
        queries.selectByRoutineExerciseId(routineExerciseId).asFlow().mapToList(ioDispatcher)

    override suspend fun add(
        routineExerciseId: Long,
        position: Long,
        targetReps: Long?,
        targetWeight: Double?,
        setType: SetType,
        updatedAt: Long,
    ) = withContext(ioDispatcher) {
        queries.insert(
            routineExerciseId = routineExerciseId,
            position = position,
            targetReps = targetReps,
            targetWeight = targetWeight,
            setType = setType,
            serverId = null,
            updatedAt = updatedAt,
            syncStatus = "PENDING",
        )
    }

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        queries.deleteById(id)
    }
}
