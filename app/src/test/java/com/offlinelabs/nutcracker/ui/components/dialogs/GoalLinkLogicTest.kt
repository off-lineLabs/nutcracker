package com.offlinelabs.nutcracker.ui.components.dialogs

import com.offlinelabs.nutcracker.data.GoalLinkMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GoalLinkLogicTest {

    @Test
    fun nextLinkMode_togglesDownAndUp() {
        assertEquals(GoalLinkMode.MACROS_TO_KCAL, nextLinkMode(GoalLinkMode.KCAL_TO_MACROS))
        assertEquals(GoalLinkMode.KCAL_TO_MACROS, nextLinkMode(GoalLinkMode.MACROS_TO_KCAL))
    }

    @Test
    fun upModeKcalForShare_reachesTheShareOfReal() {
        for (tef in listOf(0.0, 0.25)) {
            // Shares kept low enough that the 5000 kcal cap doesn't apply.
            for (share in listOf(0.0, 0.1, 0.4, 0.75)) {
                val others = 1200.0
                val kcal = upModeKcalForShare(share, others, tef)
                val real = kcal * (1 - tef) + others
                assertEquals("share=$share tef=$tef", share, kcal / real, 1e-9)
            }
        }
    }

    @Test
    fun upModeKcalForShare_capsShareAndKcal() {
        // 0.95 of Real with 200 kcal of other macros = 3800 kcal, under the absolute cap.
        assertEquals(3800.0, upModeKcalForShare(1.0, 200.0, 0.0), 1e-6)
        // With 1000 kcal of others the share cap alone would allow 19000 kcal.
        assertEquals(UP_BAR_MAX_MACRO_KCAL, upModeKcalForShare(1.0, 1000.0, 0.0), 1e-9)
        assertEquals(0.0, upModeKcalForShare(-0.2, 1000.0, 0.0), 1e-9)
    }

    @Test
    fun upMode_noOtherMacrosUsesFixedScale() {
        assertEquals(0.5 * UP_BAR_MAX_MACRO_KCAL, upModeKcalForShare(0.5, 0.0, 0.075), 1e-9)
        assertEquals(0.5, upModeFill(0.5 * UP_BAR_MAX_MACRO_KCAL, 0.0, 0.075), 1e-9)
    }

    @Test
    fun upModeFill_invertsKcalForShare() {
        for (tef in listOf(0.0, 0.02, 0.25)) {
            for (share in listOf(0.0, 0.2, 0.5, 0.7)) {
                val kcal = upModeKcalForShare(share, 1500.0, tef)
                assertEquals("share=$share tef=$tef", share, upModeFill(kcal, 1500.0, tef), 1e-9)
            }
        }
    }

    @Test
    fun actualKcal_usesFourFourNineAndSubtractsTef() {
        assertEquals(4 * 250 + 4 * 150 + 9 * 70, actualKcalFromMacros(250, 150, 70, useTef = false))
        // TEF = 150*4*.25 + 250*4*.075 + 70*9*.02 = 150 + 75 + 12.6 = 237.6 -> 238
        assertEquals(2230 - 238, actualKcalFromMacros(250, 150, 70, useTef = true))
    }

    @Test
    fun macrosForKcal_realIsWithinToleranceOfTarget() {
        val ratioSources = listOf(Triple(250, 100, 70), Triple(300, 90, 60), Triple(100, 200, 40), Triple(0, 120, 0))
        for (useTef in listOf(false, true)) {
            for (src in ratioSources) {
                for (kcal in listOf(1200, 1500, 1800, 2000, 2250, 2500, 3100, 4000)) {
                    val (c, p, f) = macrosForKcal(kcal, src.first, src.second, src.third, useTef)
                    val real = actualKcalFromMacros(c, p, f, useTef)
                    assertTrue(
                        "kcal=$kcal src=$src tef=$useTef -> $c/$p/$f real=$real",
                        abs(real - kcal) <= REAL_KCAL_ROUNDING_TOLERANCE
                    )
                }
            }
        }
    }

    @Test
    fun macrosForKcal_keepsRatios() {
        val (c, p, f) = macrosForKcal(2000, 200, 100, 50, useTef = false)
        val (c2, p2, f2) = macrosForKcal(4000, 200, 100, 50, useTef = false)
        assertTrue(abs(c2 - 2 * c) <= 3)
        assertTrue(abs(p2 - 2 * p) <= 3)
        assertTrue(abs(f2 - 2 * f) <= 3)
    }

    @Test
    fun macrosForKcal_allZeroUsesDefaultSplit() {
        val (c, p, f) = macrosForKcal(2000, 0, 0, 0, useTef = false)
        // Default split: 55% carbs, 17.5% protein, 27.5% fat.
        assertTrue(abs(c - 275) <= 2)
        assertTrue(abs(p - 88) <= 2)
        assertTrue(abs(f - 61) <= 2)
    }

    @Test
    fun macrosForKcal_nonPositiveKcalGivesZeros() {
        assertEquals(Triple(0, 0, 0), macrosForKcal(0, 100, 100, 100, useTef = false))
    }
}
