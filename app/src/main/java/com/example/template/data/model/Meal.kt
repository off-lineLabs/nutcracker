package com.example.template.data.model // Assuming this is your model package

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Auto-generated ID, default to 0 for new entries
    val name: String,
    val calories: Int,
    val carbohydrates_g: Double,
    val protein_g: Double,
    val fat_g: Double,
    val fiber_g: Double,
    val sodium_mg: Double,
    val servingSize_value: Int,
    val servingSize_unit: String, // e.g., "g", "ml", "unit"
    val notes: String? = null // Optional notes
)