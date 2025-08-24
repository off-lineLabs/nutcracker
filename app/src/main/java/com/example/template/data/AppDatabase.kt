package com.example.template.data // Placing it in the 'data' package

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.template.data.dao.MealDao
import com.example.template.data.dao.MealCheckInDao
import com.example.template.data.dao.UserGoalDao
import com.example.template.data.model.Meal
import com.example.template.data.model.MealCheckIn
import com.example.template.data.model.UserGoal

@Database(entities = [Meal::class, UserGoal::class, MealCheckIn::class], version = 3, exportSchema = false) // Added MealCheckIn, incremented version
abstract class AppDatabase : RoomDatabase() {

    abstract fun mealDao(): MealDao
    abstract fun userGoalDao(): UserGoalDao // Added UserGoalDao accessor
    abstract fun mealCheckInDao(): MealCheckInDao // Added MealCheckInDao accessor

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "food_log_database" // Name of your database file
                )
                // Wipes and rebuilds instead of migrating if no Migration object.
                // Migration is not covered in this scope for version 2.
                .fallbackToDestructiveMigration() // Be careful with this in production
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
