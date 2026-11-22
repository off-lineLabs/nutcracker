package com.offlinelabs.nutcracker.data

import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.Gym
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GymSelectionTest {

    private val gyms = listOf(Gym(id = 1, name = "A"), Gym(id = 2, name = "B"))

    private fun ex(id: Long, gymId: Long?, visible: Boolean = true) =
        Exercise(id = id, name = "E$id", gymId = gymId, isVisible = visible)

    @Test
    fun resolve_nullIsAllGyms() {
        assertEquals(GymSelection.AllGyms, resolveGymSelection(null, gyms))
    }

    @Test
    fun resolve_knownIdIsSpecific() {
        assertEquals(GymSelection.Specific(2), resolveGymSelection(2, gyms))
    }

    @Test
    fun resolve_staleIdIsAllGyms() {
        assertEquals(GymSelection.AllGyms, resolveGymSelection(99, gyms))
        assertEquals(GymSelection.AllGyms, resolveGymSelection(1, emptyList()))
    }

    @Test
    fun partition_specificSplitsGymAndShared_keepingOrder() {
        val list = listOf(ex(1, null), ex(2, 1), ex(3, 2), ex(4, 1), ex(5, null))
        val result = partitionExercisesForSelection(list, GymSelection.Specific(1))
        assertEquals(listOf(2L, 4L), result.gymExercises.map { it.id })
        assertEquals(listOf(1L, 5L), result.shared.map { it.id })
    }

    @Test
    fun partition_allGymsOnlyShared() {
        val list = listOf(ex(1, null), ex(2, 1), ex(3, null))
        val result = partitionExercisesForSelection(list, GymSelection.AllGyms)
        assertTrue(result.gymExercises.isEmpty())
        assertEquals(listOf(1L, 3L), result.shared.map { it.id })
    }

    @Test
    fun partition_excludesHidden() {
        val list = listOf(ex(1, null, visible = false), ex(2, 1, visible = false), ex(3, 1), ex(4, null))
        val result = partitionExercisesForSelection(list, GymSelection.Specific(1))
        assertEquals(listOf(3L), result.gymExercises.map { it.id })
        assertEquals(listOf(4L), result.shared.map { it.id })
    }

    @Test
    fun partition_emptyInput() {
        val result = partitionExercisesForSelection(emptyList(), GymSelection.Specific(1))
        assertTrue(result.gymExercises.isEmpty() && result.shared.isEmpty())
    }

    @Test
    fun label_withUnit() {
        assertEquals("BASIC FIT · Universitat", gymLabel("  BASIC FIT ", " Universitat "))
    }

    @Test
    fun label_withoutUnit() {
        assertEquals("BASIC FIT", gymLabel("BASIC FIT", null))
        assertEquals("BASIC FIT", gymLabel(" BASIC FIT ", "   "))
        assertEquals("BASIC FIT", gymLabel("BASIC FIT", ""))
    }
}
