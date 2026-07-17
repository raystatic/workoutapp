package com.workoutapp.composeapp.data.analytics

/**
 * Pluggable analytics event sink (Firebase Analytics later). Platform bridges
 * ([com.workoutapp.composeapp.di.platformModule]) provide the real implementation
 * per target; tests substitute a fake.
 */
interface AnalyticsSink {
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())
}

/** Default/test fallback that discards every event. */
object NoOpAnalyticsSink : AnalyticsSink {
    override fun logEvent(name: String, params: Map<String, Any?>) = Unit
}
