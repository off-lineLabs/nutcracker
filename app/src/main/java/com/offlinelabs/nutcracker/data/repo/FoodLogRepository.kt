package com.offlinelabs.nutcracker.data.repo

import com.offlinelabs.nutcracker.data.dao.MealDao
import com.offlinelabs.nutcracker.data.dao.MealCheckInDao
import com.offlinelabs.nutcracker.data.dao.UserGoalDao
import com.offlinelabs.nutcracker.data.dao.ExerciseDao
import com.offlinelabs.nutcracker.data.dao.ExerciseLogDao
import com.offlinelabs.nutcracker.data.dao.PillDao
import com.offlinelabs.nutcracker.data.dao.PillCheckInDao
import com.offlinelabs.nutcracker.data.dao.GymDao
import com.offlinelabs.nutcracker.data.dao.LastCheckIn
import com.offlinelabs.nutcracker.data.dao.RoutineDao
import com.offlinelabs.nutcracker.data.dao.MealCategoryDao
import com.offlinelabs.nutcracker.data.model.MealCategory
import com.offlinelabs.nutcracker.data.model.MealCategoryMeal
import com.offlinelabs.nutcracker.data.applySyncedFields
import com.offlinelabs.nutcracker.data.model.Routine
import com.offlinelabs.nutcracker.data.model.RoutineExercise
import com.offlinelabs.nutcracker.data.dao.RecipeDao
import com.offlinelabs.nutcracker.data.dao.RecipeIngredientDao
import com.offlinelabs.nutcracker.data.dao.DailyTotals
import com.offlinelabs.nutcracker.data.dao.DailyNutritionEntry
import com.offlinelabs.nutcracker.data.dao.DailyExerciseEntry
import com.offlinelabs.nutcracker.data.dao.DailyExerciseCaloriesRow
import com.offlinelabs.nutcracker.data.dao.DailyFoodTotalsRow
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.MealCheckIn
import com.offlinelabs.nutcracker.data.model.UserGoal
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import com.offlinelabs.nutcracker.data.model.Pill
import com.offlinelabs.nutcracker.data.model.PillCheckIn
import com.offlinelabs.nutcracker.data.model.Gym
import com.offlinelabs.nutcracker.data.model.Recipe
import com.offlinelabs.nutcracker.data.model.RecipeIngredient
import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
import com.offlinelabs.nutcracker.data.service.ExerciseImageService
import com.offlinelabs.nutcracker.data.service.ImageDownloadService
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

    // Gym operations
    fun getVisibleGyms(): Flow<List<Gym>>
    suspend fun getGymById(id: Long): Gym?
    /** Inserts or updates the gym and returns its id. */
    suspend fun upsertGym(gym: Gym): Long
    /**
     * Soft-deletes the gym. [removeExercises] false: its exercises become shared;
     * true: they are hidden (gym id kept, logs stay).
     */
    suspend fun deleteGym(gymId: Long, removeExercises: Boolean)
    /**
     * Inserts a visible copy of [exercise] in [targetGymId] (null = shared) and returns the new id.
     * Logs and tags are not copied; image files are duplicated. With [sync], the copy joins the
     * original's sync group (created if needed), so later edits to either update the other.
     */
    suspend fun copyExerciseToGym(exercise: Exercise, targetGymId: Long?, sync: Boolean = false): Long
    /** Stops syncing [exerciseId]; a group left with a single exercise is dissolved. */
    suspend fun unsyncExercise(exerciseId: Long)

    // List ordering
    /** Latest check-in per meal / exercise, for "last checked in" sorting. */
    fun getMealLastCheckIns(): Flow<List<LastCheckIn>>
    fun getExerciseLastCheckIns(): Flow<List<LastCheckIn>>
    /** Saves the custom order: each id gets its index in [orderedIds]. */
    suspend fun applyMealSortOrder(orderedIds: List<Long>)
    suspend fun applyExerciseSortOrder(orderedIds: List<Long>)

    // Routines (no management UI yet; used for grouping)
    fun getVisibleRoutines(): Flow<List<Routine>>
    fun getVisibleRoutineExercises(): Flow<List<RoutineExercise>>
    /** Saves a routine's exercise order: each exercise gets its index in [exerciseIds]. */
    suspend fun applyRoutineExerciseOrder(routineId: Long, exerciseIds: List<Long>)
    /**
     * Puts [exerciseId] in exactly [routineIds] among [offered] (the routines its form showed):
     * appended to newly chosen routines, removed from unchosen ones; other routines are untouched.
     */
    suspend fun setExerciseRoutines(exerciseId: Long, routineIds: Set<Long>, offered: Set<Long>)
    /** Inserts or updates [routine] with exactly [exerciseIds] in that order; returns its id. */
    suspend fun saveRoutine(routine: Routine, exerciseIds: List<Long>): Long
    /** Soft-deletes a routine (its exercises are untouched). */
    suspend fun deleteRoutine(routineId: Long)
    suspend fun applyRoutineOrder(routineIds: List<Long>)

    // Meal categories (Breakfast, Lunch...)
    fun getVisibleMealCategories(): Flow<List<MealCategory>>
    fun getVisibleMealCategoryLinks(): Flow<List<MealCategoryMeal>>
    /** Inserts (at the end of the order) or updates a category; returns its id. */
    suspend fun upsertMealCategory(category: MealCategory): Long
    suspend fun deleteMealCategory(categoryId: Long)
    suspend fun applyMealCategoryOrder(categoryIds: List<Long>)
    suspend fun getMealCategoryIds(mealId: Long): List<Long>
    suspend fun setMealCategories(mealId: Long, categoryIds: List<Long>)

    // ExerciseLog operations
    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long
    suspend fun updateExerciseLog(exerciseLog: ExerciseLog)
    suspend fun deleteExerciseLog(exerciseLog: ExerciseLog)
    /**
     * Removes the logs of [exerciseId] on [date] (yyyy-MM-dd) and returns them, so they can be
     * restored: only those checked in under [routineId] when given, otherwise all of them.
     */
    suspend fun removeExerciseCheckIns(exerciseId: Long, date: String, routineId: Long? = null): List<ExerciseLog>
    /** Puts back logs removed by [removeExerciseCheckIns] (same ids). */
    suspend fun restoreExerciseLogs(logs: List<ExerciseLog>)
    /** Reps of each exercise's most recent log; exercises never logged are absent. */
    fun getLastReps(): Flow<Map<Long, Int>>
    /**
     * Removes the check-ins of [mealId] on [date] and returns them, so they can be restored: only
     * those checked in under the meal type [categoryId] when given, otherwise all of them.
     */
    suspend fun removeMealCheckIns(mealId: Long, date: String, categoryId: Long? = null): List<MealCheckIn>
    /** Puts back check-ins removed by [removeMealCheckIns] (same ids). */
    suspend fun restoreMealCheckIns(checkIns: List<MealCheckIn>)
    /** Serving multiplier of each meal's most recent check-in; meals never checked in are absent. */
    fun getLastServings(): Flow<Map<Long, Double>>
    fun getExerciseLogsByDate(date: String): Flow<List<DailyExerciseEntry>>
    fun getRecentExerciseLogs(limit: Int = 10): Flow<List<ExerciseLog>>
    fun getLastLogForExercise(exerciseId: Long): Flow<ExerciseLog?>
    fun getMaxWeightForExercise(exerciseId: Long): Flow<Double?>
    /** Heaviest logged weight per exercise id; exercises never logged are absent. */
    fun getMaxWeights(): Flow<Map<Long, Double>>
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

    /**
     * Inserts a NEW hidden meal holding a nutrition snapshot of the recipe and returns its id.
     * Returns null if the recipe is missing or has no ingredients.
     */
    suspend fun createRecipeSnapshotMeal(recipeId: Long, servingMultiplier: Double = 1.0): Long?

    // Combined operations for dashboard
    fun getDailyCombinedSummary(date: String): Flow<List<Any>>
    fun getDailyCombinedTotals(date: String, includeExerciseCalories: Boolean = true, includeTEFBonus: Boolean = false): Flow<DailyTotals?>

    // Analytics range queries (inclusive yyyy-MM-dd bounds, one row per date that has data)
    fun getDailyFoodTotalsInRange(startDate: String, endDate: String): Flow<List<DailyFoodTotalsRow>>
    fun getDailyExerciseCaloriesInRange(startDate: String, endDate: String): Flow<List<DailyExerciseCaloriesRow>>
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
    private val gymDao: GymDao,
    private val routineDao: RoutineDao,
    private val mealCategoryDao: MealCategoryDao,
    private val exerciseImageService: ExerciseImageService? = null,
    private val imageDownloadService: ImageDownloadService? = null
) : FoodLogRepository {

    // Meal operations
    override fun getAllMeals(): Flow<List<Meal>> = mealDao.getAllMeals()
    override fun getMealById(mealId: Long): Flow<Meal?> = mealDao.getMealById(mealId)
    // New visible meals go to the end of the custom order
    override suspend fun insertMeal(meal: Meal): Long = mealDao.insertMeal(
        if (meal.id == 0L && meal.isVisible) meal.copy(sortOrder = mealDao.nextSortOrder()) else meal
    ).let { if (it == -1L) meal.id else it } // @Upsert returns -1 when it updated
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
        // New exercises go to the end of the custom order; @Upsert returns -1 when it updated
        val toSave = if (exercise.id == 0L) exercise.copy(sortOrder = exerciseDao.nextSortOrder()) else exercise
        val id = exerciseDao.upsertExercise(toSave).let { if (it == -1L) exercise.id else it }
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
        // An exercise moved to a gym can't stay in another gym's routines
        exercise.gymId?.let { routineDao.removeExerciseFromOtherGyms(exercise.id, it) }
        // Synced copies at other gyms take the same settings
        exercise.syncGroupId?.let { group ->
            exerciseDao.getSyncPartners(group, exercise.id).forEach { partner ->
                exerciseDao.updateExercise(applySyncedFields(partner, exercise))
            }
        }
        com.offlinelabs.nutcracker.util.logger.AppLogger.i("FoodLogRepository", "updateExercise completed")
    }
    override suspend fun deleteExercise(exercise: Exercise) {
        // Delete the associated images if they exist
        exercise.imagePaths.forEach { imagePath ->
            exerciseImageService?.deleteImage(imagePath)
        }
        exerciseDao.hideExercise(exercise.id)
        // A deleted exercise leaves its sync group
        if (exercise.syncGroupId != null) unsyncExercise(exercise.id)
    }
    override suspend fun deleteAllExercises() = exerciseDao.deleteAllExercises()

    // Gym operations
    override fun getVisibleGyms(): Flow<List<Gym>> = gymDao.getVisibleGyms()
    override suspend fun getGymById(id: Long): Gym? = gymDao.getGymById(id)
    override suspend fun upsertGym(gym: Gym): Long {
        val rowId = gymDao.upsertGym(gym)
        return if (rowId == -1L) gym.id else rowId // @Upsert returns -1 when it updated
    }
    override suspend fun deleteGym(gymId: Long, removeExercises: Boolean) = gymDao.deleteGym(gymId, removeExercises)
    override suspend fun copyExerciseToGym(exercise: Exercise, targetGymId: Long?, sync: Boolean): Long {
        val sortOrder = exerciseDao.nextSortOrder()
        val syncGroupId = if (sync) {
            exercise.syncGroupId ?: java.util.UUID.randomUUID().toString().also {
                exerciseDao.setSyncGroup(exercise.id, it)
            }
        } else {
            null
        }
        val newId = exerciseDao.upsertExercise(
            exercise.copy(
                id = 0, gymId = targetGymId, isVisible = true, imagePaths = emptyList(),
                sortOrder = sortOrder, syncGroupId = syncGroupId
            )
        )
        if (exercise.imagePaths.isNotEmpty() && exerciseImageService != null) {
            val copiedPaths = exerciseImageService.copyImages(exercise.imagePaths, newId.toString())
            if (copiedPaths.isNotEmpty()) {
                exerciseDao.updateExercise(
                    exercise.copy(
                        id = newId, gymId = targetGymId, isVisible = true, imagePaths = copiedPaths,
                        sortOrder = sortOrder, syncGroupId = syncGroupId
                    )
                )
            }
        }
        return newId
    }

    override suspend fun unsyncExercise(exerciseId: Long) {
        val exercise = exerciseDao.getExerciseByIdSync(exerciseId) ?: return
        val group = exercise.syncGroupId ?: return
        exerciseDao.setSyncGroup(exerciseId, null)
        if (exerciseDao.countVisibleInSyncGroup(group) <= 1) exerciseDao.clearSyncGroup(group)
    }

    // List ordering
    override fun getMealLastCheckIns(): Flow<List<LastCheckIn>> = mealDao.getLastCheckIns()
    override fun getExerciseLastCheckIns(): Flow<List<LastCheckIn>> = exerciseDao.getLastCheckIns()
    override suspend fun applyMealSortOrder(orderedIds: List<Long>) = mealDao.applySortOrder(orderedIds)
    override suspend fun applyExerciseSortOrder(orderedIds: List<Long>) = exerciseDao.applySortOrder(orderedIds)

    // Routines
    override fun getVisibleRoutines(): Flow<List<Routine>> = routineDao.getVisibleRoutines()
    override fun getVisibleRoutineExercises(): Flow<List<RoutineExercise>> = routineDao.getVisibleRoutineExercises()
    override suspend fun applyRoutineExerciseOrder(routineId: Long, exerciseIds: List<Long>) =
        routineDao.applyExerciseOrder(routineId, exerciseIds)
    override suspend fun setExerciseRoutines(exerciseId: Long, routineIds: Set<Long>, offered: Set<Long>) =
        routineDao.setRoutinesForExercise(exerciseId, routineIds, offered)
    override suspend fun saveRoutine(routine: Routine, exerciseIds: List<Long>): Long {
        val toSave = if (routine.id == 0L) routine.copy(sortOrder = routineDao.nextRoutineSortOrder()) else routine
        val id = routineDao.upsertRoutine(toSave).let { if (it == -1L) routine.id else it }
        routineDao.setExercisesForRoutine(id, exerciseIds)
        return id
    }
    override suspend fun deleteRoutine(routineId: Long) = routineDao.hideRoutine(routineId)
    override suspend fun applyRoutineOrder(routineIds: List<Long>) = routineDao.applyRoutineOrder(routineIds)

    // Meal categories
    override fun getVisibleMealCategories(): Flow<List<MealCategory>> = mealCategoryDao.getVisibleCategories()
    override fun getVisibleMealCategoryLinks(): Flow<List<MealCategoryMeal>> = mealCategoryDao.getVisibleLinks()
    override suspend fun upsertMealCategory(category: MealCategory): Long {
        val toSave = if (category.id == 0L) category.copy(sortOrder = mealCategoryDao.nextSortOrder()) else category
        return mealCategoryDao.upsertCategory(toSave).let { if (it == -1L) category.id else it }
    }
    override suspend fun deleteMealCategory(categoryId: Long) = mealCategoryDao.hideCategory(categoryId)
    override suspend fun applyMealCategoryOrder(categoryIds: List<Long>) = mealCategoryDao.applyOrder(categoryIds)
    override suspend fun getMealCategoryIds(mealId: Long): List<Long> = mealCategoryDao.getCategoryIdsForMeal(mealId)
    override suspend fun setMealCategories(mealId: Long, categoryIds: List<Long>) =
        mealCategoryDao.setCategoriesForMeal(mealId, categoryIds)

    // ExerciseLog operations
    override suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long = exerciseLogDao.insertExerciseLog(exerciseLog)
    override suspend fun updateExerciseLog(exerciseLog: ExerciseLog) = exerciseLogDao.updateExerciseLog(exerciseLog)
    override suspend fun deleteExerciseLog(exerciseLog: ExerciseLog) = exerciseLogDao.deleteExerciseLog(exerciseLog)
    override suspend fun removeExerciseCheckIns(exerciseId: Long, date: String, routineId: Long?): List<ExerciseLog> =
        exerciseLogDao.removeLogsForExerciseOnDate(exerciseId, date, routineId)
    override suspend fun restoreExerciseLogs(logs: List<ExerciseLog>) {
        logs.forEach { exerciseLogDao.insertExerciseLog(it) }
    }
    override fun getLastReps(): Flow<Map<Long, Int>> = exerciseLogDao.getLastReps()
    override suspend fun removeMealCheckIns(mealId: Long, date: String, categoryId: Long?): List<MealCheckIn> =
        mealCheckInDao.removeCheckInsForMealOnDate(mealId, date, categoryId)
    override suspend fun restoreMealCheckIns(checkIns: List<MealCheckIn>) {
        checkIns.forEach { mealCheckInDao.insertMealCheckIn(it) }
    }
    override fun getLastServings(): Flow<Map<Long, Double>> = mealCheckInDao.getLastServings()
    override fun getExerciseLogsByDate(date: String): Flow<List<DailyExerciseEntry>> = exerciseLogDao.getDailyExerciseSummary(date)
    override fun getRecentExerciseLogs(limit: Int): Flow<List<ExerciseLog>> = exerciseLogDao.getRecentLogs(limit)
    override fun getLastLogForExercise(exerciseId: Long): Flow<ExerciseLog?> = exerciseLogDao.getLastLogForExercise(exerciseId)
    override fun getMaxWeightForExercise(exerciseId: Long): Flow<Double?> = exerciseLogDao.getMaxWeightForExercise(exerciseId)
    override fun getMaxWeights(): Flow<Map<Long, Double>> = exerciseLogDao.getMaxWeights()
    override fun getDailyExerciseCalories(date: String): Flow<Double> = exerciseLogDao.getDailyExerciseCalories(date)
    override fun getDailyFoodTotalsInRange(startDate: String, endDate: String): Flow<List<DailyFoodTotalsRow>> =
        mealCheckInDao.getDailyFoodTotalsInRange(startDate, endDate)
    override fun getDailyExerciseCaloriesInRange(startDate: String, endDate: String): Flow<List<DailyExerciseCaloriesRow>> =
        exerciseLogDao.getDailyExerciseCaloriesInRange(startDate, endDate)

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
            mealDao.getMealByIdSync(mealId)
        }

        // Calculate nutrition for each ingredient
        ingredients.forEach { ingredient ->
            val meal = meals.find { it?.id == ingredient.mealId } ?: return@forEach
            
            // Convert the ingredient amount into a number of the meal's servings (see servingMultiplier for the rules)
            if (meal.servingSize_value <= 0.0) return@forEach // Skip invalid serving size
            val quantityMultiplier = ServingSizeUnit.servingMultiplier(
                quantity = ingredient.quantity,
                unit = ingredient.unit,
                servingValue = meal.servingSize_value,
                servingUnit = meal.servingSize_unit
            )
            
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
        val recipe = recipeDao.getRecipeByIdSync(recipeId) ?: return null

        // Create a temporary Meal object representing the recipe
        return Meal(
            id = 0, // Not persisted yet; insert generates a fresh id
            name = recipe.name,
            brand = null,
            calories = totalCalories.toInt(),
            carbohydrates_g = totalCarbohydrates,
            protein_g = totalProtein,
            fat_g = totalFat,
            fiber_g = totalFiber,
            sodium_mg = totalSodium,
            servingSize_value = servingMultiplier,
            servingSize_unit = ServingSizeUnit.SERVINGS,
            notes = "recipe:${recipeId}", // Store recipe ID in notes for reference
            isVisible = false, // Snapshot, never shown in the meal library
            saturatedFat_g = if (totalSaturatedFat > 0) totalSaturatedFat else null,
            sugars_g = if (totalSugars > 0) totalSugars else null,
            cholesterol_mg = if (totalCholesterol > 0) totalCholesterol else null,
            vitaminC_mg = if (totalVitaminC > 0) totalVitaminC else null,
            calcium_mg = if (totalCalcium > 0) totalCalcium else null,
            iron_mg = if (totalIron > 0) totalIron else null
        )
    }

    override suspend fun createRecipeSnapshotMeal(recipeId: Long, servingMultiplier: Double): Long? {
        val snapshot = calculateRecipeNutrition(recipeId, servingMultiplier) ?: return null
        return mealDao.insertMeal(snapshot.copy(id = 0, isVisible = false))
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
