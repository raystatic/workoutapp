package com.workoutapp.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.workoutapp.composeapp.data.resttimer.RestTimerNotificationPoster
import com.workoutapp.composeapp.di.initKoinAndroid

class WorkoutApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoinAndroid(this)
        createRestTimerNotificationChannel()
    }

    private fun createRestTimerNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            RestTimerNotificationPoster.CHANNEL_ID,
            "Rest timer",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifies you when your rest timer ends"
            enableVibration(true)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
