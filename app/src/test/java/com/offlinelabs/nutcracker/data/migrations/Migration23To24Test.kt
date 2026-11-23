package com.offlinelabs.nutcracker.data.migrations

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.offlinelabs.nutcracker.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Builds a real v23 database (current schema minus exercise_sets) with check-ins and opens it with
 * Room: the schema must match, and every check-in gets identical sets (at least one).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration23To24Test {

    private val dbName = "migration_23_24_test.db"
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

    private fun createV23WithData() {
        val statements = OldSchemaBuilder.statements(
            context, dbName, skipTables = emptySet(), skipIndices = emptySet(), keepCheckInContext = true
        ) { _, sql -> sql }
        val file = context.getDatabasePath(dbName)
        file.parentFile?.mkdirs()
        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            statements.forEach { raw.execSQL(it) }
            raw.execSQL(
                "INSERT INTO exercises (id, name, kcalBurnedPerUnit, defaultWeight, defaultReps, defaultSets, " +
                    "category, primaryMuscles, secondaryMuscles, instructions, imagePaths, isVisible, gymId, sortOrder, syncGroupId) " +
                    "VALUES (7, 'Bench', 5.0, 40.0, 10, 3, 'strength', 'chest', '', '', '', 1, NULL, 0, NULL)"
            )
            raw.execSQL(
                "INSERT INTO exercise_logs (id, exerciseId, logDate, logDateTime, weight, reps, sets, caloriesBurned, notes, routineId) " +
                    "VALUES (1, 7, '2026-11-22', '2026-11-22 10:00:00', 42.5, 8, 3, 15.0, NULL, 4)"
            )
            // A log with no sets recorded still gets one
            raw.execSQL(
                "INSERT INTO exercise_logs (id, exerciseId, logDate, logDateTime, weight, reps, sets, caloriesBurned, notes, routineId) " +
                    "VALUES (2, 7, '2026-11-23', '2026-11-23 10:00:00', 0.0, 30, 0, 0.0, NULL, NULL)"
            )
            raw.version = 23
        } finally {
            raw.close()
        }
    }

    @Test
    fun migrate23To24_givesEveryCheckInItsSets() = runBlocking {
        createV23WithData()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(DatabaseMigrations.MIGRATION_23_24)
            .allowMainThreadQueries()
            .build()
        try {
            val first = db.exerciseLogDao().getSetsForLog(1)
            assertEquals(listOf(0, 1, 2), first.map { it.setIndex })
            assertTrue(first.all { it.weight == 42.5 && it.reps == 8 && it.completedAt == "2026-11-22 10:00:00" })
            assertEquals(1, db.exerciseLogDao().getSetsForLog(2).size)
            // The log itself is unchanged (routine kept); deleting it takes its sets
            assertEquals(4L, db.exerciseLogDao().getLogById(1)?.routineId)
            db.exerciseLogDao().deleteLogById(1)
            assertTrue(db.exerciseLogDao().getSetsForLog(1).isEmpty())
        } finally {
            db.close()
        }
    }
}
