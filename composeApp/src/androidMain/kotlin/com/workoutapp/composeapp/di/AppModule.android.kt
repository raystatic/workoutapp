package com.workoutapp.composeapp.di

import com.workoutapp.composeapp.data.db.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory(get()) }
}
