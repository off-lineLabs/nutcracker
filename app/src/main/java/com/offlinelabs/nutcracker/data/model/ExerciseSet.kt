package com.offlinelabs.nutcracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One done set of an exercise check-in ([ExerciseLog]). Every log has its sets: logs made in one
 * go (check-in dialog, a row's box) get identical ones; a row's expanded set list adds them one by
 * one, each with its own weight and reps. The log keeps a summary (count, heaviest weight, last
 * reps, kcal) so existing totals, max weight and analytics keep working.
 */
@Entity(
    tableName = "exercise_sets",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseLog::class,
            parentColumns = ["id"],
            childColumns = ["logId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("logId")]
)
data class ExerciseSet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val logId: Long,
    /** Position in the session (0 = first set). */
    val setIndex: Int,
    val weight: Double,
    val reps: Int,
    /**
     * When the set was ticked ("yyyy-MM-dd HH:mm:ss"). Logs made in one go use the log's time.
     * Kept for a future rest timer (the gap between sets).
     */
    val completedAt: String? = null
)

/** A check-in with its sets (what undo puts back). */
data class LoggedSession(val log: ExerciseLog, val sets: List<ExerciseSet>)

/** [log]'s sets as identical copies of its weight and reps (at least one), done at its time. */
fun uniformSets(log: ExerciseLog, logId: Long = log.id): List<ExerciseSet> =
    (0 until log.sets.coerceAtLeast(1)).map {
        ExerciseSet(logId = logId, setIndex = it, weight = log.weight, reps = log.reps, completedAt = log.logDateTime)
    }

/**
 * [log] summarised from the sets of a strength exercise: the number of sets, the heaviest weight,
 * the reps of the last set and kcal per set ([kcalPerSet]).
 */
fun summarizeStrengthSets(log: ExerciseLog, sets: List<ExerciseSet>, kcalPerSet: Double): ExerciseLog {
    if (sets.isEmpty()) return log
    return log.copy(
        weight = sets.maxOf { it.weight },
        reps = sets.maxBy { it.setIndex }.reps,
        sets = sets.size,
        caloriesBurned = sets.size * kcalPerSet
    )
}

/** True when every set has the same weight and reps (shown as "3 sets · 8 reps · 70 kg"). */
fun setsAreUniform(sets: List<ExerciseSet>): Boolean =
    sets.map { it.weight to it.reps }.distinct().size <= 1
