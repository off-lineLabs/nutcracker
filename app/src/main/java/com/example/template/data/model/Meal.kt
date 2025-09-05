package com.example.template.data.model // Assuming this is your model package

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "meals")
@TypeConverters(ServingSizeUnitConverter::class)
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
    val servingSize_value: Double, // Changed to Double for more precision
    val servingSize_unit: ServingSizeUnit, // Now using enum instead of String
    val notes: String? = null // Optional notes
)

/**
 * Type converter for ServingSizeUnit enum to store in Room database
 */
class ServingSizeUnitConverter {
    @TypeConverter
    fun fromServingSizeUnit(unit: ServingSizeUnit): String {
        return unit.abbreviation
    }
    
    @TypeConverter
    fun toServingSizeUnit(abbreviation: String): ServingSizeUnit {
        return ServingSizeUnit.fromAbbreviation(abbreviation) ?: ServingSizeUnit.getDefault()
    }
}