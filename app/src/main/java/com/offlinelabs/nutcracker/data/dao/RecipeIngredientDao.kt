package com.offlinelabs.nutcracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.offlinelabs.nutcracker.data.model.RecipeIngredient
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeIngredientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipeIngredient(ingredient: RecipeIngredient): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipeIngredients(ingredients: List<RecipeIngredient>)

    @Update
    suspend fun updateRecipeIngredient(ingredient: RecipeIngredient)

    @Delete
    suspend fun deleteRecipeIngredient(ingredient: RecipeIngredient)

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY `order` ASC")
    fun getIngredientsByRecipeId(recipeId: Long): Flow<List<RecipeIngredient>>

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY `order` ASC")
    suspend fun getIngredientsByRecipeIdSync(recipeId: Long): List<RecipeIngredient>

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsByRecipeId(recipeId: Long)

    @Query("DELETE FROM recipe_ingredients WHERE id = :ingredientId")
    suspend fun deleteIngredientById(ingredientId: Long)
}
