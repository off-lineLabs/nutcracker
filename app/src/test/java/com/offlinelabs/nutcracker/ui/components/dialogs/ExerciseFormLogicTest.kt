package com.offlinelabs.nutcracker.ui.components.dialogs

import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseType
import com.offlinelabs.nutcracker.ui.components.form.FieldError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ExerciseFormLogicTest {
    private val pt = Locale.forLanguageTag("pt-BR")

    private fun strength(
        name: String = "Bench",
        kcal: String = "",
        weight: String = "",
        reps: String = "",
        sets: String = ""
    ) = ExerciseFormInput(name = name, type = ExerciseType.STRENGTH, kcal = kcal, weight = weight, reps = reps, sets = sets)

    private fun external(category: String) = com.offlinelabs.nutcracker.data.model.ExternalExercise(
        id = "x", name = "Ext", force = null, level = "beginner", mechanic = null, equipment = null,
        primaryMuscles = emptyList(), secondaryMuscles = emptyList(), instructions = emptyList(),
        category = category, images = emptyList()
    )

    @Test
    fun externalStrengthPrefillsKcalPerSet() {
        val form = exerciseFormFrom(external("strength"), Locale.US)
        assertEquals(ExerciseType.STRENGTH, form.type)
        assertEquals("5", form.kcal)
    }

    @Test
    fun externalBodyweightPrefillsKcal() {
        val form = exerciseFormFrom(external("stretching"), pt)
        assertEquals(ExerciseType.BODYWEIGHT, form.type)
        assertEquals("0,5", form.kcal)
    }

    @Test
    fun externalCardioPrefillsKcal() {
        val form = exerciseFormFrom(external("cardio"), Locale.US)
        assertEquals(ExerciseType.CARDIO, form.type)
        assertEquals("8", form.kcal)
    }

    @Test
    fun blankNumbersAreValid() {
        assertTrue(isExerciseFormValid(strength()))
    }

    @Test
    fun blankNameIsInvalid() {
        val errors = exerciseFormErrors(strength(name = "  "))
        assertEquals(mapOf(ExerciseFormField.NAME to FieldError.Required), errors)
    }

    @Test
    fun invalidNumbersAreReported() {
        val errors = exerciseFormErrors(strength(kcal = "abc", weight = "-1", reps = "1,5", sets = "-2"))
        assertEquals(FieldError.InvalidNumber, errors[ExerciseFormField.KCAL])
        assertEquals(FieldError.Negative, errors[ExerciseFormField.WEIGHT])
        assertEquals(FieldError.InvalidNumber, errors[ExerciseFormField.REPS])
        assertEquals(FieldError.Negative, errors[ExerciseFormField.SETS])
    }

    @Test
    fun commaDecimalsAreAccepted() {
        assertTrue(isExerciseFormValid(strength(kcal = "2,5", weight = "12,5", reps = "8", sets = "3")))
    }

    @Test
    fun hiddenFieldsAreNotValidated() {
        val cardio = ExerciseFormInput(
            name = "Run", type = ExerciseType.CARDIO, weight = "abc", reps = "x", sets = "y", bodyweightReps = "z"
        )
        assertTrue(isExerciseFormValid(cardio))
        assertFalse(isExerciseFormValid(cardio.copy(minutes = "1,5")))
    }

    @Test
    fun applyForm_strength() {
        val result = applyExerciseForm(
            Exercise(name = ""),
            strength(name = " Bench ", kcal = "2,5", weight = "12,5", reps = "8", sets = "3")
        )
        assertEquals("Bench", result.name)
        assertEquals("strength", result.category)
        assertEquals(2.5, result.kcalBurnedPerUnit!!, 0.0)
        assertEquals(12.5, result.defaultWeight, 0.0)
        assertEquals(8, result.defaultReps)
        assertEquals(3, result.defaultSets)
    }

    @Test
    fun applyForm_blankKcalIsNullAndOthersZero() {
        val result = applyExerciseForm(Exercise(name = ""), strength())
        assertNull(result.kcalBurnedPerUnit)
        assertEquals(0.0, result.defaultWeight, 0.0)
        assertEquals(0, result.defaultReps)
        assertEquals(0, result.defaultSets)
    }

    @Test
    fun applyForm_cardioUsesMinutesAndOneSet() {
        val result = applyExerciseForm(
            Exercise(name = ""),
            ExerciseFormInput(name = "Run", type = ExerciseType.CARDIO, kcal = "8,5", minutes = "30", reps = "99", sets = "9")
        )
        assertEquals("cardio", result.category)
        assertEquals(30, result.defaultReps)
        assertEquals(1, result.defaultSets)
    }

    @Test
    fun applyForm_bodyweightUsesBodyweightReps() {
        val result = applyExerciseForm(
            Exercise(name = ""),
            ExerciseFormInput(name = "Push-up", type = ExerciseType.BODYWEIGHT, bodyweightReps = "15", notes = "  ")
        )
        assertEquals("stretching", result.category)
        assertEquals(15, result.defaultReps)
        assertEquals(1, result.defaultSets)
        assertNull(result.notes)
    }

    @Test
    fun applyForm_keepsNonEditableFieldsOfExistingExercise() {
        val existing = Exercise(id = 7, name = "Old", equipment = "barbell", imagePaths = listOf("a.png"))
        val result = applyExerciseForm(existing, strength(name = "New"))
        assertEquals(7L, result.id)
        assertEquals("barbell", result.equipment)
        assertEquals(listOf("a.png"), result.imagePaths)
    }

    @Test
    fun prefill_formatsWithoutTrailingZeroAndWithLocale() {
        val existing = Exercise(
            name = "Squat", kcalBurnedPerUnit = 2.5, defaultWeight = 20.0, defaultReps = 8, defaultSets = 3,
            category = "strength", notes = "n"
        )
        val form = exerciseFormFrom(existing, pt)
        assertEquals("2,5", form.kcal)
        assertEquals("20", form.weight)
        assertEquals("8", form.reps)
        assertEquals("3", form.sets)
        assertEquals("n", form.notes)
    }

    @Test
    fun prefill_cardioPutsRepsIntoMinutes() {
        val existing = Exercise(name = "Run", category = "cardio", defaultReps = 30, defaultSets = 1)
        val form = exerciseFormFrom(existing, Locale.US)
        assertEquals(ExerciseType.CARDIO, form.type)
        assertEquals("30", form.minutes)
        assertEquals("", form.reps)
        assertEquals("", form.kcal)
    }

    @Test
    fun prefill_roundTripsThroughApply() {
        val existing = Exercise(
            name = "Squat", kcalBurnedPerUnit = 2.5, defaultWeight = 12.5, defaultReps = 8, defaultSets = 3,
            category = "strength"
        )
        assertEquals(existing, applyExerciseForm(existing, exerciseFormFrom(existing, pt)))
    }
}
