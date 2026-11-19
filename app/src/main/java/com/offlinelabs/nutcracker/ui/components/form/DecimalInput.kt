package com.offlinelabs.nutcracker.ui.components.form

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/*
 * Locale-tolerant decimal parsing and formatting for user-entered numbers.
 *
 * Users type either `,` or `.` as the decimal separator regardless of their locale (pt-BR/es users
 * usually type a comma, keyboards often only offer a dot), so parsing accepts both. Formatting uses
 * the locale's separator and never adds grouping separators, so a formatted value always parses back.
 */

/**
 * Parses user input such as `"12"`, `"12.5"`, `"12,5"`, `" 0,25 "`, `"5,"` or `",5"`.
 *
 * - Leading/trailing whitespace is ignored.
 * - Exactly one `,` or `.` may be used as the decimal separator; grouping separators are not
 *   supported (`"1,234.5"` and `"1,2,3"` are rejected).
 * - An optional leading `-` or `+` is accepted so callers can report negatives as "can't be
 *   negative" instead of "not a number".
 * - Only ASCII digits are accepted; exponents (`"1e3"`), `NaN`, `Infinity` and empty/blank input
 *   return null.
 *
 * @return the parsed value, or null if [text] isn't a plain decimal number.
 */
fun parseDecimal(text: String): Double? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null

    val start = if (trimmed[0] == '-' || trimmed[0] == '+') 1 else 0
    var digits = 0
    var separatorSeen = false
    for (i in start until trimmed.length) {
        when (trimmed[i]) {
            in '0'..'9' -> digits++
            ',', '.' -> {
                if (separatorSeen) return null
                separatorSeen = true
            }
            else -> return null
        }
    }
    if (digits == 0) return null

    val value = trimmed.replace(',', '.').toDoubleOrNull() ?: return null
    return if (value.isFinite()) value else null
}

/**
 * Formats [value] for an input field or label: no grouping separators, the [locale]'s decimal
 * separator (falling back to `.` if the locale uses something [parseDecimal] can't read back), at
 * most [maxFractionDigits] fraction digits (rounded half-up), and no trailing zeros.
 *
 * Examples: `12.0` -> `"12"`, `12.5` -> `"12.5"` (en) / `"12,5"` (pt-BR), `0.125` -> `"0.13"`.
 * Non-finite values format as an empty string.
 */
fun formatDecimal(
    value: Double,
    locale: Locale = Locale.getDefault(),
    maxFractionDigits: Int = 2
): String {
    if (!value.isFinite()) return ""
    val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
        decimalSeparator = decimalSeparatorFor(locale)
    }
    val format = DecimalFormat("0", symbols).apply {
        isGroupingUsed = false
        minimumFractionDigits = 0
        maximumFractionDigits = maxFractionDigits.coerceAtLeast(0)
        roundingMode = RoundingMode.HALF_UP
    }
    // BigDecimal.valueOf uses the shortest decimal representation, so 1.005 rounds to 1.01 as a
    // user would expect rather than following the binary value 1.00499999...
    val formatted = format.format(BigDecimal.valueOf(value))
    return if (formatted == "-0") "0" else formatted
}

/** The decimal separator [formatDecimal] uses for [locale]: `,` or `.`. */
fun decimalSeparatorFor(locale: Locale): Char {
    val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    return if (separator == ',') ',' else '.'
}

/**
 * Optional input filter: true if [text] could be the start of an unsigned decimal number, i.e. it
 * only contains ASCII digits and at most one `,` or `.` (the empty string is allowed).
 *
 * Usage: `onValueChange = { if (isPartialDecimalInput(it)) value = it }`. Forms that want to show
 * an inline error for pasted garbage or negatives should validate instead of filtering.
 */
fun isPartialDecimalInput(text: String): Boolean {
    var separatorSeen = false
    for (c in text) {
        when (c) {
            in '0'..'9' -> Unit
            ',', '.' -> {
                if (separatorSeen) return false
                separatorSeen = true
            }
            else -> return false
        }
    }
    return true
}
