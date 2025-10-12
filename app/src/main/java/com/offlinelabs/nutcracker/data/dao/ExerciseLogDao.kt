package com.offlinelabs.nutcracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateExerciseLog(exerciseLog: ExerciseLog)

    @Delete
    suspend fun deleteExerciseLog(exerciseLog: ExerciseLog)

    @Query("SELECT * FROM exercise_logs WHERE logDate = :date ORDER BY logDateTime DESC")
    fun getLogsByDate(date: String): Flow<List<ExerciseLog>>

    @Query("SELECT * FROM exercise_logs ORDER BY logDateTime DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 10): Flow<List<ExerciseLog>>

    @Query("DELETE FROM exercise_logs WHERE logDate = :date")
    suspend fun deleteLogsByDate(date: String)

    @Query("DELETE FROM exercise_logs")
    suspend fun deleteAllLogs()
    
    @Query("SELECT * FROM exercise_logs ORDER BY logDateTime DESC")
    fun getAllExerciseLogs(): Flow<List<ExerciseLog>>

    // Get the last log for a specific exercise to auto-fill values
    @Query("""
        SELECT * FROM exercise_logs 
        WHERE exerciseId = :exerciseId 
        ORDER BY logDateTime DESC 
        LIMIT 1
    """)
    fun getLastLogForExercise(exerciseId: Long): Flow<ExerciseLog?>

    // Get maximum weight recorded for a specific exercise
    @Query("""
        SELECT MAX(weight) FROM exercise_logs 
        WHERE exerciseId = :exerciseId
    """)
    fun getMaxWeightForExercise(exerciseId: Long): Flow<Double?>

    // Complex query to get daily exercise summary with exercise details
    @Transaction
    @Query("""
        SELECT 
            el.id as logId,
            el.exerciseId,
            el.logDate,
            el.logDateTime,
            el.weight,
            el.reps,
            el.sets,
            el.caloriesBurned,
            el.notes,
            e.name as exerciseName,
            e.category as exerciseType
        FROM exercise_logs el
        INNER JOIN exercises e ON el.exerciseId = e.id
        WHERE el.logDate = :date
        ORDER BY el.logDateTime DESC
    """)
    fun getDailyExerciseSummary(date: String): Flow<List<DailyExerciseEntry>>

    // Query to get total calories burned from exercises for a specific date
    @Query("""
        SELECT COALESCE(SUM(caloriesBurned), 0.0) as totalCaloriesBurned
        FROM exercise_logs 
        WHERE logDate = :date
    """)
    fun getDailyExerciseCalories(date: String): Flow<Double>

    // Analytics queries for exercise analytics screen
    @Query("""
        SELECT COALESCE(SUM(caloriesBurned), 0.0) as totalCaloriesBurned
        FROM exercise_logs 
        WHERE logDate >= :startDate AND logDate <= :endDate
    """)
    fun getCaloriesBurnedInDateRange(startDate: String, endDate: String): Flow<Double>

    @Query("""
        SELECT COUNT(DISTINCT logDate) as exerciseDays
        FROM exercise_logs 
        WHERE logDate >= :startDate AND logDate <= :endDate
    """)
    fun getExerciseDaysInDateRange(startDate: String, endDate: String): Flow<Int>

    @Query("""
        SELECT MAX(logDate) as lastExerciseDate
        FROM exercise_logs
    """)
    fun getLastExerciseDate(): Flow<String?>

    @Query("""
        SELECT DISTINCT logDate
        FROM exercise_logs 
        WHERE logDate >= :startDate AND logDate <= :endDate
        ORDER BY logDate
    """)
    fun getExerciseDatesInRange(startDate: String, endDate: String): Flow<List<String>>

    // Get primary muscles from the last exercise session
    @Query("""
        SELECT e.primaryMuscles
        FROM exercise_logs el
        INNER JOIN exercises e ON el.exerciseId = e.id
        WHERE el.logDate = (
            SELECT MAX(logDate) 
            FROM exercise_logs
        )
        ORDER BY el.logDateTime DESC
    """)
    fun getPrimaryMusclesFromLastExercise(): Flow<List<String>>
}

// Data class for the complex query result
data class DailyExerciseEntry(
    val logId: Long,
    val exerciseId: Long,
    val logDate: String,
    val logDateTime: String,
    val weight: Double,
    val reps: Int,
    val sets: Int,
    val caloriesBurned: Double,
    val notes: String?,
    val exerciseName: String,
    val exerciseType: String
)
