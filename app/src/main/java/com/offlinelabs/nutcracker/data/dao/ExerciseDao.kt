package com.offlinelabs.nutcracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.offlinelabs.nutcracker.data.model.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
}

