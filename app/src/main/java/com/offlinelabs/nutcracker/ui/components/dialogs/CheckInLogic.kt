package com.offlinelabs.nutcracker.ui.components.dialogs

import com.offlinelabs.nutcracker.data.model.ExerciseType
import com.offlinelabs.nutcracker.ui.components.form.FieldError
import com.offlinelabs.nutcracker.ui.components.form.parseDecimal
import com.offlinelabs.nutcracker.ui.components.form.validateDecimal

/*
 * Pure logic behind the check-in dialogs (UnifiedCheckInDialog), kept free of Compose so it can be
 * unit tested.
 */

/**
 * Timestamp for a new check-in while the dashboard shows [selectedDate]: [now] when that is today,
 * otherwise the selected day at the current time of day (so a back-filled log lands on that day).
 */
fun defaultCheckInDateTime(
    selectedDate: java.time.LocalDate,
    now: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    zone: java.time.ZoneId = java.time.ZoneId.systemDefault()
): java.util.Date =
    java.util.Date.from(selectedDate.atTime(now.toLocalTime()).atZone(zone).toInstant())

/** Values of a quick check-in from the exercise list (weight in kg; reps hold minutes for cardio). */
data class QuickCheckIn(val weight: Double, val reps: Int, val sets: Int, val caloriesBurned: Double)

/**
 * What the list row's wheel adjusts: the weight for strength, the minutes for cardio and the reps
 * for bodyweight. Strength starts at the heaviest logged weight ([maxWeight]), or the default
 * weight before the first log; the others start at the default reps (minutes).
 */
fun quickWheelStart(exercise: com.offlinelabs.nutcracker.data.model.Exercise, maxWeight: Double? = null): Double =
    when (com.offlinelabs.nutcracker.data.model.ExerciseCategoryMapper.getExerciseType(exercise.category)) {
        ExerciseType.STRENGTH -> maxWeight?.takeIf { it > 0.0 } ?: exercise.defaultWeight
        ExerciseType.CARDIO, ExerciseType.BODYWEIGHT -> exercise.defaultReps.toDouble()
    }.coerceAtLeast(0.0)

/**
 * Wheel values in 1-unit steps from 0 to [max], keeping the fraction of [start] (42.5 kg gives
 * 0.5, 1.5 … 42.5 …). Whole units only (no fraction) for minutes and reps.
 */
fun quickWheelValues(start: Double, max: Int): List<Double> {
    val fraction = start - kotlin.math.floor(start)
    return (0..max).map { it + fraction }
}

/** Highest value of the strength rows' reps wheel. */
const val MAX_QUICK_REPS = 100

/**
 * Where a strength row's reps wheel starts: the reps of the exercise's last check-in
 * ([lastReps]), or its default reps before the first one; kept within 1..[MAX_QUICK_REPS].
 */
fun quickRepsStart(exercise: com.offlinelabs.nutcracker.data.model.Exercise, lastReps: Int?): Int =
    (lastReps?.takeIf { it > 0 } ?: exercise.defaultReps).coerceIn(1, MAX_QUICK_REPS)

/**
 * A quick check-in with the wheel's [value] and the exercise's defaults for the rest. Strength:
 * value = weight, [reps] (the reps wheel; default reps when not given) and the default sets, kcal
 * per set. Cardio: value = minutes, one set, kcal per minute. Bodyweight: value = reps, one set,
 * kcal per rep.
 */
fun quickCheckIn(
    exercise: com.offlinelabs.nutcracker.data.model.Exercise,
    value: Double,
    reps: Int = exercise.defaultReps
): QuickCheckIn {
    val kcal = exercise.kcalBurnedPerUnit ?: 0.0
    return when (com.offlinelabs.nutcracker.data.model.ExerciseCategoryMapper.getExerciseType(exercise.category)) {
        ExerciseType.STRENGTH -> QuickCheckIn(value, reps, exercise.defaultSets, exercise.defaultSets * kcal)
        ExerciseType.CARDIO -> value.toInt().let { minutes -> QuickCheckIn(0.0, minutes, 1, minutes * kcal) }
        ExerciseType.BODYWEIGHT -> value.toInt().let { reps -> QuickCheckIn(0.0, reps, 1, reps * kcal) }
    }
}

/** Step of a food row's portion wheel: 10 g / 10 ml, 0.1 kg / L, otherwise ½ (pieces, servings, cups...). */
fun portionStep(unit: com.offlinelabs.nutcracker.data.model.ServingSizeUnit): Double = when (unit) {
    com.offlinelabs.nutcracker.data.model.ServingSizeUnit.GRAMS,
    com.offlinelabs.nutcracker.data.model.ServingSizeUnit.MILLILITERS -> 10.0
    com.offlinelabs.nutcracker.data.model.ServingSizeUnit.KILOGRAMS,
    com.offlinelabs.nutcracker.data.model.ServingSizeUnit.LITERS -> 0.1
    else -> 0.5
}

