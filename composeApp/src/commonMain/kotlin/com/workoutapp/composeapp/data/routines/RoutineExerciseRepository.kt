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

    /** Inserts the routine exercise and returns its generated [RoutineExercise.id]. */
    suspend fun add(
        routineId: Long,
        exerciseId: Long,
        position: Long = 0,
        supersetGroup: String? = null,
        restSeconds: Long? = null,
        notes: String? = null,
        updatedAt: Long,
    ): Long

    suspend fun updatePosition(id: Long, position: Long)

    suspend fun updateSupersetGroup(id: Long, supersetGroup: String?)

    suspend fun updateRestSeconds(id: Long, restSeconds: Long?)

    suspend fun updateNotes(id: Long, notes: String?)

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
    ): Long = withContext(ioDispatcher) {
        queries.transactionWithResult {
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
            queries.lastInsertRowId().executeAsOne()
        }
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

    override suspend fun updateNotes(id: Long, notes: String?) = withContext(ioDispatcher) {
        queries.updateNotes(notes, id)
    }

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        queries.deleteById(id)
    }
}
