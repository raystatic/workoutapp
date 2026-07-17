package com.workoutapp.composeapp.di

import com.workoutapp.composeapp.data.analytics.AnalyticsSink
import com.workoutapp.composeapp.data.analytics.AndroidAnalyticsSink
import com.workoutapp.composeapp.data.db.DatabaseDriverFactory
import com.workoutapp.composeapp.data.resttimer.AndroidRestTimerNotifier
import com.workoutapp.composeapp.data.resttimer.RestTimerNotifier
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory(get()) }
    single<RestTimerNotifier> { AndroidRestTimerNotifier(get()) }
    single<AnalyticsSink> { AndroidAnalyticsSink() }
}
