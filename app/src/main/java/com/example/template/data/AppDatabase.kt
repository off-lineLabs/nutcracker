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
    version = 10, 
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

        // Migration from version 9 to 10 - Add new fields to meals table
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to meals table
                db.execSQL("ALTER TABLE meals ADD COLUMN brand TEXT")
                db.execSQL("ALTER TABLE meals ADD COLUMN imageUrl TEXT")
                db.execSQL("ALTER TABLE meals ADD COLUMN localImagePath TEXT")
                db.execSQL("ALTER TABLE meals ADD COLUMN novaClassification TEXT")
                db.execSQL("ALTER TABLE meals ADD COLUMN greenScore TEXT")
                db.execSQL("ALTER TABLE meals ADD COLUMN nutriscore TEXT")
                db.execSQL("ALTER TABLE meals ADD COLUMN ingredients TEXT")
                db.execSQL("ALTER TABLE meals ADD COLUMN categories TEXT")
                db.execSQL("ALTER TABLE meals ADD COLUMN quantity TEXT")
                db.execSQL("ALTER TABLE meals ADD COLUMN servingSize TEXT")
                db.execSQL("ALTER TABLE meals ADD COLUMN barcode TEXT")
                db.execSQL("ALTER TABLE meals ADD COLUMN source TEXT NOT NULL DEFAULT 'manual'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "food_log_database"
                )
                .addMigrations(MIGRATION_9_10)
                .fallbackToDestructiveMigration() // Keep as fallback for development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
