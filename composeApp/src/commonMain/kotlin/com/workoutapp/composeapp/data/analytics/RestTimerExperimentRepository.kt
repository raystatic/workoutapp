package com.workoutapp.composeapp.data.analytics

import com.workoutapp.composeapp.db.AppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Variant bucket for the "default rest timer ON vs OFF" A/B test (EXECUTION_PLAN.md §6). */
enum class RestTimerDefaultVariant { ON, OFF }

interface RestTimerExperimentRepository {
    /** Sticky per-install assignment: assigned once on first read, then always returned unchanged. */
    suspend fun getVariant(): RestTimerDefaultVariant
}

class RestTimerExperimentRepositoryImpl(
    database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val randomValue: () -> Double = { Random.nextDouble() },
) : RestTimerExperimentRepository {
    private val queries = database.appSettingQueries

    override suspend fun getVariant(): RestTimerDefaultVariant = withContext(ioDispatcher) {
        val stored = queries.selectByKey(EXPERIMENT_KEY).executeAsOneOrNull()
        if (stored != null) return@withContext RestTimerDefaultVariant.valueOf(stored)
        val assigned = assignVariant(randomValue())
        queries.upsert(EXPERIMENT_KEY, assigned.name)
        assigned
    }

    companion object {
        private const val EXPERIMENT_KEY = "experiment_rest_timer_default"
    }
}

/** Pure 50/50 split; [randomValue] expected in `[0, 1)`. */
fun assignVariant(randomValue: Double): RestTimerDefaultVariant =
    if (randomValue < 0.5) RestTimerDefaultVariant.ON else RestTimerDefaultVariant.OFF
