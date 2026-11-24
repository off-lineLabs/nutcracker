package com.offlinelabs.nutcracker.ui.components.dialogs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetsLogicTest {

    @Test
    fun sessionLog_isTheLatestOfTheDay_inTheRoutineOrWithoutOne() {
        val logs = listOf(
            DayLog(1, exerciseId = 7, routineId = null, logDateTime = "2026-11-23 09:00:00"),
            DayLog(2, exerciseId = 7, routineId = 4, logDateTime = "2026-11-23 10:00:00"),
            DayLog(3, exerciseId = 7, routineId = 5, logDateTime = "2026-11-23 11:00:00"),
            DayLog(4, exerciseId = 8, routineId = 4, logDateTime = "2026-11-23 12:00:00")
        )
        assertEquals(3L, sessionLog(logs, 7, routineId = null)?.logId)
        assertEquals(2L, sessionLog(logs, 7, routineId = 4)?.logId)
        assertEquals(1L, sessionLog(logs, 7, routineId = 9)?.logId)
        assertNull(sessionLog(logs, 99, routineId = null))
    }

    @Test
    fun plannedSetCount_coversDefaultsDoneSetsAndAddedOnes() {
        assertEquals(3, plannedSetCount(3, emptyList(), 0))
        assertEquals(1, plannedSetCount(0, emptyList(), 0))
        assertEquals(5, plannedSetCount(3, listOf(0, 4), 0))
        assertEquals(4, plannedSetCount(3, listOf(0), 1))
        // Regression: an added set, then all four done, is all done (it used to plan a fifth,
        // and ticking the row's box kept adding sets)
        assertEquals(4, plannedSetCount(3, listOf(0, 1, 2, 3), 1))
        assertEquals(SetsTick.ALL, setsTick(4, plannedSetCount(3, listOf(0, 1, 2, 3), 1)))
    }

    @Test
    fun carriedSetValue_followsTheLatestEarlierChange() {
        val changes = mapOf(1 to 45.0, 3 to 50.0)
        assertEquals(40.0, carriedSetValue(0, changes, rowValue = 40.0), 0.0)
        assertEquals(45.0, carriedSetValue(1, changes, rowValue = 40.0), 0.0)
        assertEquals(45.0, carriedSetValue(2, changes, rowValue = 40.0), 0.0)
        assertEquals(50.0, carriedSetValue(4, changes, rowValue = 40.0), 0.0)
    }

    @Test
    fun setsTick_states() {
        assertEquals(SetsTick.NONE, setsTick(0, 3))
        assertEquals(SetsTick.PARTIAL, setsTick(2, 3))
        assertEquals(SetsTick.ALL, setsTick(3, 3))
        assertEquals(SetsTick.ALL, setsTick(4, 3))
    }
}
