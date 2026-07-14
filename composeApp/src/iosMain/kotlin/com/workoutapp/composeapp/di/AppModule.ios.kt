package com.workoutapp.composeapp.di

import com.workoutapp.composeapp.data.db.DatabaseDriverFactory
import com.workoutapp.composeapp.data.resttimer.IosRestTimerNotifier
import com.workoutapp.composeapp.data.resttimer.RestTimerNotifier
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory() }
    single<RestTimerNotifier> { IosRestTimerNotifier() }
}
