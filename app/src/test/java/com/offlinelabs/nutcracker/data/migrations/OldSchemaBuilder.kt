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
                        name == "android_metadata" || name in skipIndices
                    ) continue
                    statements += if (type == "table") 0 to transformTable(table, sql) else 1 to sql
                }
            }
        } finally {
            current.close()
            context.deleteDatabase(dbName)
        }
        return statements.sortedBy { it.first }.map { it.second }
    }
}
