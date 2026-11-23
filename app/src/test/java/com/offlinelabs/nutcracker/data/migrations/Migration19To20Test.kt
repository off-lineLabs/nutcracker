package com.offlinelabs.nutcracker.data.migrations

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.offlinelabs.nutcracker.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Builds a real v19 database file and opens it with Room, so Room's open-time schema validation
 * proves that MIGRATION_19_20 produces exactly what the entities declare.
 *
 * The v19 SQL is derived from Room's own generated schema of the current entities: every table and
 * index except the gym additions (the gyms table and exercises.gymId plus its FK and index), which
 * are exactly what v20 introduced. All other tables are unchanged since v19.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration19To20Test {

    private val dbName = "migration_19_20_test.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    /** CREATE statements of the current schema, minus the gym (v20) and v21 additions. */
    private fun v19Statements(): List<String> = OldSchemaBuilder.statements(
        context, dbName,
        skipTables = setOf("gyms") + OldSchemaBuilder.V21_TABLES + OldSchemaBuilder.V22_TABLES,
        skipIndices = setOf("index_exercises_gymId") + OldSchemaBuilder.V21_INDICES + OldSchemaBuilder.V22_INDICES
    ) { table, sql ->
        when (table) {
            "meals" -> OldSchemaBuilder.stripSortOrder(sql)
            "exercises" -> OldSchemaBuilder.stripSortOrder(OldSchemaBuilder.stripSyncGroup(sql))
                .replace(Regex(",\\s*`gymId` INTEGER"), "")
                .replace(Regex(",\\s*FOREIGN KEY\\(`gymId`\\)[^)]*\\)[^,)]*?ON DELETE SET NULL"), "")
                .also { assertFalse("gymId must be stripped from the v19 exercises table", it.contains("gymId")) }
            else -> sql
        }
    }

    private fun createV19WithData() {
        val statements = v19Statements()
        val file = context.getDatabasePath(dbName)
        file.parentFile?.mkdirs()
        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            statements.forEach { raw.execSQL(it) }
            raw.execSQL(
                "INSERT INTO exercises (id, name, kcalBurnedPerUnit, defaultWeight, defaultReps, defaultSets, " +
                    "category, primaryMuscles, secondaryMuscles, instructions, imagePaths, isVisible) " +
                    "VALUES (7, 'Bench', NULL, 40.0, 10, 3, 'strength', 'chest', '', '', '', 1)"
            )
            raw.execSQL(
                "INSERT INTO exercise_logs (id, exerciseId, logDate, logDateTime, weight, reps, sets, caloriesBurned) " +
                    "VALUES (1, 7, '2025-01-01', '2025-01-01 10:00:00', 40.0, 10, 3, 120.0)"
            )
            raw.execSQL("INSERT INTO tags (id, name, color, type) VALUES (1, 'Push', '#FF0000', 'EXERCISE')")
            raw.execSQL("INSERT INTO exercise_tags (id, exerciseId, tagId) VALUES (1, 7, 1)")
            raw.version = 19
        } finally {
            raw.close()
        }
    }

    @Test
    fun migrate19To20_keepsRows_andPassesRoomValidation() = runBlocking {
        createV19WithData()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                DatabaseMigrations.MIGRATION_17_18,
                DatabaseMigrations.MIGRATION_18_19,
                DatabaseMigrations.MIGRATION_19_20,
                DatabaseMigrations.MIGRATION_20_21,
                DatabaseMigrations.MIGRATION_21_22,
                DatabaseMigrations.MIGRATION_22_23
            )
            .allowMainThreadQueries()
            .build()
        try {
            // Opening runs the migration and Room's schema validation; it throws on any mismatch.
            val exercises = db.exerciseDao().getAllExercisesIncludingHiddenSync()
            assertEquals(1, exercises.size)
            assertEquals(7L, exercises[0].id)
            assertEquals("Bench", exercises[0].name)
            assertNull(exercises[0].gymId)

            assertEquals(1, db.exerciseLogDao().getAllExerciseLogsSync().size)
            assertTrue(db.gymDao().getAllGymsIncludingHiddenSync().isEmpty())

            val sqlite = db.openHelper.readableDatabase
            sqlite.query("PRAGMA foreign_key_list(exercises)").use { c ->
                var found = false
                while (c.moveToNext()) {
                    val table = c.getString(c.getColumnIndexOrThrow("table"))
                    val from = c.getString(c.getColumnIndexOrThrow("from"))
                    val onDelete = c.getString(c.getColumnIndexOrThrow("on_delete"))
                    if (table == "gyms" && from == "gymId" && onDelete == "SET NULL") found = true
                }
                assertTrue("FK exercises.gymId -> gyms.id ON DELETE SET NULL", found)
            }
            sqlite.query("PRAGMA index_list(exercises)").use { c ->
                val names = mutableListOf<String>()
                while (c.moveToNext()) names += c.getString(c.getColumnIndexOrThrow("name"))
                assertTrue("index_exercises_gymId in $names", "index_exercises_gymId" in names)
            }
            sqlite.query("SELECT COUNT(*) FROM exercise_tags").use { c ->
                c.moveToFirst()
                assertEquals(1, c.getInt(0))
            }
        } finally {
            db.close()
        }
    }
}
