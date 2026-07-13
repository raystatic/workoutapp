package com.workoutapp.composeapp.data.progress

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.workoutapp.composeapp.db.AppDatabase
import com.workoutapp.composeapp.db.PersonalRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface PersonalRecordRepository {
    fun observeByExerciseId(exerciseId: Long): Flow<List<PersonalRecord>>

    suspend fun add(
        exerciseId: Long,
        type: String,
        value: Double,
        workoutId: Long,
        updatedAt: Long,
    )

    suspend fun delete(id: Long)
}

class PersonalRecordRepositoryImpl(
    database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : PersonalRecordRepository {
    private val queries = database.personalRecordQueries

    override fun observeByExerciseId(exerciseId: Long): Flow<List<PersonalRecord>> =
        queries.selectByExerciseId(exerciseId).asFlow().mapToList(ioDispatcher)

    override suspend fun add(
        exerciseId: Long,
        type: String,
        value: Double,
        workoutId: Long,
        updatedAt: Long,
    ) = withContext(ioDispatcher) {
        queries.insert(
            exerciseId = exerciseId,
            type = type,
            value = value,
            workoutId = workoutId,
            serverId = null,
            updatedAt = updatedAt,
            syncStatus = "PENDING",
        )
    }

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        queries.deleteById(id)
    }
}