/** The food's serving size, or 1 when it has none (a multiplier then equals the amount). */
private fun baseServing(meal: com.offlinelabs.nutcracker.data.model.Meal): Double =
    meal.servingSize_value.takeIf { it > 0 } ?: 1.0

/** Whole grams / millilitres, otherwise one decimal (what the wheel can show). */
private fun roundPortion(amount: Double, unit: com.offlinelabs.nutcracker.data.model.ServingSizeUnit): Double =
    if (portionStep(unit) >= 1.0) kotlin.math.round(amount) else kotlin.math.round(amount * 10) / 10

/**
 * Where a food row's portion wheel starts, in the food's unit: the amount of its last check-in
 * ([lastMultiplier] × serving size), or its serving size before the first one.
 */
fun portionWheelStart(meal: com.offlinelabs.nutcracker.data.model.Meal, lastMultiplier: Double?): Double {
    val base = baseServing(meal)
    return roundPortion(lastMultiplier?.takeIf { it > 0 }?.let { it * base } ?: base, meal.servingSize_unit)
        .takeIf { it > 0 } ?: portionStep(meal.servingSize_unit)
}

/**
 * The portion wheel's values in [portionStep]s, all above zero and including [start] (125 g gives
 * 5, 15 … 125 …). Goes up to 4× the start, and at least 100 steps.
 */
fun portionWheelValues(start: Double, unit: com.offlinelabs.nutcracker.data.model.ServingSizeUnit): List<Double> {
    val step = portionStep(unit)
    val remainder = start - kotlin.math.floor(start / step + 1e-9) * step
    val first = if (remainder < 1e-6 || step - remainder < 1e-6) step else remainder
    val max = maxOf(start * 4, step * 100)
    val count = ((max - first) / step).toInt() + 1
    return (0 until count).map { kotlin.math.round((first + it * step) * 100) / 100 }
}

/** The serving multiplier stored for a check-in of [amount] (in the food's unit). */
fun portionMultiplier(amount: Double, meal: com.offlinelabs.nutcracker.data.model.Meal): Double = amount / baseServing(meal)

/** Smallest serving multiplier the meal check-in accepts (the thumbwheel's lower bound). */
const val MIN_SERVING_MULTIPLIER = 0.1

/** The inputs of the exercise check-in. For cardio, [REPS] holds the minutes. */
enum class ExerciseField { WEIGHT, REPS, SETS }

/** The fields the dialog shows (and therefore validates) for an exercise of [type]. */
fun requiredExerciseFields(type: ExerciseType): List<ExerciseField> = when (type) {
    ExerciseType.STRENGTH -> listOf(ExerciseField.WEIGHT, ExerciseField.REPS, ExerciseField.SETS)
    ExerciseType.BODYWEIGHT -> listOf(ExerciseField.REPS, ExerciseField.SETS)
    ExerciseType.CARDIO -> listOf(ExerciseField.REPS)
}

/** Validates a whole-number field such as reps, sets or minutes. Blank is [FieldError.Required]. */
fun validateWholeNumber(text: String): FieldError? {
    if (text.isBlank()) return FieldError.Required
    val value = text.trim().toIntOrNull() ?: return FieldError.InvalidNumber
    return if (value < 0) FieldError.Negative else null
}

/** Validation of a single exercise field's text. */
fun validateExerciseField(field: ExerciseField, text: String): FieldError? = when (field) {
    ExerciseField.WEIGHT -> validateDecimal(text, required = true, allowZero = true)
    ExerciseField.REPS, ExerciseField.SETS -> validateWholeNumber(text)
}

/** True if every field shown for [type] is valid. Fields that are hidden don't matter. */
fun isExerciseInputValid(type: ExerciseType, weight: String, reps: String, sets: String): Boolean =
    requiredExerciseFields(type).all { field ->
        val text = when (field) {
            ExerciseField.WEIGHT -> weight
            ExerciseField.REPS -> reps
            ExerciseField.SETS -> sets
        }
        validateExerciseField(field, text) == null
    }

/** Weight text to kilograms; accepts `,` or `.`; blank or invalid is 0. */
fun parseWeight(text: String): Double = parseDecimal(text) ?: 0.0

/** Validation of the manual serving amount: required and greater than zero. */
fun validateServingAmount(text: String): FieldError? =
    validateDecimal(text, required = true, allowZero = false)

/**
 * Converts a manually typed amount (in the meal's serving unit) to a serving multiplier, or null
 * if the text isn't a valid positive number. Never returns less than [MIN_SERVING_MULTIPLIER].
 * A non-positive [baseServing] is treated as 1.
 */
fun amountToMultiplier(amountText: String, baseServing: Double): Double? {
    if (validateServingAmount(amountText) != null) return null
    val amount = parseDecimal(amountText) ?: return null
    val base = if (baseServing > 0) baseServing else 1.0
    return maxOf(MIN_SERVING_MULTIPLIER, amount / base)
}

/** The amount in the meal's serving unit for a [multiplier]. */
fun multiplierToAmount(multiplier: Double, baseServing: Double): Double {
    val base = if (baseServing > 0) baseServing else 1.0
    return multiplier * base
}
