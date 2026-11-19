package com.offlinelabs.nutcracker.ui.components.dialogs

import com.offlinelabs.nutcracker.data.model.UserGoal
import com.offlinelabs.nutcracker.ui.components.form.FieldError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class GoalFormLogicTest {
    private val pt = Locale.forLanguageTag("pt-BR")
    private val en = Locale.US

    @Test
    fun saltMgToGramsText_formatsWithoutTrailingZeros() {
        assertEquals("5.75", saltMgToGramsText(5750, en))
        assertEquals("5,75", saltMgToGramsText(5750, pt))
        assertEquals("6", saltMgToGramsText(6000, en))
        assertEquals("0", saltMgToGramsText(0, en))
        assertEquals("0.001", saltMgToGramsText(1, en))
        assertEquals("2.3", saltMgToGramsText(2300, en))
    }

    @Test
    fun saltGramsTextToMg_acceptsCommaAndDot() {
        assertEquals(5750, saltGramsTextToMg("5,75"))
        assertEquals(5750, saltGramsTextToMg("5.75"))
        assertEquals(6000, saltGramsTextToMg("6"))
        assertEquals(1, saltGramsTextToMg("0,001"))
        assertEquals(0, saltGramsTextToMg("0"))
        assertNull(saltGramsTextToMg("abc"))
        assertNull(saltGramsTextToMg(""))
    }

    @Test
    fun saltGoal_roundTripsThroughGrams() {
        listOf(0, 1, 999, 2300, 5750, 6000, 12345).forEach { mg ->
            assertEquals(mg, saltGramsTextToMg(saltMgToGramsText(mg, pt)))
            assertEquals(mg, saltGramsTextToMg(saltMgToGramsText(mg, en)))
        }
    }

    @Test
    fun saltGramsTextToMg_roundsToWholeMilligram() {
        assertEquals(1235, saltGramsTextToMg("1,2345"))
        assertEquals(1234, saltGramsTextToMg("1,2344"))
    }

    @Test
    fun validateCaloriesGoal_requiresPositive() {
        assertNull(validateCaloriesGoal("2000"))
        assertNull(validateCaloriesGoal("1800,4"))
        assertEquals(FieldError.Required, validateCaloriesGoal(""))
        assertEquals(FieldError.InvalidNumber, validateCaloriesGoal("abc"))
        assertEquals(FieldError.MustBePositive, validateCaloriesGoal("0"))
        assertEquals(FieldError.MustBePositive, validateCaloriesGoal("-5"))
        assertEquals(FieldError.MustBePositive, validateCaloriesGoal("0,4"))
    }

    @Test
    fun validateFiberAndSalt_allowZeroRejectNegative() {
        assertNull(validateFiberGoal("0"))
        assertNull(validateFiberGoal("30"))
        assertEquals(FieldError.Negative, validateFiberGoal("-1"))
        assertEquals(FieldError.Required, validateFiberGoal(" "))
        assertNull(validateSaltGoal("0"))
        assertNull(validateSaltGoal("5,75"))
        assertEquals(FieldError.Negative, validateSaltGoal("-0,5"))
        assertEquals(FieldError.InvalidNumber, validateSaltGoal("x"))
    }

    @Test
    fun buildUserGoal_convertsSaltAndKeepsId() {
        val goal = buildUserGoal(1, "2000", 250, 100, 65, "30", "5,75")
        assertNotNull(goal)
        assertEquals(UserGoal(1, 2000, 250, 100, 65, 30, 5750), goal)
    }

    @Test
    fun buildUserGoal_nullWhenInvalid() {
        assertNull(buildUserGoal(1, "0", 1, 1, 1, "30", "5"))
        assertNull(buildUserGoal(1, "2000", 1, 1, 1, "-1", "5"))
        assertNull(buildUserGoal(1, "2000", 1, 1, 1, "30", "abc"))
    }

    @Test
    fun buildUserGoal_roundsFiberAndClampsMacros() {
        val goal = buildUserGoal(1, "2000,4", -3, 100, 65, "25,6", "0")!!
        assertEquals(2000, goal.caloriesGoal)
        assertEquals(0, goal.carbsGoal_g)
        assertEquals(26, goal.fiberGoal_g)
        assertEquals(0, goal.sodiumGoal_mg)
    }

    @Test
    fun defaultGoalPrefill_isFivePointSevenFiveGrams() {
        assertEquals("5.75", saltMgToGramsText(UserGoal.default().sodiumGoal_mg, en))
        assertEquals("30", wholeGoalText(UserGoal.default().fiberGoal_g, en))
    }
}
