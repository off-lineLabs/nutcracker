package com.offlinelabs.nutcracker.data.import

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportMergeKeysTest {

    private fun meal(
        name: String = "Rice",
        brand: String? = null,
        calories: Int = 130,
        value: Double = 100.0,
        unit: String = "g",
        visible: Boolean = true
    ) = ImportMergeKeys.mealKey(name, brand, calories, value, unit, visible)

    @Test
    fun mealKey_trimsTextAndTreatsBlankBrandAsNull() {
        assertEquals(meal(), meal(name = "  Rice ", brand = "   "))
        assertEquals(meal(brand = null), meal(brand = ""))
        assertEquals(meal(brand = "Tio Joao"), meal(brand = " Tio Joao "))
    }

    @Test
    fun mealKey_isCaseSensitiveAndDistinguishesFields() {
        assertNotEquals(meal(), meal(name = "rice"))
        assertNotEquals(meal(), meal(brand = "X"))
        assertNotEquals(meal(), meal(calories = 131))
        assertNotEquals(meal(), meal(value = 101.0))
        assertNotEquals(meal(), meal(unit = "ml"))
        assertNotEquals(meal(visible = true), meal(visible = false))
    }

    @Test
    fun exerciseRecipeTagPillKeys() {
        assertEquals(ImportMergeKeys.exerciseKey("Squat", "strength"), ImportMergeKeys.exerciseKey(" Squat ", "strength "))
        assertNotEquals(ImportMergeKeys.exerciseKey("Squat", "strength"), ImportMergeKeys.exerciseKey("Squat", "cardio"))
        assertEquals(
            ImportMergeKeys.recipeKey("Soup", "2025-01-01 10:00:00"),
            ImportMergeKeys.recipeKey("Soup ", "2025-01-01 10:00:00")
        )
        assertNotEquals(
            ImportMergeKeys.recipeKey("Soup", "2025-01-01 10:00:00"),
            ImportMergeKeys.recipeKey("Soup", "2025-01-02 10:00:00")
        )
        assertNotEquals(ImportMergeKeys.tagKey("Vegan", "MEAL"), ImportMergeKeys.tagKey("Vegan", "EXERCISE"))
        assertEquals(ImportMergeKeys.pillKey("Vitamin D"), ImportMergeKeys.pillKey(" Vitamin D "))
    }

    @Test
    fun historyKeys_useParentNaturalKeyAndTimestamp() {
        assertEquals(
            ImportMergeKeys.mealCheckInKey(meal(), "2025-01-01 08:00:00"),
            ImportMergeKeys.mealCheckInKey(meal(name = " Rice"), "2025-01-01 08:00:00 ")
        )
        assertNotEquals(ImportMergeKeys.mealCheckInKey(meal(), "t"), ImportMergeKeys.mealCheckInKey(meal(name = "Beans"), "t"))
        val squat = ImportMergeKeys.exerciseKey("Squat", "strength")
        assertNotEquals(ImportMergeKeys.exerciseLogKey(squat, "a"), ImportMergeKeys.exerciseLogKey(squat, "b"))
        assertNotEquals(ImportMergeKeys.pillCheckInKey("A", "a"), ImportMergeKeys.pillCheckInKey("B", "a"))
        val vegan = ImportMergeKeys.tagKey("Vegan", "MEAL")
        assertEquals(ImportMergeKeys.mealTagKey(meal(), vegan), ImportMergeKeys.mealTagKey(meal(), vegan))
        assertNotEquals(
            ImportMergeKeys.mealTagKey(meal(), vegan),
            ImportMergeKeys.mealTagKey(meal(), ImportMergeKeys.tagKey("Fast", "MEAL"))
        )
        assertNotEquals(ImportMergeKeys.exerciseTagKey(squat, vegan), ImportMergeKeys.exerciseTagKey(ImportMergeKeys.exerciseKey("Run", "cardio"), vegan))
    }

    @Test
    fun existingIdQueues_mapOneToOneWithinKeyGroup() {
        val k = meal()
        val other = meal(name = "Beans")
        val q = ImportMergeKeys.ExistingIdQueues(listOf(k to 9L, other to 4L, k to 3L))
        assertEquals(3L, q.take(k))
        assertEquals(9L, q.take(k))
        assertEquals("two existing, third backup row inserts", null, q.take(k))
        assertEquals(4L, q.take(other))
        assertEquals(null, q.take(meal(name = "unknown")))
    }

    @Test
    fun reimportWithIdenticalMeals_isIdempotentWhateverTheExistingOrder() {
        // Two identical meals (S1, S2) with check-ins at t1/t2; existing rows were created in the opposite order.
        val k = meal()
        val existingCheckIns = setOf(
            ImportMergeKeys.mealCheckInKey(k, "t2"),
            ImportMergeKeys.mealCheckInKey(k, "t1")
        )
        // Both backup meals map to a pre-existing id, and their check-ins are found by (mealKey, datetime)
        assertTrue(ImportMergeKeys.mealCheckInKey(k, "t1") in existingCheckIns)
        assertTrue(ImportMergeKeys.mealCheckInKey(k, "t2") in existingCheckIns)
        assertFalse(ImportMergeKeys.mealCheckInKey(k, "t3") in existingCheckIns)
    }

    @Test
    fun legacyCheckInKey_ignoresMealIdButUsesNotes() {
        assertEquals(
            ImportMergeKeys.legacyCheckInKey("t", "recipe:3"),
            ImportMergeKeys.legacyCheckInKey(" t", " recipe:3 ")
        )
        assertNotEquals(ImportMergeKeys.legacyCheckInKey("t", "recipe:3"), ImportMergeKeys.legacyCheckInKey("t", "recipe:4"))
    }

    @Test
    fun defaultPillRule() {
        assertTrue(ImportMergeKeys.shouldRemoveSeededDefaultPill(1, 0, 1))
        assertTrue(ImportMergeKeys.shouldRemoveSeededDefaultPill(1, 0, 3))
        assertFalse("pill has history", ImportMergeKeys.shouldRemoveSeededDefaultPill(1, 2, 1))
        assertFalse("user has several pills", ImportMergeKeys.shouldRemoveSeededDefaultPill(2, 0, 1))
        assertFalse("no pills at all", ImportMergeKeys.shouldRemoveSeededDefaultPill(0, 0, 1))
        assertFalse("backup has no pills", ImportMergeKeys.shouldRemoveSeededDefaultPill(1, 0, 0))
    }

    @Test
    fun backupGoalApplied_onlyWhenMissingOrUntouchedDefault() {
        val default = com.offlinelabs.nutcracker.data.model.UserGoal.default()
        assertTrue(ImportMergeKeys.shouldApplyBackupGoal(null))
        assertTrue(ImportMergeKeys.shouldApplyBackupGoal(default))
        assertTrue(ImportMergeKeys.shouldApplyBackupGoal(default.copy(id = 7)))
        assertFalse(ImportMergeKeys.shouldApplyBackupGoal(default.copy(caloriesGoal = default.caloriesGoal + 1)))
        assertFalse(ImportMergeKeys.shouldApplyBackupGoal(default.copy(sodiumGoal_mg = 1)))
    }

    @Test
    fun legacyDecision_prefersRecipeThenPlaceholderThenSkip() {
        assertEquals(
            ImportMergeKeys.LegacyCheckInAction.SnapshotFromRecipe(7),
            ImportMergeKeys.decideLegacyCheckIn(7, 9)
        )
        assertEquals(
            ImportMergeKeys.LegacyCheckInAction.SnapshotFromRecipe(7),
            ImportMergeKeys.decideLegacyCheckIn(7, null)
        )
        assertEquals(
            ImportMergeKeys.LegacyCheckInAction.CloneMeal(9),
            ImportMergeKeys.decideLegacyCheckIn(null, 9)
        )
        assertEquals(ImportMergeKeys.LegacyCheckInAction.Skip, ImportMergeKeys.decideLegacyCheckIn(null, null))
        assertEquals("recipe:12", ImportMergeKeys.legacyNotes(12))
    }

    @Test
    fun tsvCodec_roundTripsTrickyStrings() {
        val samples = listOf(
            "plain", "tab\there", "line\nbreak", "cr\rhere", "back\\slash", "back\\nnot-newline",
            "\\\\", "trailing\\", "mix\t\n\r\\t\\n", ""
        )
        samples.forEach { assertEquals(it, TsvCodec.unescape(TsvCodec.escape(it))) }
        assertFalse(TsvCodec.escape("a\tb\nc").contains('\t'))
        assertFalse(TsvCodec.escape("a\tb\nc").contains('\n'))
    }
}
