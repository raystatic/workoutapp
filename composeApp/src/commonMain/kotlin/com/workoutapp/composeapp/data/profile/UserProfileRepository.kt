package com.workoutapp.composeapp.data.profile

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.workoutapp.composeapp.db.AppDatabase
import com.workoutapp.composeapp.db.UserProfile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface UserProfileRepository {
    fun observeAll(): Flow<List<UserProfile>>

    suspend fun add(
        displayName: String,
        avatar: String? = null,
        isPublic: Boolean = false,
        streak: Long = 0,
        proUntil: Long? = null,
        updatedAt: Long,
    )

    suspend fun delete(id: Long)
}

class UserProfileRepositoryImpl(
    database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : UserProfileRepository {
    private val queries = database.userProfileQueries

    override fun observeAll(): Flow<List<UserProfile>> =
        queries.selectAll().asFlow().mapToList(ioDispatcher)

    override suspend fun add(
        displayName: String,
        avatar: String?,
        isPublic: Boolean,
        streak: Long,
        proUntil: Long?,
        updatedAt: Long,
    ) = withContext(ioDispatcher) {
        queries.insert(
            displayName = displayName,
            avatar = avatar,
            isPublic = isPublic,
            streak = streak,
            proUntil = proUntil,
            serverId = null,
            updatedAt = updatedAt,
            syncStatus = "PENDING",
        )
    }

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        queries.deleteById(id)
    }
}
