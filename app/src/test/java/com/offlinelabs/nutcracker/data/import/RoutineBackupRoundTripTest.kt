package com.offlinelabs.nutcracker.data.import

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.offlinelabs.nutcracker.data.AppDatabase
import com.offlinelabs.nutcracker.data.export.DatabaseExportManager
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.Routine
import com.offlinelabs.nutcracker.data.model.RoutineExercise
import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Export → import keeps the custom sort order and routine memberships (v21 additions). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoutineBackupRoundTripTest {

    private lateinit var context: Context
    private lateinit var source: AppDatabase
    private lateinit var target: AppDatabase
    private lateinit var zip: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        source = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        target = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        zip = File(context.cacheDir, "roundtrip.zip")
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
        zip.delete()
    }

    @Test
    fun sortOrderAndRoutinesSurviveRoundTrip() = runBlocking {
        source.mealDao().insertMeal(
            Meal(
                name = "Oats", calories = 380, carbohydrates_g = 60.0, protein_g = 13.0, fat_g = 7.0,
                fiber_g = 10.0, sodium_mg = 5.0, servingSize_value = 100.0, servingSize_unit = ServingSizeUnit.GRAMS,
                sortOrder = 4
            )
        )
        val benchId = source.exerciseDao().upsertExercise(Exercise(name = "Bench", sortOrder = 2))
        val squatId = source.exerciseDao().upsertExercise(Exercise(name = "Squat", sortOrder = 1))
        val push = source.routineDao().upsertRoutine(Routine(name = "Push day", sortOrder = 1))
        val full = source.routineDao().upsertRoutine(Routine(name = "Full body", sortOrder = 0))
        source.routineDao().insertRoutineExercise(RoutineExercise(push, benchId, 0))
        source.routineDao().insertRoutineExercise(RoutineExercise(full, benchId, 1))
        source.routineDao().insertRoutineExercise(RoutineExercise(full, squatId, 0))

        assertTrue(DatabaseExportManager(context, source).exportToFile(zip).isSuccess)
        val result = DatabaseImportManager(context, target).importDatabase(Uri.fromFile(zip), ImportMode.REPLACE)
        assertTrue(result.exceptionOrNull()?.toString() ?: "", result.isSuccess)

        assertEquals(4, target.mealDao().getAllMealsIncludingHiddenSync().single().sortOrder)
        val exercises = target.exerciseDao().getAllExercisesIncludingHiddenSync().associateBy { it.name }
        assertEquals(2, exercises.getValue("Bench").sortOrder)
        assertEquals(1, exercises.getValue("Squat").sortOrder)

        val routines = target.routineDao().getAllRoutinesIncludingHiddenSync().associateBy { it.name }
        assertEquals(setOf("Push day", "Full body"), routines.keys)
        assertEquals(1, routines.getValue("Push day").sortOrder)
        val links = target.routineDao().getAllRoutineExercisesSync()
            .map { Triple(it.routineId, it.exerciseId, it.position) }.toSet()
        assertEquals(
            setOf(
                Triple(routines.getValue("Push day").id, exercises.getValue("Bench").id, 0),
                Triple(routines.getValue("Full body").id, exercises.getValue("Bench").id, 1),
                Triple(routines.getValue("Full body").id, exercises.getValue("Squat").id, 0)
            ),
            links
        )
    }
}
