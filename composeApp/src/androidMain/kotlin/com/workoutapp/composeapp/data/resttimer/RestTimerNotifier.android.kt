package com.workoutapp.composeapp.data.resttimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Uses `AlarmManager.setAndAllowWhileIdle` (RTC_WAKEUP) rather than the exact
 * variant: exact alarms need the user-grantable `SCHEDULE_EXACT_ALARM`
 * permission on API 31+, which is unnecessary friction for a rest timer where
 * a few seconds of slop is fine. `...AllowWhileIdle` still delivers reliably
 * while the app is backgrounded, including in Doze.
 */
class AndroidRestTimerNotifier(private val context: Context) : RestTimerNotifier {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleEndNotification(secondsFromNow: Int) {
        val triggerAtMillis = System.currentTimeMillis() + secondsFromNow.coerceAtLeast(0) * 1000L
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, alarmPendingIntent())
    }

    override fun cancel() {
        alarmManager.cancel(alarmPendingIntent())
    }

    private fun alarmPendingIntent(): PendingIntent {
        val intent = Intent(context, RestTimerAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val ALARM_REQUEST_CODE = 4201
    }
}
