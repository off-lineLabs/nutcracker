package com.offlinelabs.nutcracker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SplitKcalTest {

    @Test
    fun splitsByCaloriesNotGrams() {
        // Protein locked at 400 kcal of a 2000 kcal goal; carbs 250 g (1000 kcal), fat 65 g (585 kcal).
        val (carbsKcal, fatKcal) = splitKcal(1600.0, 1000.0, 585.0)
        assertEquals(1009.5, carbsKcal, 0.1)
        assertEquals(590.5, fatKcal, 0.1)
        // So the markers sit next to the current values: ~252 g carbs, ~66 g fat.
        assertEquals(252.0, carbsKcal / 4.0, 1.0)
        assertEquals(66.0, fatKcal / 9.0, 1.0)
    }

    @Test
    fun bothZeroSplitsEvenly() {
        assertEquals(800.0 to 800.0, splitKcal(1600.0, 0.0, 0.0))
    }

    @Test
    fun negativeRemainingGivesZero() {
        assertEquals(0.0 to 0.0, splitKcal(-300.0, 1000.0, 585.0))
    }
}
