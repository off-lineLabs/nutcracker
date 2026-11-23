package com.offlinelabs.nutcracker.data.migrations

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.offlinelabs.nutcracker.data.AppDatabase
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import com.offlinelabs.nutcracker.data.model.MealCheckIn
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Builds a real v22 database (current schema minus the check-in context columns) with logs and
 * opens it with Room, whose open-time validation proves MIGRATION_22_23 matches the entities.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration22To23Test {

    private val dbName = "migration_22_23_test.db"
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

    private fun createV22WithData() {
        val statements = OldSchemaBuilder.statements(context, dbName, skipTables = emptySet(), skipIndices = emptySet()) { _, sql -> sql }
        val file = context.getDatabasePath(dbName)
        file.parentFile?.mkdirs()
        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            statements.forEach { raw.execSQL(it) }
            raw.execSQL(
                "INSERT INTO exercises (id, name, kcalBurnedPerUnit, defaultWeight, defaultReps, defaultSets, " +
                    "category, primaryMuscles, secondaryMuscles, instructions, imagePaths, isVisible, gymId, sortOrder, syncGroupId) " +
                    "VALUES (7, 'Bench', NULL, 40.0, 10, 3, 'strength', 'chest', '', '', '', 1, NULL, 0, NULL)"
            )
            raw.execSQL(
                "INSERT INTO exercise_logs (id, exerciseId, logDate, logDateTime, weight, reps, sets, caloriesBurned, notes) " +
                    "VALUES (1, 7, '2026-11-22', '2026-11-22 10:00:00', 42.5, 8, 3, 30.0, NULL)"
            )
            raw.execSQL(
                "INSERT INTO meals (id, name, calories, carbohydrates_g, protein_g, fat_g, fiber_g, sodium_mg, " +
                    "servingSize_value, servingSize_unit, isVisible, source, sortOrder) " +
                    "VALUES (5, 'Oats', 380, 60.0, 13.0, 7.0, 10.0, 5.0, 100.0, 'g', 1, 'manual', 0)"
            )
            raw.execSQL(
                "INSERT INTO meal_check_ins (id, mealId, checkInDate, checkInDateTime, servingSize, notes) " +
                    "VALUES (1, 5, '2026-11-22', '2026-11-22 08:00:00', 1.5, NULL)"
            )
            raw.version = 22
        } finally {
            raw.close()
        }
    }

    @Test
    fun migrate22To23_keepsLogs_withoutContext_andStoresNewContext() = runBlocking {
        createV22WithData()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(DatabaseMigrations.MIGRATION_22_23)
            .allowMainThreadQueries()
            .build()
        try {
            val log = db.exerciseLogDao().getAllExerciseLogsSync().single()
            assertEquals(42.5, log.weight, 0.0)
            assertNull(log.routineId)
            val checkIn = db.mealCheckInDao().getAllMealCheckInsSync().single()
            assertEquals(1.5, checkIn.servingSize, 0.0)
            assertNull(checkIn.mealCategoryId)

            // New check-ins keep their routine / meal type
            db.exerciseLogDao().insertExerciseLog(
                ExerciseLog(exerciseId = 7, logDate = "2026-11-23", logDateTime = "2026-11-23 10:00:00",
                    weight = 45.0, reps = 6, sets = 3, caloriesBurned = 30.0, routineId = 4)
            )
            db.mealCheckInDao().insertMealCheckIn(
                MealCheckIn(mealId = 5, checkInDate = "2026-11-23", checkInDateTime = "2026-11-23 08:00:00", mealCategoryId = 2)
            )
            assertEquals(4L, db.exerciseLogDao().getAllExerciseLogsSync().first { it.logDate == "2026-11-23" }.routineId)
            assertEquals(2L, db.mealCheckInDao().getAllMealCheckInsSync().first { it.checkInDate == "2026-11-23" }.mealCategoryId)
        } finally {
            db.close()
        }
    }
}
