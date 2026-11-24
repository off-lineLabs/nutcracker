package com.offlinelabs.nutcracker.data.migrations

import android.content.Context
import androidx.room.Room
import com.offlinelabs.nutcracker.data.AppDatabase
import org.junit.Assert.assertFalse

/**
 * Derives the CREATE statements of an older schema from Room's generated schema of the current
 * entities, minus the additions of later versions. Tables come first, then indices.
 */
object OldSchemaBuilder {

    /** Tables and indices added in v21 (routines). */
    val V21_TABLES = setOf("routines", "routine_exercises")
    val V21_INDICES = setOf("index_routine_exercises_exerciseId")

    /** Tables and indices added in v22 (meal categories, routine gym, sync group). */
    val V22_TABLES = setOf("meal_categories", "meal_category_meals")
    val V22_INDICES = setOf("index_meal_category_meals_mealId", "index_routines_gymId", "index_exercises_syncGroupId")

    /** Strips the v22 `syncGroupId` column from the exercises CREATE statement. */
    fun stripSyncGroup(sql: String): String =
        sql.replace(Regex(",\\s*`syncGroupId` TEXT"), "").also {
            assertFalse("syncGroupId must be stripped", it.contains("syncGroupId"))
        }

    /** The v21 routines table: no gymId column and no gym foreign key. */
    fun stripRoutineGym(sql: String): String =
        sql.replace(Regex(",\\s*`gymId` INTEGER NOT NULL"), "")
            .replace(Regex(",\\s*FOREIGN KEY\\(`gymId`\\)[^)]*\\)[^,)]*?ON DELETE CASCADE"), "")
            .also { assertFalse("gymId must be stripped from v21 routines", it.contains("gymId")) }

    /**
     * Strips the v23 check-in context columns: `routineId` from exercise_logs and `mealCategoryId`
     * from meal_check_ins. Every older schema lacks them, so [statements] applies this by default.
     */
    fun stripCheckInContext(table: String, sql: String): String = when (table) {
        "exercise_logs" -> sql.replace(Regex(",\\s*`routineId` INTEGER"), "").also {
            assertFalse("routineId must be stripped from exercise_logs", it.contains("routineId"))
        }
        "meal_check_ins" -> sql.replace(Regex(",\\s*`mealCategoryId` INTEGER"), "").also {
            assertFalse("mealCategoryId must be stripped from meal_check_ins", it.contains("mealCategoryId"))
        }
        else -> sql
    }

    /** Strips the v21 `sortOrder` column from the meals/exercises CREATE statement. */
    fun stripSortOrder(sql: String): String =
        sql.replace(Regex(",\\s*`sortOrder` INTEGER NOT NULL DEFAULT 0"), "").also {
            assertFalse("sortOrder must be stripped", it.contains("sortOrder"))
        }

    fun statements(
        context: Context,
        dbName: String,
        skipTables: Set<String>,
        skipIndices: Set<String>,
        /** Keep the v23 check-in context columns (for a v23+ schema); older schemas strip them. */
        keepCheckInContext: Boolean = false,
        /** Keep the v24 exercise_sets table (for a v24+ schema); older schemas skip it. */
        keepExerciseSets: Boolean = false,
        transformTable: (table: String, sql: String) -> String
    ): List<String> {
        val statements = mutableListOf<Pair<Int, String>>()
        val current = Room.databaseBuilder(context, AppDatabase::class.java, dbName).build()
        try {
            val sqlite = current.openHelper.writableDatabase
            sqlite.query("SELECT type, name, tbl_name, sql FROM sqlite_master WHERE sql IS NOT NULL").use { c ->
                while (c.moveToNext()) {
                    val type = c.getString(0)
                    val name = c.getString(1)
                    val table = c.getString(2)
                    val sql = c.getString(3)
                    if (table in skipTables || table == "room_master_table" || name.startsWith("sqlite_") ||
                        name == "android_metadata" || name in skipIndices ||
                        (!keepExerciseSets && table == "exercise_sets")
                    ) continue
                    statements += if (type == "table") {
                        val old = if (keepCheckInContext) sql else stripCheckInContext(table, sql)
                        0 to transformTable(table, old)
                    } else {
                        1 to sql
                    }
                }
            }
        } finally {
            current.close()
            context.deleteDatabase(dbName)
        }
        return statements.sortedBy { it.first }.map { it.second }
    }
}
