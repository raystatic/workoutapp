package com.workoutapp.composeapp.data.analytics

import platform.Foundation.NSLog

/** NSLog-backed placeholder; swap for Firebase Analytics once the app takes that dependency. */
class IosAnalyticsSink : AnalyticsSink {
    override fun logEvent(name: String, params: Map<String, Any?>) {
        NSLog("Analytics: $name $params")
    }
}
