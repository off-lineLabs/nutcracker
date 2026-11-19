package com.offlinelabs.nutcracker.ui.components.form

import androidx.annotation.StringRes
import com.offlinelabs.nutcracker.R

/** Why a form field's value can't be accepted. [messageRes] is the short inline error text. */
enum class FieldError(@param:StringRes val messageRes: Int) {
    /** The field is required and blank. */
    Required(R.string.form_error_required),

    /** The text isn't a number (see [parseDecimal]). */
    InvalidNumber(R.string.form_error_invalid_number),

    /** The value must be strictly greater than zero (e.g. a serving size). */
    MustBePositive(R.string.form_error_must_be_positive),

    /** The value is below zero. */
    Negative(R.string.form_error_negative)
}

/**
 * Validates a decimal text field.
 *
 * @param required if false, blank input is valid (callers typically treat it as 0).
 * @param allowZero if false, the value must be > 0 ([FieldError.MustBePositive] for 0 or negatives).
 * @return null if valid, otherwise the first problem found.
 */
fun validateDecimal(text: String, required: Boolean, allowZero: Boolean = true): FieldError? {
    if (text.isBlank()) return if (required) FieldError.Required else null
    val value = parseDecimal(text) ?: return FieldError.InvalidNumber
    return when {
        !allowZero && value <= 0.0 -> FieldError.MustBePositive
        value < 0.0 -> FieldError.Negative
        else -> null
    }
}

/** Validates a required free-text field (blank or whitespace-only is [FieldError.Required]). */
fun validateRequiredText(text: String): FieldError? =
    if (text.isBlank()) FieldError.Required else null
