package com.workoutapp.composeapp.ui.designsystem.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ColorSchemeTest {
    @Test
    fun lightScheme_resolvesToLightTokens() {
        assertEquals(Indigo40, AppLightColorScheme.primary)
        assertEquals(Teal40, AppLightColorScheme.secondary)
        assertEquals(Amber40, AppLightColorScheme.tertiary)
    }

    @Test
    fun darkScheme_resolvesToDarkTokens() {
        assertEquals(Indigo80, AppDarkColorScheme.primary)
        assertEquals(Teal80, AppDarkColorScheme.secondary)
        assertEquals(Amber80, AppDarkColorScheme.tertiary)
    }

    @Test
    fun lightAndDarkSchemes_useDistinctBackgrounds() {
        assertNotEquals(AppLightColorScheme.background, AppDarkColorScheme.background)
        assertNotEquals(AppLightColorScheme.onBackground, AppDarkColorScheme.onBackground)
    }
}
