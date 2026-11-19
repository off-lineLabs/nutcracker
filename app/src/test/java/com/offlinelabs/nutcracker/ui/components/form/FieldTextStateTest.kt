package com.offlinelabs.nutcracker.ui.components.form

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldTextStateTest {

    @Test
    fun syncKeepsSameInstanceWhenTextMatches() {
        val current = TextFieldValue("100", TextRange(1))
        assertSame(current, syncExternalText(current, "100"))
    }

    @Test
    fun syncShowsExternalTextAndClampsSelection() {
        val current = TextFieldValue("12345", TextRange(5))
        val synced = syncExternalText(current, "12")
        assertEquals("12", synced.text)
        assertEquals(TextRange(2), synced.selection)
    }

    @Test
    fun syncKeepsValidSelection() {
        val synced = syncExternalText(TextFieldValue("1", TextRange(0)), "250")
        assertEquals("250", synced.text)
        assertEquals(TextRange(0), synced.selection)
    }

    @Test
    fun guardSwallowsOnlyFirstSelectionChangeAfterFocus() {
        val guard = FocusCursorGuard(windowMs = 400)
        guard.onFocusGained(1_000)
        assertTrue(guard.consumeIfGuarded(1_050))
        assertFalse(guard.consumeIfGuarded(1_100))
    }

    @Test
    fun guardExpires() {
        val guard = FocusCursorGuard(windowMs = 400)
        guard.onFocusGained(1_000)
        assertFalse(guard.consumeIfGuarded(1_500))
    }

    @Test
    fun guardNotArmedWithoutFocusOrAfterTypingOrFocusLoss() {
        val guard = FocusCursorGuard()
        assertFalse(guard.consumeIfGuarded(10))
        guard.onFocusGained(10)
        guard.disarm()
        assertFalse(guard.consumeIfGuarded(20))
        guard.onFocusGained(30)
        guard.onFocusLost()
        assertFalse(guard.consumeIfGuarded(40))
    }
}
