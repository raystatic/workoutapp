package com.workoutapp.composeapp.data.resttimer

import com.workoutapp.composeapp.db.AppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface RestTimerSettingsRepository {
    suspend fun getDefaultRestSeconds(): Int

    suspend fun setDefaultRestSeconds(seconds: Int)
}

class RestTimerSettingsRepositoryImpl(
    database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : RestTimerSettingsRepository {
    private val queries = database.appSettingQueries

    override suspend fun getDefaultRestSeconds(): Int = withContext(ioDispatcher) {
        queries.selectByKey(DEFAULT_REST_SECONDS_KEY).executeAsOneOrNull()?.toIntOrNull()
            ?: DEFAULT_REST_SECONDS
    }

    override suspend fun setDefaultRestSeconds(seconds: Int) = withContext(ioDispatcher) {
        queries.upsert(DEFAULT_REST_SECONDS_KEY, seconds.coerceAtLeast(1).toString())
    }

    companion object {
        const val DEFAULT_REST_SECONDS = 90
        private const val DEFAULT_REST_SECONDS_KEY = "default_rest_seconds"
    }
}

/** [exerciseOverrideSeconds] wins whenever it's a positive value; otherwise falls back to [defaultSeconds]. */
fun resolveRestSeconds(exerciseOverrideSeconds: Long?, defaultSeconds: Int): Int =
    exerciseOverrideSeconds?.toInt()?.takeIf { it > 0 } ?: defaultSeconds
