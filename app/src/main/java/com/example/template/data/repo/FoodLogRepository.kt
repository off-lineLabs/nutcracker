package com.example.template.data.repo

import com.example.template.data.dao.MealDao
import com.example.template.data.dao.MealCheckInDao
import com.example.template.data.dao.UserGoalDao
import com.example.template.data.dao.ExerciseDao
import com.example.template.data.dao.ExerciseLogDao
import com.example.template.data.dao.PillDao
import com.example.template.data.dao.PillCheckInDao
import com.example.template.data.dao.DailyTotals
import com.example.template.data.dao.DailyNutritionEntry
import com.example.template.data.dao.DailyExerciseEntry
import com.example.template.data.model.Meal
import com.example.template.data.model.MealCheckIn
import com.example.template.data.model.UserGoal
import com.example.template.data.model.Exercise
import com.example.template.data.model.ExerciseLog
import com.example.template.data.model.Pill
import com.example.template.data.model.PillCheckIn
import com.example.template.data.service.ExerciseImageService
import com.example.template.data.service.ImageDownloadService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

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
    suspend fun updateMealCheckIn(mealCheckIn: MealCheckIn)
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
    suspend fun updateExerciseLog(exerciseLog: ExerciseLog)
    suspend fun deleteExerciseLog(exerciseLog: ExerciseLog)
    fun getExerciseLogsByDate(date: String): Flow<List<DailyExerciseEntry>>
    fun getRecentExerciseLogs(limit: Int = 10): Flow<List<ExerciseLog>>
    fun getLastLogForExercise(exerciseId: Long): Flow<ExerciseLog?>
    fun getMaxWeightForExercise(exerciseId: Long): Flow<Double?>
    fun getDailyExerciseCalories(date: String): Flow<Double>

    // Pill operations
    fun getAllPills(): Flow<List<Pill>>
    fun getPillById(pillId: Long): Flow<Pill?>
    suspend fun insertPill(pill: Pill): Long
    suspend fun updatePill(pill: Pill)
    suspend fun deletePill(pill: Pill)
    suspend fun deleteAllPills()

    // PillCheckIn operations
    suspend fun insertPillCheckIn(pillCheckIn: PillCheckIn): Long
    suspend fun updatePillCheckIn(pillCheckIn: PillCheckIn)
    suspend fun deletePillCheckIn(pillCheckIn: PillCheckIn)
    fun getPillCheckInsByDate(date: String): Flow<List<PillCheckIn>>
    fun getPillCheckInByPillIdAndDate(pillId: Long, date: String): Flow<PillCheckIn?>
    suspend fun deletePillCheckInByPillIdAndDate(pillId: Long, date: String)

    // Combined operations for dashboard
    fun getDailyCombinedSummary(date: String): Flow<List<Any>>
    fun getDailyCombinedTotals(date: String, includeExerciseCalories: Boolean = true, includeTEFBonus: Boolean = false): Flow<DailyTotals?>
}

/**
 * Implementation of the repository that uses Room DAOs.
 */
