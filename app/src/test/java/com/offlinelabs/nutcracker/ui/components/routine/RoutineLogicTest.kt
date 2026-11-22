package com.offlinelabs.nutcracker.ui.components.routine

import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.Routine
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineLogicTest {

    @Test
    fun availableExercises_areTheGymsAndShared() {
        val list = listOf(Exercise(id = 1, name = "a", gymId = 10), Exercise(id = 2, name = "b", gymId = 20), Exercise(id = 3, name = "c"))
        assertEquals(listOf(1L, 3L), exercisesAvailableAtGym(list, 10).map { it.id })
    }

    @Test
    fun routinesForGym_filtersAndOrders() {
        val routines = listOf(
            Routine(id = 1, name = "B", gymId = 10, sortOrder = 2),
            Routine(id = 2, name = "A", gymId = 10, sortOrder = 0),
            Routine(id = 3, name = "C", gymId = 20, sortOrder = 1)
        )
        assertEquals(listOf(2L, 1L), routinesForGym(routines, 10).map { it.id })
        assertEquals(listOf(2L, 3L, 1L), routinesForGym(routines, null).map { it.id })
    }
}
