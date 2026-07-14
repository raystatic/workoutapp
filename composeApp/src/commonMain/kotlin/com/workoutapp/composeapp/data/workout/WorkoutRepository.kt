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

    /** One-shot fetch, used to seed an editable form from the current row. */
    suspend fun getById(id: Long): Workout?

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

    /** Applies the finish/save form's edits to an existing workout. */
    suspend fun update(
        id: Long,
        name: String,
        finishedAt: Long?,
        note: String?,
        privacy: WorkoutPrivacy,
        media: List<String>,
        updatedAt: Long,
    )

    /** `finishedAt` of every completed workout, for streak/count computation. */
    suspend fun getCompletedWorkoutFinishedAtTimestamps(): List<Long>

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

    override suspend fun getById(id: Long): Workout? = withContext(ioDispatcher) {
        queries.selectById(id).executeAsOneOrNull()
    }

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

    override suspend fun update(
        id: Long,
        name: String,
        finishedAt: Long?,
        note: String?,
        privacy: WorkoutPrivacy,
        media: List<String>,
        updatedAt: Long,
    ) = withContext(ioDispatcher) {
        queries.update(
            name = name,
            finishedAt = finishedAt,
            note = note,
            privacy = privacy,
            media = media,
            updatedAt = updatedAt,
            id = id,
        )
    }

    override suspend fun getCompletedWorkoutFinishedAtTimestamps(): List<Long> = withContext(ioDispatcher) {
        queries.selectFinishedAtTimestamps().executeAsList().filterNotNull()
    }

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        queries.deleteById(id)
    }
}
