package com.example.template.data.dao // I'll create this new 'dao' package

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.template.data.model.Meal
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: Meal): Long // Returns the new rowId of the inserted item

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
}