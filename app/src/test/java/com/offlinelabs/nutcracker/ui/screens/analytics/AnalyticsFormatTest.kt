package com.offlinelabs.nutcracker.ui.screens.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class AnalyticsFormatTest {

    private val us = Locale.US
    private val es = Locale("es", "ES")
    private val ptBr = Locale("pt", "BR")

    // ---- numbers ----

    @Test
    fun formatWholeGroupsByLocaleAndRoundsHalfUp() {
        assertEquals("1,850", formatWhole(1849.5, us))
        assertEquals("12.345", formatWhole(12345.0, es))
        assertEquals("12.345", formatWhole(12345.0, ptBr))
        assertEquals("0", formatWhole(-0.2, us))
        assertEquals("", formatWhole(Double.NaN, us))
    }

    @Test
    fun formatSignedWholeUsesPlusAndTypographicMinus() {
        assertEquals("+150", formatSignedWhole(149.6, us))
        assertEquals("−1,200", formatSignedWhole(-1200.0, us))
        assertEquals("0", formatSignedWhole(0.4, us))
        assertEquals("0", formatSignedWhole(-0.4, us))
    }

    @Test
    fun axisValuesAreFullBelowTenThousandAndCompactAbove() {
        assertEquals("0", formatAxisValue(0.0, us))
        assertEquals("2,500", formatAxisValue(2500.0, us))
        assertEquals("12k", formatAxisValue(12000.0, us))
        assertEquals("12.5k", formatAxisValue(12500.0, us))
        assertEquals("12,5k", formatAxisValue(12500.0, es))
    }

    @Test
    fun goalStatusRoundsToWholeKcal() {
        assertEquals(GoalStatus.OVER, goalStatus(0.6))
        assertEquals(GoalStatus.ON, goalStatus(0.4))
        assertEquals(GoalStatus.ON, goalStatus(-0.4))
        assertEquals(GoalStatus.UNDER, goalStatus(-250.0))
    }

    @Test
    fun goalFractionNeedsValueAndPositiveGoal() {
        assertEquals(0.5, goalFraction(125.0, 250.0)!!, 1e-9)
        assertNull(goalFraction(null, 250.0))
        assertNull(goalFraction(100.0, 0.0))
    }

    @Test
    fun roundPercentagesAlwaysSumToHundred() {
        assertEquals(listOf(33, 33, 34).sorted(), roundPercentages(listOf(1.0, 1.0, 1.0)).sorted())
        assertEquals(100, roundPercentages(listOf(1.0, 1.0, 1.0)).sum())
        assertEquals(listOf(50, 50, 0), roundPercentages(listOf(2.0, 2.0, 0.0)))
        assertEquals(listOf(0, 0), roundPercentages(listOf(0.0, 0.0)))
    }

    @Test
    fun macroEnergyUsesFourFourNine() {
        // 100 g carbs = 400, 60 g protein = 240, 40 g fat = 360 kcal
        assertEquals(listOf(40, 24, 36), macroEnergyPercentages(100.0, 60.0, 40.0))
        assertNull(macroEnergyPercentages(null, null, null))
        assertNull(macroEnergyPercentages(0.0, 0.0, 0.0))
    }

    // ---- axis ----

    @Test
    fun niceTicksStartAtZeroAndCoverTheMax() {
        assertEquals(listOf(0.0, 1000.0, 2000.0, 3000.0), niceAxisTicks(2300.0))
        assertEquals(listOf(0.0, 500.0, 1000.0, 1500.0, 2000.0), niceAxisTicks(1800.0))
        assertEquals(listOf(0.0, 500.0, 1000.0, 1500.0, 2000.0), niceAxisTicks(2000.0))
        val ticks = niceAxisTicks(0.0)
        assertEquals(0.0, ticks.first(), 0.0)
        assertTrue(ticks.last() > 0)
        for (max in listOf(1.0, 7.0, 95.0, 1234.0, 2601.0, 48_000.0)) {
            val t = niceAxisTicks(max)
            assertTrue("top ${t.last()} < $max", t.last() >= max)
            assertTrue("too many ticks for $max: $t", t.size in 3..6)
        }
    }

    @Test
    fun labelStepFitsWidestLabel() {
        assertEquals(1, labelStep(maxLabelWidth = 20f, slotWidth = 40f, minGap = 6f))
        assertEquals(2, labelStep(maxLabelWidth = 40f, slotWidth = 26f, minGap = 6f))
        assertEquals(1, labelStep(10f, 0f, 6f))
    }

    @Test
    fun visibleLabelsAreAnchoredOnTheNewestAndNeverOverlap() {
        // 12 month labels of ~22px in 26px slots with a 6px gap -> every other label
        val widths = List(12) { 22f }
        val shown = visibleLabelIndices(widths, slotWidth = 26f, minGap = 6f)
        assertEquals(listOf(1, 3, 5, 7, 9, 11), shown)
        assertNoOverlap(shown, widths, 26f, 6f)
        // Wide enough slots show everything
        assertEquals((0 until 7).toList(), visibleLabelIndices(List(7) { 20f }, 44f, 6f))
    }

    @Test
    fun selectedLabelIsAlwaysShownAndNeighboursMakeRoom() {
        val widths = List(12) { 22f }
        val shown = visibleLabelIndices(widths, slotWidth = 26f, minGap = 6f, selected = 4)
        assertTrue(4 in shown)
        assertFalse(3 in shown)
        assertFalse(5 in shown)
        assertNoOverlap(shown, widths, 26f, 6f)
    }

    @Test
    fun groupLabelsAppearOnlyWhereTheGroupChanges() {
        val groups = listOf("2025", "2026", "2026", "2026", "2026", "2026")
        // All primaries visible, slots wide enough
        assertEquals(listOf(0, 1), groupLabelIndices((0..5).toList(), groups, List(6) { 26f }, 40f, 6f))
        // Adjacent captions that would touch: the first one wins
        assertEquals(listOf(0), groupLabelIndices((0..5).toList(), groups, List(6) { 30f }, 26f, 6f))
        // Only every other primary visible: the year shows under the first visible of each year
        val months = listOf("2025", "2025", "2026", "2026", "2026", "2026")
        assertEquals(listOf(1, 3), groupLabelIndices(listOf(1, 3, 5), months, List(6) { 26f }, 26f, 6f))
    }

    @Test
    fun axisLabelsPerPeriod() {
        val monthsStart = LocalDate.of(2025, 12, 1)
        val months = axisLabels(TimePeriod.MONTHS, listOf(monthsStart, monthsStart.plusMonths(1)), us)
        assertEquals(AxisLabel("Dec", "2025"), months[0])
        assertEquals(AxisLabel("Jan", "2026"), months[1])
        val weeks = axisLabels(TimePeriod.WEEKS, listOf(LocalDate.of(2026, 11, 9)), us)
        assertEquals(AxisLabel("9", "Nov"), weeks[0])
        val days = axisLabels(TimePeriod.DAYS, listOf(LocalDate.of(2026, 11, 20)), us)
        assertEquals(AxisLabel("Fri", "20"), days[0])
        // Capitalized in Spanish too
        val esMonth = axisLabels(TimePeriod.MONTHS, listOf(monthsStart), es)[0].primary
        assertTrue(esMonth, esMonth.first().isUpperCase())
    }

    // ---- dates ----

    @Test
    fun dateRanges() {
        val nov = LocalDate.of(2026, 11, 1)
        assertEquals("November 2026", formatDateRange(nov, LocalDate.of(2026, 11, 20), monthly = true, locale = us))
        assertEquals("Dec 2025 – Nov 2026", formatDateRange(LocalDate.of(2025, 12, 1), LocalDate.of(2026, 11, 20), true, us))
        assertEquals("Nov 10, 2026 – Nov 16, 2026", formatDateRange(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 16), false, us))
        assertEquals("Friday, November 20, 2026", formatDateRange(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 20), false, us))
    }

    @Test
    fun elapsedEndClipsFutureDays() {
        val stats = AnalyticsAggregator.statsFor(
            emptyList(), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 20),
            2000, includeExercise = false, includeTef = false
        )
        val summary = AnalyticsSummary(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), stats, 0, 0, daysInRange = 20)
        assertEquals(LocalDate.of(2026, 11, 20), elapsedEnd(summary))
    }

    private fun assertNoOverlap(shown: List<Int>, widths: List<Float>, slot: Float, gap: Float) {
        shown.zipWithNext { a, b ->
            assertTrue("labels $a and $b overlap", (b - a) * slot >= (widths[a] + widths[b]) / 2 + gap)
        }
    }
}
