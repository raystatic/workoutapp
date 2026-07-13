package com.workoutapp.composeapp.ui.designsystem.components

import kotlin.test.Test
import kotlin.test.assertEquals

class SetTypeTest {
    @Test
    fun everySetType_hasAUniqueSymbol() {
        val symbols = SetType.entries.map { it.symbol }

        assertEquals(symbols.distinct(), symbols)
    }

    @Test
    fun everySetType_hasAUniqueLabel() {
        val labels = SetType.entries.map { it.label }

        assertEquals(labels.distinct(), labels)
    }
}
