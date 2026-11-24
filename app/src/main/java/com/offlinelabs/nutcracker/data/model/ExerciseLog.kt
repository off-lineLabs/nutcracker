package com.offlinelabs.nutcracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "exercise_logs",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT // Prevent deletion of exercises that have logs
        )
    ],
    indices = [Index("exerciseId"), Index("logDate")]
)
data class ExerciseLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseId: Long,
    val logDate: String, // Date in YYYY-MM-DD format
    val logDateTime: String, // Full timestamp
    val weight: Double, // Weight in kg
    val reps: Int,
    val sets: Int,
    val caloriesBurned: Double,
    val notes: String? = null,
    /**
     * The routine the exercise was checked in under (from a routine group of the Exercise tab), or
     * null. A routine group only shows its own check-ins as done. No foreign key: routines are only
     * hidden, never deleted.
     */
    val routineId: Long? = null
) {
    companion object {
        fun create(
            exerciseId: Long,
            weight: Double,
            reps: Int,
            sets: Int,
            caloriesBurned: Double,
            notes: String? = null,
            customDateTime: Date? = null, // Optional custom timestamp
            routineId: Long? = null
        ): ExerciseLog {
            val dateTime = customDateTime ?: Date() // Use custom or current time
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
            
            return ExerciseLog(
                exerciseId = exerciseId,
                logDate = dateFormatter.format(dateTime),
                logDateTime = dateTimeFormatter.format(dateTime),
                weight = weight,
                reps = reps,
                sets = sets,
                caloriesBurned = caloriesBurned,
                notes = notes,
                routineId = routineId
            )
        }
    }
}
