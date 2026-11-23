package com.offlinelabs.nutcracker.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.offlinelabs.nutcracker.data.AppDatabase
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.MealCheckIn
import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unticking in a routine / meal type group removes that group's check-ins plus the ones without
 * a group (they show there too), and leaves other groups' check-ins alone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CheckInContextDaoTest {

    private lateinit var db: AppDatabase
    private val day = "2026-11-23"

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun log(exerciseId: Long, routineId: Long?, time: String, reps: Int = 8) = ExerciseLog(
        exerciseId = exerciseId, logDate = day, logDateTime = "$day $time", weight = 40.0,
        reps = reps, sets = 3, caloriesBurned = 30.0, routineId = routineId
    )

    @Test
    fun removeExerciseLogsInRoutine_keepsOtherRoutines() = runBlocking {
        val bench = db.exerciseDao().upsertExercise(Exercise(name = "Bench"))
        val logs = db.exerciseLogDao()
        logs.insertExerciseLog(log(bench, 1, "10:00:00"))
        logs.insertExerciseLog(log(bench, 2, "10:05:00"))
        logs.insertExerciseLog(log(bench, null, "10:10:00", reps = 5))

        val removed = logs.removeLogsForExerciseOnDate(bench, day, 1)
        assertEquals(setOf(1L, null), removed.map { it.routineId }.toSet())
        assertEquals(listOf(2L), logs.getAllExerciseLogsSync().map { it.routineId })

        // Without a routine everything of the day goes
        logs.insertExerciseLog(log(bench, null, "11:00:00", reps = 6))
        logs.removeLogsForExerciseOnDate(bench, day, null)
        assertEquals(0, logs.getAllExerciseLogsSync().size)
    }

    @Test
    fun lastReps_comesFromTheMostRecentLog() = runBlocking {
        val bench = db.exerciseDao().upsertExercise(Exercise(name = "Bench"))
        db.exerciseLogDao().insertExerciseLog(log(bench, null, "09:00:00", reps = 5))
        db.exerciseLogDao().insertExerciseLog(log(bench, null, "10:00:00", reps = 12))
        assertEquals(mapOf(bench to 12), db.exerciseLogDao().getLastReps().first())
    }

    @Test
    fun removeMealCheckInsInCategory_keepsOtherCategories_andLastServing() = runBlocking {
        db.mealDao().insertMeal(
            Meal(name = "Banana", calories = 90, carbohydrates_g = 0.0, protein_g = 0.0, fat_g = 0.0, fiber_g = 0.0,
                sodium_mg = 0.0, servingSize_value = 100.0, servingSize_unit = ServingSizeUnit.GRAMS)
        )
        val banana = db.mealDao().getAllMealsIncludingHiddenSync().single().id
        val checkIns = db.mealCheckInDao()
        checkIns.insertMealCheckIn(MealCheckIn(mealId = banana, checkInDate = day, checkInDateTime = "$day 08:00:00", servingSize = 1.0, mealCategoryId = 1))
        checkIns.insertMealCheckIn(MealCheckIn(mealId = banana, checkInDate = day, checkInDateTime = "$day 16:00:00", servingSize = 1.5, mealCategoryId = 4))
        assertEquals(mapOf(banana to 1.5), checkIns.getLastServings().first())

        checkIns.removeCheckInsForMealOnDate(banana, day, 1)
        assertEquals(listOf(4L), checkIns.getAllMealCheckInsSync().map { it.mealCategoryId })
    }
}
