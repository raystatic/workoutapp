package com.workoutapp.composeapp.data.routines

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.workoutapp.composeapp.db.AppDatabase
import com.workoutapp.composeapp.db.Routine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface RoutineRepository {
    fun observeAll(): Flow<List<Routine>>

    fun observeById(id: Long): Flow<Routine?>

    /** Inserts the routine and returns its generated [Routine.id]. */
    suspend fun add(
        name: String,
        folderId: Long? = null,
        position: Long = 0,
        notes: String? = null,
        updatedAt: Long,
    ): Long

    suspend fun update(
        id: Long,
        name: String,
        folderId: Long?,
        notes: String?,
        updatedAt: Long,
    )

    suspend fun delete(id: Long)
}

class RoutineRepositoryImpl(
    database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : RoutineRepository {
    private val queries = database.routineQueries

    override fun observeAll(): Flow<List<Routine>> =
        queries.selectAll().asFlow().mapToList(ioDispatcher)

    override fun observeById(id: Long): Flow<Routine?> =
        queries.selectById(id).asFlow().mapToOneOrNull(ioDispatcher)

    override suspend fun add(
        name: String,
        folderId: Long?,
        position: Long,
        notes: String?,
        updatedAt: Long,
    ): Long = withContext(ioDispatcher) {
        queries.transactionWithResult {
            queries.insert(
                name = name,
                folderId = folderId,
                position = position,
                notes = notes,
                serverId = null,
                updatedAt = updatedAt,
                syncStatus = "PENDING",
            )
            queries.lastInsertRowId().executeAsOne()
        }
    }

    override suspend fun update(
        id: Long,
        name: String,
        folderId: Long?,
        notes: String?,
        updatedAt: Long,
    ) = withContext(ioDispatcher) {
        queries.update(name = name, folderId = folderId, notes = notes, updatedAt = updatedAt, id = id)
    }

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        queries.deleteById(id)
    }
}
