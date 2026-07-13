package com.workoutapp.composeapp.di

import com.workoutapp.composeapp.data.library.ExerciseSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Starts the global Koin container with the platform module + [appModule].
 * Safe to call more than once (e.g. iOS recreating the root view controller)
 * — a no-op after the first call.
 */
fun initKoin(config: KoinAppDeclaration = {}) {
    if (GlobalContext.getOrNull() != null) return
    val koin = startKoin {
        config()
        modules(platformModule(), appModule)
    }.koin

    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        koin.get<ExerciseSeeder>().seedIfNeeded()
    }
}
