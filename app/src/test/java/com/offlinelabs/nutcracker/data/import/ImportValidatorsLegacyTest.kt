package com.offlinelabs.nutcracker.data.import

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportValidatorsLegacyTest {

    private fun checkIn(mealId: String, notes: String?): CsvRow {
        val data = mutableMapOf(
            "id" to "1",
            "mealId" to mealId,
            "checkInDate" to "2025-01-01",
            "checkInDateTime" to "2025-01-01 08:00:00",
            "servingSize" to "1.0"
        )
        if (notes != null) data["notes"] = notes
        return CsvRow(2, data, data.keys.toList())
    }

    @Test
    fun acceptsPositiveMealId() {
        assertTrue(ImportValidators.validateMealCheckInData(checkIn("5", null)).isValid)
    }

    @Test
    fun acceptsLegacyPlaceholderWithRecipeNotes() {
        assertTrue(ImportValidators.validateMealCheckInData(checkIn("-1", "recipe:12")).isValid)
        assertTrue(ImportValidators.validateMealCheckInData(checkIn("0", "recipe:3")).isValid)
    }

    @Test
    fun rejectsInvalidMealIdWithoutRecipeNotes() {
        assertFalse(ImportValidators.validateMealCheckInData(checkIn("-1", null)).isValid)
        assertFalse(ImportValidators.validateMealCheckInData(checkIn("-1", "")).isValid)
        assertFalse(ImportValidators.validateMealCheckInData(checkIn("-1", "recipe:abc")).isValid)
        assertFalse(ImportValidators.validateMealCheckInData(checkIn("-1", "something else")).isValid)
        assertFalse(ImportValidators.validateMealCheckInData(checkIn("abc", "recipe:1")).isValid)
    }
}
