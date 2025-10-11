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
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.MealCheckIn
import com.offlinelabs.nutcracker.data.model.UserGoal
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import com.offlinelabs.nutcracker.data.model.ExerciseTypeConverters
import com.offlinelabs.nutcracker.data.model.Pill
import com.offlinelabs.nutcracker.data.model.PillCheckIn
import com.offlinelabs.nutcracker.data.model.DateTimeTypeConverters

@Database(
    entities = [
        Meal::class, 
        UserGoal::class, 
        MealCheckIn::class,
        Exercise::class,
        ExerciseLog::class,
        Pill::class,
        PillCheckIn::class
    ], 
    version = 17, 
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // No migrations needed - database will be recreated from scratch

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "food_log_database_v2"
                    )
                    .fallbackToDestructiveMigration() // Always recreate database from scratch
                    .build()
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    com.offlinelabs.nutcracker.util.logger.AppLogger.e("AppDatabase", "Database initialization failed", e)
                    throw e
                }
            }
        }
    }
}
