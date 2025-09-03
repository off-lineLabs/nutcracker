package com.example.template.data.model

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
            onDelete = ForeignKey.CASCADE
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
    val notes: String? = null
) {
    companion object {
        fun create(
            exerciseId: Long,
            weight: Double,
            reps: Int,
            sets: Int,
            caloriesBurned: Double,
            notes: String? = null
        ): ExerciseLog {
            val now = Date()
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            
            return ExerciseLog(
                exerciseId = exerciseId,
                logDate = dateFormatter.format(now),
                logDateTime = dateTimeFormatter.format(now),
                weight = weight,
                reps = reps,
                sets = sets,
                caloriesBurned = caloriesBurned,
                notes = notes
            )
        }
    }
}
