package com.offlinelabs.nutcracker.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.offlinelabs.nutcracker.data.model.Gym
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {
    /**
     * Insert or update; returns the row id (-1 on update, so callers should use gym.id then).
     * Uses @Upsert, not REPLACE: REPLACE would delete the row and SET_NULL the exercises' gymId.
     */
    @Upsert
    suspend fun upsertGym(gym: Gym): Long

    @Query("SELECT * FROM gyms WHERE isVisible = 1 ORDER BY name COLLATE NOCASE ASC")
    fun getVisibleGyms(): Flow<List<Gym>>

    /** Includes hidden gyms. */
    @Query("SELECT * FROM gyms WHERE id = :id")
    suspend fun getGymById(id: Long): Gym?

    /** One-shot list of every gym including hidden ones (used by export). */
    @Query("SELECT * FROM gyms ORDER BY id ASC")
    suspend fun getAllGymsIncludingHiddenSync(): List<Gym>

    @Query("UPDATE gyms SET isVisible = 0 WHERE id = :id")
    suspend fun hideGym(id: Long)

    @Query("UPDATE exercises SET gymId = NULL WHERE gymId = :gymId")
    suspend fun makeExercisesShared(gymId: Long)

    @Query("UPDATE exercises SET isVisible = 0 WHERE gymId = :gymId")
    suspend fun hideExercisesOfGym(gymId: Long)

    /**
     * Soft-deletes the gym. If [removeExercises], its exercises are hidden (gymId kept, logs untouched);
     * otherwise they become shared.
     */
    @Transaction
    suspend fun deleteGym(gymId: Long, removeExercises: Boolean) {
        if (removeExercises) hideExercisesOfGym(gymId) else makeExercisesShared(gymId)
        hideGym(gymId)
    }

    @Query("DELETE FROM gyms")
    suspend fun deleteAllGyms()
}
