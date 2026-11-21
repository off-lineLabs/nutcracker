package com.offlinelabs.nutcracker.ui.components.dialogs

import com.offlinelabs.nutcracker.data.GoalLinkMode
import com.offlinelabs.nutcracker.data.model.UserGoal
import com.offlinelabs.nutcracker.ui.components.form.FieldError
import com.offlinelabs.nutcracker.ui.components.form.formatDecimal
import com.offlinelabs.nutcracker.ui.components.form.parseDecimal
import com.offlinelabs.nutcracker.ui.components.form.validateDecimal
import com.offlinelabs.nutcracker.utils.MacroCalculator
import com.offlinelabs.nutcracker.utils.TEFCalculator
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** The Real line is hidden in Down mode when it is this close to the target (gram rounding). */
const val REAL_KCAL_ROUNDING_TOLERANCE = 5

/** Highest fraction of Real one macro bar can be dragged to in Up mode (100% would need infinite grams). */
const val MAX_UP_BAR_SHARE = 0.95

/**
 * Up mode: most kcal one macro can be dragged to. Also the bar's full width when the other macros
 * are all 0 (a share of Real would always be 100% then).
 */
const val UP_BAR_MAX_MACRO_KCAL = 5000.0

/** Down <-> Up. */
fun nextLinkMode(mode: GoalLinkMode): GoalLinkMode = when (mode) {
    GoalLinkMode.KCAL_TO_MACROS -> GoalLinkMode.MACROS_TO_KCAL
    GoalLinkMode.MACROS_TO_KCAL -> GoalLinkMode.KCAL_TO_MACROS
}

/**
 * Up mode drag. Each bar shows its macro's kcal as a fraction of the Real kcal, the same scale
 * Down mode uses with the calories goal; but in Up mode Real grows with the macro being dragged.
 * Returns the (gross) kcal the dragged macro needs so that `kcal / (its net kcal + otherNetKcal)`
 * equals [share]. [tefFraction] is the macro's TEF percentage (0 without TEF), so its net kcal is
 * `kcal * (1 - tefFraction)`.
 */
fun upModeKcalForShare(share: Double, otherNetKcal: Double, tefFraction: Double): Double {
    if (otherNetKcal <= 0.0) return share.coerceIn(0.0, 1.0) * UP_BAR_MAX_MACRO_KCAL
    val s = share.coerceIn(0.0, MAX_UP_BAR_SHARE)
    return (s * otherNetKcal / (1 - s * (1 - tefFraction))).coerceAtMost(UP_BAR_MAX_MACRO_KCAL)
}

/** Up mode fill of a bar: the inverse of [upModeKcalForShare] (before its caps). */
fun upModeFill(kcal: Double, otherNetKcal: Double, tefFraction: Double): Double {
    if (otherNetKcal <= 0.0) return kcal / UP_BAR_MAX_MACRO_KCAL
    val real = kcal * (1 - tefFraction) + otherNetKcal
    return if (real > 0.0) kcal / real else 0.0
}

/**
 * Net kcal the macros add up to: 4/4/9 kcal per gram of carbs/protein/fat, minus the (rounded) TEF
 * bonus when [useTef]. This is the "Real" value of the goals dialog.
 */
fun actualKcalFromMacros(carbsGrams: Int, proteinGrams: Int, fatGrams: Int, useTef: Boolean): Int {
    val gross = MacroCalculator.calculateKcalFromMacros(
        carbsGrams.toDouble(), proteinGrams.toDouble(), fatGrams.toDouble(), useTEF = false
    )
    if (!useTef) return gross
    val tef = TEFCalculator.calculateTEFBonus(
        proteinGrams.toDouble(), carbsGrams.toDouble(), fatGrams.toDouble()
    ).roundToInt()
    return gross - tef
}

/**
 * Whole-gram (carbs, protein, fat) whose [actualKcalFromMacros] is as close as possible to [kcal],
 * keeping the ratios of the given macros (the app's default split when they are all 0).
 * Rounding to whole grams alone can be ~8 kcal off, so neighbouring gram values are tried too.
 */
