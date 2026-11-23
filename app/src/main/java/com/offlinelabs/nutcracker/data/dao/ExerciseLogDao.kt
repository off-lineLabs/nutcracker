package com.offlinelabs.nutcracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import com.offlinelabs.nutcracker.data.model.ExerciseSet
import com.offlinelabs.nutcracker.data.model.LoggedSession
import com.offlinelabs.nutcracker.data.model.summarizeStrengthSets
import com.offlinelabs.nutcracker.data.model.uniformSets
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long

    /** A real UPDATE: REPLACE would delete the row first and cascade-delete its sets. */
    @Update
    suspend fun updateExerciseLog(exerciseLog: ExerciseLog)

    @Query("SELECT * FROM exercise_logs WHERE id = :id")
    suspend fun getLogById(id: Long): ExerciseLog?

    @Query("DELETE FROM exercise_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    // ----- Sets -----

    @Insert
    suspend fun insertSets(sets: List<ExerciseSet>)

    @Query("SELECT * FROM exercise_sets WHERE logId = :logId ORDER BY setIndex")
    suspend fun getSetsForLog(logId: Long): List<ExerciseSet>

    @Query("SELECT * FROM exercise_sets WHERE logId IN (:logIds) ORDER BY logId, setIndex")
    suspend fun getSetsForLogs(logIds: List<Long>): List<ExerciseSet>

    /** Sets of the logs of [date] (the Exercise tab's set lists and Home). */
    @Query("""
        SELECT s.* FROM exercise_sets s INNER JOIN exercise_logs l ON s.logId = l.id
        WHERE l.logDate = :date ORDER BY s.logId, s.setIndex
    """)
    fun getSetsForDate(date: String): Flow<List<ExerciseSet>>

    @Query("SELECT * FROM exercise_sets ORDER BY id ASC")
    suspend fun getAllSetsSync(): List<ExerciseSet>

    @Query("DELETE FROM exercise_sets WHERE logId = :logId AND setIndex = :setIndex")
    suspend fun deleteSet(logId: Long, setIndex: Int)

    @Query("DELETE FROM exercise_sets WHERE logId = :logId")
    suspend fun deleteSetsOfLog(logId: Long)

    @Query("SELECT l.* FROM exercise_logs l WHERE NOT EXISTS (SELECT 1 FROM exercise_sets s WHERE s.logId = l.id)")
    suspend fun getLogsWithoutSets(): List<ExerciseLog>

    /** Inserts [log] with [sets] (their logId is set to the new log's); returns the log id. */
    @Transaction
    suspend fun insertLogWithSets(log: ExerciseLog, sets: List<ExerciseSet>): Long {
        val id = insertExerciseLog(log)
        insertSets(sets.map { it.copy(id = 0, logId = id) })
        return id
    }

    /** Inserts [log] with identical sets of its weight and reps (a check-in made in one go). */
    @Transaction
    suspend fun insertLogWithUniformSets(log: ExerciseLog): Long = insertLogWithSets(log, uniformSets(log))

    /**
     * Updates [log]. When its weight, reps or set count changed (the check-in dialog edits them
     * for all sets) its sets become identical copies; otherwise (time, notes) they're kept.
     */
    @Transaction
    suspend fun updateLogKeepingSets(log: ExerciseLog) {
        val old = getLogById(log.id)
        updateExerciseLog(log)
        if (old == null || old.weight != log.weight || old.reps != log.reps || old.sets != log.sets) {
            deleteSetsOfLog(log.id)
            insertSets(uniformSets(log))
        }
    }

    /** Gives every log without sets identical ones (after importing a backup made before sets). */
    @Transaction
    suspend fun backfillMissingSets() {
        getLogsWithoutSets().forEach { insertSets(uniformSets(it)) }
    }

    /**
     * Ticks [sets] of a strength exercise: added to the session log [sessionLogId] (replacing sets
     * with the same index), or to a new log made from [newLog] when there's none. The log's summary
     * follows its sets. Returns the log id.
     */
    @Transaction
    suspend fun checkInSets(sessionLogId: Long?, newLog: ExerciseLog, sets: List<ExerciseSet>, kcalPerSet: Double): Long {
        val logId = sessionLogId?.takeIf { getLogById(it) != null } ?: insertExerciseLog(newLog)
        sets.forEach { deleteSet(logId, it.setIndex) }
        insertSets(sets.map { it.copy(id = 0, logId = logId) })
        refreshSummary(logId, kcalPerSet)
        return logId
    }

    /** Unticks set [setIndex] of log [logId]; the log goes when it has no set left. */
    @Transaction
    suspend fun uncheckSet(logId: Long, setIndex: Int, kcalPerSet: Double) {
        deleteSet(logId, setIndex)
        if (getSetsForLog(logId).isEmpty()) deleteLogById(logId) else refreshSummary(logId, kcalPerSet)
    }

    /** Recomputes log [logId]'s summary (count, heaviest weight, last reps, kcal) from its sets. */
    @Transaction
    suspend fun refreshSummary(logId: Long, kcalPerSet: Double) {
        val log = getLogById(logId) ?: return
        updateExerciseLog(summarizeStrengthSets(log, getSetsForLog(logId), kcalPerSet))
    }

    @Delete
    suspend fun deleteExerciseLog(exerciseLog: ExerciseLog)

    @Query("SELECT * FROM exercise_logs WHERE logDate = :date ORDER BY logDateTime DESC")
    fun getLogsByDate(date: String): Flow<List<ExerciseLog>>

    @Query("SELECT * FROM exercise_logs ORDER BY logDateTime DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 10): Flow<List<ExerciseLog>>

    @Query("DELETE FROM exercise_logs WHERE logDate = :date")
    suspend fun deleteLogsByDate(date: String)

    @Query("SELECT * FROM exercise_logs WHERE exerciseId = :exerciseId AND logDate = :date")
    suspend fun getLogsForExerciseOnDate(exerciseId: Long, date: String): List<ExerciseLog>

    @Query("DELETE FROM exercise_logs WHERE exerciseId = :exerciseId AND logDate = :date")
    suspend fun deleteLogsForExerciseOnDate(exerciseId: Long, date: String)

    // A log without a routine counts for every routine group, so it goes too
    @Query("SELECT * FROM exercise_logs WHERE exerciseId = :exerciseId AND logDate = :date AND (routineId = :routineId OR routineId IS NULL)")
    suspend fun getLogsForExerciseInRoutineOnDate(exerciseId: Long, date: String, routineId: Long): List<ExerciseLog>

    @Query("DELETE FROM exercise_logs WHERE exerciseId = :exerciseId AND logDate = :date AND (routineId = :routineId OR routineId IS NULL)")
    suspend fun deleteLogsForExerciseInRoutineOnDate(exerciseId: Long, date: String, routineId: Long)

    /**
     * Removes [exerciseId]'s logs of [date] with their sets and returns them (for undo). With
     * [routineId]: those checked in under that routine plus those without a routine (they show in
     * every routine group); otherwise all of them.
     */
    @Transaction
    suspend fun removeLogsForExerciseOnDate(exerciseId: Long, date: String, routineId: Long?): List<LoggedSession> {
        val logs = if (routineId == null) {
            getLogsForExerciseOnDate(exerciseId, date)
        } else {
            getLogsForExerciseInRoutineOnDate(exerciseId, date, routineId)
        }
        val setsByLog = getSetsForLogs(logs.map { it.id }).groupBy { it.logId }
        if (routineId == null) {
            deleteLogsForExerciseOnDate(exerciseId, date)
        } else {
            deleteLogsForExerciseInRoutineOnDate(exerciseId, date, routineId)
        }
        return logs.map { LoggedSession(it, setsByLog[it.id].orEmpty()) }
    }

    /** Reps of each exercise's most recent log (where the row's reps wheel starts). */
    @Query("""
        SELECT el.exerciseId AS exerciseId, el.reps AS reps FROM exercise_logs el
        WHERE el.id = (SELECT id FROM exercise_logs WHERE exerciseId = el.exerciseId ORDER BY logDateTime DESC, id DESC LIMIT 1)
    """)
    fun getLastReps(): Flow<Map<@MapColumn(columnName = "exerciseId") Long, @MapColumn(columnName = "reps") Int>>

    @Query("DELETE FROM exercise_logs")
    suspend fun deleteAllLogs()

    @Query("SELECT * FROM exercise_logs ORDER BY id ASC")
    suspend fun getAllExerciseLogsSync(): List<ExerciseLog>
    
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

    /** Heaviest logged weight per exercise, in one query for the whole list (exercises without logs are absent). */
    @Query("SELECT exerciseId, MAX(weight) AS maxWeight FROM exercise_logs GROUP BY exerciseId")
    fun getMaxWeights(): Flow<Map<@MapColumn(columnName = "exerciseId") Long, @MapColumn(columnName = "maxWeight") Double>>

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
            el.routineId,
            e.name as exerciseName,
            e.category as exerciseType,
            g.name as gymName,
            g.unit as gymUnit
        FROM exercise_logs el
        INNER JOIN exercises e ON el.exerciseId = e.id
        LEFT JOIN gyms g ON e.gymId = g.id
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

    // Per-date burned kcal for a range (same SUM as getDailyExerciseCalories), one row per date with logs
    @Query("""
        SELECT logDate as logDate, COALESCE(SUM(caloriesBurned), 0.0) as caloriesBurned
        FROM exercise_logs
        WHERE logDate >= :startDate AND logDate <= :endDate
        GROUP BY logDate
        ORDER BY logDate ASC
    """)
    fun getDailyExerciseCaloriesInRange(startDate: String, endDate: String): Flow<List<DailyExerciseCaloriesRow>>

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

/** One row of [ExerciseLogDao.getDailyExerciseCaloriesInRange]. */
data class DailyExerciseCaloriesRow(
    val logDate: String,
    val caloriesBurned: Double
)

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
    val exerciseType: String,
    val gymName: String? = null, // Name of the exercise's gym (hidden gyms included); null when shared
    val gymUnit: String? = null,
    val routineId: Long? = null // Routine it was checked in under, if any
)
