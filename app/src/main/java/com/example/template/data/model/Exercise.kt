package com.example.template.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val kcalBurnedPerRep: Double? = null, // Kcal per single repetition (for rep-based exercises)
    val kcalBurnedPerMinute: Double? = null, // Kcal per minute (for time-based exercises)
    val defaultWeight: Double = 0.0, // Default weight in kg
    val defaultReps: Int = 0, // Default number of reps
    val defaultSets: Int = 0, // Default number of sets
    val exerciseType: ExerciseType = ExerciseType.STRENGTH, // Type of exercise
    val notes: String? = null // Optional notes
)

enum class ExerciseType {
    STRENGTH, // Weight training
    CARDIO,   // Running, cycling, etc.
    BODYWEIGHT // Push-ups, pull-ups, etc.
}
