package com.offlinelabs.nutcracker.data.dao // I'll create this new 'dao' package

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Upsert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.offlinelabs.nutcracker.data.model.Meal
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    /**
     * Insert or update; returns the new row id, or -1 when an existing row was updated.
     * @Upsert rather than REPLACE: REPLACE deletes the row first, which would cascade-delete its
     * meal-category links.
     */
    @Upsert
    suspend fun insertMeal(meal: Meal): Long

    @Update
    suspend fun updateMeal(meal: Meal)

    @Delete
    suspend fun deleteMeal(meal: Meal)

    @Query("SELECT * FROM meals WHERE isVisible = 1 ORDER BY name ASC")
    fun getAllMeals(): Flow<List<Meal>>

    @Query("SELECT * FROM meals WHERE id = :mealId")
    fun getMealById(mealId: Long): Flow<Meal?> // Meal can be null if not found

    @Query("SELECT * FROM meals WHERE isVisible = 1 AND id = :mealId")
    fun getVisibleMealById(mealId: Long): Flow<Meal?> // Get visible meal by ID

    @Query("UPDATE meals SET isVisible = 0 WHERE id = :mealId")
    suspend fun hideMeal(mealId: Long) // Soft delete - hide meal

    @Query("DELETE FROM meals")
    suspend fun deleteAllMeals() // Useful for clearing all meals if needed

    /** Sort order that puts a new meal at the end of the custom order. */
    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM meals")
    suspend fun nextSortOrder(): Int

    @Query("UPDATE meals SET sortOrder = :sortOrder WHERE id = :mealId")
    suspend fun setSortOrder(mealId: Long, sortOrder: Int)

    /** Stores the custom order: each id gets its index in [orderedIds]. */
    @Transaction
    suspend fun applySortOrder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> setSortOrder(id, index) }
    }

    /** Latest check-in per meal. */
    @Query("SELECT mealId AS id, MAX(checkInDateTime) AS lastCheckIn FROM meal_check_ins GROUP BY mealId")
    fun getLastCheckIns(): Flow<List<LastCheckIn>>

    /** One-shot list of every meal including hidden ones (used by export and import). */
    @Query("SELECT * FROM meals ORDER BY id ASC")
    suspend fun getAllMealsIncludingHiddenSync(): List<Meal>

    /** Deletes one meal only if no check-in, recipe ingredient or tag references it. */
    @Query("""
        DELETE FROM meals WHERE id = :mealId
        AND id NOT IN (SELECT mealId FROM meal_check_ins)
        AND id NOT IN (SELECT mealId FROM recipe_ingredients)
        AND id NOT IN (SELECT mealId FROM meal_tags)
    """)
    suspend fun deleteMealIfUnreferenced(mealId: Long): Int

    @Query("SELECT * FROM meals WHERE id = :mealId")
    suspend fun getMealByIdSync(mealId: Long): Meal? // One-shot lookup, ignores isVisible

    /**
     * Removes placeholder meals (id <= 0) that nothing references any more.
     * Used by the legacy recipe check-in repair; never deletes meals that have history.
     */
    @Query("""
        DELETE FROM meals WHERE id <= 0
        AND id NOT IN (SELECT mealId FROM meal_check_ins)
        AND id NOT IN (SELECT mealId FROM recipe_ingredients)
        AND id NOT IN (SELECT mealId FROM meal_tags)
    """)
    suspend fun deleteUnreferencedInvalidMeals(): Int
}