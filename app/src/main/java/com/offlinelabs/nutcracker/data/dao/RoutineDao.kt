package com.offlinelabs.nutcracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.offlinelabs.nutcracker.data.model.Routine
import com.offlinelabs.nutcracker.data.model.RoutineExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    /** Insert or update; returns the row id (-1 on update). @Upsert keeps the memberships (REPLACE would cascade-delete them). */
    @Upsert
    suspend fun upsertRoutine(routine: Routine): Long

    @Query("SELECT * FROM routines WHERE isVisible = 1 ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun getVisibleRoutines(): Flow<List<Routine>>

    /** Memberships of visible routines. */
    @Query("""
        SELECT re.* FROM routine_exercises re
        INNER JOIN routines r ON r.id = re.routineId
        WHERE r.isVisible = 1
        ORDER BY re.routineId, re.position
    """)
    fun getVisibleRoutineExercises(): Flow<List<RoutineExercise>>

    /** Ignores a membership that already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRoutineExercise(routineExercise: RoutineExercise): Long

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId AND exerciseId = :exerciseId")
    suspend fun deleteRoutineExercise(routineId: Long, exerciseId: Long)

    /** One-shot lists including hidden routines (used by export and import). */
    @Query("SELECT * FROM routines ORDER BY id ASC")
    suspend fun getAllRoutinesIncludingHiddenSync(): List<Routine>

    @Query("SELECT * FROM routine_exercises ORDER BY routineId, position")
    suspend fun getAllRoutineExercisesSync(): List<RoutineExercise>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutineById(id: Long): Routine?

    @Query("DELETE FROM routine_exercises")
    suspend fun deleteAllRoutineExercises()

    @Query("DELETE FROM routines")
    suspend fun deleteAllRoutines()
}
