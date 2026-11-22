package com.offlinelabs.nutcracker.data.migrations

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.offlinelabs.nutcracker.data.AppDatabase
import com.offlinelabs.nutcracker.data.model.Routine
import com.offlinelabs.nutcracker.data.model.RoutineExercise
import kotlinx.coroutines.flow.first
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
 * Builds a real v20 database (current schema minus the v21 additions) and opens it with Room, whose
 * open-time validation proves MIGRATION_20_21 matches the entities exactly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration20To21Test {

    private val dbName = "migration_20_21_test.db"
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

    private fun createV20WithData() {
        val statements = OldSchemaBuilder.statements(
            context, dbName,
            skipTables = OldSchemaBuilder.V21_TABLES,
            skipIndices = OldSchemaBuilder.V21_INDICES
        ) { table, sql ->
            if (table == "meals" || table == "exercises") OldSchemaBuilder.stripSortOrder(sql) else sql
        }
        val file = context.getDatabasePath(dbName)
        file.parentFile?.mkdirs()
        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            statements.forEach { raw.execSQL(it) }
            raw.execSQL("INSERT INTO gyms (id, name, unit, mapsPlaceId, mapsProvider, isVisible) VALUES (3, 'Basic', 'Uni', NULL, NULL, 1)")
            raw.execSQL(
                "INSERT INTO exercises (id, name, kcalBurnedPerUnit, defaultWeight, defaultReps, defaultSets, " +
                    "category, primaryMuscles, secondaryMuscles, instructions, imagePaths, isVisible, gymId) " +
                    "VALUES (7, 'Bench', NULL, 40.0, 10, 3, 'strength', 'chest', '', '', '', 1, 3)"
            )
            raw.execSQL(
                "INSERT INTO meals (id, name, calories, carbohydrates_g, protein_g, fat_g, fiber_g, sodium_mg, " +
                    "servingSize_value, servingSize_unit, isVisible, source) " +
                    "VALUES (5, 'Oats', 380, 60.0, 13.0, 7.0, 10.0, 5.0, 100.0, 'g', 1, 'manual')"
            )
            raw.version = 20
        } finally {
            raw.close()
        }
    }

    @Test
    fun migrate20To21_addsSortOrderAndRoutines() = runBlocking {
        createV20WithData()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(DatabaseMigrations.MIGRATION_19_20, DatabaseMigrations.MIGRATION_20_21)
            .allowMainThreadQueries()
            .build()
        try {
            val exercises = db.exerciseDao().getAllExercisesIncludingHiddenSync()
            assertEquals(1, exercises.size)
            assertEquals(0, exercises[0].sortOrder)
            assertEquals(3L, exercises[0].gymId)
            val meals = db.mealDao().getAllMealsIncludingHiddenSync()
            assertEquals(0, meals.single().sortOrder)

            // New tables work, including the many-to-many link and its cascade on routine delete
            val routineId = db.routineDao().upsertRoutine(Routine(name = "Push day"))
            db.routineDao().insertRoutineExercise(RoutineExercise(routineId, 7L, 0))
            assertEquals(-1L, db.routineDao().insertRoutineExercise(RoutineExercise(routineId, 7L, 1)))
            assertEquals(1, db.routineDao().getVisibleRoutineExercises().first().size)
            db.routineDao().deleteAllRoutines()
            assertTrue(db.routineDao().getAllRoutineExercisesSync().isEmpty())

            // Updating an exercise keeps its routine memberships (@Upsert, not REPLACE)
            val r2 = db.routineDao().upsertRoutine(Routine(name = "Legs"))
            db.routineDao().insertRoutineExercise(RoutineExercise(r2, 7L, 0))
            assertEquals(-1L, db.exerciseDao().upsertExercise(exercises[0].copy(defaultWeight = 45.0)))
            assertEquals(1, db.routineDao().getAllRoutineExercisesSync().size)
        } finally {
            db.close()
        }
    }
}
