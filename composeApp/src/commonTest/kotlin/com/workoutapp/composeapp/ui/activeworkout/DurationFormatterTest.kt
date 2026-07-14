package com.workoutapp.composeapp.ui.activeworkout

import kotlin.test.Test
import kotlin.test.assertEquals

class DurationFormatterTest {
    @Test
    fun zeroSeconds_formatsAsZeroZero() {
        assertEquals("00:00", formatElapsedDuration(0))
    }

    @Test
    fun secondsUnderAMinute_padsSeconds() {
        assertEquals("00:45", formatElapsedDuration(45))
    }

    @Test
    fun exactlyOneMinute_showsMinutes() {
        assertEquals("01:00", formatElapsedDuration(60))
    }

    @Test
    fun minutesAndSeconds_bothPadded() {
        assertEquals("02:05", formatElapsedDuration(125))
    }

    @Test
    fun exactlyOneHour_switchesToHourFormat() {
        assertEquals("1:00:00", formatElapsedDuration(3600))
    }

    @Test
    fun hoursMinutesAndSeconds_allPadded() {
        assertEquals("1:01:01", formatElapsedDuration(3661))
    }

    @Test
    fun negativeSeconds_clampsToZero() {
        assertEquals("00:00", formatElapsedDuration(-5))
    }
}
