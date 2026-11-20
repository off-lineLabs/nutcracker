package com.offlinelabs.nutcracker.data.repair

import androidx.room.withTransaction
import com.offlinelabs.nutcracker.data.AppDatabase
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.MealCheckIn
import com.offlinelabs.nutcracker.data.repo.FoodLogRepository

/**
 * Narrow storage view needed by [LegacyRecipeRepair], so the logic can be unit-tested with a fake.
 */
interface LegacyRecipeRepairStore {
    suspend fun getCheckInsWithInvalidMeal(): List<MealCheckIn>
    suspend fun getMeal(mealId: Long): Meal?
    suspend fun createRecipeSnapshotMeal(recipeId: Long): Long?
    suspend fun insertMeal(meal: Meal): Long
    suspend fun updateMealCheckIn(checkIn: MealCheckIn)
    suspend fun deleteUnreferencedInvalidMeals(): Int
    suspend fun <T> inTransaction(block: suspend () -> T): T
}

class RoomLegacyRecipeRepairStore(
    private val database: AppDatabase,
    private val repository: FoodLogRepository
) : LegacyRecipeRepairStore {
    override suspend fun getCheckInsWithInvalidMeal() = database.mealCheckInDao().getCheckInsWithInvalidMeal()
    override suspend fun getMeal(mealId: Long) = database.mealDao().getMealByIdSync(mealId)
    override suspend fun createRecipeSnapshotMeal(recipeId: Long) = repository.createRecipeSnapshotMeal(recipeId, 1.0)
    override suspend fun insertMeal(meal: Meal) = database.mealDao().insertMeal(meal)
    override suspend fun updateMealCheckIn(checkIn: MealCheckIn) = database.mealCheckInDao().updateMealCheckIn(checkIn)
    override suspend fun deleteUnreferencedInvalidMeals() = database.mealDao().deleteUnreferencedInvalidMeals()
    override suspend fun <T> inTransaction(block: suspend () -> T): T = database.withTransaction { block() }
}

/**
 * Repairs recipe check-ins written by the old logging code, which all pointed at one shared meal
 * row with id -1 (so every check-in showed the nutrition of the last logged recipe).
 *
 * Each such check-in is repointed to its own hidden meal snapshot, then the stray row is removed.
 * Idempotent: once nothing references an invalid meal id, later runs return 0.
 */
class LegacyRecipeRepair(private val store: LegacyRecipeRepairStore) {

    /** @return number of check-ins repaired */
    suspend fun repairLegacyRecipeCheckIns(): Int = store.inTransaction {
        val broken = store.getCheckInsWithInvalidMeal()
        var repaired = 0
        for (checkIn in broken) {
            val newMealId = snapshotFor(checkIn) ?: continue
            store.updateMealCheckIn(checkIn.copy(mealId = newMealId))
            repaired++
        }
        // Repoint first (FK is RESTRICT); only now drop placeholder rows nothing references.
        store.deleteUnreferencedInvalidMeals()
        repaired
    }

    private suspend fun snapshotFor(checkIn: MealCheckIn): Long? {
        val recipeId = parseRecipeId(checkIn.notes)
        if (recipeId != null) {
            val fromRecipe = store.createRecipeSnapshotMeal(recipeId)
            if (fromRecipe != null) return fromRecipe
        }
        // Fallback: clone whatever the check-in currently points at
        val source = store.getMeal(checkIn.mealId) ?: return null
        return store.insertMeal(source.copy(id = 0, isVisible = false))
    }

    companion object {
        private val RECIPE_NOTES = Regex("^recipe:(\\d+)$")

        fun parseRecipeId(notes: String?): Long? =
            notes?.trim()?.let { RECIPE_NOTES.matchEntire(it) }?.groupValues?.get(1)?.toLongOrNull()
    }
}
