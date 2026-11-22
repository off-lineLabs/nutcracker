package com.offlinelabs.nutcracker.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.offlinelabs.nutcracker.data.AppDatabase
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import com.offlinelabs.nutcracker.data.model.Gym
import com.offlinelabs.nutcracker.data.model.Routine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GymRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: OfflineFoodLogRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = OfflineFoodLogRepository(
            db.mealDao(), db.userGoalDao(), db.mealCheckInDao(), db.exerciseDao(), db.exerciseLogDao(),
            db.pillDao(), db.pillCheckInDao(), db.recipeDao(), db.recipeIngredientDao(), db.gymDao(),
            db.routineDao(), db.mealCategoryDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun exerciseWithLog(gymId: Long?, name: String = "Press"): Long {
        val id = repo.insertExercise(Exercise(name = name, gymId = gymId))
        repo.insertExerciseLog(
            ExerciseLog(
                exerciseId = id, logDate = "2025-01-01", logDateTime = "2025-01-01 10:00:00",
                weight = 35.0, reps = 10, sets = 3, caloriesBurned = 100.0
            )
        )
        return id
    }

    private suspend fun exercise(id: Long) = db.exerciseDao().getExerciseById(id).first()!!

    @Test
    fun deleteGym_onlyGym_makesExercisesShared_andHidesGym() = runBlocking {
        val gymId = repo.upsertGym(Gym(name = "Basic Fit", unit = "Uni"))
        val exId = exerciseWithLog(gymId)

        repo.deleteGym(gymId, removeExercises = false)

        val ex = exercise(exId)
        assertNull(ex.gymId)
        assertTrue(ex.isVisible)
        val gym = repo.getGymById(gymId)
        assertNotNull(gym)
        assertEquals(false, gym!!.isVisible)
        assertTrue(repo.getVisibleGyms().first().isEmpty())
        assertEquals(1, db.exerciseLogDao().getAllExerciseLogsSync().size)
    }

    @Test
    fun deleteGym_withExercises_hidesThem_keepsGymIdAndLogs() = runBlocking {
        val gymId = repo.upsertGym(Gym(name = "Basic Fit"))
        val otherGymId = repo.upsertGym(Gym(name = "Other"))
        val exId = exerciseWithLog(gymId)
        val otherId = exerciseWithLog(otherGymId, "Other press")
        val sharedId = exerciseWithLog(null, "Shared press")

        repo.deleteGym(gymId, removeExercises = true)

        val ex = exercise(exId)
        assertEquals(false, ex.isVisible)
        assertEquals(gymId, ex.gymId)
        assertEquals(false, repo.getGymById(gymId)!!.isVisible)
        assertEquals(3, db.exerciseLogDao().getAllExerciseLogsSync().size)
        assertTrue(exercise(otherId).isVisible)
        assertTrue(exercise(sharedId).isVisible)
        assertEquals(listOf("Other"), repo.getVisibleGyms().first().map { it.name })
    }

    @Test
    fun upsertGym_update_keepsExercisesAttached() = runBlocking {
        val gymId = repo.upsertGym(Gym(name = "Old"))
        val exId = repo.insertExercise(Exercise(name = "Press", gymId = gymId))

        val returned = repo.upsertGym(Gym(id = gymId, name = "New"))

        assertEquals(gymId, returned)
        assertEquals("New", repo.getGymById(gymId)!!.name)
        assertEquals(gymId, exercise(exId).gymId)
    }

    @Test
    fun visibleGyms_orderedByNameIgnoringCase() = runBlocking {
        repo.upsertGym(Gym(name = "beta"))
        repo.upsertGym(Gym(name = "Alpha"))
        repo.upsertGym(Gym(name = "Charlie"))
        assertEquals(listOf("Alpha", "beta", "Charlie"), repo.getVisibleGyms().first().map { it.name })
    }

    @Test
    fun copyExerciseToGym_createsNewRow_withoutLogs() = runBlocking {
        val gymA = repo.upsertGym(Gym(name = "A"))
        val gymB = repo.upsertGym(Gym(name = "B"))
        val original = exercise(exerciseWithLog(gymA))

        val newId = repo.copyExerciseToGym(original, gymB)

        assertNotEquals(original.id, newId)
        val copy = exercise(newId)
        assertEquals(gymB, copy.gymId)
        assertTrue(copy.isVisible)
        assertEquals(original.name, copy.name)
        assertEquals(gymA, exercise(original.id).gymId)
        val logs = db.exerciseLogDao().getAllExerciseLogsSync()
        assertEquals(1, logs.size)
        assertEquals(original.id, logs[0].exerciseId)

        val sharedCopyId = repo.copyExerciseToGym(original, null)
        assertNull(exercise(sharedCopyId).gymId)
    }

    @Test
    fun syncedCopy_editPropagatesEverything_exceptPerCopyFields() = runBlocking {
        val gymA = repo.upsertGym(Gym(name = "A"))
        val gymB = repo.upsertGym(Gym(name = "B"))
        val original = exercise(repo.insertExercise(Exercise(name = "Press", gymId = gymA, defaultWeight = 35.0)))
        val copyId = repo.copyExerciseToGym(original, gymB, sync = true)
        val synced = exercise(original.id)
        assertNotNull(synced.syncGroupId)
        assertEquals(synced.syncGroupId, exercise(copyId).syncGroupId)

        repo.updateExercise(synced.copy(defaultWeight = 45.0, defaultReps = 12, notes = "slow"))

        val copy = exercise(copyId)
        assertEquals(45.0, copy.defaultWeight, 0.0) // weight syncs too (user's choice)
        assertEquals(12, copy.defaultReps)
        assertEquals("slow", copy.notes)
        assertEquals(gymB, copy.gymId) // the gym stays per copy
    }

    @Test
    fun unsync_lastPairDissolvesGroup_andStopsPropagation() = runBlocking {
        val gymA = repo.upsertGym(Gym(name = "A"))
        val gymB = repo.upsertGym(Gym(name = "B"))
        val original = exercise(repo.insertExercise(Exercise(name = "Press", gymId = gymA)))
        val copyId = repo.copyExerciseToGym(original, gymB, sync = true)

        repo.unsyncExercise(copyId)

        assertNull(exercise(copyId).syncGroupId)
        assertNull(exercise(original.id).syncGroupId) // alone in its group, so unlinked too
        repo.updateExercise(exercise(original.id).copy(defaultReps = 20))
        assertEquals(original.defaultReps, exercise(copyId).defaultReps)
    }

    @Test
    fun movingExerciseToAnotherGym_dropsItFromThatGymsRoutines() = runBlocking {
        val gymA = repo.upsertGym(Gym(name = "A"))
        val gymB = repo.upsertGym(Gym(name = "B"))
        val exId = repo.insertExercise(Exercise(name = "Press", gymId = gymA))
        val routineId = repo.saveRoutine(Routine(name = "Day 1", gymId = gymA), listOf(exId))
        assertEquals(1, db.routineDao().getAllRoutineExercisesSync().count { it.routineId == routineId })

        repo.updateExercise(exercise(exId).copy(gymId = gymB))

        assertTrue(db.routineDao().getAllRoutineExercisesSync().isEmpty())
    }

    @Test
    fun deleteGym_hidesItsRoutines() = runBlocking {
        val gymA = repo.upsertGym(Gym(name = "A"))
        repo.saveRoutine(Routine(name = "Day 1", gymId = gymA), emptyList())
        repo.deleteGym(gymA, removeExercises = false)
        assertTrue(repo.getVisibleRoutines().first().isEmpty())
    }

    @Test
    fun saveRoutine_keepsGivenExerciseOrder_andReplacesOnEdit() = runBlocking {
        val gymA = repo.upsertGym(Gym(name = "A"))
        val a = repo.insertExercise(Exercise(name = "A", gymId = gymA))
        val b = repo.insertExercise(Exercise(name = "B"))
        val routineId = repo.saveRoutine(Routine(name = "Day 1", gymId = gymA), listOf(b, a))
        assertEquals(listOf(b, a), db.routineDao().getAllRoutineExercisesSync().sortedBy { it.position }.map { it.exerciseId })
        repo.saveRoutine(repo.getVisibleRoutines().first().single().copy(name = "Day 2"), listOf(a))
        assertEquals(listOf(a), db.routineDao().getAllRoutineExercisesSync().map { it.exerciseId })
        assertEquals("Day 2", db.routineDao().getRoutineById(routineId)!!.name)
    }

    @Test
    fun dailySummary_showsGymName_evenWhenGymHidden() = runBlocking {
        val gymId = repo.upsertGym(Gym(name = "Basic Fit", unit = "Uni"))
        exerciseWithLog(gymId)
        exerciseWithLog(null, "Shared")
        repo.deleteGym(gymId, removeExercises = true)

        val entries = repo.getExerciseLogsByDate("2025-01-01").first()

        assertEquals(2, entries.size)
        val gymEntry = entries.first { it.exerciseName == "Press" }
        assertEquals("Basic Fit", gymEntry.gymName)
        assertEquals("Uni", gymEntry.gymUnit)
        assertNull(entries.first { it.exerciseName == "Shared" }.gymName)
    }
}
