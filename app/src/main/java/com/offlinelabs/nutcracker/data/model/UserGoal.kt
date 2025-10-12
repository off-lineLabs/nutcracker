package com.offlinelabs.nutcracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_goals")
data class UserGoal(
    @PrimaryKey val id: Int = 1, // Fixed ID for a single user's goal set
    val caloriesGoal: Int,
    val carbsGoal_g: Int,
    val proteinGoal_g: Int,
    val fatGoal_g: Int,
    val fiberGoal_g: Int, // in grams
    val sodiumGoal_mg: Int  // in milligrams
) {
    // Companion object with default values if no goal is set yet
    companion object {
        fun default(): UserGoal = UserGoal(
            caloriesGoal = 2000,
            carbsGoal_g = 250,
            proteinGoal_g = 100,
            fatGoal_g = 65,
            fiberGoal_g = 30,
            sodiumGoal_mg = 2300
        )
    }
}
