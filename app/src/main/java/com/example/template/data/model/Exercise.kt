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
    // Add other relevant fields like default reps, default weight, notes, type (cardio/strength) if needed
)
