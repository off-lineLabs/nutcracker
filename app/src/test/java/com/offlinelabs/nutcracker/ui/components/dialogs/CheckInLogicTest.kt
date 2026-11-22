package com.offlinelabs.nutcracker.ui.components.dialogs

import com.offlinelabs.nutcracker.data.model.ExerciseType
import com.offlinelabs.nutcracker.ui.components.form.FieldError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInLogicTest {

    private val zone = java.time.ZoneId.of("Europe/Madrid")
    private val now = java.time.LocalDateTime.of(2026, 11, 22, 18, 45, 10)

    private fun local(date: java.util.Date) = java.time.LocalDateTime.ofInstant(date.toInstant(), zone)

    @Test
    fun defaultCheckInIsNowForToday() {
        assertEquals(now, local(defaultCheckInDateTime(now.toLocalDate(), now, zone)))
    }

    @Test
    fun defaultCheckInKeepsTimeOfDayOnAnotherDay() {
        val past = java.time.LocalDate.of(2026, 11, 19)
        assertEquals(past.atTime(18, 45, 10), local(defaultCheckInDateTime(past, now, zone)))
    }

    @Test
    fun requiredFieldsPerType() {
        assertEquals(
            listOf(ExerciseField.WEIGHT, ExerciseField.REPS, ExerciseField.SETS),
            requiredExerciseFields(ExerciseType.STRENGTH)
        )
        assertEquals(listOf(ExerciseField.REPS, ExerciseField.SETS), requiredExerciseFields(ExerciseType.BODYWEIGHT))
        assertEquals(listOf(ExerciseField.REPS), requiredExerciseFields(ExerciseType.CARDIO))
    }

    @Test
    fun cardioIsValidWithOnlyMinutes() {
        assertTrue(isExerciseInputValid(ExerciseType.CARDIO, weight = "", reps = "30", sets = ""))
        assertFalse(isExerciseInputValid(ExerciseType.CARDIO, weight = "", reps = "", sets = "3"))
    }

    @Test
    fun strengthNeedsWeightRepsAndSets() {
        assertTrue(isExerciseInputValid(ExerciseType.STRENGTH, "12,5", "10", "3"))
        assertFalse(isExerciseInputValid(ExerciseType.STRENGTH, "", "10", "3"))
        assertFalse(isExerciseInputValid(ExerciseType.STRENGTH, "12", "", "3"))
        assertFalse(isExerciseInputValid(ExerciseType.STRENGTH, "12", "10", ""))
        assertFalse(isExerciseInputValid(ExerciseType.STRENGTH, "abc", "10", "3"))
    }

    @Test
    fun bodyweightIgnoresWeight() {
        assertTrue(isExerciseInputValid(ExerciseType.BODYWEIGHT, "", "15", "3"))
        assertFalse(isExerciseInputValid(ExerciseType.BODYWEIGHT, "", "15", ""))
    }

    @Test
    fun wholeNumberValidation() {
        assertNull(validateWholeNumber("12"))
        assertNull(validateWholeNumber("0"))
        assertEquals(FieldError.Required, validateWholeNumber(" "))
        assertEquals(FieldError.InvalidNumber, validateWholeNumber("1,5"))
        assertEquals(FieldError.InvalidNumber, validateWholeNumber("x"))
        assertEquals(FieldError.Negative, validateWholeNumber("-2"))
    }

    @Test
    fun weightAcceptsCommaAndDot() {
        assertEquals(12.5, parseWeight("12,5"), 1e-9)
        assertEquals(12.5, parseWeight("12.5"), 1e-9)
        assertEquals(0.0, parseWeight(""), 0.0)
        assertEquals(0.0, parseWeight("abc"), 0.0)
    }

    @Test
    fun amountToMultiplierAcceptsCommaAndDot() {
        assertEquals(1.5, amountToMultiplier("150", 100.0)!!, 1e-9)
        assertEquals(1.5, amountToMultiplier("1,5", 1.0)!!, 1e-9)
        assertEquals(1.5, amountToMultiplier("1.5", 1.0)!!, 1e-9)
    }

    @Test
    fun amountToMultiplierRejectsInvalidInput() {
        assertNull(amountToMultiplier("", 100.0))
        assertNull(amountToMultiplier("0", 100.0))
        assertNull(amountToMultiplier("-3", 100.0))
        assertNull(amountToMultiplier("abc", 100.0))
        assertEquals(FieldError.MustBePositive, validateServingAmount("0"))
        assertEquals(FieldError.Required, validateServingAmount(""))
    }

    @Test
    fun multiplierHasAMinimumAndHandlesBadBase() {
        assertEquals(MIN_SERVING_MULTIPLIER, amountToMultiplier("1", 100.0)!!, 1e-9)
        assertEquals(2.0, amountToMultiplier("2", 0.0)!!, 1e-9)
        assertEquals(150.0, multiplierToAmount(1.5, 100.0), 1e-9)
        assertEquals(1.5, multiplierToAmount(1.5, 0.0), 1e-9)
    }
}