fun macrosForKcal(kcal: Int, carbs: Int, protein: Int, fat: Int, useTef: Boolean): Triple<Int, Int, Int> {
    if (kcal <= 0) return Triple(0, 0, 0)
    val ratios = MacroCalculator.calculateCurrentRatios(
        carbs.coerceAtLeast(0).toDouble(), protein.coerceAtLeast(0).toDouble(), fat.coerceAtLeast(0).toDouble()
    )
    val exact = MacroCalculator.calculateMacrosFromKcal(kcal, ratios, useTef)
    val c0 = exact.carbsGrams.roundToInt()
    val p0 = exact.proteinGrams.roundToInt()
    val f0 = exact.fatGrams.roundToInt()
    var best = Triple(c0.coerceAtLeast(0), p0.coerceAtLeast(0), f0.coerceAtLeast(0))
    var bestScore = Int.MAX_VALUE
    for (dc in -1..1) for (dp in -1..1) for (df in -1..1) {
        val c = (c0 + dc).coerceAtLeast(0)
        val p = (p0 + dp).coerceAtLeast(0)
        val f = (f0 + df).coerceAtLeast(0)
        val error = abs(actualKcalFromMacros(c, p, f, useTef) - kcal)
        // Prefer the closest kcal, then the candidate nearest the exact ratios.
        val drift = abs(dc) + abs(dp) + abs(df)
        val score = error * 10 + drift
        if (score < bestScore) {
            bestScore = score
            best = Triple(c, p, f)
        }
    }
    return best
}

/*
 * Pure logic behind SetGoalDialog, kept free of Compose so it can be unit tested.
 *
 * The salt goal is stored in milligrams (`UserGoal.sodiumGoal_mg`, like `Meal.sodium_mg`, which
 * holds salt). The dialog shows and edits it in whole milligrams, like the rest of the app.
 */

/** Calories goal text: required and, once rounded to a whole number, greater than zero. */
fun validateCaloriesGoal(text: String): FieldError? {
    validateDecimal(text, required = true, allowZero = false)?.let { return it }
    val rounded = parseDecimal(text)?.roundToInt() ?: return FieldError.InvalidNumber
    return if (rounded <= 0) FieldError.MustBePositive else null
}

/** Fiber goal text (grams): required and not negative. */
fun validateFiberGoal(text: String): FieldError? =
    validateDecimal(text, required = true, allowZero = true)

/** Salt goal text (mg): required and not negative. */
fun validateSaltGoal(text: String): FieldError? =
    validateDecimal(text, required = true, allowZero = true)

/** Whole-number grams/kcal for a stored `Int` goal; accepts `,` or `.`. Null if not a number. */
fun parseWholeGoal(text: String): Int? = parseDecimal(text)?.roundToInt()

/** Whole goal (kcal or g) as shown in an input. */
fun wholeGoalText(value: Int, locale: Locale = Locale.getDefault()): String =
    formatDecimal(value.toDouble(), locale, 0)

/** True if every typed goal field is valid. */
fun isGoalInputValid(calories: String, fiber: String, salt: String): Boolean =
    validateCaloriesGoal(calories) == null &&
        validateFiberGoal(fiber) == null &&
        validateSaltGoal(salt) == null

/**
 * Builds the goal to save, or null if any typed field is invalid. Carbs, protein and fat come from
 * the macro bar, so they are already numbers; negatives are clamped to 0.
 */
fun buildUserGoal(
    id: Int,
    calories: String,
    carbsGrams: Int,
    proteinGrams: Int,
    fatGrams: Int,
    fiber: String,
    saltMg: String
): UserGoal? {
    if (!isGoalInputValid(calories, fiber, saltMg)) return null
    return UserGoal(
        id = id,
        caloriesGoal = parseWholeGoal(calories) ?: return null,
        carbsGoal_g = carbsGrams.coerceAtLeast(0),
        proteinGoal_g = proteinGrams.coerceAtLeast(0),
        fatGoal_g = fatGrams.coerceAtLeast(0),
        fiberGoal_g = parseWholeGoal(fiber) ?: return null,
        sodiumGoal_mg = parseWholeGoal(saltMg) ?: return null
    )
}
