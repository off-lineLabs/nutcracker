package com.offlinelabs.nutcracker.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DateTimePickerLogicTest {

    @Test
    fun timePartRanges() {
        assertTrue(isValidTimePart("00", 23))
        assertTrue(isValidTimePart("23", 23))
        assertFalse(isValidTimePart("24", 23))
        assertTrue(isValidTimePart("59", 59))
        assertFalse(isValidTimePart("60", 59))
        assertFalse(isValidTimePart("", 59))
        assertFalse(isValidTimePart("-1", 59))
    }

    private fun withZone(id: String, block: () -> Unit) {
        val old = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            block()
        } finally {
            TimeZone.setDefault(old)
        }
    }

    @Test
    fun pickedDayKeepsLocalDayAndTimeInNegativeOffsetZone() {
        withZone("America/Sao_Paulo") {
            val current = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 19, 21, 45, 30) }.time
            val utcMidnightNov20 = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear(); set(2026, Calendar.NOVEMBER, 20)
            }.timeInMillis
            val result = Calendar.getInstance().apply { time = applyPickedDate(current, utcMidnightNov20) }
            assertEquals(2026, result.get(Calendar.YEAR))
            assertEquals(Calendar.NOVEMBER, result.get(Calendar.MONTH))
            assertEquals(20, result.get(Calendar.DAY_OF_MONTH))
            assertEquals(21, result.get(Calendar.HOUR_OF_DAY))
            assertEquals(45, result.get(Calendar.MINUTE))
            assertEquals(0, result.get(Calendar.SECOND))
        }
    }

    @Test
    fun pickerMillisRoundTripsInPositiveOffsetZone() {
        withZone("Pacific/Auckland") {
            val current = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 19, 1, 10, 0) }.time
            val millis = dateToPickerMillis(current)
            val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
            assertEquals(19, utc.get(Calendar.DAY_OF_MONTH))
            val back = Calendar.getInstance().apply { time = applyPickedDate(current, millis) }
            assertEquals(19, back.get(Calendar.DAY_OF_MONTH))
            assertEquals(1, back.get(Calendar.HOUR_OF_DAY))
        }
    }
}
