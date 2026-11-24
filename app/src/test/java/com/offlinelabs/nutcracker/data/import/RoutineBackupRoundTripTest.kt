package com.offlinelabs.nutcracker.data.import

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.offlinelabs.nutcracker.data.AppDatabase
import com.offlinelabs.nutcracker.data.export.DatabaseExportManager
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import com.offlinelabs.nutcracker.data.model.ExerciseSet
import com.offlinelabs.nutcracker.data.model.MealCheckIn
import com.offlinelabs.nutcracker.data.model.Gym
import com.offlinelabs.nutcracker.data.model.MealCategory
import com.offlinelabs.nutcracker.data.model.MealCategoryDefaults
import com.offlinelabs.nutcracker.data.model.MealCategoryMeal
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.Routine
import com.offlinelabs.nutcracker.data.model.RoutineExercise
import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
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
        val gymId = source.gymDao().upsertGym(Gym(name = "Basic", unit = "Uni"))
        val benchId = source.exerciseDao().upsertExercise(Exercise(name = "Bench", sortOrder = 2, gymId = gymId, syncGroupId = "sync-1"))
        val squatId = source.exerciseDao().upsertExercise(Exercise(name = "Squat", sortOrder = 1))
        val push = source.routineDao().upsertRoutine(Routine(name = "Push day", gymId = gymId, sortOrder = 1))
        val full = source.routineDao().upsertRoutine(Routine(name = "Full body", gymId = gymId, sortOrder = 0))
        val oatsId = source.mealDao().getAllMealsIncludingHiddenSync().single().id
        val breakfast = source.mealCategoryDao().upsertCategory(MealCategoryDefaults.all.first())
        val preWorkout = source.mealCategoryDao().upsertCategory(MealCategory(name = "Pre-workout", sortOrder = 4))
        source.mealCategoryDao().insertLink(MealCategoryMeal(breakfast, oatsId))
        source.mealCategoryDao().insertLink(MealCategoryMeal(preWorkout, oatsId))
        source.routineDao().insertRoutineExercise(RoutineExercise(push, benchId, 0))
        source.routineDao().insertRoutineExercise(RoutineExercise(full, benchId, 1))
        source.routineDao().insertRoutineExercise(RoutineExercise(full, squatId, 0))
        // Check-ins remember the routine / meal type they were ticked under (v23)
        // A pyramid: its own sets survive the round trip
        source.exerciseLogDao().insertLogWithSets(
            ExerciseLog(exerciseId = benchId, logDate = "2026-11-23", logDateTime = "2026-11-23 10:00:00",
                weight = 45.0, reps = 6, sets = 2, caloriesBurned = 30.0, routineId = push),
            listOf(
                ExerciseSet(logId = 0, setIndex = 0, weight = 40.0, reps = 8, completedAt = "2026-11-23 10:00:00"),
                ExerciseSet(logId = 0, setIndex = 1, weight = 45.0, reps = 6, completedAt = "2026-11-23 10:03:00")
            )
        )
        source.exerciseLogDao().insertExerciseLog(
            ExerciseLog(exerciseId = squatId, logDate = "2026-11-23", logDateTime = "2026-11-23 10:05:00",
                weight = 60.0, reps = 5, sets = 5, caloriesBurned = 30.0)
        )
        source.mealCheckInDao().insertMealCheckIn(
            MealCheckIn(mealId = oatsId, checkInDate = "2026-11-23", checkInDateTime = "2026-11-23 08:00:00", mealCategoryId = preWorkout)
        )

        assertTrue(DatabaseExportManager(context, source).exportToFile(zip).isSuccess)
        val result = DatabaseImportManager(context, target).importDatabase(Uri.fromFile(zip), ImportMode.REPLACE)
        assertTrue(result.exceptionOrNull()?.toString() ?: "", result.isSuccess)

        assertEquals(4, target.mealDao().getAllMealsIncludingHiddenSync().single().sortOrder)
        val exercises = target.exerciseDao().getAllExercisesIncludingHiddenSync().associateBy { it.name }
        assertEquals(2, exercises.getValue("Bench").sortOrder)
        assertEquals(1, exercises.getValue("Squat").sortOrder)
        assertEquals("sync-1", exercises.getValue("Bench").syncGroupId)

        val categories = target.mealCategoryDao().getAllCategoriesSync().associateBy { it.name }
        assertEquals(setOf("Breakfast", "Pre-workout"), categories.keys)
        assertEquals(MealCategoryDefaults.BREAKFAST, categories.getValue("Breakfast").defaultKey)
        val newOats = target.mealDao().getAllMealsIncludingHiddenSync().single().id
        assertEquals(
            categories.values.map { it.id }.toSet(),
            target.mealCategoryDao().getCategoryIdsForMeal(newOats).toSet()
        )
        val targetGym = target.gymDao().getAllGymsIncludingHiddenSync().single().id
        assertTrue(target.routineDao().getAllRoutinesIncludingHiddenSync().all { it.gymId == targetGym })

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

        val logs = target.exerciseLogDao().getAllExerciseLogsSync().associateBy { it.exerciseId }
        assertEquals(routines.getValue("Push day").id, logs.getValue(exercises.getValue("Bench").id).routineId)
        assertNull(logs.getValue(exercises.getValue("Squat").id).routineId)
        val benchSets = target.exerciseLogDao().getSetsForLog(logs.getValue(exercises.getValue("Bench").id).id)
        assertEquals(listOf(40.0 to 8, 45.0 to 6), benchSets.map { it.weight to it.reps })
        assertEquals("2026-11-23 10:03:00", benchSets.last().completedAt)
        // The squat log was stored without sets: the import fills in identical ones
        assertEquals(5, target.exerciseLogDao().getSetsForLog(logs.getValue(exercises.getValue("Squat").id).id).size)
        assertEquals(categories.getValue("Pre-workout").id, target.mealCheckInDao().getAllMealCheckInsSync().single().mealCategoryId)
    }
}
