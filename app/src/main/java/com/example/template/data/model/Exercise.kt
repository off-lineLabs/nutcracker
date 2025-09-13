package com.example.template.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "exercises")
@TypeConverters(ExerciseTypeConverters::class)
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val kcalBurnedPerRep: Double? = null, // Kcal per set (for strength exercises) or per rep (for bodyweight exercises)
    val kcalBurnedPerMinute: Double? = null, // Kcal per minute (for time-based exercises)
    val defaultWeight: Double = 0.0, // Default weight in kg
    val defaultReps: Int = 0, // Default number of reps
    val defaultSets: Int = 0, // Default number of sets
    val category: String = "strength", // Database category: powerlifting, strength, stretching, cardio, olympic weightlifting, strongman, plyometrics
    val equipment: String? = null, // Equipment used: medicine ball, dumbbell, body only, bands, kettlebells, foam roll, cable, machine, barbell, exercise ball, e-z curl bar, other
    val primaryMuscles: List<String> = emptyList(), // Primary muscles worked
    val secondaryMuscles: List<String> = emptyList(), // Secondary muscles worked
    val force: String? = null, // Force type: static, pull, push
    val level: String? = null, // Exercise level: beginner, intermediate, expert
    val mechanic: String? = null, // Exercise mechanic: compound, isolation
    val instructions: List<String> = emptyList(), // Exercise instructions
    val notes: String? = null, // Optional notes
    val imagePaths: List<String> = emptyList(), // Local paths to exercise images
    val isVisible: Boolean = true // Soft delete field - true means visible in My Exercises
)

// UI Exercise Types for user interface (mapped from database categories)
enum class ExerciseType {
    STRENGTH, // Weight training
    CARDIO,   // Running, cycling, etc.
    BODYWEIGHT // Push-ups, pull-ups, etc.
}

// Type converters for Room database
class ExerciseTypeConverters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }
}

// Utility functions for category mapping
object ExerciseCategoryMapper {
    // Map database category to UI exercise type
    fun getExerciseType(category: String): ExerciseType {
        return when (category.lowercase()) {
            "powerlifting", "olympic weightlifting", "strongman", "strength" -> ExerciseType.STRENGTH
            "cardio", "plyometrics" -> ExerciseType.CARDIO
            "stretching" -> ExerciseType.BODYWEIGHT
            else -> ExerciseType.STRENGTH // Default fallback
        }
    }

    // Map UI exercise type to database category
    fun getCategory(exerciseType: ExerciseType): String {
        return when (exerciseType) {
            ExerciseType.STRENGTH -> "strength"
            ExerciseType.CARDIO -> "cardio"
            ExerciseType.BODYWEIGHT -> "stretching"
        }
    }

    // Get all valid database categories
    fun getAllCategories(): List<String> {
        return listOf(
            "powerlifting", "strength", "stretching", "cardio", 
            "olympic weightlifting", "strongman", "plyometrics"
        )
    }

    // Get all valid equipment types
    fun getAllEquipment(): List<String> {
        return listOf(
            "medicine ball", "dumbbell", "body only", "bands", "kettlebells", 
            "foam roll", "cable", "machine", "barbell", "exercise ball", 
            "e-z curl bar", "other"
        )
    }

    // Get all valid muscle groups
    fun getAllMuscles(): List<String> {
        return listOf(
            "abdominals", "abductors", "adductors", "biceps", "calves", "chest", 
            "forearms", "glutes", "hamstrings", "lats", "lower back", "middle back", 
            "neck", "quadriceps", "shoulders", "traps", "triceps"
        )
    }

    // Get all valid force types
    fun getAllForceTypes(): List<String> {
        return listOf("static", "pull", "push")
    }
}
