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
    
    // Migration from version 19 to 20 - Add gyms; exercises can belong to a gym (null = shared)
    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS gyms (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    unit TEXT,
                    mapsPlaceId TEXT,
                    mapsProvider TEXT,
                    isVisible INTEGER NOT NULL
                )
            """)

            // Existing exercises keep gymId NULL, so they all become shared
            db.execSQL(
                "ALTER TABLE exercises ADD COLUMN gymId INTEGER " +
                    "REFERENCES gyms(id) ON UPDATE NO ACTION ON DELETE SET NULL"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_exercises_gymId ON exercises(gymId)")
        }
    }

    // Migration from version 20 to 21 - Custom sort order for meals/exercises; routines (many-to-many)
    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // All rows start at 0, so the custom order begins as alphabetical (name breaks ties)
            db.execSQL("ALTER TABLE meals ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE exercises ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS routines (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    isVisible INTEGER NOT NULL
                )
            """)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS routine_exercises (
                    routineId INTEGER NOT NULL,
                    exerciseId INTEGER NOT NULL,
                    position INTEGER NOT NULL,
                    PRIMARY KEY(routineId, exerciseId),
                    FOREIGN KEY(routineId) REFERENCES routines(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_exercises_exerciseId ON routine_exercises(exerciseId)")
        }
    }

    // Migration from version 21 to 22 - Routines belong to a gym; synced exercise copies; meal categories
    val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // v21 routines had no UI and no gym, so there is nothing to keep: recreate both tables
            db.execSQL("DROP TABLE IF EXISTS routine_exercises")
            db.execSQL("DROP TABLE IF EXISTS routines")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS routines (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    gymId INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    isVisible INTEGER NOT NULL,
                    FOREIGN KEY(gymId) REFERENCES gyms(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_routines_gymId ON routines(gymId)")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS routine_exercises (
                    routineId INTEGER NOT NULL,
                    exerciseId INTEGER NOT NULL,
                    position INTEGER NOT NULL,
                    PRIMARY KEY(routineId, exerciseId),
                    FOREIGN KEY(routineId) REFERENCES routines(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_exercises_exerciseId ON routine_exercises(exerciseId)")

            db.execSQL("ALTER TABLE exercises ADD COLUMN syncGroupId TEXT")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_exercises_syncGroupId ON exercises(syncGroupId)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS meal_categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    defaultKey TEXT,
                    sortOrder INTEGER NOT NULL,
                    isVisible INTEGER NOT NULL
                )
            """)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS meal_category_meals (
                    categoryId INTEGER NOT NULL,
                    mealId INTEGER NOT NULL,
                    PRIMARY KEY(categoryId, mealId),
                    FOREIGN KEY(categoryId) REFERENCES meal_categories(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(mealId) REFERENCES meals(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_category_meals_mealId ON meal_category_meals(mealId)")
            com.offlinelabs.nutcracker.data.model.MealCategoryDefaults.insertStatements().forEach { db.execSQL(it) }
        }
    }

    // Migration from version 22 to 23 - A check-in remembers the routine / meal type it was ticked under
    val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Existing check-ins get no context (null): like check-ins made outside a routine / meal
            // type group, they count in every group
            db.execSQL("ALTER TABLE exercise_logs ADD COLUMN routineId INTEGER")
            db.execSQL("ALTER TABLE meal_check_ins ADD COLUMN mealCategoryId INTEGER")
        }
    }

    // Migration from version 23 to 24 - The sets of each exercise check-in
    val MIGRATION_23_24 = object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `exercise_sets` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `logId` INTEGER NOT NULL,
                    `setIndex` INTEGER NOT NULL,
                    `weight` REAL NOT NULL,
                    `reps` INTEGER NOT NULL,
                    `completedAt` TEXT,
                    FOREIGN KEY(`logId`) REFERENCES `exercise_logs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_sets_logId` ON `exercise_sets` (`logId`)")
            // Each existing check-in gets its sets as identical copies of its weight and reps (at
            // least one), done at its time, so nothing changes on screen
            db.execSQL("""
                INSERT INTO exercise_sets (logId, setIndex, weight, reps, completedAt)
                WITH RECURSIVE n(i) AS (SELECT 0 UNION ALL SELECT i + 1 FROM n WHERE i < 99)
                SELECT l.id, n.i, l.weight, l.reps, l.logDateTime
                FROM exercise_logs l JOIN n ON n.i < MAX(l.sets, 1)
            """)
        }
    }

    // Add more migrations here as needed
}
