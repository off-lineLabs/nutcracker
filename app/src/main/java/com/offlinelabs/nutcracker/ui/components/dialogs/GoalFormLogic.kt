package com.offlinelabs.nutcracker.ui.components.dialogs

import com.offlinelabs.nutcracker.data.model.UserGoal
import com.offlinelabs.nutcracker.ui.components.form.FieldError
import com.offlinelabs.nutcracker.ui.components.form.formatDecimal
import com.offlinelabs.nutcracker.ui.components.form.parseDecimal
import com.offlinelabs.nutcracker.ui.components.form.validateDecimal
import java.util.Locale
import kotlin.math.roundToInt

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
