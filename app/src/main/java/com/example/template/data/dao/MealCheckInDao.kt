package com.example.template.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.template.data.model.MealCheckIn
import kotlinx.coroutines.flow.Flow

@Dao
interface MealCheckInDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealCheckIn(mealCheckIn: MealCheckIn): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateMealCheckIn(mealCheckIn: MealCheckIn)

    @Delete
    suspend fun deleteMealCheckIn(mealCheckIn: MealCheckIn)

    @Query("SELECT * FROM meal_check_ins WHERE checkInDate = :date ORDER BY checkInDateTime DESC")
    fun getCheckInsByDate(date: String): Flow<List<MealCheckIn>>

    @Query("SELECT * FROM meal_check_ins ORDER BY checkInDateTime DESC LIMIT :limit")
    fun getRecentCheckIns(limit: Int = 10): Flow<List<MealCheckIn>>

    @Query("DELETE FROM meal_check_ins WHERE checkInDate = :date")
    suspend fun deleteCheckInsByDate(date: String)

    @Query("DELETE FROM meal_check_ins")
    suspend fun deleteAllCheckIns()

    // Complex query to get daily calorie consumption with meal details
    @Transaction
    @Query("""
        SELECT 
            mci.id as checkInId,
            mci.mealId,
            mci.checkInDate,
            mci.checkInDateTime,
            mci.servingSize,
            mci.notes,
            m.name as mealName,
            m.calories as mealCalories,
            m.carbohydrates_g as mealCarbs,
            m.protein_g as mealProtein,
            m.fat_g as mealFat,
            m.fiber_g as mealFiber,
            m.sodium_mg as mealSodium,
            (m.calories * mci.servingSize) as totalCalories,
            (m.carbohydrates_g * mci.servingSize) as totalCarbs,
            (m.protein_g * mci.servingSize) as totalProtein,
            (m.fat_g * mci.servingSize) as totalFat,
            (m.fiber_g * mci.servingSize) as totalFiber,
            (m.sodium_mg * mci.servingSize) as totalSodium
        FROM meal_check_ins mci
        INNER JOIN meals m ON mci.mealId = m.id
        WHERE mci.checkInDate = :date
        ORDER BY mci.checkInDateTime DESC
    """)
    fun getDailyNutritionSummary(date: String): Flow<List<DailyNutritionEntry>>

    // Query to get total calories and nutrients for a specific date
    @Query("""
        SELECT 
            COALESCE(SUM(m.calories * mci.servingSize), 0.0) as totalCalories,
            COALESCE(SUM(m.carbohydrates_g * mci.servingSize), 0.0) as totalCarbohydrates,
            COALESCE(SUM(m.protein_g * mci.servingSize), 0.0) as totalProtein,
            COALESCE(SUM(m.fat_g * mci.servingSize), 0.0) as totalFat,
            COALESCE(SUM(m.fiber_g * mci.servingSize), 0.0) as totalFiber,
            COALESCE(SUM(m.sodium_mg * mci.servingSize), 0.0) as totalSodium
        FROM meal_check_ins mci
        INNER JOIN meals m ON mci.mealId = m.id
        WHERE mci.checkInDate = :date
    """)
    fun getDailyNutrientTotals(date: String): Flow<DailyTotals?>
}

// Data class for the complex query result
data class DailyNutritionEntry(
    val checkInId: Long,
    val mealId: Long,
    val checkInDate: String,
    val checkInDateTime: String,
    val servingSize: Double,
    val notes: String?,
    val mealName: String,
    val mealCalories: Int,
    val mealCarbs: Double,
    val mealProtein: Double,
    val mealFat: Double,
    val mealFiber: Double,
    val mealSodium: Double,
    val totalCalories: Double,
    val totalCarbs: Double,
    val totalProtein: Double,
    val totalFat: Double,
    val totalFiber: Double,
    val totalSodium: Double
)

// Data class for daily nutrient totals
data class DailyTotals(
    val totalCalories: Double,
    val totalCarbohydrates: Double,
    val totalProtein: Double,
    val totalFat: Double,
    val totalFiber: Double,
    val totalSodium: Double
)
