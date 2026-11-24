package com.offlinelabs.nutcracker.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.offlinelabs.nutcracker.data.AppDatabase
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.Gym
import com.offlinelabs.nutcracker.data.model.Routine
import com.offlinelabs.nutcracker.data.model.RoutineExercise
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Picking routines in the exercise form: joins at the end, leaves unpicked ones, ignores others. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoutineMembershipDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun setRoutinesForExercise_addsAtTheEnd_removesUnpicked_keepsOutOfScope() = runBlocking {
        val gym = db.gymDao().upsertGym(Gym(name = "Basic"))
        val squat = db.exerciseDao().upsertExercise(Exercise(name = "Squat"))
        val bench = db.exerciseDao().upsertExercise(Exercise(name = "Bench"))
        val routines = db.routineDao()
        val legs = routines.upsertRoutine(Routine(name = "Legs", gymId = gym))
        val push = routines.upsertRoutine(Routine(name = "Push", gymId = gym))
        val other = routines.upsertRoutine(Routine(name = "Other", gymId = gym))
        routines.insertRoutineExercise(RoutineExercise(legs, squat, 0))
        routines.insertRoutineExercise(RoutineExercise(push, bench, 0))
        routines.insertRoutineExercise(RoutineExercise(other, bench, 0))

        // Bench: picked Legs (new), unpicked Push; Other wasn't offered by the form
        routines.setRoutinesForExercise(bench, routineIds = setOf(legs), scope = setOf(legs, push))

        val links = routines.getAllRoutineExercisesSync().map { Triple(it.routineId, it.exerciseId, it.position) }.toSet()
        assertEquals(
            setOf(Triple(legs, squat, 0), Triple(legs, bench, 1), Triple(other, bench, 0)),
            links
        )
    }
}