class OfflineFoodLogRepository(
    private val mealDao: MealDao,
    private val userGoalDao: UserGoalDao,
    private val mealCheckInDao: MealCheckInDao,
    private val exerciseDao: ExerciseDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val pillDao: PillDao,
    private val pillCheckInDao: PillCheckInDao,
    private val exerciseImageService: ExerciseImageService? = null,
    private val imageDownloadService: ImageDownloadService? = null
) : FoodLogRepository {

    // Meal operations
    override fun getAllMeals(): Flow<List<Meal>> = mealDao.getAllMeals()
    override fun getMealById(mealId: Long): Flow<Meal?> = mealDao.getMealById(mealId)
    override suspend fun insertMeal(meal: Meal): Long = mealDao.insertMeal(meal)
    override suspend fun updateMeal(meal: Meal) = mealDao.updateMeal(meal)
    override suspend fun deleteMeal(meal: Meal) {
        // Delete the associated image if it exists
        meal.localImagePath?.let { imagePath ->
            imageDownloadService?.deleteLocalImage(imagePath)
        }
        mealDao.hideMeal(meal.id)
    }
    override suspend fun deleteAllMeals() = mealDao.deleteAllMeals()

    // UserGoal operations
    override fun getUserGoal(): Flow<UserGoal?> = userGoalDao.getUserGoal()
    override suspend fun upsertUserGoal(userGoal: UserGoal) = userGoalDao.upsertUserGoal(userGoal)

    // MealCheckIn operations
    override suspend fun insertMealCheckIn(mealCheckIn: MealCheckIn): Long = mealCheckInDao.insertMealCheckIn(mealCheckIn)
    override suspend fun updateMealCheckIn(mealCheckIn: MealCheckIn) = mealCheckInDao.updateMealCheckIn(mealCheckIn)
    override suspend fun deleteMealCheckIn(mealCheckIn: MealCheckIn) = mealCheckInDao.deleteMealCheckIn(mealCheckIn)
    override fun getCheckInsByDate(date: String): Flow<List<DailyNutritionEntry>> = mealCheckInDao.getDailyNutritionSummary(date)
    override fun getRecentCheckIns(limit: Int): Flow<List<MealCheckIn>> = mealCheckInDao.getRecentCheckIns(limit)
    override fun getDailyNutrientTotals(date: String): Flow<DailyTotals?> = mealCheckInDao.getDailyNutrientTotals(date)

    // Exercise operations
    override fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAllExercises()
    override fun getExerciseById(exerciseId: Long): Flow<Exercise?> = exerciseDao.getExerciseById(exerciseId)
    override suspend fun insertExercise(exercise: Exercise): Long {
        com.example.template.util.logger.AppLogger.i("FoodLogRepository", "insertExercise called:")
        com.example.template.util.logger.AppLogger.i("FoodLogRepository", "  - name: ${exercise.name}")
        com.example.template.util.logger.AppLogger.i("FoodLogRepository", "  - imagePaths.size: ${exercise.imagePaths.size}")
        exercise.imagePaths.forEachIndexed { index, path ->
            com.example.template.util.logger.AppLogger.i("FoodLogRepository", "    imagePath $index: $path")
        }
        val id = exerciseDao.upsertExercise(exercise)
        com.example.template.util.logger.AppLogger.i("FoodLogRepository", "insertExercise returned ID: $id")
        return id
    }
    override suspend fun updateExercise(exercise: Exercise) {
        com.example.template.util.logger.AppLogger.i("FoodLogRepository", "updateExercise called:")
        com.example.template.util.logger.AppLogger.i("FoodLogRepository", "  - ID: ${exercise.id}")
        com.example.template.util.logger.AppLogger.i("FoodLogRepository", "  - name: ${exercise.name}")
        com.example.template.util.logger.AppLogger.i("FoodLogRepository", "  - imagePaths.size: ${exercise.imagePaths.size}")
        exercise.imagePaths.forEachIndexed { index, path ->
            com.example.template.util.logger.AppLogger.i("FoodLogRepository", "    imagePath $index: $path")
        }
        exerciseDao.updateExercise(exercise)
        com.example.template.util.logger.AppLogger.i("FoodLogRepository", "updateExercise completed")
    }
    override suspend fun deleteExercise(exercise: Exercise) {
        // Delete the associated images if they exist
        exercise.imagePaths.forEach { imagePath ->
            exerciseImageService?.deleteImage(imagePath)
        }
        exerciseDao.hideExercise(exercise.id)
    }
    override suspend fun deleteAllExercises() = exerciseDao.deleteAllExercises()

    // ExerciseLog operations
    override suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long = exerciseLogDao.insertExerciseLog(exerciseLog)
    override suspend fun updateExerciseLog(exerciseLog: ExerciseLog) = exerciseLogDao.updateExerciseLog(exerciseLog)
    override suspend fun deleteExerciseLog(exerciseLog: ExerciseLog) = exerciseLogDao.deleteExerciseLog(exerciseLog)
    override fun getExerciseLogsByDate(date: String): Flow<List<DailyExerciseEntry>> = exerciseLogDao.getDailyExerciseSummary(date)
    override fun getRecentExerciseLogs(limit: Int): Flow<List<ExerciseLog>> = exerciseLogDao.getRecentLogs(limit)
    override fun getLastLogForExercise(exerciseId: Long): Flow<ExerciseLog?> = exerciseLogDao.getLastLogForExercise(exerciseId)
    override fun getMaxWeightForExercise(exerciseId: Long): Flow<Double?> = exerciseLogDao.getMaxWeightForExercise(exerciseId)
    override fun getDailyExerciseCalories(date: String): Flow<Double> = exerciseLogDao.getDailyExerciseCalories(date)

    // Pill operations
    override fun getAllPills(): Flow<List<Pill>> = pillDao.getAllPills()
    override fun getPillById(pillId: Long): Flow<Pill?> = pillDao.getPillById(pillId)
    override suspend fun insertPill(pill: Pill): Long = pillDao.insertPill(pill)
    override suspend fun updatePill(pill: Pill) = pillDao.updatePill(pill)
    override suspend fun deletePill(pill: Pill) = pillDao.deletePill(pill)
    override suspend fun deleteAllPills() = pillDao.deleteAllPills()

    // PillCheckIn operations
    override suspend fun insertPillCheckIn(pillCheckIn: PillCheckIn): Long = pillCheckInDao.insertPillCheckIn(pillCheckIn)
    override suspend fun updatePillCheckIn(pillCheckIn: PillCheckIn) = pillCheckInDao.updatePillCheckIn(pillCheckIn)
    override suspend fun deletePillCheckIn(pillCheckIn: PillCheckIn) = pillCheckInDao.deletePillCheckIn(pillCheckIn)
    override fun getPillCheckInsByDate(date: String): Flow<List<PillCheckIn>> = pillCheckInDao.getPillCheckInsByDate(date)
    override fun getPillCheckInByPillIdAndDate(pillId: Long, date: String): Flow<PillCheckIn?> = pillCheckInDao.getPillCheckInByPillIdAndDate(pillId, date)
    override suspend fun deletePillCheckInByPillIdAndDate(pillId: Long, date: String) = pillCheckInDao.deletePillCheckInByPillIdAndDate(pillId, date)

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

    override fun getDailyCombinedTotals(date: String, includeExerciseCalories: Boolean, includeTEFBonus: Boolean): Flow<DailyTotals?> {
        return when {
            includeExerciseCalories && includeTEFBonus -> {
                combine(
                    getDailyNutrientTotals(date),
                    getDailyExerciseCalories(date)
                ) { mealTotals, exerciseCalories ->
                    mealTotals?.let { totals ->
                        // Calculate TEF bonus
                        val tefBonus = com.example.template.utils.TEFCalculator.calculateTEFBonus(totals)
                        // Apply both exercise and TEF bonuses
                        val netCalories = (totals.totalCalories - exerciseCalories - tefBonus).coerceAtLeast(0.0)
                        DailyTotals(
                            totalCalories = netCalories,
                            totalCarbohydrates = totals.totalCarbohydrates,
                            totalProtein = totals.totalProtein,
                            totalFat = totals.totalFat,
                            totalFiber = totals.totalFiber,
                            totalSodium = totals.totalSodium,
                            totalSaturatedFat = totals.totalSaturatedFat,
                            totalSugars = totals.totalSugars,
                            totalCholesterol = totals.totalCholesterol,
                            totalVitaminC = totals.totalVitaminC,
                            totalCalcium = totals.totalCalcium,
                            totalIron = totals.totalIron
                        )
                    }
                }
            }
            includeExerciseCalories -> {
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
                            totalSodium = totals.totalSodium,
                            totalSaturatedFat = totals.totalSaturatedFat,
                            totalSugars = totals.totalSugars,
                            totalCholesterol = totals.totalCholesterol,
                            totalVitaminC = totals.totalVitaminC,
                            totalCalcium = totals.totalCalcium,
                            totalIron = totals.totalIron
                        )
                    }
                }
            }
            includeTEFBonus -> {
                getDailyNutrientTotals(date).map { mealTotals ->
                    mealTotals?.let { totals ->
                        // Calculate TEF bonus
                        val tefBonus = com.example.template.utils.TEFCalculator.calculateTEFBonus(totals)
                        // Apply TEF bonus
                        val netCalories = (totals.totalCalories - tefBonus).coerceAtLeast(0.0)
                        DailyTotals(
                            totalCalories = netCalories,
                            totalCarbohydrates = totals.totalCarbohydrates,
                            totalProtein = totals.totalProtein,
                            totalFat = totals.totalFat,
                            totalFiber = totals.totalFiber,
                            totalSodium = totals.totalSodium,
                            totalSaturatedFat = totals.totalSaturatedFat,
                            totalSugars = totals.totalSugars,
                            totalCholesterol = totals.totalCholesterol,
                            totalVitaminC = totals.totalVitaminC,
                            totalCalcium = totals.totalCalcium,
                            totalIron = totals.totalIron
                        )
                    }
                }
            }
            else -> {
                getDailyNutrientTotals(date)
            }
        }
    }
}
