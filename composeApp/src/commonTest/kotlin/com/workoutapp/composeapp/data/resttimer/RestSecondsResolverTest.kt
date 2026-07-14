package com.workoutapp.composeapp.data.resttimer

import kotlin.test.Test
import kotlin.test.assertEquals

class RestSecondsResolverTest {
    @Test
    fun resolveRestSeconds_withPositiveOverride_usesTheOverride() {
        assertEquals(45, resolveRestSeconds(exerciseOverrideSeconds = 45L, defaultSeconds = 90))
    }

    @Test
    fun resolveRestSeconds_withNullOverride_fallsBackToTheDefault() {
        assertEquals(90, resolveRestSeconds(exerciseOverrideSeconds = null, defaultSeconds = 90))
    }

    @Test
    fun resolveRestSeconds_withZeroOverride_fallsBackToTheDefault() {
        assertEquals(90, resolveRestSeconds(exerciseOverrideSeconds = 0L, defaultSeconds = 90))
    }

    @Test
    fun resolveRestSeconds_withNegativeOverride_fallsBackToTheDefault() {
        assertEquals(90, resolveRestSeconds(exerciseOverrideSeconds = -5L, defaultSeconds = 90))
    }
}
