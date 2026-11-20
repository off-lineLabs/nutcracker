package com.offlinelabs.nutcracker.ui.components.form

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/*
 * Salt is stored in milligrams (Meal.sodium_mg, UserGoal.sodiumGoal_mg) and shown and entered in
 * milligrams everywhere. This is the one place that formats it.
 */

/** Most fraction digits accepted/shown in a salt input (mg). */
const val SALT_INPUT_FRACTION_DIGITS = 1

/** Salt [mg] as a whole number with the [locale]'s grouping, e.g. "1,500" (en) / "1.500" (es, pt). */
fun formatSaltMg(mg: Double, locale: Locale = Locale.getDefault()): String {
    if (!mg.isFinite()) return ""
    val format = NumberFormat.getIntegerInstance(locale).apply {
        roundingMode = RoundingMode.HALF_UP
        isGroupingUsed = true
    }
    return format.format(BigDecimal.valueOf(mg))
}

/** Salt [mg] as an input field text: no grouping, no trailing ".0", up to one decimal. */
fun saltMgToInputText(mg: Double, locale: Locale = Locale.getDefault()): String =
    formatDecimal(mg, locale, SALT_INPUT_FRACTION_DIGITS)
