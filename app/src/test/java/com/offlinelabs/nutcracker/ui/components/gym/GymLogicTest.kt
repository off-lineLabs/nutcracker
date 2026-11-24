package com.offlinelabs.nutcracker.ui.components.gym

import com.offlinelabs.nutcracker.data.GymSelection
import com.offlinelabs.nutcracker.data.model.Gym
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GymLogicTest {
    @Test
    fun blankNamesAreInvalid() {
        assertFalse(isGymNameValid(""))
        assertFalse(isGymNameValid("   "))
        assertTrue(isGymNameValid(" Basic Fit "))
    }

    @Test
    fun buildTrimsAndNullsBlankUnit() {
        val gym = buildGymFromForm(null, "  Basic Fit ", "   ")
        assertEquals("Basic Fit", gym.name)
        assertNull(gym.unit)
        assertEquals(0L, gym.id)
        assertEquals("Universitat", buildGymFromForm(null, "A", " Universitat ").unit)
    }

    @Test
    fun editKeepsIdAndMapsFields() {
        val existing = Gym(id = 7, name = "Old", unit = "x", mapsPlaceId = "p", mapsProvider = "osm")
        val gym = buildGymFromForm(existing, "New", "")
        assertEquals(7L, gym.id)
        assertEquals("New", gym.name)
        assertNull(gym.unit)
        assertEquals("p", gym.mapsPlaceId)
        assertEquals("osm", gym.mapsProvider)
        assertTrue(gym.isVisible)
    }

    @Test
    fun selectedGymLookup() {
        val gyms = listOf(Gym(id = 1, name = "A"), Gym(id = 2, name = "B"))
        assertEquals("B", selectedGymOrNull(GymSelection.Specific(2), gyms)?.name)
        assertNull(selectedGymOrNull(GymSelection.Specific(9), gyms))
        assertNull(selectedGymOrNull(GymSelection.AllGyms, gyms))
    }
}
