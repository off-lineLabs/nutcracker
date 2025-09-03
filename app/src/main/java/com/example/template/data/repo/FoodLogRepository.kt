package com.example.template.data.repo

import com.example.template.data.dao.MealDao
import com.example.template.data.dao.MealCheckInDao
import com.example.template.data.dao.UserGoalDao
import com.example.template.data.dao.ExerciseDao
import com.example.template.data.dao.ExerciseLogDao
import com.example.template.data.dao.DailyTotals
import com.example.template.data.dao.DailyNutritionEntry
import com.example.template.data.dao.DailyExerciseEntry
import com.example.template.data.model.Meal
import com.example.template.data.model.MealCheckIn
import com.example.template.data.model.UserGoal
import com.example.template.data.model.Exercise
import com.example.template.data.model.ExerciseLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

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
    fun getCheckInsByDate(date: String): Flow<List<DailyNutritionEntry>>
    fun getRecentCheckIns(limit: Int = 10): Flow<List<MealCheckIn>>
    fun getDailyNutrientTotals(date: String): Flow<DailyTotals?>

    // Exercise operations
    fun getAllExercises(): Flow<List<Exercise>>
    fun getExerciseById(exerciseId: Long): Flow<Exercise?>
    suspend fun insertExercise(exercise: Exercise): Long
    suspend fun updateExercise(exercise: Exercise)
    suspend fun deleteExercise(exercise: Exercise)
    suspend fun deleteAllExercises()

    // ExerciseLog operations
    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long
    suspend fun deleteExerciseLog(exerciseLog: ExerciseLog)
    fun getExerciseLogsByDate(date: String): Flow<List<DailyExerciseEntry>>
    fun getRecentExerciseLogs(limit: Int = 10): Flow<List<ExerciseLog>>
    fun getLastLogForExercise(exerciseId: Long): Flow<ExerciseLog?>
    fun getMaxWeightForExercise(exerciseId: Long): Flow<Double?>
    fun getDailyExerciseCalories(date: String): Flow<Double>

    // Combined operations for dashboard
    fun getDailyCombinedSummary(date: String): Flow<List<Any>>
    fun getDailyCombinedTotals(date: String, includeExerciseCalories: Boolean = true): Flow<DailyTotals?>
}

/**
 * Implementation of the repository that uses Room DAOs.
 */
class OfflineFoodLogRepository(
    private val mealDao: MealDao,
    private val userGoalDao: UserGoalDao,
    private val mealCheckInDao: MealCheckInDao,
    private val exerciseDao: ExerciseDao,
    private val exerciseLogDao: ExerciseLogDao
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
    override fun getCheckInsByDate(date: String): Flow<List<DailyNutritionEntry>> = mealCheckInDao.getDailyNutritionSummary(date)
    override fun getRecentCheckIns(limit: Int): Flow<List<MealCheckIn>> = mealCheckInDao.getRecentCheckIns(limit)
    override fun getDailyNutrientTotals(date: String): Flow<DailyTotals?> = mealCheckInDao.getDailyNutrientTotals(date)

    // Exercise operations
    override fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAllExercises()
    override fun getExerciseById(exerciseId: Long): Flow<Exercise?> = exerciseDao.getExerciseById(exerciseId)
    override suspend fun insertExercise(exercise: Exercise): Long = exerciseDao.upsertExercise(exercise)
    override suspend fun updateExercise(exercise: Exercise) = exerciseDao.updateExercise(exercise)
    override suspend fun deleteExercise(exercise: Exercise) = exerciseDao.deleteExercise(exercise)
    override suspend fun deleteAllExercises() = exerciseDao.deleteAllExercises()

    // ExerciseLog operations
    override suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long = exerciseLogDao.insertExerciseLog(exerciseLog)
    override suspend fun deleteExerciseLog(exerciseLog: ExerciseLog) = exerciseLogDao.deleteExerciseLog(exerciseLog)
    override fun getExerciseLogsByDate(date: String): Flow<List<DailyExerciseEntry>> = exerciseLogDao.getDailyExerciseSummary(date)
    override fun getRecentExerciseLogs(limit: Int): Flow<List<ExerciseLog>> = exerciseLogDao.getRecentLogs(limit)
    override fun getLastLogForExercise(exerciseId: Long): Flow<ExerciseLog?> = exerciseLogDao.getLastLogForExercise(exerciseId)
    override fun getMaxWeightForExercise(exerciseId: Long): Flow<Double?> = exerciseLogDao.getMaxWeightForExercise(exerciseId)
    override fun getDailyExerciseCalories(date: String): Flow<Double> = exerciseLogDao.getDailyExerciseCalories(date)

    // Combined operations for dashboard
    override fun getDailyCombinedSummary(date: String): Flow<List<Any>> {
        return combine(
            getCheckInsByDate(date),
            getExerciseLogsByDate(date)
        ) { meals, exercises ->
            val combined = mutableListOf<Any>()
            combined.addAll(meals)
            combined.addAll(exercises)
            combined.sortedByDescending { 
                when (it) {
                    is DailyNutritionEntry -> it.checkInDateTime
                    is DailyExerciseEntry -> it.logDateTime
                    else -> ""
                }
            }
        }
    }

    override fun getDailyCombinedTotals(date: String, includeExerciseCalories: Boolean): Flow<DailyTotals?> {
        return if (includeExerciseCalories) {
            combine(
                getDailyNutrientTotals(date),
                getDailyExerciseCalories(date)
            ) { mealTotals, exerciseCalories ->
                mealTotals?.let { totals ->
                    // Cap consumed calories at 0 to prevent negative values
                    val netCalories = (totals.totalCalories - exerciseCalories).coerceAtLeast(0.0)
                    DailyTotals(
                        totalCalories = netCalories,
                        totalCarbohydrates = totals.totalCarbohydrates,
                        totalProtein = totals.totalProtein,
                        totalFat = totals.totalFat,
                        totalFiber = totals.totalFiber,
                        totalSodium = totals.totalSodium
                    )
                }
            }
        } else {
            getDailyNutrientTotals(date)
        }
    }
}
