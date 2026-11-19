package com.offlinelabs.nutcracker.ui.components.form

import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MealFormStateTest {

    private val en = Locale.US
    private val ptBR = Locale.forLanguageTag("pt-BR")

    private val meal = Meal(
        id = 7,
        name = "Granola",
        calories = 250,
        carbohydrates_g = 30.0,
        protein_g = 12.0,
        fat_g = 8.5,
        fiber_g = 3.0,
        sodium_mg = 400.0,
        servingSize_value = 100.0,
        servingSize_unit = ServingSizeUnit.GRAMS,
        source = "barcode",
        barcode = "123"
    )

    private fun newForm(locale: Locale = en) = MealFormState(null, locale).apply {
        onValueChange(MealField.Name, "Soup")
        onValueChange(MealField.Calories, "120")
    }

    // ----- add -----

    @Test
    fun newForm_defaults() {
        val state = MealFormState(null, en)
        assertEquals("100", state.text(MealField.Quantity))
        assertEquals(ServingSizeUnit.GRAMS, state.unit)
        assertFalse(state.canRescale)
        // Name and calories are required, so a fresh form can't be saved...
        assertFalse(state.isValid)
        assertNull(state.toResult())
        // ...but no "Required" error is shown before the user touches the field.
        assertNull(state.visibleError(MealField.Name))
        assertNull(state.visibleError(MealField.Calories))
        assertEquals(FieldError.Required, state.error(MealField.Name))
    }

    @Test
    fun requiredError_becomesVisibleAfterEditing() {
        val state = MealFormState(null, en)
        state.onValueChange(MealField.Name, "a")
        state.onValueChange(MealField.Name, "")
        assertEquals(FieldError.Required, state.visibleError(MealField.Name))
    }

    @Test
    fun blankOptionalNutrients_saveAsZero() {
        val result = newForm().toResult()!!
        assertEquals("Soup", result.name)
        assertEquals(120, result.calories)
        assertEquals(0.0, result.carbohydratesG, 0.0)
        assertEquals(0.0, result.proteinG, 0.0)
        assertEquals(0.0, result.fatG, 0.0)
        assertEquals(0.0, result.fiberG, 0.0)
        assertEquals(0.0, result.sodiumMg, 0.0)
        assertEquals(100.0, result.servingSizeValue, 0.0)
    }

    @Test
    fun commaAndDot_bothParse() {
        val state = newForm().apply {
            onValueChange(MealField.Protein, "12,5")
            onValueChange(MealField.Carbs, "12.5")
            onValueChange(MealField.Salt, "1,5")
            onValueChange(MealField.Quantity, "2,5")
        }
        val result = state.toResult()!!
        assertEquals(12.5, result.proteinG, 1e-9)
        assertEquals(12.5, result.carbohydratesG, 1e-9)
        assertEquals(1500.0, result.sodiumMg, 0.0) // salt typed in g, stored in mg
        assertEquals(2.5, result.servingSizeValue, 1e-9)
    }

    // ----- salt (typed in g, stored in mg as Meal.sodium_mg) -----

    @Test
    fun salt_typedGramsSaveAsMilligrams() {
        fun saltFor(text: String) = newForm().apply { onValueChange(MealField.Salt, text) }.toResult()!!.sodiumMg
        assertEquals(1500.0, saltFor("1,5"), 0.0)
        assertEquals(1500.0, saltFor("1.5"), 0.0)
        assertEquals(1234.0, saltFor("1,234"), 0.0)
        assertEquals(400.0, saltFor("0.4"), 0.0)
        assertEquals(0.0, saltFor(""), 0.0)
        assertEquals(1500.0, newForm().apply { onValueChange(MealField.Salt, "1,5") }.toResult()!!.toNewMeal().sodium_mg, 0.0)
    }

    @Test
    fun salt_validatesLikeOtherNutrients() {
        val negative = newForm().apply { onValueChange(MealField.Salt, "-1") }
        assertEquals(FieldError.Negative, negative.error(MealField.Salt))
        assertNull(negative.toResult())
        val garbage = newForm().apply { onValueChange(MealField.Salt, "abc") }
        assertEquals(FieldError.InvalidNumber, garbage.error(MealField.Salt))
        assertNull(newForm().error(MealField.Salt))
    }

    @Test
    fun salt_prefillsMilligramsAsGramsWithThreeDecimals() {
        val salty = meal.copy(sodium_mg = 1234.0)
        assertEquals("1.234", MealFormState(salty, en).text(MealField.Salt))
        assertEquals("1,234", MealFormState(salty, ptBR).text(MealField.Salt))
        assertEquals("0.4", MealFormState(meal, en).text(MealField.Salt))
        assertEquals("0,4", MealFormState(meal, ptBR).text(MealField.Salt))
        assertEquals("0", MealFormState(meal.copy(sodium_mg = 0.0), en).text(MealField.Salt))
    }

    @Test
    fun salt_untouchedPrefillKeepsExactMilligrams() {
        val precise = meal.copy(sodium_mg = 1234.5678)
        val state = MealFormState(precise, en)
        assertEquals("1.235", state.text(MealField.Salt))
        assertEquals(1234.5678, state.toResult()!!.sodiumMg, 0.0)
        assertEquals(precise, state.toResult()!!.applyTo(precise))
    }

    @Test
    fun salt_editedPrefillSavesNewValue() {
        val state = MealFormState(meal, ptBR).apply { onValueChange(MealField.Salt, "1,5") }
        assertEquals(meal.copy(sodium_mg = 1500.0), state.toResult()!!.applyTo(meal))
    }

    @Test
    fun salt_rescalesFromOriginalMilligrams() {
        val state = MealFormState(meal, en)
        state.onValueChange(MealField.Quantity, "200")
        state.toggleLink()
        assertEquals("0.8", state.text(MealField.Salt))
        assertEquals(800.0, state.toResult()!!.sodiumMg, 0.0)

        val pt = MealFormState(meal.copy(sodium_mg = 1234.0), ptBR)
        pt.toggleLink()
        pt.onValueChange(MealField.Quantity, "50")
        assertEquals("0,617", pt.text(MealField.Salt))
        assertEquals(617.0, pt.toResult()!!.sodiumMg, 0.0)
    }

    @Test
    fun calories_acceptDecimalsAndRound() {
        fun caloriesFor(text: String) = newForm().apply { onValueChange(MealField.Calories, text) }.toResult()!!.calories
        assertEquals(121, caloriesFor("120.5"))
        assertEquals(121, caloriesFor("120,5"))
        assertEquals(120, caloriesFor("120,4"))
        assertEquals(0, caloriesFor("0"))
    }

    @Test
    fun invalidInput_blocksSave() {
        for ((field, text) in listOf(
            MealField.Fat to "abc",
            MealField.Fat to "-3",
            MealField.Carbs to "1,2,3",
            MealField.Calories to "-1",
            MealField.Calories to "",
            MealField.Name to "   ",
            MealField.Quantity to "",
            MealField.Quantity to "0",
            MealField.Quantity to "-5"
        )) {
            val state = newForm().apply { onValueChange(field, text) }
            assertFalse("$field = \"$text\" should be invalid", state.isValid)
            assertNull(state.toResult())
            assertNotNull("$field = \"$text\" should show an error", state.visibleError(field))
        }
    }

    @Test
    fun errorKinds() {
        val state = newForm().apply {
            onValueChange(MealField.Fat, "-3")
            onValueChange(MealField.Fiber, "abc")
            onValueChange(MealField.Quantity, "0")
        }
        assertEquals(FieldError.Negative, state.error(MealField.Fat))
        assertEquals(FieldError.InvalidNumber, state.error(MealField.Fiber))
        assertEquals(FieldError.MustBePositive, state.error(MealField.Quantity))
    }

    @Test
    fun nameIsTrimmed_andUnitIsUsed() {
        val state = newForm().apply {
            onValueChange(MealField.Name, "  Soup  ")
            onUnitChange(ServingSizeUnit.MILLILITERS)
        }
        val meal = state.toResult()!!.toNewMeal()
        assertEquals("Soup", meal.name)
        assertEquals(ServingSizeUnit.MILLILITERS, meal.servingSize_unit)
        assertEquals("manual", meal.source)
        assertEquals(0L, meal.id)
    }

    // ----- edit -----

    @Test
    fun edit_prefillsWithoutTrailingZeroAndWithLocaleSeparator() {
        val pt = MealFormState(meal, ptBR)
        assertEquals("Granola", pt.text(MealField.Name))
        assertEquals("250", pt.text(MealField.Calories))
        assertEquals("12", pt.text(MealField.Protein))
        assertEquals("8,5", pt.text(MealField.Fat))
        assertEquals("100", pt.text(MealField.Quantity))
        assertTrue(pt.isValid)
        assertTrue(pt.canRescale)

        assertEquals("8.5", MealFormState(meal, en).text(MealField.Fat))
    }

    @Test
    fun edit_untouchedFieldsKeepExactOriginalValues() {
        val precise = meal.copy(protein_g = 12.3456, servingSize_value = 33.333)
        val state = MealFormState(precise, en)
        assertEquals("12.35", state.text(MealField.Protein))
        val updated = state.toResult()!!.applyTo(precise)
        assertEquals(precise, updated)
    }

    @Test
    fun edit_preservesOtherFields() {
        val state = MealFormState(meal, en).apply { onValueChange(MealField.Fat, "9") }
        val updated = state.toResult()!!.applyTo(meal)
        assertEquals(meal.copy(fat_g = 9.0), updated)
        assertEquals("barcode", updated.source)
        assertEquals(7L, updated.id)
    }

    @Test
    fun link_rescalesAllNutrientsWhenQuantityChanges() {
        val state = MealFormState(meal, en)
        state.toggleLink()
        assertTrue(state.isLinked)
        state.onValueChange(MealField.Quantity, "50")
        assertEquals("125", state.text(MealField.Calories))
        assertEquals("15", state.text(MealField.Carbs))
        assertEquals("6", state.text(MealField.Protein))
        assertEquals("4.25", state.text(MealField.Fat))
        assertEquals("1.5", state.text(MealField.Fiber))
        assertEquals("0.2", state.text(MealField.Salt)) // 400 mg * 0.5 = 200 mg = 0.2 g

        val result = state.toResult()!!
        assertEquals(125, result.calories)
        assertEquals(4.25, result.fatG, 1e-9)
        assertEquals(50.0, result.servingSizeValue, 0.0)
    }

    @Test
    fun link_usesLocaleSeparatorAndAcceptsCommaQuantity() {
        val state = MealFormState(meal, ptBR)
        state.toggleLink()
        state.onValueChange(MealField.Quantity, "150,5")
        assertEquals("376", state.text(MealField.Calories)) // 250 * 1.505 = 376.25
        assertEquals("12,79", state.text(MealField.Fat))     // 8.5 * 1.505 = 12.7925
    }

    @Test
    fun link_turningOnRescalesToCurrentQuantity() {
        val state = MealFormState(meal, en)
        state.onValueChange(MealField.Quantity, "200")
        assertEquals("250", state.text(MealField.Calories)) // not linked yet
        state.toggleLink()
        assertEquals("500", state.text(MealField.Calories))
        assertEquals("0.8", state.text(MealField.Salt))
    }

    @Test
    fun link_ignoresInvalidQuantity_andUnlinkStopsRescaling() {
        val state = MealFormState(meal, en)
        state.toggleLink()
        state.onValueChange(MealField.Quantity, "abc")
        assertEquals("250", state.text(MealField.Calories))
        state.onValueChange(MealField.Quantity, "0")
        assertEquals("250", state.text(MealField.Calories))

        state.toggleLink()
        assertFalse(state.isLinked)
        state.onValueChange(MealField.Quantity, "300")
        assertEquals("250", state.text(MealField.Calories))
    }

    @Test
    fun link_isUnavailableWhenAdding() {
        val state = MealFormState(null, en)
        state.toggleLink()
        assertFalse(state.isLinked)
    }
}
