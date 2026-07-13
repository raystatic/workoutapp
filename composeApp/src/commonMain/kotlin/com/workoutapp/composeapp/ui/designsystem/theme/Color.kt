package com.workoutapp.composeapp.ui.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand seed tones — indigo primary (energy/focus), teal secondary, amber
// tertiary. Kept distinct in both hue and lightness so the set-type badges
// built on top of them stay distinguishable without relying on color alone.
internal val Indigo40 = Color(0xFF3D5AFE)
internal val Indigo80 = Color(0xFFB9C3FF)
internal val Teal40 = Color(0xFF00897B)
internal val Teal80 = Color(0xFF80D4C6)
internal val Amber40 = Color(0xFFB35C00)
internal val Amber80 = Color(0xFFFFB870)
internal val Red40 = Color(0xFFBA1A1A)
internal val Red80 = Color(0xFFFFB4AB)

val AppLightColorScheme: ColorScheme = lightColorScheme(
    primary = Indigo40,
    onPrimary = Color.White,
    secondary = Teal40,
    onSecondary = Color.White,
    tertiary = Amber40,
    onTertiary = Color.White,
    error = Red40,
    onError = Color.White,
    background = Color(0xFFFDFCFF),
    onBackground = Color(0xFF1A1B1F),
    surface = Color(0xFFFDFCFF),
    onSurface = Color(0xFF1A1B1F),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44464F),
)

val AppDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Indigo80,
    onPrimary = Color(0xFF00227A),
    secondary = Teal80,
    onSecondary = Color(0xFF00382E),
    tertiary = Amber80,
    onTertiary = Color(0xFF4A2800),
    error = Red80,
    onError = Color(0xFF690005),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF44464F),
    onSurfaceVariant = Color(0xFFC5C6D0),
)
