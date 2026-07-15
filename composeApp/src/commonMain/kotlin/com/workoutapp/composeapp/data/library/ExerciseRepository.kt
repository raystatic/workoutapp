package com.workoutapp.composeapp.data.library

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.workoutapp.composeapp.db.AppDatabase
import com.workoutapp.composeapp.db.Exercise
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface ExerciseRepository {
    fun observeAll(): Flow<List<Exercise>>

    /** A single exercise by id, or `null` if it doesn't exist (e.g. deleted). */
    fun observeById(id: Long): Flow<Exercise?>

    /** Distinct exercises used in any workout, most-recently-used first, capped at [limit]. */
    fun observeRecentlyUsed(limit: Int): Flow<List<Exercise>>

    suspend fun add(
        name: String,
        primaryMuscle: String,
        equipment: String,
        secondaryMuscles: List<String> = emptyList(),
        mediaUrl: String? = null,
        isCustom: Boolean = false,
        instructions: String? = null,
        updatedAt: Long,
    )

    suspend fun delete(id: Long)
}

class ExerciseRepositoryImpl(
    database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ExerciseRepository {
    private val queries = database.exerciseQueries

    override fun observeAll(): Flow<List<Exercise>> =
        queries.selectAll().asFlow().mapToList(ioDispatcher)

    override fun observeById(id: Long): Flow<Exercise?> =
        queries.selectById(id).asFlow().mapToOneOrNull(ioDispatcher)

    override fun observeRecentlyUsed(limit: Int): Flow<List<Exercise>> =
        queries.selectRecentlyUsed(limit.toLong()).asFlow().mapToList(ioDispatcher)

    override suspend fun add(
        name: String,
        primaryMuscle: String,
        equipment: String,
        secondaryMuscles: List<String>,
        mediaUrl: String?,
        isCustom: Boolean,
        instructions: String?,
        updatedAt: Long,
    ) = withContext(ioDispatcher) {
        queries.insert(
            name = name,
            primaryMuscle = primaryMuscle,
            secondaryMuscles = secondaryMuscles,
            equipment = equipment,
            mediaUrl = mediaUrl,
            isCustom = isCustom,
            instructions = instructions,
            serverId = null,
            updatedAt = updatedAt,
            syncStatus = "PENDING",
        )
    }

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        queries.deleteById(id)
    }
}
