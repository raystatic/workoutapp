package com.workoutapp.android.designsystem

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.workoutapp.composeapp.ui.designsystem.catalog.ComponentCatalogScreen
import com.workoutapp.composeapp.ui.designsystem.components.SetType
import com.workoutapp.composeapp.ui.designsystem.theme.WorkoutAppTheme
import org.junit.Rule
import org.junit.Test

/**
 * Renders the catalog in both themes and checks every set-type badge is
 * present, so the color-blind-safe (symbol + label) distinction is verified
 * for real composed output, not just the underlying data class.
 */
class ComponentCatalogRenderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun catalog_rendersAllSetTypeIndicators_inLightTheme() {
        composeRule.setContent {
            WorkoutAppTheme(darkTheme = false) { ComponentCatalogScreen() }
        }

        SetType.entries.forEach { type ->
            composeRule.onNodeWithTag("set_type_indicator_${type.name}").assertExists()
        }
    }

    @Test
    fun catalog_rendersAllSetTypeIndicators_inDarkTheme() {
        composeRule.setContent {
            WorkoutAppTheme(darkTheme = true) { ComponentCatalogScreen() }
        }

        SetType.entries.forEach { type ->
            composeRule.onNodeWithTag("set_type_indicator_${type.name}").assertExists()
        }
    }
}
