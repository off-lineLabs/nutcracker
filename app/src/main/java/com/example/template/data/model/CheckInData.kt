package com.example.template.data.model

/**
 * Sealed class hierarchy for type-safe check-in data.
 * This eliminates the need for unsafe casting in the UnifiedCheckInDialog.
 */
sealed class CheckInData {
    /**
     * Represents a meal check-in with the associated MealCheckIn data.
     */
    data class Meal(val mealCheckIn: MealCheckIn) : CheckInData()
    
    /**
     * Represents an exercise check-in with the associated ExerciseLog data.
     */
    data class Exercise(val exerciseLog: ExerciseLog) : CheckInData()
}
