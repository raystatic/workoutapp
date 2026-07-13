package com.workoutapp.android.designsystem

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.workoutapp.android.MainActivity
import org.junit.Rule
import org.junit.Test

/**
 * From the Profile tab, opens the debug component catalog and exercises a
 * couple of interactive components end to end (button -> dialog, text input).
 */
class ComponentCatalogNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun openCatalogFromProfile_rendersCoreComponents() {
        composeRule.onNodeWithTag("tab_profile").performClick()
        composeRule.onNodeWithTag("open_component_catalog").performClick()

        composeRule.onNodeWithText("Component Catalog").assertExists()
        composeRule.onNodeWithTag("catalog_primary_button").assertExists()
        composeRule.onNodeWithTag("catalog_secondary_button").assertExists()
        composeRule.onNodeWithTag("catalog_text_field").assertExists()
        composeRule.onNodeWithTag("catalog_number_field").assertExists()
    }

    @Test
    fun textField_acceptsInput() {
        composeRule.onNodeWithTag("tab_profile").performClick()
        composeRule.onNodeWithTag("open_component_catalog").performClick()

        composeRule.onNodeWithTag("catalog_text_field").performTextInput("Bench Press")

        composeRule.onNodeWithText("Bench Press").assertExists()
    }

    @Test
    fun showDialogButton_opensAndDismissesTheDialog() {
        composeRule.onNodeWithTag("tab_profile").performClick()
        composeRule.onNodeWithTag("open_component_catalog").performClick()

        composeRule.onNodeWithTag("catalog_show_dialog_button").performClick()
        composeRule.onNodeWithText("Delete set?").assertExists()

        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Delete set?").assertDoesNotExist()
    }
}
