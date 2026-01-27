package com.offlinelabs.nutcracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.offlinelabs.nutcracker.data.model.Recipe
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe): Long

    @Update
    suspend fun updateRecipe(recipe: Recipe)

    @Delete
    suspend fun deleteRecipe(recipe: Recipe)

    @Query("SELECT * FROM recipes WHERE isVisible = 1 ORDER BY name ASC")
    fun getAllRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    fun getRecipeById(recipeId: Long): Flow<Recipe?>

    @Query("SELECT * FROM recipes WHERE isVisible = 1 AND id = :recipeId")
    fun getVisibleRecipeById(recipeId: Long): Flow<Recipe?>

    @Query("UPDATE recipes SET isVisible = 0 WHERE id = :recipeId")
    suspend fun hideRecipe(recipeId: Long) // Soft delete - hide recipe

    @Query("DELETE FROM recipes")
    suspend fun deleteAllRecipes()
}
