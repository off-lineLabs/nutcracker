package com.offlinelabs.nutcracker.data.repo

import com.offlinelabs.nutcracker.data.dao.MealDao
import com.offlinelabs.nutcracker.data.dao.MealCheckInDao
import com.offlinelabs.nutcracker.data.dao.UserGoalDao
import com.offlinelabs.nutcracker.data.dao.ExerciseDao
import com.offlinelabs.nutcracker.data.dao.ExerciseLogDao
import com.offlinelabs.nutcracker.data.dao.PillDao
import com.offlinelabs.nutcracker.data.dao.PillCheckInDao
import com.offlinelabs.nutcracker.data.dao.RecipeDao
import com.offlinelabs.nutcracker.data.dao.RecipeIngredientDao
import com.offlinelabs.nutcracker.data.dao.DailyTotals
import com.offlinelabs.nutcracker.data.dao.DailyNutritionEntry
import com.offlinelabs.nutcracker.data.dao.DailyExerciseEntry
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.MealCheckIn
import com.offlinelabs.nutcracker.data.model.UserGoal
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import com.offlinelabs.nutcracker.data.model.Pill
import com.offlinelabs.nutcracker.data.model.PillCheckIn
import com.offlinelabs.nutcracker.data.model.Recipe
import com.offlinelabs.nutcracker.data.model.RecipeIngredient
import com.offlinelabs.nutcracker.data.service.ExerciseImageService
import com.offlinelabs.nutcracker.data.service.ImageDownloadService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

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
    fun getPillCheckInsByPillId(pillId: Long): Flow<List<PillCheckIn>>
    fun getPillCheckInByPillIdAndDate(pillId: Long, date: String): Flow<PillCheckIn?>
    suspend fun deletePillCheckInByPillIdAndDate(pillId: Long, date: String)

    // Recipe operations
    fun getAllRecipes(): Flow<List<Recipe>>
    fun getRecipeById(recipeId: Long): Flow<Recipe?>
    suspend fun insertRecipe(recipe: Recipe): Long
    suspend fun updateRecipe(recipe: Recipe)
    suspend fun deleteRecipe(recipe: Recipe)
    suspend fun deleteAllRecipes()

    // RecipeIngredient operations
    fun getIngredientsByRecipeId(recipeId: Long): Flow<List<RecipeIngredient>>
    suspend fun getIngredientsByRecipeIdSync(recipeId: Long): List<RecipeIngredient>
    suspend fun insertRecipeIngredient(ingredient: RecipeIngredient): Long
    suspend fun insertRecipeIngredients(ingredients: List<RecipeIngredient>)
    suspend fun updateRecipeIngredient(ingredient: RecipeIngredient)
    suspend fun deleteRecipeIngredient(ingredient: RecipeIngredient)
    suspend fun deleteIngredientsByRecipeId(recipeId: Long)

    // Recipe nutrition calculation
    suspend fun calculateRecipeNutrition(recipeId: Long, servingMultiplier: Double = 1.0): Meal?

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
    private val recipeDao: RecipeDao,
    private val recipeIngredientDao: RecipeIngredientDao,
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
        com.offlinelabs.nutcracker.util.logger.AppLogger.i("FoodLogRepository", "insertExercise called:")
        com.offlinelabs.nutcracker.util.logger.AppLogger.i("FoodLogRepository", "  - name: ${exercise.name}")
        com.offlinelabs.nutcracker.util.logger.AppLogger.i("FoodLogRepository", "  - imagePaths.size: ${exercise.imagePaths.size}")
        exercise.imagePaths.forEachIndexed { index, path ->
            com.offlinelabs.nutcracker.util.logger.AppLogger.i("FoodLogRepository", "    imagePath $index: $path")
        }
        val id = exerciseDao.upsertExercise(exercise)
        com.offlinelabs.nutcracker.util.logger.AppLogger.i("FoodLogRepository", "insertExercise returned ID: $id")
        return id
    }
    override suspend fun updateExercise(exercise: Exercise) {
        com.offlinelabs.nutcracker.util.logger.AppLogger.i("FoodLogRepository", "updateExercise called:")
        com.offlinelabs.nutcracker.util.logger.AppLogger.i("FoodLogRepository", "  - ID: ${exercise.id}")
        com.offlinelabs.nutcracker.util.logger.AppLogger.i("FoodLogRepository", "  - name: ${exercise.name}")
        com.offlinelabs.nutcracker.util.logger.AppLogger.i("FoodLogRepository", "  - imagePaths.size: ${exercise.imagePaths.size}")
        exercise.imagePaths.forEachIndexed { index, path ->
            com.offlinelabs.nutcracker.util.logger.AppLogger.i("FoodLogRepository", "    imagePath $index: $path")
        }
        exerciseDao.updateExercise(exercise)
        com.offlinelabs.nutcracker.util.logger.AppLogger.i("FoodLogRepository", "updateExercise completed")
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
    override fun getPillCheckInsByPillId(pillId: Long): Flow<List<PillCheckIn>> = pillCheckInDao.getPillCheckInsByPillId(pillId)
    override fun getPillCheckInByPillIdAndDate(pillId: Long, date: String): Flow<PillCheckIn?> = pillCheckInDao.getPillCheckInByPillIdAndDate(pillId, date)
    override suspend fun deletePillCheckInByPillIdAndDate(pillId: Long, date: String) = pillCheckInDao.deletePillCheckInByPillIdAndDate(pillId, date)

    // Recipe operations
    override fun getAllRecipes(): Flow<List<Recipe>> = recipeDao.getAllRecipes()
    override fun getRecipeById(recipeId: Long): Flow<Recipe?> = recipeDao.getRecipeById(recipeId)
    override suspend fun insertRecipe(recipe: Recipe): Long = recipeDao.insertRecipe(recipe)
    override suspend fun updateRecipe(recipe: Recipe) = recipeDao.updateRecipe(recipe)
    override suspend fun deleteRecipe(recipe: Recipe) {
        // Delete the associated image if it exists
        recipe.localImagePath?.let { imagePath ->
            imageDownloadService?.deleteLocalImage(imagePath)
        }
        recipeDao.hideRecipe(recipe.id)
    }
    override suspend fun deleteAllRecipes() = recipeDao.deleteAllRecipes()

    // RecipeIngredient operations
    override fun getIngredientsByRecipeId(recipeId: Long): Flow<List<RecipeIngredient>> = recipeIngredientDao.getIngredientsByRecipeId(recipeId)
    override suspend fun getIngredientsByRecipeIdSync(recipeId: Long): List<RecipeIngredient> = recipeIngredientDao.getIngredientsByRecipeIdSync(recipeId)
    override suspend fun insertRecipeIngredient(ingredient: RecipeIngredient): Long = recipeIngredientDao.insertRecipeIngredient(ingredient)
    override suspend fun insertRecipeIngredients(ingredients: List<RecipeIngredient>) = recipeIngredientDao.insertRecipeIngredients(ingredients)
    override suspend fun updateRecipeIngredient(ingredient: RecipeIngredient) = recipeIngredientDao.updateRecipeIngredient(ingredient)
    override suspend fun deleteRecipeIngredient(ingredient: RecipeIngredient) = recipeIngredientDao.deleteRecipeIngredient(ingredient)
    override suspend fun deleteIngredientsByRecipeId(recipeId: Long) = recipeIngredientDao.deleteIngredientsByRecipeId(recipeId)

    // Recipe nutrition calculation
    override suspend fun calculateRecipeNutrition(recipeId: Long, servingMultiplier: Double): Meal? {
        val ingredients = getIngredientsByRecipeIdSync(recipeId)
        if (ingredients.isEmpty()) return null

        var totalCalories = 0.0
        var totalCarbohydrates = 0.0
        var totalProtein = 0.0
        var totalFat = 0.0
        var totalFiber = 0.0
        var totalSodium = 0.0
        var totalSaturatedFat = 0.0
        var totalSugars = 0.0
        var totalCholesterol = 0.0
        var totalVitaminC = 0.0
        var totalCalcium = 0.0
        var totalIron = 0.0

        // Get all meal IDs
        val mealIds = ingredients.map { it.mealId }.distinct()
        
        // Fetch all meals
        val meals = mealIds.mapNotNull { mealId ->
            mealDao.getMealById(mealId).first()
        }

        // Calculate nutrition for each ingredient
        ingredients.forEach { ingredient ->
            val meal = meals.find { it?.id == ingredient.mealId } ?: return@forEach
            
            // Convert ingredient quantity to the meal's serving size unit
            // For simplicity, we'll assume quantities are in the same unit as the meal's serving size
            // In a more sophisticated implementation, we'd convert between units
            if (meal.servingSize_value <= 0.0) return@forEach // Skip invalid serving size
            val quantityMultiplier = ingredient.quantity / meal.servingSize_value
            
            totalCalories += meal.calories * quantityMultiplier
            totalCarbohydrates += meal.carbohydrates_g * quantityMultiplier
            totalProtein += meal.protein_g * quantityMultiplier
            totalFat += meal.fat_g * quantityMultiplier
            totalFiber += meal.fiber_g * quantityMultiplier
            totalSodium += meal.sodium_mg * quantityMultiplier
            meal.saturatedFat_g?.let { totalSaturatedFat += it * quantityMultiplier }
            meal.sugars_g?.let { totalSugars += it * quantityMultiplier }
            meal.cholesterol_mg?.let { totalCholesterol += it * quantityMultiplier }
            meal.vitaminC_mg?.let { totalVitaminC += it * quantityMultiplier }
            meal.calcium_mg?.let { totalCalcium += it * quantityMultiplier }
            meal.iron_mg?.let { totalIron += it * quantityMultiplier }
        }

        // Apply serving multiplier
        totalCalories *= servingMultiplier
        totalCarbohydrates *= servingMultiplier
        totalProtein *= servingMultiplier
        totalFat *= servingMultiplier
        totalFiber *= servingMultiplier
        totalSodium *= servingMultiplier
        totalSaturatedFat *= servingMultiplier
        totalSugars *= servingMultiplier
        totalCholesterol *= servingMultiplier
        totalVitaminC *= servingMultiplier
        totalCalcium *= servingMultiplier
        totalIron *= servingMultiplier

        // Get recipe name
        val recipe = recipeDao.getRecipeById(recipeId).first() ?: return null

        // Create a temporary Meal object representing the recipe
        return Meal(
            id = -1, // Not a real meal, just for nutrition calculation
            name = recipe.name,
            brand = null,
            calories = totalCalories.toInt(),
            carbohydrates_g = totalCarbohydrates,
            protein_g = totalProtein,
            fat_g = totalFat,
            fiber_g = totalFiber,
            sodium_mg = totalSodium,
            servingSize_value = servingMultiplier,
            servingSize_unit = com.offlinelabs.nutcracker.data.model.ServingSizeUnit.SERVINGS,
            notes = "recipe:${recipeId}", // Store recipe ID in notes for reference
            isVisible = true,
            saturatedFat_g = if (totalSaturatedFat > 0) totalSaturatedFat else null,
            sugars_g = if (totalSugars > 0) totalSugars else null,
            cholesterol_mg = if (totalCholesterol > 0) totalCholesterol else null,
            vitaminC_mg = if (totalVitaminC > 0) totalVitaminC else null,
            calcium_mg = if (totalCalcium > 0) totalCalcium else null,
            iron_mg = if (totalIron > 0) totalIron else null
        )
    }

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
                        val tefBonus = com.offlinelabs.nutcracker.utils.TEFCalculator.calculateTEFBonus(totals)
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
                        val tefBonus = com.offlinelabs.nutcracker.utils.TEFCalculator.calculateTEFBonus(totals)
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
