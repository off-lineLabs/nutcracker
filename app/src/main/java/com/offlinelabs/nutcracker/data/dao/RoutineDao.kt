package com.offlinelabs.nutcracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("UPDATE routines SET isVisible = 0 WHERE id = :id")
    suspend fun hideRoutine(id: Long)

    @Query("UPDATE routines SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setRoutineSortOrder(id: Long, sortOrder: Int)

    /** Stores the order of routines: each id gets its index in [routineIds]. */
    @Transaction
    suspend fun applyRoutineOrder(routineIds: List<Long>) {
        routineIds.forEachIndexed { index, id -> setRoutineSortOrder(id, index) }
    }

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM routines")
    suspend fun nextRoutineSortOrder(): Int

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteExercisesOfRoutine(routineId: Long)

    /** Replaces the exercises of [routineId] with [exerciseIds], in that order. */
    @Transaction
    suspend fun setExercisesForRoutine(routineId: Long, exerciseIds: List<Long>) {
        deleteExercisesOfRoutine(routineId)
        exerciseIds.distinct().forEachIndexed { index, id -> insertRoutineExercise(RoutineExercise(routineId, id, index)) }
    }

    @Query("SELECT routineId FROM routine_exercises WHERE exerciseId = :exerciseId")
    suspend fun getRoutineIdsForExercise(exerciseId: Long): List<Long>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM routine_exercises WHERE routineId = :routineId")
    suspend fun nextExercisePosition(routineId: Long): Int

    /**
     * Puts [exerciseId] in exactly [routineIds] among the routines of [scope] (the ones the exercise
     * form offered): it's added at the end of newly chosen routines and removed from unchosen ones.
     * Routines outside [scope] are left alone.
     */
    @Transaction
    suspend fun setRoutinesForExercise(exerciseId: Long, routineIds: Set<Long>, scope: Set<Long>) {
        val current = getRoutineIdsForExercise(exerciseId).toSet()
        (current intersect scope).minus(routineIds).forEach { deleteRoutineExercise(it, exerciseId) }
        (routineIds intersect scope).minus(current).forEach {
            insertRoutineExercise(RoutineExercise(it, exerciseId, nextExercisePosition(it)))
        }
    }

    /** Drops [exerciseId] from routines of gyms other than [gymId] (after it moved to [gymId]). */
    @Query("""
        DELETE FROM routine_exercises WHERE exerciseId = :exerciseId
        AND routineId IN (SELECT id FROM routines WHERE gymId != :gymId)
    """)
    suspend fun removeExerciseFromOtherGyms(exerciseId: Long, gymId: Long)

    @Query("UPDATE routine_exercises SET position = :position WHERE routineId = :routineId AND exerciseId = :exerciseId")
    suspend fun setExercisePosition(routineId: Long, exerciseId: Long, position: Int)

    /** Stores a routine's exercise order: each exercise gets its index in [exerciseIds]. */
    @Transaction
    suspend fun applyExerciseOrder(routineId: Long, exerciseIds: List<Long>) {
        exerciseIds.forEachIndexed { index, id -> setExercisePosition(routineId, id, index) }
    }

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
