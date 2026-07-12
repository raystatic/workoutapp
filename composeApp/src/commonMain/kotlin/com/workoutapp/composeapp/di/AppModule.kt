package com.workoutapp.composeapp.di

import com.workoutapp.composeapp.data.db.DatabaseDriverFactory
import com.workoutapp.composeapp.data.sample.SampleNoteRepository
import com.workoutapp.composeapp.data.sample.SampleNoteRepositoryImpl
import com.workoutapp.composeapp.db.AppDatabase
import com.workoutapp.composeapp.ui.workout.WorkoutStore
import org.koin.core.module.Module
import org.koin.dsl.module

/** Platform-specific bindings (currently just [DatabaseDriverFactory]). */
expect fun platformModule(): Module

val appModule = module {
    single { AppDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single<SampleNoteRepository> { SampleNoteRepositoryImpl(get()) }
    single { WorkoutStore(get()) }
}
