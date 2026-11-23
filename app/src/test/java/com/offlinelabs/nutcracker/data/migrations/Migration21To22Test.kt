package com.offlinelabs.nutcracker.data.migrations

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.offlinelabs.nutcracker.data.AppDatabase
import com.offlinelabs.nutcracker.data.model.MealCategoryDefaults
import com.offlinelabs.nutcracker.data.model.MealCategoryMeal
import com.offlinelabs.nutcracker.data.model.Routine
import com.offlinelabs.nutcracker.data.model.RoutineExercise
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Builds a real v21 database (current schema minus the v22 additions, routines without a gym) and
 * opens it with Room, whose open-time validation proves MIGRATION_21_22 matches the entities.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration21To22Test {

    private val dbName = "migration_21_22_test.db"
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

    private fun createV21WithData() {
        val statements = OldSchemaBuilder.statements(
            context, dbName,
            skipTables = OldSchemaBuilder.V22_TABLES,
            skipIndices = OldSchemaBuilder.V22_INDICES
        ) { table, sql ->
            when (table) {
                "exercises" -> OldSchemaBuilder.stripSyncGroup(sql)
                "routines" -> OldSchemaBuilder.stripRoutineGym(sql)
                else -> sql
            }
        }
        val file = context.getDatabasePath(dbName)
        file.parentFile?.mkdirs()
        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            statements.forEach { raw.execSQL(it) }
            raw.execSQL("INSERT INTO gyms (id, name, unit, mapsPlaceId, mapsProvider, isVisible) VALUES (3, 'Basic', NULL, NULL, NULL, 1)")
            raw.execSQL(
                "INSERT INTO exercises (id, name, kcalBurnedPerUnit, defaultWeight, defaultReps, defaultSets, " +
                    "category, primaryMuscles, secondaryMuscles, instructions, imagePaths, isVisible, gymId, sortOrder) " +
                    "VALUES (7, 'Bench', NULL, 40.0, 10, 3, 'strength', 'chest', '', '', '', 1, 3, 5)"
            )
            raw.execSQL(
                "INSERT INTO meals (id, name, calories, carbohydrates_g, protein_g, fat_g, fiber_g, sodium_mg, " +
                    "servingSize_value, servingSize_unit, isVisible, source, sortOrder) " +
                    "VALUES (5, 'Oats', 380, 60.0, 13.0, 7.0, 10.0, 5.0, 100.0, 'g', 1, 'manual', 2)"
            )
            // A gym-less v21 routine (there was no UI to create one) is dropped by the migration
            raw.execSQL("INSERT INTO routines (id, name, sortOrder, isVisible) VALUES (1, 'Old', 0, 1)")
            raw.execSQL("INSERT INTO routine_exercises (routineId, exerciseId, position) VALUES (1, 7, 0)")
            raw.version = 21
        } finally {
            raw.close()
        }
    }

    @Test
    fun migrate21To22_seedsCategories_recreatesRoutines_addsSyncGroup() = runBlocking {
        createV21WithData()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(DatabaseMigrations.MIGRATION_21_22, DatabaseMigrations.MIGRATION_22_23, DatabaseMigrations.MIGRATION_23_24)
            .allowMainThreadQueries()
            .build()
        try {
            val exercise = db.exerciseDao().getAllExercisesIncludingHiddenSync().single()
            assertEquals(5, exercise.sortOrder)
            assertNull(exercise.syncGroupId)
            assertTrue(db.routineDao().getAllRoutinesIncludingHiddenSync().isEmpty())
            assertTrue(db.routineDao().getAllRoutineExercisesSync().isEmpty())

            val categories = db.mealCategoryDao().getAllCategoriesSync()
            assertEquals(MealCategoryDefaults.all.map { it.defaultKey }, categories.map { it.defaultKey })

            // Routines need a gym and cascade-delete their memberships; meal links work
            val routineId = db.routineDao().upsertRoutine(Routine(name = "Leg day", gymId = 3))
            db.routineDao().insertRoutineExercise(RoutineExercise(routineId, 7, 0))
            db.mealCategoryDao().insertLink(MealCategoryMeal(categories.first().id, 5))
            assertEquals(listOf(categories.first().id), db.mealCategoryDao().getCategoryIdsForMeal(5))
            db.routineDao().deleteAllRoutines()
            assertTrue(db.routineDao().getAllRoutineExercisesSync().isEmpty())
        } finally {
            db.close()
        }
    }
}
