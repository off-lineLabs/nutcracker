package com.offlinelabs.nutcracker.ui.components.dialogs

import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseCategoryMapper
import com.offlinelabs.nutcracker.data.model.ExerciseType
import com.offlinelabs.nutcracker.data.model.ExternalExercise
import com.offlinelabs.nutcracker.data.model.getDefaultKcalPerUnit
import com.offlinelabs.nutcracker.ui.components.form.FieldError
import com.offlinelabs.nutcracker.ui.components.form.formatDecimal
import com.offlinelabs.nutcracker.ui.components.form.parseDecimal
import com.offlinelabs.nutcracker.ui.components.form.validateDecimal
import com.offlinelabs.nutcracker.ui.components.form.validateRequiredText
import java.util.Locale

/*
 * Pure logic behind AddExerciseDialog, kept free of Compose so it can be unit tested.
 */

/** The typed inputs of the exercise form. Numbers are kept as text, as typed. */
data class ExerciseFormInput(
    val name: String = "",
    val type: ExerciseType = ExerciseType.STRENGTH,
    val kcal: String = "",
    val weight: String = "",
    /** Strength reps. */
    val reps: String = "",
    val sets: String = "",
    /** Cardio minutes. */
    val minutes: String = "",
    /** Bodyweight reps. */
    val bodyweightReps: String = "",
    val notes: String = ""
)

/** Fields that can be invalid. */
enum class ExerciseFormField { NAME, KCAL, WEIGHT, REPS, SETS, MINUTES, BODYWEIGHT_REPS }

/** The numeric fields the form shows for [type] (hidden ones are neither validated nor saved). */
fun visibleExerciseFormFields(type: ExerciseType): List<ExerciseFormField> = when (type) {
    ExerciseType.STRENGTH -> listOf(
        ExerciseFormField.KCAL, ExerciseFormField.WEIGHT, ExerciseFormField.REPS, ExerciseFormField.SETS
    )
    ExerciseType.CARDIO -> listOf(ExerciseFormField.KCAL, ExerciseFormField.MINUTES)
    ExerciseType.BODYWEIGHT -> listOf(ExerciseFormField.KCAL, ExerciseFormField.BODYWEIGHT_REPS)
}

/** An optional whole-number field: blank is fine (saved as 0), otherwise it must be an integer >= 0. */
fun validateOptionalWholeNumber(text: String): FieldError? =
    if (text.isBlank()) null else validateWholeNumber(text)

/** Validation of one field of [input]; null if valid. */
fun validateExerciseFormField(field: ExerciseFormField, input: ExerciseFormInput): FieldError? = when (field) {
    ExerciseFormField.NAME -> validateRequiredText(input.name)
    ExerciseFormField.KCAL -> validateDecimal(input.kcal, required = false)
    ExerciseFormField.WEIGHT -> validateDecimal(input.weight, required = false)
    ExerciseFormField.REPS -> validateOptionalWholeNumber(input.reps)
    ExerciseFormField.SETS -> validateOptionalWholeNumber(input.sets)
    ExerciseFormField.MINUTES -> validateOptionalWholeNumber(input.minutes)
    ExerciseFormField.BODYWEIGHT_REPS -> validateOptionalWholeNumber(input.bodyweightReps)
}

/** Errors for the name and every visible field. Empty if the form can be saved. */
fun exerciseFormErrors(input: ExerciseFormInput): Map<ExerciseFormField, FieldError> =
    buildMap {
        (listOf(ExerciseFormField.NAME) + visibleExerciseFormFields(input.type)).forEach { field ->
            validateExerciseFormField(field, input)?.let { put(field, it) }
        }
    }

fun isExerciseFormValid(input: ExerciseFormInput): Boolean = exerciseFormErrors(input).isEmpty()

private fun wholeOrZero(text: String): Int = text.trim().toIntOrNull() ?: 0

/**
 * Applies a valid [input] to [base] (a new or existing exercise). Reps hold minutes for cardio and
 * bodyweight reps for bodyweight; cardio and bodyweight always have one set.
 */
fun applyExerciseForm(base: Exercise, input: ExerciseFormInput): Exercise = base.copy(
    name = input.name.trim(),
    category = ExerciseCategoryMapper.getCategory(input.type),
    kcalBurnedPerUnit = parseDecimal(input.kcal),
    defaultWeight = parseDecimal(input.weight) ?: 0.0,
    defaultReps = when (input.type) {
        ExerciseType.CARDIO -> wholeOrZero(input.minutes)
        ExerciseType.BODYWEIGHT -> wholeOrZero(input.bodyweightReps)
        ExerciseType.STRENGTH -> wholeOrZero(input.reps)
    },
    defaultSets = when (input.type) {
        ExerciseType.STRENGTH -> wholeOrZero(input.sets)
        ExerciseType.CARDIO, ExerciseType.BODYWEIGHT -> 1
    },
    notes = input.notes.takeIf { it.isNotBlank() }
)

private fun whole(value: Int): String = value.toString()

/** Initial form contents when editing [existing]. Numbers use [locale]'s separator, no trailing ".0". */
fun exerciseFormFrom(existing: Exercise, locale: Locale = Locale.getDefault()): ExerciseFormInput {
    val type = ExerciseCategoryMapper.getExerciseType(existing.category)
    return ExerciseFormInput(
        name = existing.name,
        type = type,
        kcal = existing.kcalBurnedPerUnit?.let { formatDecimal(it, locale) } ?: "",
        weight = formatDecimal(existing.defaultWeight, locale),
        reps = if (type == ExerciseType.STRENGTH) whole(existing.defaultReps) else "",
        sets = if (type == ExerciseType.STRENGTH) whole(existing.defaultSets) else "",
        minutes = if (type == ExerciseType.CARDIO) whole(existing.defaultReps) else "",
        bodyweightReps = if (type == ExerciseType.BODYWEIGHT) whole(existing.defaultReps) else "",
        notes = existing.notes ?: ""
    )
}

/** Initial form contents when importing [external], with the suggested defaults per type. */
fun exerciseFormFrom(external: ExternalExercise, locale: Locale = Locale.getDefault()): ExerciseFormInput {
    val type = ExerciseCategoryMapper.getExerciseType(external.category)
    val base = ExerciseFormInput(
        name = external.name,
        type = type,
        kcal = formatDecimal(getDefaultKcalPerUnit(external), locale)
    )
    return when (type) {
        ExerciseType.STRENGTH -> {
            val weight = when (external.equipment?.lowercase()) {
                "barbell", "dumbbell", "kettlebells" -> 20.0
                else -> 0.0
            }
            base.copy(weight = formatDecimal(weight, locale), reps = "8", sets = "3")
        }
        ExerciseType.CARDIO -> base
        ExerciseType.BODYWEIGHT -> base.copy(
            bodyweightReps = when (external.category.lowercase()) {
                "stretching" -> "1"
                else -> "10"
            }
        )
    }
}
