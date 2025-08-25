package com.example.template.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "meal_check_ins",
    foreignKeys = [
        ForeignKey(
            entity = Meal::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE // If meal is deleted, check-ins are also deleted
        )
    ],
    indices = [Index("mealId"), Index("checkInDate")] // Index for better query performance
)
data class MealCheckIn(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mealId: Long, // Foreign key to Meal
    val checkInDate: String, // Date in YYYY-MM-DD format for easy filtering
    val checkInDateTime: String, // Full timestamp for display
    val servingSize: Double = 1.0, // Multiplier for the meal's serving size
    val notes: String? = null
) {
    companion object {
        fun create(mealId: Long, servingSize: Double = 1.0, notes: String? = null): MealCheckIn {
            val now = Date()
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            
            return MealCheckIn(
                mealId = mealId,
                checkInDate = dateFormatter.format(now),
                checkInDateTime = dateTimeFormatter.format(now),
                servingSize = servingSize,
                notes = notes
            )
        }
    }
}
