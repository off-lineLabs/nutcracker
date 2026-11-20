package com.offlinelabs.nutcracker.ui.screens.analytics

import com.offlinelabs.nutcracker.data.dao.DailyExerciseCaloriesRow
import com.offlinelabs.nutcracker.data.dao.DailyFoodTotalsRow
import com.offlinelabs.nutcracker.data.model.UserGoal
import com.offlinelabs.nutcracker.utils.TEFCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class AnalyticsAggregatorTest {

    private val us = Locale.US
    private val es = Locale("es", "ES")
    private val goal = UserGoal.default().copy(caloriesGoal = 2000)

    private fun day(
        date: LocalDate,
        kcal: Double = 2000.0,
        carbs: Double = 0.0,
        protein: Double = 0.0,
        fat: Double = 0.0,
        exercise: Double = 0.0,
        count: Int = 1
    ) = AnalyticsDayInput(date, kcal, carbs, protein, fat, 10.0, 1000.0, exercise, count)

    private fun state(
        days: List<AnalyticsDayInput>,
        today: LocalDate,
        period: TimePeriod = TimePeriod.DAYS,
        ex: Boolean = false,
        tef: Boolean = false,
        selected: Int? = null,
        locale: Locale = us
    ) = AnalyticsAggregator.buildState(days, goal, today, period, ex, tef, selected, locale)

    @Test
    fun averageBalanceDividesByLoggedDaysOnly() {
        val today = LocalDate.of(2025, 11, 20)
        val days = listOf(
            day(LocalDate.of(2025, 11, 14), kcal = 2200.0),
            day(LocalDate.of(2025, 11, 16), kcal = 2400.0),
            day(today, kcal = 2600.0)
        )
        val s = state(days, today)
        val stats = s.summary!!.stats
        assertEquals(3, stats.loggedDays)
        assertEquals(3, s.summary!!.daysLogged)
        assertEquals(6000.0, stats.goalKcal, 0.001)
        assertEquals(1200.0, stats.balanceKcal, 0.001)
        assertEquals(400.0, stats.avgBalancePerLoggedDay!!, 0.001)
        assertEquals(2400.0, stats.avgIntakePerLoggedDay!!, 0.001)
        assertEquals(10.0, stats.avgFiberG!!, 0.001)
        assertEquals(1000.0, stats.avgSaltMg!!, 0.001)
        assertFalse(s.isEmpty)

        // Selecting the last bar (today) only aggregates that day
        val sel = state(days, today, selected = 6).summary!!
        assertEquals(1, sel.daysLogged)
        assertEquals(600.0, sel.stats.avgBalancePerLoggedDay!!, 0.001)
        assertEquals(today, sel.startDate)
    }

    @Test
    fun daysBucketsEndToday() {
        val today = LocalDate.of(2025, 11, 20)
        val s = state(emptyList(), today)
        assertEquals(7, s.buckets.size)
        assertEquals(today.minusDays(6), s.buckets.first().startDate)
        assertEquals(today, s.buckets.last().startDate)
        assertEquals("Thu", s.buckets.last().label)
    }

    @Test
    fun weeksAcrossMonthBoundaryUseLocaleWeekStart() {
        // Tue 2 Dec 2025. Sun 30 Nov and Mon 1 Dec.
        val today = LocalDate.of(2025, 12, 2)
        val days = listOf(
            day(LocalDate.of(2025, 11, 30), kcal = 1000.0),
            day(LocalDate.of(2025, 12, 1), kcal = 2000.0)
        )
        val usState = state(days, today, TimePeriod.WEEKS, locale = us)
        assertEquals(12, usState.buckets.size)
        val usCurrent = usState.buckets.last()
        assertEquals(LocalDate.of(2025, 11, 30), usCurrent.startDate) // Sunday
        assertEquals(LocalDate.of(2025, 12, 6), usCurrent.endDate)
        assertEquals(2, usCurrent.stats.loggedDays)
        assertEquals(3000.0, usCurrent.stats.consumedKcal, 0.001)
        assertEquals(LocalDate.of(2025, 11, 30).minusWeeks(11), usState.buckets.first().startDate)
        assertEquals("Nov 30", usCurrent.label)

        val esState = state(days, today, TimePeriod.WEEKS, locale = es)
        val esCurrent = esState.buckets.last()
        assertEquals(LocalDate.of(2025, 12, 1), esCurrent.startDate) // Monday
        assertEquals(1, esCurrent.stats.loggedDays)
        val esPrev = esState.buckets[10]
        assertEquals(LocalDate.of(2025, 11, 24), esPrev.startDate)
        assertEquals(LocalDate.of(2025, 11, 30), esPrev.endDate)
        assertEquals(1, esPrev.stats.loggedDays)
        assertTrue(esCurrent.label, esCurrent.label.startsWith("1 "))
        assertTrue(esPrev.label, esPrev.label.contains("nov", ignoreCase = true))
    }

    @Test
    fun monthsCoverTwelveMonthsAcrossYearBoundary() {
        val today = LocalDate.of(2026, 2, 10)
        val days = listOf(
            day(LocalDate.of(2025, 12, 31), kcal = 1800.0),
            day(LocalDate.of(2026, 1, 1), kcal = 2200.0)
        )
        val s = state(days, today, TimePeriod.MONTHS)
        assertEquals(12, s.buckets.size)
        assertEquals(LocalDate.of(2025, 3, 1), s.buckets.first().startDate)
        assertEquals(LocalDate.of(2026, 2, 1), s.buckets.last().startDate)
        assertEquals(LocalDate.of(2026, 2, 28), s.buckets.last().endDate)
        val dec = s.buckets[9]
        val jan = s.buckets[10]
        assertEquals(LocalDate.of(2025, 12, 1), dec.startDate)
        assertEquals(1, dec.stats.loggedDays)
        assertEquals(1800.0, dec.stats.consumedKcal, 0.001)
        assertEquals(1, jan.stats.loggedDays)
        assertEquals(2200.0, jan.stats.consumedKcal, 0.001)
        // Year only shown when it differs from the current year
        assertEquals("Dec 2025", dec.label)
        assertEquals("Jan", jan.label)
        assertEquals("Mar 2025", s.buckets.first().label)
    }

    @Test
    fun spanishLabelsAreLocalized() {
        val today = LocalDate.of(2026, 2, 10) // Tuesday
        val months = state(emptyList(), today, TimePeriod.MONTHS, locale = es)
        assertTrue(months.buckets[10].label, months.buckets[10].label.startsWith("ene", ignoreCase = true))
        val days = state(emptyList(), today, TimePeriod.DAYS, locale = es)
        assertTrue(days.buckets.last().label, days.buckets.last().label.startsWith("mar", ignoreCase = true))
    }

    @Test
    fun futureDaysAreNeverCounted() {
        val today = LocalDate.of(2025, 11, 20) // Thursday
        val days = listOf(
            day(today, kcal = 2000.0),
            day(today.plusDays(1), kcal = 5000.0),
            day(today.plusDays(2), kcal = 5000.0)
        )
        val week = state(days, today, TimePeriod.WEEKS).buckets.last()
        assertEquals(LocalDate.of(2025, 11, 22), week.endDate)
        assertEquals(1, week.stats.loggedDays)
        assertEquals(2000.0, week.stats.consumedKcal, 0.001)
        val month = state(days, today, TimePeriod.MONTHS).buckets.last()
        assertEquals(1, month.stats.loggedDays)
        assertEquals(1, state(days, today).summary!!.daysLogged)
        assertEquals(1, state(days, today).summary!!.streak)
    }

    @Test
    fun streakCases() {
        val today = LocalDate.of(2025, 11, 20)
        fun streak(vararg offsets: Int) = AnalyticsAggregator.currentStreak(
            offsets.map { day(today.minusDays(it.toLong())) }, today
        )
        assertEquals(3, streak(0, 1, 2)) // today logged
        assertEquals(2, streak(1, 2)) // today not logged yet: ends yesterday
        assertEquals(1, streak(0, 2, 3)) // gap
        assertEquals(0, streak(2, 3)) // neither today nor yesterday
        assertEquals(0, streak())
        // a day with zero check-ins does not count
        val zero = listOf(day(today, count = 0), day(today.minusDays(1)))
        assertEquals(1, AnalyticsAggregator.currentStreak(zero, today))
    }

    @Test
    fun togglesChangeNetAndBalanceLikeCombinedTotals() {
        val today = LocalDate.of(2025, 11, 20)
        val d = day(today, kcal = 2000.0, carbs = 200.0, protein = 100.0, fat = 70.0, exercise = 300.0)
        val tef = TEFCalculator.calculateTEFBonus(100.0, 200.0, 70.0)

        fun stats(ex: Boolean, t: Boolean) =
            state(listOf(d), today, ex = ex, tef = t).summary!!.stats

        assertEquals(2000.0, stats(false, false).netKcal, 0.001)
        assertEquals(1700.0, stats(true, false).netKcal, 0.001)
        assertEquals(2000.0 - tef, stats(false, true).netKcal, 0.001)
        assertEquals(2000.0 - 300.0 - tef, stats(true, true).netKcal, 0.001)
        assertEquals(2000.0 - 300.0 - tef - 2000.0, stats(true, true).balanceKcal, 0.001)
        assertEquals(-300.0, stats(true, false).avgBalancePerLoggedDay!!, 0.001)
        // raw exercise and TEF are always filled, whatever the toggles
        assertEquals(300.0, stats(false, false).exerciseKcal, 0.001)
        assertEquals(tef, stats(false, false).tefKcal, 0.001)
        // consumed never changes
        assertEquals(2000.0, stats(true, true).consumedKcal, 0.001)
    }

    @Test
    fun netIsCappedAtZeroPerDay() {
        val today = LocalDate.of(2025, 11, 20)
        val d = day(today, kcal = 500.0, exercise = 5000.0)
        val stats = state(listOf(d), today, ex = true).summary!!.stats
        assertEquals(0.0, stats.netKcal, 0.001)
        assertEquals(-2000.0, stats.balanceKcal, 0.001)
    }

    @Test
    fun zeroLoggedDaysGivesNullAveragesAndNoCrash() {
        val today = LocalDate.of(2025, 11, 20)
        for (period in TimePeriod.values()) {
            val s = state(emptyList(), today, period, ex = true, tef = true)
            val stats = s.summary!!.stats
            assertTrue(s.isEmpty)
            assertEquals(0, stats.loggedDays)
            assertEquals(0, s.summary!!.streak)
            assertNull(stats.avgIntakePerLoggedDay)
            assertNull(stats.avgNetPerLoggedDay)
            assertNull(stats.avgBalancePerLoggedDay)
            assertNull(stats.avgCarbsG)
            assertNull(stats.avgSaltMg)
            assertEquals(0.0, stats.balanceKcal, 0.0)
            s.buckets.forEach { assertNull(it.stats.avgNetPerLoggedDay) }
        }
        // out-of-range selection is ignored
        assertNull(state(emptyList(), today, selected = 99).selectedBucketIndex)
    }

    @Test
    fun mergeRowsJoinsExerciseByDate() {
        val food = listOf(
            DailyFoodTotalsRow("2025-11-19", 1800.0, 200.0, 90.0, 60.0, 25.0, 4000.0, 3),
            DailyFoodTotalsRow("2025-11-20", 2100.0, 250.0, 80.0, 70.0, 20.0, 5000.0, 2)
        )
        val exercise = listOf(
            DailyExerciseCaloriesRow("2025-11-20", 250.0),
            DailyExerciseCaloriesRow("2025-11-01", 999.0)
        )
        val merged = AnalyticsAggregator.mergeRows(food, exercise)
        assertEquals(2, merged.size)
        assertEquals(0.0, merged[0].exerciseKcal, 0.0)
        assertEquals(250.0, merged[1].exerciseKcal, 0.0)
        assertEquals(3, merged[0].checkInCount)
        assertEquals(5000.0, merged[1].saltMg, 0.0)
        assertEquals(LocalDate.of(2025, 11, 20), merged[1].date)
    }

    @Test
    fun historyStartCoversTwelveMonths() {
        assertEquals(
            LocalDate.of(2025, 3, 1),
            AnalyticsAggregator.historyStart(LocalDate.of(2026, 2, 10))
        )
    }
}
