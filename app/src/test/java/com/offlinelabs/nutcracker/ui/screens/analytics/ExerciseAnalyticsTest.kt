package com.offlinelabs.nutcracker.ui.screens.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class ExerciseAnalyticsTest {
    @Test
    fun gridStartMondayFirst() {
        // 1 Nov 2026 is a Sunday
        assertEquals(LocalDate.of(2026, 10, 26), calendarGridStart(YearMonth.of(2026, 11), DayOfWeek.MONDAY))
    }

    @Test
    fun gridStartSundayFirstWhenMonthStartsOnSunday() {
        assertEquals(LocalDate.of(2026, 11, 1), calendarGridStart(YearMonth.of(2026, 11), DayOfWeek.SUNDAY))
    }

    @Test
    fun gridHas42ConsecutiveDays() {
        val days = calendarGridDays(YearMonth.of(2026, 2), DayOfWeek.MONDAY)
        assertEquals(42, days.size)
        assertEquals(DayOfWeek.MONDAY, days.first().dayOfWeek)
        assertEquals(days.first().plusDays(41), days.last())
    }

    @Test
    fun weekdaysStartAtFirstDay() {
        val days = orderedWeekdays(DayOfWeek.SUNDAY)
        assertEquals(DayOfWeek.SUNDAY, days.first())
        assertEquals(DayOfWeek.SATURDAY, days.last())
    }

    @Test
    fun avgPerActiveDay() {
        assertNull(avgKcalPerActiveDay(500.0, 0))
        assertEquals(250.0, avgKcalPerActiveDay(500.0, 2)!!, 0.0001)
    }

    @Test
    fun daysSince() {
        val today = LocalDate.of(2026, 11, 18)
        assertNull(daysSinceLastExercise(null, today))
        assertEquals(0, daysSinceLastExercise(today, today))
        assertEquals(3, daysSinceLastExercise(today.minusDays(3), today))
        assertEquals(0, daysSinceLastExercise(today.plusDays(2), today))
    }

    @Test
    fun parseMusclesSplitsDedupesAndCapitalizes() {
        assertEquals(
            listOf("Chest", "Triceps", "Back"),
            parseMuscles(listOf("chest|triceps", "|chest|", "back", ""), Locale.ENGLISH)
        )
    }
}
