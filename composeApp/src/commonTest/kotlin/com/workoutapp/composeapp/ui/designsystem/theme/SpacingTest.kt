package com.workoutapp.composeapp.ui.designsystem.theme

import kotlin.test.Test
import kotlin.test.assertTrue

class SpacingTest {
    @Test
    fun defaultScale_isStrictlyIncreasing() {
        val spacing = Spacing()

        assertTrue(spacing.xs < spacing.sm)
        assertTrue(spacing.sm < spacing.md)
        assertTrue(spacing.md < spacing.lg)
        assertTrue(spacing.lg < spacing.xl)
    }
}
