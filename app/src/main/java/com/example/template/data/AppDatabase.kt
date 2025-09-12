package com.example.template.data // Placing it in the 'data' package

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.template.data.dao.MealDao
import com.example.template.data.dao.MealCheckInDao
import com.example.template.data.dao.UserGoalDao
import com.example.template.data.dao.ExerciseDao
import com.example.template.data.dao.ExerciseLogDao
import com.example.template.data.dao.PillDao
import com.example.template.data.dao.PillCheckInDao
import com.example.template.data.model.Meal
import com.example.template.data.model.MealCheckIn
import com.example.template.data.model.UserGoal
import com.example.template.data.model.Exercise
import com.example.template.data.model.ExerciseLog
import com.example.template.data.model.ExerciseTypeConverters
import com.example.template.data.model.Pill
import com.example.template.data.model.PillCheckIn
import com.example.template.data.model.DateTimeTypeConverters

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
    version = 1, 
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
                    android.util.Log.e("AppDatabase", "Database initialization failed", e)
                    throw e
                }
            }
        }
    }
}
