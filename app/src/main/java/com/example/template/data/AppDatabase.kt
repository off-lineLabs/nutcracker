package com.example.template.data // Placing it in the 'data' package

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.template.data.dao.MealDao
import com.example.template.data.dao.MealCheckInDao
import com.example.template.data.dao.UserGoalDao
import com.example.template.data.dao.ExerciseDao
import com.example.template.data.dao.ExerciseLogDao
import com.example.template.data.model.Meal
import com.example.template.data.model.MealCheckIn
import com.example.template.data.model.UserGoal
import com.example.template.data.model.Exercise
import com.example.template.data.model.ExerciseLog

@Database(
    entities = [
        Meal::class, 
        UserGoal::class, 
        MealCheckIn::class,
        Exercise::class,
        ExerciseLog::class
    ], 
    version = 4, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mealDao(): MealDao
    abstract fun userGoalDao(): UserGoalDao
    abstract fun mealCheckInDao(): MealCheckInDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseLogDao(): ExerciseLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "food_log_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
