package com.workoutapp.composeapp.data.analytics

import android.util.Log

/** Logcat-backed placeholder; swap for Firebase Analytics once the app takes that dependency. */
class AndroidAnalyticsSink : AnalyticsSink {
    override fun logEvent(name: String, params: Map<String, Any?>) {
        Log.d("Analytics", "$name $params")
    }
}
