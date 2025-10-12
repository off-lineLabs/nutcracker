package com.offlinelabs.nutcracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.offlinelabs.nutcracker.data.model.MealCheckIn
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
    
    @Query("SELECT * FROM meal_check_ins ORDER BY checkInDateTime DESC")
    fun getAllMealCheckIns(): Flow<List<MealCheckIn>>

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
            m.brand as mealBrand,
            m.calories as mealCalories,
            m.carbohydrates_g as mealCarbs,
            m.protein_g as mealProtein,
            m.fat_g as mealFat,
            m.fiber_g as mealFiber,
            m.sodium_mg as mealSodium,
            m.saturatedFat_g as mealSaturatedFat,
            m.sugars_g as mealSugars,
            m.cholesterol_mg as mealCholesterol,
            m.vitaminC_mg as mealVitaminC,
            m.calcium_mg as mealCalcium,
            m.iron_mg as mealIron,
            m.imageUrl as mealImageUrl,
            m.localImagePath as mealLocalImagePath,
            m.novaClassification as mealNovaClassification,
            m.greenScore as mealGreenScore,
            m.nutriscore as mealNutriscore,
            m.source as mealSource,
            (m.calories * mci.servingSize) as totalCalories,
            (m.carbohydrates_g * mci.servingSize) as totalCarbs,
            (m.protein_g * mci.servingSize) as totalProtein,
            (m.fat_g * mci.servingSize) as totalFat,
            (m.fiber_g * mci.servingSize) as totalFiber,
            (m.sodium_mg * mci.servingSize) as totalSodium,
            (m.saturatedFat_g * mci.servingSize) as totalSaturatedFat,
            (m.sugars_g * mci.servingSize) as totalSugars,
            (m.cholesterol_mg * mci.servingSize) as totalCholesterol,
            (m.vitaminC_mg * mci.servingSize) as totalVitaminC,
            (m.calcium_mg * mci.servingSize) as totalCalcium,
            (m.iron_mg * mci.servingSize) as totalIron
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
            COALESCE(SUM(m.sodium_mg * mci.servingSize), 0.0) as totalSodium,
            COALESCE(SUM(m.saturatedFat_g * mci.servingSize), 0.0) as totalSaturatedFat,
            COALESCE(SUM(m.sugars_g * mci.servingSize), 0.0) as totalSugars,
            COALESCE(SUM(m.cholesterol_mg * mci.servingSize), 0.0) as totalCholesterol,
            COALESCE(SUM(m.vitaminC_mg * mci.servingSize), 0.0) as totalVitaminC,
            COALESCE(SUM(m.calcium_mg * mci.servingSize), 0.0) as totalCalcium,
            COALESCE(SUM(m.iron_mg * mci.servingSize), 0.0) as totalIron
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
    val mealBrand: String?,
    val mealCalories: Int,
    val mealCarbs: Double,
    val mealProtein: Double,
    val mealFat: Double,
    val mealFiber: Double,
    val mealSodium: Double,
    val mealSaturatedFat: Double?,
    val mealSugars: Double?,
    val mealCholesterol: Double?,
    val mealVitaminC: Double?,
    val mealCalcium: Double?,
    val mealIron: Double?,
    val mealImageUrl: String?,
    val mealLocalImagePath: String?,
    val mealNovaClassification: String?,
    val mealGreenScore: String?,
    val mealNutriscore: String?,
    val mealSource: String,
    val totalCalories: Double,
    val totalCarbs: Double,
    val totalProtein: Double,
    val totalFat: Double,
    val totalFiber: Double,
    val totalSodium: Double,
    val totalSaturatedFat: Double?,
    val totalSugars: Double?,
    val totalCholesterol: Double?,
    val totalVitaminC: Double?,
    val totalCalcium: Double?,
    val totalIron: Double?
)

// Data class for daily nutrient totals
data class DailyTotals(
    val totalCalories: Double,
    val totalCarbohydrates: Double,
    val totalProtein: Double,
    val totalFat: Double,
    val totalFiber: Double,
    val totalSodium: Double,
    val totalSaturatedFat: Double?,
    val totalSugars: Double?,
    val totalCholesterol: Double?,
    val totalVitaminC: Double?,
    val totalCalcium: Double?,
    val totalIron: Double?
)
