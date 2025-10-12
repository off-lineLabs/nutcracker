package com.offlinelabs.nutcracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.offlinelabs.nutcracker.data.model.ExerciseTag
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseTagDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseTag(exerciseTag: ExerciseTag): Long
    
    @Delete
    suspend fun deleteExerciseTag(exerciseTag: ExerciseTag)
    
    @Query("SELECT * FROM exercise_tags WHERE exerciseId = :exerciseId")
    fun getTagsForExercise(exerciseId: Long): Flow<List<ExerciseTag>>
    
    @Query("SELECT * FROM exercise_tags WHERE tagId = :tagId")
    fun getExercisesForTag(tagId: Long): Flow<List<ExerciseTag>>
    
    @Query("DELETE FROM exercise_tags WHERE exerciseId = :exerciseId")
    suspend fun deleteTagsForExercise(exerciseId: Long)
    
    @Query("DELETE FROM exercise_tags WHERE tagId = :tagId")
    suspend fun deleteExercisesForTag(tagId: Long)
    
    @Query("DELETE FROM exercise_tags")
    suspend fun deleteAllExerciseTags()
    
    @Query("SELECT * FROM exercise_tags")
    fun getAllExerciseTags(): Flow<List<ExerciseTag>>
}
