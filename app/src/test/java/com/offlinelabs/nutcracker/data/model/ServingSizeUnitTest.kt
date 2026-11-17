package com.offlinelabs.nutcracker.data.model

import com.offlinelabs.nutcracker.data.model.ServingSizeUnit.Companion.servingMultiplier
import org.junit.Assert.assertEquals
import org.junit.Test

class ServingSizeUnitTest {

    private val delta = 1e-6

    @Test
    fun sameUnit_isPlainRatio() {
        assertEquals(1.5, servingMultiplier(150.0, ServingSizeUnit.GRAMS, 100.0, ServingSizeUnit.GRAMS), delta)
    }

    @Test
    fun weightUnits_areConverted() {
        // 0.5 kg of a food whose serving is 100 g = 5 servings
        assertEquals(5.0, servingMultiplier(0.5, ServingSizeUnit.KILOGRAMS, 100.0, ServingSizeUnit.GRAMS), delta)
        // 1 lb vs 100 g serving
        assertEquals(4.5359237, servingMultiplier(1.0, ServingSizeUnit.POUNDS, 100.0, ServingSizeUnit.GRAMS), delta)
    }

    @Test
    fun volumeUnits_areConverted() {
        // 1 L of a drink whose serving is 250 ml = 4 servings
        assertEquals(4.0, servingMultiplier(1.0, ServingSizeUnit.LITERS, 250.0, ServingSizeUnit.MILLILITERS), delta)
        // 3 tsp = 1 tbsp
        assertEquals(1.0, servingMultiplier(3.0, ServingSizeUnit.TEASPOONS, 1.0, ServingSizeUnit.TABLESPOONS), delta)
    }

    @Test
    fun servingsUnit_countsMealServings() {
        assertEquals(2.0, servingMultiplier(2.0, ServingSizeUnit.SERVINGS, 100.0, ServingSizeUnit.GRAMS), delta)
        assertEquals(0.5, servingMultiplier(0.5, ServingSizeUnit.PORTIONS, 30.0, ServingSizeUnit.MILLILITERS), delta)
    }

    @Test
    fun nonConvertibleUnits_fallBackToSameUnitAssumption() {
        // grams <-> millilitres would need a density, so keep the old behaviour
        assertEquals(2.0, servingMultiplier(200.0, ServingSizeUnit.MILLILITERS, 100.0, ServingSizeUnit.GRAMS), delta)
        assertEquals(3.0, servingMultiplier(3.0, ServingSizeUnit.PIECES, 1.0, ServingSizeUnit.SLICES), delta)
    }

    @Test
    fun nonPositiveServingSize_returnsZero() {
        assertEquals(0.0, servingMultiplier(100.0, ServingSizeUnit.GRAMS, 0.0, ServingSizeUnit.GRAMS), delta)
        assertEquals(0.0, servingMultiplier(100.0, ServingSizeUnit.GRAMS, -5.0, ServingSizeUnit.GRAMS), delta)
    }
}
