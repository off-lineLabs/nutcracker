package com.offlinelabs.nutcracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.offlinelabs.nutcracker.data.model.MealTag
import kotlinx.coroutines.flow.Flow

@Dao
interface MealTagDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealTag(mealTag: MealTag): Long
    
    @Delete
    suspend fun deleteMealTag(mealTag: MealTag)
    
    @Query("SELECT * FROM meal_tags WHERE mealId = :mealId")
    fun getTagsForMeal(mealId: Long): Flow<List<MealTag>>
    
    @Query("SELECT * FROM meal_tags WHERE tagId = :tagId")
    fun getMealsForTag(tagId: Long): Flow<List<MealTag>>
    
    @Query("DELETE FROM meal_tags WHERE mealId = :mealId")
    suspend fun deleteTagsForMeal(mealId: Long)
    
    @Query("DELETE FROM meal_tags WHERE tagId = :tagId")
    suspend fun deleteMealsForTag(tagId: Long)
    
    @Query("DELETE FROM meal_tags")
    suspend fun deleteAllMealTags()
    
    @Query("SELECT * FROM meal_tags")
    fun getAllMealTags(): Flow<List<MealTag>>
}
