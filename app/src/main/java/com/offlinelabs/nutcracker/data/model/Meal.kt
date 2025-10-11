package com.offlinelabs.nutcracker.data.model // Assuming this is your model package

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "meals")
@TypeConverters(ServingSizeUnitConverter::class, NovaClassificationConverter::class, GreenScoreConverter::class, NutriscoreConverter::class)
data class Meal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Auto-generated ID, default to 0 for new entries
    val name: String,
    val brand: String? = null, // Brand from Open Food Facts
    val calories: Int,
    val carbohydrates_g: Double,
    val protein_g: Double,
    val fat_g: Double,
    val fiber_g: Double,
    val sodium_mg: Double,
    val servingSize_value: Double, // Changed to Double for more precision
    val servingSize_unit: ServingSizeUnit, // Now using enum instead of String
    val notes: String? = null, // Optional notes
    val isVisible: Boolean = true, // Soft delete field - true means visible in My Meals
    
    // Additional nutrition fields from Open Food Facts
    val saturatedFat_g: Double? = null,
    val sugars_g: Double? = null,
    val cholesterol_mg: Double? = null,
    val vitaminC_mg: Double? = null,
    val calcium_mg: Double? = null,
    val iron_mg: Double? = null,
    
    // Open Food Facts specific fields
    val imageUrl: String? = null, // URL to the food image
    val localImagePath: String? = null, // Local path to downloaded image
    val novaClassification: NovaClassification? = null, // NOVA classification
    val greenScore: GreenScore? = null, // Eco-Score
    val nutriscore: Nutriscore? = null, // Nutri-Score
    val ingredients: String? = null, // Ingredients text
    val quantity: String? = null, // Product quantity
    val servingSize: String? = null, // Original serving size from Open Food Facts
    val barcode: String? = null, // Barcode if scanned
    val source: String = "manual" // Source: "manual", "search", "barcode"
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

/**
 * Type converter for NovaClassification to store in Room database
 */
class NovaClassificationConverter {
    @TypeConverter
    fun fromNovaClassification(classification: NovaClassification?): String? {
        return classification?.let { "${it.group}|${it.description}" }
    }
    
    @TypeConverter
    fun toNovaClassification(value: String?): NovaClassification? {
        return value?.let {
            val parts = it.split("|")
            if (parts.size == 2) {
                NovaClassification(parts[0].toIntOrNull() ?: 0, parts[1])
            } else null
        }
    }
}

/**
 * Type converter for GreenScore to store in Room database
 */
class GreenScoreConverter {
    @TypeConverter
    fun fromGreenScore(score: GreenScore?): String? {
        return score?.let { "${it.grade}|${it.score}" }
    }
    
    @TypeConverter
    fun toGreenScore(value: String?): GreenScore? {
        return value?.let {
            val parts = it.split("|")
            if (parts.size == 2) {
                GreenScore(parts[0], parts[1].toIntOrNull())
            } else null
        }
    }
}

/**
 * Type converter for Nutriscore to store in Room database
 */
class NutriscoreConverter {
    @TypeConverter
    fun fromNutriscore(score: Nutriscore?): String? {
        return score?.let { "${it.grade}|${it.score}" }
    }
    
    @TypeConverter
    fun toNutriscore(value: String?): Nutriscore? {
        return value?.let {
            val parts = it.split("|")
            if (parts.size == 2) {
                Nutriscore(parts[0], parts[1].toIntOrNull())
            } else null
        }
    }
}