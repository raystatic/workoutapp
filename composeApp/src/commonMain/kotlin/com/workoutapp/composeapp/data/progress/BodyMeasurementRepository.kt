package com.workoutapp.composeapp.data.progress

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.workoutapp.composeapp.db.AppDatabase
import com.workoutapp.composeapp.db.BodyMeasurement
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface BodyMeasurementRepository {
    fun observeAll(): Flow<List<BodyMeasurement>>

    suspend fun add(
        takenAt: Long,
        type: String,
        value: Double,
        photoUrl: String? = null,
        updatedAt: Long,
    )

    suspend fun delete(id: Long)
}

class BodyMeasurementRepositoryImpl(
    database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : BodyMeasurementRepository {
    private val queries = database.bodyMeasurementQueries

    override fun observeAll(): Flow<List<BodyMeasurement>> =
        queries.selectAll().asFlow().mapToList(ioDispatcher)

    override suspend fun add(
        takenAt: Long,
        type: String,
        value: Double,
        photoUrl: String?,
        updatedAt: Long,
    ) = withContext(ioDispatcher) {
        queries.insert(
            takenAt = takenAt,
            type = type,
            value = value,
            photoUrl = photoUrl,
            serverId = null,
            updatedAt = updatedAt,
            syncStatus = "PENDING",
        )
    }

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        queries.deleteById(id)
    }
}
