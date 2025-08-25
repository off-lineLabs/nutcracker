package com.example.template.data.repo

import com.example.template.data.dao.MealDao
import com.example.template.data.dao.MealCheckInDao
import com.example.template.data.dao.UserGoalDao
import com.example.template.data.dao.DailyTotals
import com.example.template.data.model.Meal
import com.example.template.data.model.MealCheckIn
import com.example.template.data.model.UserGoal
import kotlinx.coroutines.flow.Flow

/**
 * Interface for data operations.
 */
interface FoodLogRepository {
    // Meal operations
    fun getAllMeals(): Flow<List<Meal>>
    fun getMealById(mealId: Long): Flow<Meal?>
    suspend fun insertMeal(meal: Meal): Long
    suspend fun updateMeal(meal: Meal)
    suspend fun deleteMeal(meal: Meal)
    suspend fun deleteAllMeals()

    // UserGoal operations
    fun getUserGoal(): Flow<UserGoal?>
    suspend fun upsertUserGoal(userGoal: UserGoal)

    // MealCheckIn operations
    suspend fun insertMealCheckIn(mealCheckIn: MealCheckIn): Long
    suspend fun deleteMealCheckIn(mealCheckIn: MealCheckIn)
    fun getCheckInsByDate(date: String): Flow<List<MealCheckIn>>
    fun getRecentCheckIns(limit: Int = 10): Flow<List<MealCheckIn>>
    fun getDailyNutritionSummary(date: String): Flow<List<com.example.template.data.dao.DailyNutritionEntry>>
    fun getDailyNutrientTotals(date: String): Flow<DailyTotals?>

    // TODO: Add methods for Exercise operations later
}

/**
 * Implementation of the repository that uses Room DAOs.
 */
class OfflineFoodLogRepository(
    private val mealDao: MealDao,
    private val userGoalDao: UserGoalDao, // Added UserGoalDao
    private val mealCheckInDao: MealCheckInDao // Added MealCheckInDao
) : FoodLogRepository {

    // Meal operations
    override fun getAllMeals(): Flow<List<Meal>> = mealDao.getAllMeals()
    override fun getMealById(mealId: Long): Flow<Meal?> = mealDao.getMealById(mealId)
    override suspend fun insertMeal(meal: Meal): Long = mealDao.insertMeal(meal)
    override suspend fun updateMeal(meal: Meal) = mealDao.updateMeal(meal)
    override suspend fun deleteMeal(meal: Meal) = mealDao.deleteMeal(meal)
    override suspend fun deleteAllMeals() = mealDao.deleteAllMeals()

    // UserGoal operations
    override fun getUserGoal(): Flow<UserGoal?> = userGoalDao.getUserGoal()
    override suspend fun upsertUserGoal(userGoal: UserGoal) = userGoalDao.upsertUserGoal(userGoal)

    // MealCheckIn operations
    override suspend fun insertMealCheckIn(mealCheckIn: MealCheckIn): Long = mealCheckInDao.insertMealCheckIn(mealCheckIn)
    override suspend fun deleteMealCheckIn(mealCheckIn: MealCheckIn) = mealCheckInDao.deleteMealCheckIn(mealCheckIn)
    override fun getCheckInsByDate(date: String): Flow<List<MealCheckIn>> = mealCheckInDao.getCheckInsByDate(date)
    override fun getRecentCheckIns(limit: Int): Flow<List<MealCheckIn>> = mealCheckInDao.getRecentCheckIns(limit)
    override fun getDailyNutritionSummary(date: String): Flow<List<com.example.template.data.dao.DailyNutritionEntry>> = mealCheckInDao.getDailyNutritionSummary(date)
    override fun getDailyNutrientTotals(date: String): Flow<DailyTotals?> = mealCheckInDao.getDailyNutrientTotals(date)

    // TODO: Implement methods for Exercise operations later
}
