package com.workoutapp.composeapp.data.resttimer

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/**
 * `UNTimeIntervalNotificationTrigger` is scheduled with the OS, so it fires
 * even while the app is backgrounded or suspended — unlike an in-process
 * timer, which iOS can pause/terminate.
 */
@OptIn(ExperimentalForeignApi::class)
class IosRestTimerNotifier : RestTimerNotifier {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override fun scheduleEndNotification(secondsFromNow: Int) {
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { _, _ -> }

        val content = UNMutableNotificationContent().apply {
            title = "Rest over"
            body = "Time for your next set."
            sound = UNNotificationSound.defaultSound
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            secondsFromNow.coerceAtLeast(1).toDouble(),
            repeats = false,
        )
        val request = UNNotificationRequest.requestWithIdentifier(
            REQUEST_ID,
            content,
            trigger,
        )
        center.addNotificationRequest(request, withCompletionHandler = null)
    }

    override fun cancel() {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(REQUEST_ID))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(REQUEST_ID))
    }

    private companion object {
        const val REQUEST_ID = "rest_timer"
    }
}
