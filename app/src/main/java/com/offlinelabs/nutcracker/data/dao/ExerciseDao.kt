package com.offlinelabs.nutcracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.offlinelabs.nutcracker.data.model.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    /**
     * Insert or update; returns the new row id, or -1 when an existing row was updated.
     * @Upsert rather than REPLACE: REPLACE deletes the row first, which would cascade-delete its
     * routine memberships.
     */
    @Upsert
    suspend fun upsertExercise(exercise: Exercise): Long

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Query("SELECT * FROM exercises WHERE isVisible = 1 ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    fun getExerciseById(exerciseId: Long): Flow<Exercise?>

    @Query("SELECT * FROM exercises WHERE isVisible = 1 AND id = :exerciseId")
    fun getVisibleExerciseById(exerciseId: Long): Flow<Exercise?> // Get visible exercise by ID

    @Query("UPDATE exercises SET isVisible = 0 WHERE id = :exerciseId")
    suspend fun hideExercise(exerciseId: Long) // Soft delete - hide exercise

    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()

    /** Sort order that puts a new exercise at the end of the custom order. */
    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM exercises")
    suspend fun nextSortOrder(): Int

    @Query("UPDATE exercises SET sortOrder = :sortOrder WHERE id = :exerciseId")
    suspend fun setSortOrder(exerciseId: Long, sortOrder: Int)

    /** Stores the custom order: each id gets its index in [orderedIds]. */
    @Transaction
    suspend fun applySortOrder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> setSortOrder(id, index) }
    }

    /** One-shot lookup, ignores isVisible. */
    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getExerciseByIdSync(exerciseId: Long): Exercise?

    /** Other members of a sync group (hidden ones included, so they stay consistent if restored). */
    @Query("SELECT * FROM exercises WHERE syncGroupId = :groupId AND id != :exceptId")
    suspend fun getSyncPartners(groupId: String, exceptId: Long): List<Exercise>

    @Query("UPDATE exercises SET syncGroupId = :groupId WHERE id = :exerciseId")
    suspend fun setSyncGroup(exerciseId: Long, groupId: String?)

    @Query("SELECT COUNT(*) FROM exercises WHERE syncGroupId = :groupId AND isVisible = 1")
    suspend fun countVisibleInSyncGroup(groupId: String): Int

    @Query("UPDATE exercises SET syncGroupId = NULL WHERE syncGroupId = :groupId")
    suspend fun clearSyncGroup(groupId: String)

    /** Latest log per exercise. */
    @Query("SELECT exerciseId AS id, MAX(logDateTime) AS lastCheckIn FROM exercise_logs GROUP BY exerciseId")
    fun getLastCheckIns(): Flow<List<LastCheckIn>>

    /** One-shot list of every exercise including hidden ones (used by export and import). */
    @Query("SELECT * FROM exercises ORDER BY id ASC")
    suspend fun getAllExercisesIncludingHiddenSync(): List<Exercise>
}

