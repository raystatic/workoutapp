package com.workoutapp.composeapp.di

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

/** Call once from [Application.onCreate]. */
fun initKoinAndroid(application: Application) {
    initKoin {
        androidLogger()
        androidContext(application)
    }
}
