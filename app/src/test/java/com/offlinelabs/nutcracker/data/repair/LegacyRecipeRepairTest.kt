package com.offlinelabs.nutcracker.data.repair

import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.MealCheckIn
import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyRecipeRepairTest {

    private class FakeStore : LegacyRecipeRepairStore {
        val meals = mutableMapOf<Long, Meal>()
        val checkIns = mutableMapOf<Long, MealCheckIn>()
        /** recipeId -> calories of its snapshot; missing key = recipe missing / no ingredients */
        val recipeCalories = mutableMapOf<Long, Int>()
        private var nextMealId = 100L

        fun meal(id: Long, name: String, calories: Int, visible: Boolean = true) =
            Meal(
                id = id, name = name, calories = calories, carbohydrates_g = 0.0, protein_g = 0.0,
                fat_g = 0.0, fiber_g = 0.0, sodium_mg = 0.0, servingSize_value = 1.0,
                servingSize_unit = ServingSizeUnit.SERVINGS, isVisible = visible
            ).also { meals[id] = it }

        fun checkIn(id: Long, mealId: Long, notes: String?) =
            MealCheckIn(
                id = id, mealId = mealId, checkInDate = "2026-01-01",
                checkInDateTime = "2026-01-01 10:00:0$id", servingSize = 1.0, notes = notes
            ).also { checkIns[id] = it }

        override suspend fun getCheckInsWithInvalidMeal() = checkIns.values.filter { it.mealId <= 0 }.sortedBy { it.id }
        override suspend fun getMeal(mealId: Long) = meals[mealId]
        override suspend fun createRecipeSnapshotMeal(recipeId: Long): Long? {
            val cal = recipeCalories[recipeId] ?: return null
            return insertMeal(
                Meal(
                    name = "Recipe $recipeId", calories = cal, carbohydrates_g = 0.0, protein_g = 0.0,
                    fat_g = 0.0, fiber_g = 0.0, sodium_mg = 0.0, servingSize_value = 1.0,
                    servingSize_unit = ServingSizeUnit.SERVINGS, notes = "recipe:$recipeId", isVisible = false
                )
            )
        }
        override suspend fun insertMeal(meal: Meal): Long {
            val id = if (meal.id == 0L) nextMealId++ else meal.id
            meals[id] = meal.copy(id = id)
            return id
        }
        override suspend fun updateMealCheckIn(checkIn: MealCheckIn) { checkIns[checkIn.id] = checkIn }
        override suspend fun deleteUnreferencedInvalidMeals(): Int {
            val used = checkIns.values.map { it.mealId }.toSet()
            val doomed = meals.keys.filter { it <= 0 && it !in used }
            doomed.forEach { meals.remove(it) }
            return doomed.size
        }
        override suspend fun <T> inTransaction(block: suspend () -> T): T = block()
    }

    @Test
    fun brokenCheckInsGetOwnSnapshotsAndStrayMealIsDeleted() = runBlocking {
        val store = FakeStore()
        store.meal(-1, "Last recipe", 999)
        store.recipeCalories[1] = 300
        store.recipeCalories[2] = 500
        store.checkIn(1, -1, "recipe:1")
        store.checkIn(2, -1, "recipe:2")
        store.checkIn(3, -1, "recipe:1")

        assertEquals(3, LegacyRecipeRepair(store).repairLegacyRecipeCheckIns())

        val ids = store.checkIns.values.map { it.mealId }
        assertEquals(3, ids.toSet().size)
        assertTrue(ids.all { it > 0 })
        assertEquals(300, store.meals[store.checkIns[1]!!.mealId]!!.calories)
        assertEquals(500, store.meals[store.checkIns[2]!!.mealId]!!.calories)
        assertEquals(300, store.meals[store.checkIns[3]!!.mealId]!!.calories)
        assertTrue(ids.all { !store.meals[it]!!.isVisible })
        assertNull(store.meals[-1L])
        // History fields preserved
        assertEquals("recipe:2", store.checkIns[2]!!.notes)
        assertEquals("2026-01-01 10:00:02", store.checkIns[2]!!.checkInDateTime)
    }

    @Test
    fun missingRecipeFallsBackToCloningReferencedMeal() = runBlocking {
        val store = FakeStore()
        store.meal(-1, "Stray", 777)
        store.checkIn(1, -1, "recipe:42") // recipe 42 does not exist
        store.checkIn(2, -1, "something else")

        assertEquals(2, LegacyRecipeRepair(store).repairLegacyRecipeCheckIns())

        val a = store.meals[store.checkIns[1]!!.mealId]!!
        val b = store.meals[store.checkIns[2]!!.mealId]!!
        assertEquals(777, a.calories)
        assertEquals(777, b.calories)
        assertFalse(a.isVisible)
        assertNotEquals(a.id, b.id)
        assertNull(store.meals[-1L])
    }

    @Test
    fun secondRunDoesNothing() = runBlocking {
        val store = FakeStore()
        store.meal(-1, "Stray", 1)
        store.recipeCalories[1] = 300
        store.checkIn(1, -1, "recipe:1")
        val repair = LegacyRecipeRepair(store)

        assertEquals(1, repair.repairLegacyRecipeCheckIns())
        val after = store.checkIns.toMap()
        val mealCount = store.meals.size

        assertEquals(0, repair.repairLegacyRecipeCheckIns())
        assertEquals(after, store.checkIns)
        assertEquals(mealCount, store.meals.size)
    }

    @Test
    fun normalCheckInsAreUntouched() = runBlocking {
        val store = FakeStore()
        store.meal(5, "Apple", 95)
        store.meal(-1, "Stray", 1)
        store.recipeCalories[1] = 300
        store.checkIn(1, 5, null)
        store.checkIn(2, 5, "recipe:1") // valid meal id, even with recipe-like notes
        store.checkIn(3, -1, "recipe:1")
        val before1 = store.checkIns[1]
        val before2 = store.checkIns[2]

        assertEquals(1, LegacyRecipeRepair(store).repairLegacyRecipeCheckIns())

        assertEquals(before1, store.checkIns[1])
        assertEquals(before2, store.checkIns[2])
        assertEquals(95, store.meals[5L]!!.calories)
        assertTrue(store.meals[5L]!!.isVisible)
    }

    @Test
    fun parseRecipeIdOnlyAcceptsExactFormat() {
        assertEquals(12L, LegacyRecipeRepair.parseRecipeId("recipe:12"))
        assertNull(LegacyRecipeRepair.parseRecipeId("recipe:abc"))
        assertNull(LegacyRecipeRepair.parseRecipeId("my recipe:1"))
        assertNull(LegacyRecipeRepair.parseRecipeId(null))
    }
}
