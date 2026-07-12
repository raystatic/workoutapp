package com.workoutapp.android

import android.app.Application
import com.workoutapp.composeapp.di.initKoinAndroid

class WorkoutApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoinAndroid(this)
    }
}
