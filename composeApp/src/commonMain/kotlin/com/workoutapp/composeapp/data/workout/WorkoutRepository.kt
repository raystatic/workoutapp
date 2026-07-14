package com.workoutapp.composeapp.data.workout

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.workoutapp.composeapp.data.db.WorkoutPrivacy
import com.workoutapp.composeapp.db.AppDatabase
import com.workoutapp.composeapp.db.Workout
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface WorkoutRepository {
    fun observeAll(): Flow<List<Workout>>

    fun observeById(id: Long): Flow<Workout?>

    /** Inserts the workout and returns its generated [Workout.id]. */
    suspend fun add(
        name: String,
        startedAt: Long,
        finishedAt: Long? = null,
        note: String? = null,
        privacy: WorkoutPrivacy = WorkoutPrivacy.PRIVATE,
        media: List<String> = emptyList(),
        updatedAt: Long,
    ): Long

    suspend fun delete(id: Long)
}

class WorkoutRepositoryImpl(
    database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : WorkoutRepository {
    private val queries = database.workoutQueries

    override fun observeAll(): Flow<List<Workout>> =
        queries.selectAll().asFlow().mapToList(ioDispatcher)

    override fun observeById(id: Long): Flow<Workout?> =
        queries.selectById(id).asFlow().mapToOneOrNull(ioDispatcher)

    override suspend fun add(
        name: String,
        startedAt: Long,
        finishedAt: Long?,
        note: String?,
        privacy: WorkoutPrivacy,
        media: List<String>,
        updatedAt: Long,
    ): Long = withContext(ioDispatcher) {
        queries.transactionWithResult {
            queries.insert(
                name = name,
                startedAt = startedAt,
                finishedAt = finishedAt,
                note = note,
                privacy = privacy,
                media = media,
                serverId = null,
                updatedAt = updatedAt,
                syncStatus = "PENDING",
            )
            queries.lastInsertRowId().executeAsOne()
        }
    }

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        queries.deleteById(id)
    }
}
