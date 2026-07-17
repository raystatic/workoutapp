package com.workoutapp.android

import com.workoutapp.composeapp.data.analytics.AnalyticsSink
import java.util.Collections

/** Installed in place of the real [AnalyticsSink] by [WorkoutInstrumentationTestRunner] so instrumentation tests can inspect fired events. */
class FakeAnalyticsSink : AnalyticsSink {
    private val events: MutableList<Pair<String, Map<String, Any?>>> = Collections.synchronizedList(mutableListOf())

    override fun logEvent(name: String, params: Map<String, Any?>) {
        events += name to params
    }

    fun eventsSnapshot(): List<Pair<String, Map<String, Any?>>> = synchronized(events) { events.toList() }
}
