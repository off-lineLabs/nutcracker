package com.offlinelabs.nutcracker.data // Placing it in the 'data' package

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.offlinelabs.nutcracker.data.dao.MealDao
import com.offlinelabs.nutcracker.data.dao.MealCheckInDao
import com.offlinelabs.nutcracker.data.dao.UserGoalDao
import com.offlinelabs.nutcracker.data.dao.ExerciseDao
import com.offlinelabs.nutcracker.data.dao.ExerciseLogDao
import com.offlinelabs.nutcracker.data.dao.PillDao
import com.offlinelabs.nutcracker.data.dao.PillCheckInDao
import com.offlinelabs.nutcracker.data.dao.TagDao
import com.offlinelabs.nutcracker.data.dao.MealTagDao
import com.offlinelabs.nutcracker.data.dao.ExerciseTagDao
import com.offlinelabs.nutcracker.data.dao.RecipeDao
import com.offlinelabs.nutcracker.data.dao.RecipeIngredientDao
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.MealCheckIn
import com.offlinelabs.nutcracker.data.model.UserGoal
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import com.offlinelabs.nutcracker.data.model.ExerciseTypeConverters
import com.offlinelabs.nutcracker.data.model.Pill
import com.offlinelabs.nutcracker.data.model.PillCheckIn
import com.offlinelabs.nutcracker.data.model.DateTimeTypeConverters
import com.offlinelabs.nutcracker.data.model.Tag
import com.offlinelabs.nutcracker.data.model.MealTag
import com.offlinelabs.nutcracker.data.model.ExerciseTag
import com.offlinelabs.nutcracker.data.model.Recipe
import com.offlinelabs.nutcracker.data.model.RecipeIngredient
import com.offlinelabs.nutcracker.data.migrations.DatabaseMigrations

@Database(
    entities = [
        Meal::class, 
        UserGoal::class, 
        MealCheckIn::class,
        Exercise::class,
        ExerciseLog::class,
        Pill::class,
        PillCheckIn::class,
        Tag::class,
        MealTag::class,
        ExerciseTag::class,
        Recipe::class,
        RecipeIngredient::class
    ], 
    version = 19, 
    exportSchema = false
)
@androidx.room.TypeConverters(ExerciseTypeConverters::class, DateTimeTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mealDao(): MealDao
    abstract fun userGoalDao(): UserGoalDao
    abstract fun mealCheckInDao(): MealCheckInDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseLogDao(): ExerciseLogDao
    abstract fun pillDao(): PillDao
    abstract fun pillCheckInDao(): PillCheckInDao
    abstract fun tagDao(): TagDao
    abstract fun mealTagDao(): MealTagDao
    abstract fun exerciseTagDao(): ExerciseTagDao
    abstract fun recipeDao(): RecipeDao
    abstract fun recipeIngredientDao(): RecipeIngredientDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    // #region agent log
                    try { java.io.File("c:\\Users\\jonas\\StudioProjects\\offline-calorie-calculator\\.cursor\\debug.log").appendText("{\"timestamp\":${System.currentTimeMillis()},\"location\":\"AppDatabase.kt:76\",\"message\":\"Database builder start\",\"data\":{\"version\":19,\"dbName\":\"food_log_database_v2\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n") } catch (e: Exception) {}
                    // #endregion
                    val builder = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "food_log_database_v2"
                    )
                    .addMigrations(
                        DatabaseMigrations.MIGRATION_17_18,
                        DatabaseMigrations.MIGRATION_18_19
                        // Add more migrations here as needed
                        // IMPORTANT: When incrementing database version, you MUST create a proper migration
                        // in DatabaseMigrations.kt. Never use fallbackToDestructiveMigration() in production.
                    )
                    // #region agent log
                    try { java.io.File("c:\\Users\\jonas\\StudioProjects\\offline-calorie-calculator\\.cursor\\debug.log").appendText("{\"timestamp\":${System.currentTimeMillis()},\"location\":\"AppDatabase.kt:88\",\"message\":\"Migrations registered\",\"data\":{\"migrations\":[\"17_18\",\"18_19\"]},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n") } catch (e: Exception) {}
                    // #endregion
                    val instance = builder.build()
                    // #region agent log
                    try { java.io.File("c:\\Users\\jonas\\StudioProjects\\offline-calorie-calculator\\.cursor\\debug.log").appendText("{\"timestamp\":${System.currentTimeMillis()},\"location\":\"AppDatabase.kt:92\",\"message\":\"Database built successfully\",\"data\":{},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n") } catch (e: Exception) {}
                    // #endregion
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    // #region agent log
                    try { java.io.File("c:\\Users\\jonas\\StudioProjects\\offline-calorie-calculator\\.cursor\\debug.log").appendText("{\"timestamp\":${System.currentTimeMillis()},\"location\":\"AppDatabase.kt:98\",\"message\":\"Database initialization failed\",\"data\":{\"error\":\"${e.message}\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n") } catch (ex: Exception) {}
                    // #endregion
                    com.offlinelabs.nutcracker.util.logger.AppLogger.e("AppDatabase", "Database initialization failed", e)
                    throw e
                }
            }
        }
    }
}
