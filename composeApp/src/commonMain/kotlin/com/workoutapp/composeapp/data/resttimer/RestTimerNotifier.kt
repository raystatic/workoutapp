package com.workoutapp.composeapp.data.resttimer

/**
 * Schedules the platform's local "rest over" notification (sound + vibration)
 * so the rest timer still fires when the app is backgrounded. The countdown
 * shown in the UI is derived from wall-clock time and is just a reflection —
 * this notification is the actual reliable signal, since it's handed off to
 * the OS scheduler (`AlarmManager` on Android, `UNUserNotificationCenter` on
 * iOS) rather than kept alive by an in-process coroutine.
 *
 * Platform bridges ([com.workoutapp.composeapp.di.platformModule]) provide the
 * real implementation per target; tests substitute a fake.
 */
interface RestTimerNotifier {
    fun scheduleEndNotification(secondsFromNow: Int)

    fun cancel()
}
