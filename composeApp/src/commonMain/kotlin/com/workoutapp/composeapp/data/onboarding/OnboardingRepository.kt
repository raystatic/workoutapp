package com.workoutapp.composeapp.data.onboarding

import com.workoutapp.composeapp.db.AppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface OnboardingRepository {
    suspend fun hasSeenWalkthrough(): Boolean

    suspend fun setHasSeenWalkthrough(seen: Boolean)
}

class OnboardingRepositoryImpl(
    database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : OnboardingRepository {
    private val queries = database.appSettingQueries

    override suspend fun hasSeenWalkthrough(): Boolean = withContext(ioDispatcher) {
        queries.selectByKey(HAS_SEEN_WALKTHROUGH_KEY).executeAsOneOrNull()?.toBoolean() ?: false
    }

    override suspend fun setHasSeenWalkthrough(seen: Boolean) = withContext(ioDispatcher) {
        queries.upsert(HAS_SEEN_WALKTHROUGH_KEY, seen.toString())
    }

    companion object {
        private const val HAS_SEEN_WALKTHROUGH_KEY = "has_seen_walkthrough"
    }
}
