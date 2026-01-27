package com.offlinelabs.nutcracker.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    
    // Migration from version 17 to 18 - Add tags system
    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create tags table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS tags (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    color TEXT NOT NULL,
                    type TEXT NOT NULL
                )
            """)
            
            // Create meal_tags junction table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS meal_tags (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    mealId INTEGER NOT NULL,
                    tagId INTEGER NOT NULL,
                    FOREIGN KEY(mealId) REFERENCES meals(id) ON DELETE CASCADE,
                    FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE
                )
            """)
            
            // Create exercise_tags junction table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS exercise_tags (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    exerciseId INTEGER NOT NULL,
                    tagId INTEGER NOT NULL,
                    FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE,
                    FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE
                )
            """)
            
            // Create indices for better performance
            db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_tags_mealId ON meal_tags(mealId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_tags_tagId ON meal_tags(tagId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_tags_exerciseId ON exercise_tags(exerciseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_tags_tagId ON exercise_tags(tagId)")
        }
    }
    
    // Migration from version 18 to 19 - Add recipes system
    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create recipes table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS recipes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT,
                    imageUrl TEXT,
                    localImagePath TEXT,
                    isVisible INTEGER NOT NULL,
                    createdDate TEXT NOT NULL
                )
            """)
            
            // Create recipe_ingredients junction table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS recipe_ingredients (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    recipeId INTEGER NOT NULL,
                    mealId INTEGER NOT NULL,
                    quantity REAL NOT NULL,
                    unit TEXT NOT NULL,
                    `order` INTEGER NOT NULL,
                    FOREIGN KEY(recipeId) REFERENCES recipes(id) ON DELETE CASCADE,
                    FOREIGN KEY(mealId) REFERENCES meals(id) ON DELETE RESTRICT
                )
            """)
            
            // Create indices for better performance
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_ingredients_recipeId ON recipe_ingredients(recipeId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_ingredients_mealId ON recipe_ingredients(mealId)")
        }
    }
    
    // Add more migrations here as needed
}
