package com.offlinelabs.nutcracker.ui.components.form

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Pure rule: the text changed from outside (e.g. a rescale updated the nutrient fields), so show
 * [external] while keeping the current selection, clamped to the new text length. Returns
 * [current] untouched when the text already matches.
 */
internal fun syncExternalText(current: TextFieldValue, external: String): TextFieldValue {
    if (current.text == external) return current
    val max = external.length
    return TextFieldValue(
        text = external,
        selection = TextRange(current.selection.start.coerceIn(0, max), current.selection.end.coerceIn(0, max))
    )
}

/**
 * Pure helper for "cursor at the end when a field gains focus".
 *
 * A tap that focuses a field delivers its own selection update (the tap position) just after the
 * focus event, which would undo the move-to-end. [onFocusGained] arms a short-lived guard and
 * [consumeIfGuarded] swallows exactly the first selection-only change within [windowMs]. The guard
 * expires, so with no such update (e.g. focus via the keyboard's Next action) a later deliberate
 * tap still moves the cursor.
 */
internal class FocusCursorGuard(private val windowMs: Long = DEFAULT_WINDOW_MS) {
    private var armedAt = NOT_ARMED

    fun onFocusGained(nowMs: Long) {
        armedAt = nowMs
    }

    fun onFocusLost() {
        armedAt = NOT_ARMED
    }

    /** Call for a selection-only change. True means: ignore it. Always disarms. */
    fun consumeIfGuarded(nowMs: Long): Boolean {
        val armed = armedAt
        armedAt = NOT_ARMED
        return armed != NOT_ARMED && nowMs - armed in 0..windowMs
    }

    /** Call when the text itself changed (typing): the user is past the focus moment. */
    fun disarm() {
        armedAt = NOT_ARMED
    }

    companion object {
        const val DEFAULT_WINDOW_MS = 400L
        private const val NOT_ARMED = Long.MIN_VALUE
    }
}

/**
 * Holds a [TextFieldValue] for a String-based field: mirrors the external value, puts the cursor at
 * the end on focus and only reports real text changes (not selection-only ones).
 */
internal class FieldTextState {
    var field by mutableStateOf(TextFieldValue(""))
        private set
    private val guard = FocusCursorGuard()
    private var hasFocus = false

    fun sync(external: String) {
        if (field.text == external) return
        // First composition / unknown text: start with the cursor at the end.
        field = if (field.text.isEmpty() && field.selection == TextRange.Zero) {
            TextFieldValue(external, TextRange(external.length))
        } else {
            syncExternalText(field, external)
        }
    }

    /** Returns the new text if it changed and must be reported, else null. */
    fun onFieldChange(new: TextFieldValue): String? {
        val textChanged = new.text != field.text
        if (!textChanged && new.selection != field.selection && guard.consumeIfGuarded(SystemClock.uptimeMillis())) {
            return null // the focusing tap's own selection update; keep the cursor at the end
        }
        if (textChanged) guard.disarm()
        field = new
        return if (textChanged) new.text else null
    }

    fun onFocusChanged(state: FocusState) {
        val focused = state.isFocused
        if (focused && !hasFocus) {
            field = field.copy(selection = TextRange(field.text.length))
            guard.onFocusGained(SystemClock.uptimeMillis())
        } else if (!focused) {
            guard.onFocusLost()
        }
        hasFocus = focused
    }
}

@Composable
internal fun rememberFieldTextState(value: String): FieldTextState {
    val state = remember { FieldTextState().also { it.sync(value) } }
    state.sync(value)
    return state
}
