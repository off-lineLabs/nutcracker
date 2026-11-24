package com.offlinelabs.nutcracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.offlinelabs.nutcracker.data.model.MealCategory
import com.offlinelabs.nutcracker.data.model.MealCategoryMeal
import kotlinx.coroutines.flow.Flow

@Dao
interface MealCategoryDao {
    /** Insert or update; returns the row id (-1 on update). @Upsert keeps the links (REPLACE would cascade-delete them). */
    @Upsert
    suspend fun upsertCategory(category: MealCategory): Long

    @Query("SELECT * FROM meal_categories WHERE isVisible = 1 ORDER BY sortOrder ASC, id ASC")
    fun getVisibleCategories(): Flow<List<MealCategory>>

    @Query("""
        SELECT l.* FROM meal_category_meals l
        INNER JOIN meal_categories c ON c.id = l.categoryId
        WHERE c.isVisible = 1
    """)
    fun getVisibleLinks(): Flow<List<MealCategoryMeal>>

    @Query("SELECT categoryId FROM meal_category_meals WHERE mealId = :mealId")
    suspend fun getCategoryIdsForMeal(mealId: Long): List<Long>

    @Query("UPDATE meal_categories SET isVisible = 0 WHERE id = :id")
    suspend fun hideCategory(id: Long)

    @Query("UPDATE meal_categories SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setSortOrder(id: Long, sortOrder: Int)

    @Transaction
    suspend fun applyOrder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> setSortOrder(id, index) }
    }

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM meal_categories")
    suspend fun nextSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLink(link: MealCategoryMeal): Long

    @Query("DELETE FROM meal_category_meals WHERE mealId = :mealId")
    suspend fun deleteLinksForMeal(mealId: Long)

    /** Replaces the categories of [mealId] with [categoryIds]. */
    @Transaction
    suspend fun setCategoriesForMeal(mealId: Long, categoryIds: List<Long>) {
        deleteLinksForMeal(mealId)
        categoryIds.distinct().forEach { insertLink(MealCategoryMeal(it, mealId)) }
    }

    /** One-shot lists including hidden categories (export and import). */
    @Query("SELECT * FROM meal_categories ORDER BY id ASC")
    suspend fun getAllCategoriesSync(): List<MealCategory>

    @Query("SELECT * FROM meal_category_meals ORDER BY categoryId, mealId")
    suspend fun getAllLinksSync(): List<MealCategoryMeal>

    @Query("DELETE FROM meal_category_meals")
    suspend fun deleteAllLinks()

    @Query("DELETE FROM meal_categories")
    suspend fun deleteAllCategories()
}
