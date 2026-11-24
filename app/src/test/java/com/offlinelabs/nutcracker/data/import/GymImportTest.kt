package com.offlinelabs.nutcracker.data.import

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GymImportTest {

    private fun row(vararg pairs: Pair<String, String>): CsvRow {
        val data = pairs.toMap()
        return CsvRow(2, data, data.keys.toList())
    }

    @Test
    fun exerciseKey_sameNameDifferentGymDiffers() {
        val a = ImportMergeKeys.exerciseKey("Leg press", "strength", 1L)
        val b = ImportMergeKeys.exerciseKey("Leg press", "strength", 2L)
        assertNotEquals(a, b)
        assertEquals(a, ImportMergeKeys.exerciseKey(" Leg press ", "strength", 1L))
    }

    @Test
    fun exerciseKey_sharedDedupesAgainstSharedOnly() {
        val shared = ImportMergeKeys.exerciseKey("Leg press", "strength")
        assertEquals(shared, ImportMergeKeys.exerciseKey("Leg press", "strength", null))
        assertNotEquals(shared, ImportMergeKeys.exerciseKey("Leg press", "strength", 1L))
    }

    @Test
    fun gymKey_ignoresCaseAndWhitespace_blankUnitEqualsNull() {
        assertEquals(ImportMergeKeys.gymKey("Gym A", "kg"), ImportMergeKeys.gymKey("  gym a ", " KG "))
        assertEquals(ImportMergeKeys.gymKey("Gym A", null), ImportMergeKeys.gymKey("Gym A", "  "))
        assertNotEquals(ImportMergeKeys.gymKey("Gym A", "kg"), ImportMergeKeys.gymKey("Gym A", "lb"))
        assertNotEquals(ImportMergeKeys.gymKey("Gym A", null), ImportMergeKeys.gymKey("Gym B", null))
    }

    @Test
    fun resolveGymId_mapsKnownIdsAndFallsBackToShared() {
        val mapping = mapOf("5" to 50L)
        val lookup: (String) -> Long? = { mapping[it] }
        assertEquals(50L, ImportMergeKeys.resolveGymId("5", lookup))
        assertNull(ImportMergeKeys.resolveGymId(null, lookup)) // column missing (old backup)
        assertNull(ImportMergeKeys.resolveGymId("", lookup)) // empty = shared
        assertNull(ImportMergeKeys.resolveGymId("9", lookup)) // not in mapping
        assertNull(ImportMergeKeys.resolveGymId("abc", lookup))
    }

    @Test
    fun exerciseRow_withAndWithoutGymIdColumn_parsesAndValidates() {
        val old = row("id" to "1", "name" to "Squat", "category" to "strength", "isVisible" to "true")
        val withGym = row("id" to "1", "name" to "Squat", "category" to "strength", "isVisible" to "true", "gymId" to "3")
        val empty = row("id" to "1", "name" to "Squat", "category" to "strength", "isVisible" to "true", "gymId" to "")
        listOf(old, withGym, empty).forEach { assertTrue(ImportValidators.validateExerciseData(it).isValid) }
        val lookup: (String) -> Long? = { if (it == "3") 30L else null }
        assertNull(ImportMergeKeys.resolveGymId(old.getValue("gymId"), lookup))
        assertEquals(30L, ImportMergeKeys.resolveGymId(withGym.getValue("gymId"), lookup))
        assertNull(ImportMergeKeys.resolveGymId(empty.getValue("gymId"), lookup))
    }

    @Test
    fun validateGymData_requiresNameAndParsableVisibility() {
        assertTrue(ImportValidators.validateGymData(row("name" to "Gym A", "isVisible" to "false")).isValid)
        assertTrue(ImportValidators.validateGymData(row("name" to "Gym A")).isValid)
        assertFalse(ImportValidators.validateGymData(row("name" to "  ", "isVisible" to "true")).isValid)
        assertFalse(ImportValidators.validateGymData(row("name" to "Gym A", "isVisible" to "maybe")).isValid)
    }
}
