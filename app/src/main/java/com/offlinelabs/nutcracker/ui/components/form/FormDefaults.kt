package com.offlinelabs.nutcracker.ui.components.form

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.ui.theme.BrandRedLight
import com.offlinelabs.nutcracker.ui.theme.appTextSecondaryColor
import com.offlinelabs.nutcracker.ui.theme.dialogPrimaryColorMaxContrast

/** Colors used by the form kit. Get the themed set from [FormDefaults.colors]. */
@Immutable
data class FormColors(
    /** Dialog card background. */
    val container: Color,
    /** Background of a [FormSection] card, slightly set off from [container]. */
    val section: Color,
    /** Background of compact input fields and the unit button. */
    val field: Color,
    val fieldBorder: Color,
    val fieldBorderFocused: Color,
    val divider: Color,
    /** Primary text (labels, values, title). */
    val content: Color,
    /** Secondary text (units, hints, section headers). */
    val contentSecondary: Color,
    /** Accent for focused borders, cursor, text buttons and the confirm button. */
    val accent: Color,
    /** Content color on top of [accent] (confirm button text). */
    val onAccent: Color,
    val error: Color
)

/** True inside a narrow [FormDialogSurface]; provided by the surface, read via [FormDefaults.isCompactWidth]. */
internal val LocalFormCompactWidth = compositionLocalOf { false }

/** Sizes and colors shared by the form kit. */
object FormDefaults {
    /** Width of the compact number field in [NumericInputRow]. */
    val NumberFieldWidth: Dp = 84.dp

    /** Narrower number field used on small phones, leaving more room for row labels. */
    val NumberFieldWidthCompact: Dp = 72.dp

    /**
     * A [FormDialogSurface] narrower than this (phones under ~400 dp wide) uses tighter paddings
     * and [NumberFieldWidthCompact], so labels like "Carbohidratos" fit on one line.
     */
    val CompactWidth: Dp = 380.dp

    /**
     * A [FormDialogSurface] shorter than this (e.g. landscape with the keyboard open) scrolls title,
     * content and actions together instead of pinning title and actions.
     */
    val CompactHeight: Dp = 320.dp

    /** Whether the enclosing [FormDialogSurface] is narrower than [CompactWidth]. */
    val isCompactWidth: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalFormCompactWidth.current

    /** Number field width for the current form width: [NumberFieldWidth] or [NumberFieldWidthCompact]. */
    @Composable
    @ReadOnlyComposable
    fun numberFieldWidth(): Dp = if (isCompactWidth) NumberFieldWidthCompact else NumberFieldWidth

    /** Width reserved for the unit suffix after a number field, so rows line up. */
    val UnitSuffixWidth: Dp = 36.dp

    /** Minimum height of compact fields and the unit button (keeps a usable touch target). */
    val FieldMinHeight: Dp = 44.dp

    /** Size of the leading icon slot in rows. */
    val LeadingSize: Dp = 24.dp

    /**
     * Themed colors for the current [MaterialTheme]. Works for the app's light and dark schemes
     * (dark is detected from the surface luminance, so previews and nested themes behave too).
     */
    @Composable
    fun colors(): FormColors {
        val scheme = MaterialTheme.colorScheme
        val dark = scheme.surface.luminance() < 0.5f
        return if (dark) {
            val accent = dialogPrimaryColorMaxContrast()
            FormColors(
                container = scheme.surface,
                // Recessed panel: the navy background blended into the surface.
                section = scheme.background.copy(alpha = 0.45f).compositeOver(scheme.surface),
                field = scheme.surface,
                fieldBorder = scheme.outline,
                fieldBorderFocused = accent,
                divider = scheme.outline.copy(alpha = 0.6f),
                content = scheme.onSurface,
                contentSecondary = appTextSecondaryColor(),
                accent = accent,
                onAccent = scheme.background,
                error = BrandRedLight
            )
        } else {
            FormColors(
                container = scheme.surface,
                section = scheme.surfaceVariant,
                field = scheme.surface,
                fieldBorder = scheme.outline,
                fieldBorderFocused = scheme.primary,
                divider = scheme.outlineVariant,
                content = scheme.onSurface,
                contentSecondary = appTextSecondaryColor(),
                accent = scheme.primary,
                onAccent = scheme.onPrimary,
                error = scheme.error
            )
        }
    }

    /** Colors for a full-size [androidx.compose.material3.OutlinedTextField] inside a form dialog. */
    @Composable
    fun outlinedTextFieldColors(colors: FormColors = colors()): TextFieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.content,
            unfocusedTextColor = colors.content,
            focusedContainerColor = colors.field,
            unfocusedContainerColor = colors.field,
            errorContainerColor = colors.field,
            focusedBorderColor = colors.fieldBorderFocused,
            unfocusedBorderColor = colors.fieldBorder,
            focusedLabelColor = colors.fieldBorderFocused,
            unfocusedLabelColor = colors.contentSecondary,
            cursorColor = colors.accent,
            errorBorderColor = colors.error,
            errorLabelColor = colors.error,
            errorSupportingTextColor = colors.error,
            errorCursorColor = colors.error,
            unfocusedPlaceholderColor = colors.contentSecondary,
            focusedPlaceholderColor = colors.contentSecondary
        )
}
