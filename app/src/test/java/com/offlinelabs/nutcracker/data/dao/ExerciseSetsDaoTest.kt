package com.offlinelabs.nutcracker.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.offlinelabs.nutcracker.data.AppDatabase
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import com.offlinelabs.nutcracker.data.model.ExerciseSet
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Ticking sets one by one: the check-in's summary and kcal follow its sets. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseSetsDaoTest {

    private lateinit var db: AppDatabase
    private val day = "2026-11-23"

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun set(index: Int, weight: Double, reps: Int) =
        ExerciseSet(logId = 0, setIndex = index, weight = weight, reps = reps, completedAt = "$day 10:0$index:00")

    @Test
    fun checkInSetsOneByOne_summaryFollows_andLastUntickRemovesTheLog() = runBlocking {
        val bench = db.exerciseDao().upsertExercise(Exercise(name = "Bench"))
        val dao = db.exerciseLogDao()
        val newLog = ExerciseLog(exerciseId = bench, logDate = day, logDateTime = "$day 10:00:00",
            weight = 0.0, reps = 0, sets = 0, caloriesBurned = 0.0, routineId = 3)

        val logId = dao.checkInSets(null, newLog, listOf(set(0, 40.0, 10)), kcalPerSet = 5.0)
        dao.checkInSets(logId, newLog, listOf(set(1, 45.0, 8)), kcalPerSet = 5.0)
        dao.checkInSets(logId, newLog, listOf(set(2, 50.0, 6)), kcalPerSet = 5.0)

        val log = dao.getLogById(logId)!!
        assertEquals(3, log.sets)
        assertEquals(50.0, log.weight, 0.0)
        assertEquals(6, log.reps)
        assertEquals(15.0, log.caloriesBurned, 0.0)
        assertEquals(3L, log.routineId)

        // Re-ticking an index replaces it
        dao.checkInSets(logId, newLog, listOf(set(1, 47.5, 8)), kcalPerSet = 5.0)
        assertEquals(listOf(40.0, 47.5, 50.0), dao.getSetsForLog(logId).map { it.weight })

        dao.uncheckSet(logId, 2, kcalPerSet = 5.0)
        assertEquals(2, dao.getLogById(logId)!!.sets)
        assertEquals(47.5, dao.getLogById(logId)!!.weight, 0.0)
        dao.uncheckSet(logId, 0, kcalPerSet = 5.0)
        dao.uncheckSet(logId, 1, kcalPerSet = 5.0)
        assertNull(dao.getLogById(logId))
    }

    @Test
    fun updatingTheSummary_resetsSets_onlyWhenItsValuesChange() = runBlocking {
        val bench = db.exerciseDao().upsertExercise(Exercise(name = "Bench"))
        val dao = db.exerciseLogDao()
        val log = ExerciseLog(exerciseId = bench, logDate = day, logDateTime = "$day 10:00:00",
            weight = 45.0, reps = 6, sets = 2, caloriesBurned = 10.0)
        val id = dao.insertLogWithSets(log, listOf(set(0, 40.0, 8), set(1, 45.0, 6)))

        // Notes only: the pyramid stays
        dao.updateLogKeepingSets(log.copy(id = id, notes = "felt good"))
        assertEquals(listOf(40.0, 45.0), dao.getSetsForLog(id).map { it.weight })

        // New weight for all sets (the check-in dialog): identical sets
        dao.updateLogKeepingSets(log.copy(id = id, weight = 50.0, sets = 3))
        assertEquals(listOf(50.0, 50.0, 50.0), dao.getSetsForLog(id).map { it.weight })
    }
}
