package com.workoutapp.composeapp.data.resttimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires when a scheduled rest timer ends (even if the app is backgrounded) and posts the notification. */
class RestTimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RestTimerNotificationPoster.post(context)
    }
}
