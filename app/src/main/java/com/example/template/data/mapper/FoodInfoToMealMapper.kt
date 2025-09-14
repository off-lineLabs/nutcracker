package com.example.template.data.mapper

import com.example.template.data.model.*
import android.util.Log

object FoodInfoToMealMapper {
    
    /**
     * Converts FoodInfo to Meal entity
     * @param foodInfo The FoodInfo from Open Food Facts
     * @param servingSizeValue The serving size value (e.g., 100.0 for 100g)
     * @param servingSizeUnit The serving size unit (e.g., GRAMS)
     * @param barcode The barcode if scanned (optional)
     * @param source The source of the food ("search" or "barcode")
     * @return Meal entity ready for database insertion
     */
    fun mapToMeal(
        foodInfo: FoodInfo,
        servingSizeValue: Double = 100.0,
        servingSizeUnit: ServingSizeUnit = ServingSizeUnit.GRAMS,
        barcode: String? = null,
        source: String = "search"
    ): Meal {
        val nutrition = foodInfo.nutrition
        
        // Calculate the multiplier to scale nutrition values from 100g/ml to the actual serving size
        // Open Food Facts nutrition values are always per 100g/ml
        val multiplier = servingSizeValue / 100.0
        
        return Meal(
            name = foodInfo.name,
            brand = foodInfo.brand,
            calories = ((nutrition.calories ?: 0.0) * multiplier).toInt(),
            carbohydrates_g = (nutrition.carbohydrates ?: 0.0) * multiplier,
            protein_g = (nutrition.proteins ?: 0.0) * multiplier,
            fat_g = (nutrition.fat ?: 0.0) * multiplier,
            fiber_g = (nutrition.fiber ?: 0.0) * multiplier,
            sodium_mg = (nutrition.sodium ?: 0.0) * multiplier,
            servingSize_value = servingSizeValue,
            servingSize_unit = servingSizeUnit,
            notes = null,
            
            // Additional nutrition fields - scale these too
            saturatedFat_g = nutrition.saturatedFat?.let { it * multiplier },
            sugars_g = nutrition.sugars?.let { it * multiplier },
            cholesterol_mg = nutrition.cholesterol?.let { it * multiplier },
            vitaminC_mg = nutrition.vitaminC?.let { it * multiplier },
            calcium_mg = nutrition.calcium?.let { it * multiplier },
            iron_mg = nutrition.iron?.let { it * multiplier },
            
            // Open Food Facts specific fields
            imageUrl = foodInfo.imageUrl,
            localImagePath = null, // Will be set after image download
            novaClassification = foodInfo.novaClassification,
            greenScore = foodInfo.greenScore,
            nutriscore = foodInfo.nutriscore,
            ingredients = foodInfo.ingredients,
            categories = foodInfo.categories,
            quantity = foodInfo.quantity,
            servingSize = foodInfo.servingSize,
            barcode = barcode,
            source = source
        )
    }
    
    /**
     * Parses serving size from Open Food Facts serving size string
     * @param servingSizeString The serving size string (e.g., "1 cup (240ml)")
     * @return Pair of (value, unit) or null if parsing fails
     */
    fun parseServingSize(servingSizeString: String?): Pair<Double, ServingSizeUnit>? {
        if (servingSizeString.isNullOrBlank()) {
            return null
        }
        
        return try {
            // Common patterns to match:
            // "100g", "1 cup (240ml)", "1 slice (30g)", "1 serving (150g)"
            val patterns = listOf(
                // Weight patterns
                Regex("""(\d+(?:\.\d+)?)\s*g""", RegexOption.IGNORE_CASE) to ServingSizeUnit.GRAMS,
                Regex("""(\d+(?:\.\d+)?)\s*kg""", RegexOption.IGNORE_CASE) to ServingSizeUnit.KILOGRAMS,
                Regex("""(\d+(?:\.\d+)?)\s*oz""", RegexOption.IGNORE_CASE) to ServingSizeUnit.OUNCES,
                Regex("""(\d+(?:\.\d+)?)\s*lb""", RegexOption.IGNORE_CASE) to ServingSizeUnit.POUNDS,
                
                // Volume patterns
                Regex("""(\d+(?:\.\d+)?)\s*ml""", RegexOption.IGNORE_CASE) to ServingSizeUnit.MILLILITERS,
                Regex("""(\d+(?:\.\d+)?)\s*l""", RegexOption.IGNORE_CASE) to ServingSizeUnit.LITERS,
                Regex("""(\d+(?:\.\d+)?)\s*fl\s*oz""", RegexOption.IGNORE_CASE) to ServingSizeUnit.FLUID_OUNCES,
                Regex("""(\d+(?:\.\d+)?)\s*cup""", RegexOption.IGNORE_CASE) to ServingSizeUnit.CUPS,
                Regex("""(\d+(?:\.\d+)?)\s*tbsp""", RegexOption.IGNORE_CASE) to ServingSizeUnit.TABLESPOONS,
                Regex("""(\d+(?:\.\d+)?)\s*tsp""", RegexOption.IGNORE_CASE) to ServingSizeUnit.TEASPOONS,
                
                // Count patterns
                Regex("""(\d+(?:\.\d+)?)\s*slice""", RegexOption.IGNORE_CASE) to ServingSizeUnit.SLICES,
                Regex("""(\d+(?:\.\d+)?)\s*piece""", RegexOption.IGNORE_CASE) to ServingSizeUnit.PIECES,
                Regex("""(\d+(?:\.\d+)?)\s*serving""", RegexOption.IGNORE_CASE) to ServingSizeUnit.SERVINGS,
                Regex("""(\d+(?:\.\d+)?)\s*portion""", RegexOption.IGNORE_CASE) to ServingSizeUnit.PORTIONS
            )
            
            for ((pattern, unit) in patterns) {
                val match = pattern.find(servingSizeString)
                if (match != null) {
                    val value = match.groupValues[1].toDouble()
                    return Pair(value, unit)
                }
            }
            
            // If no pattern matches, try to extract just a number
            val numberPattern = Regex("""(\d+(?:\.\d+)?)""")
            val numberMatch = numberPattern.find(servingSizeString)
            if (numberMatch != null) {
                val value = numberMatch.groupValues[1].toDouble()
                return Pair(value, ServingSizeUnit.UNITS)
            }
            
            null
        } catch (e: Exception) {
            Log.e("FoodInfoToMealMapper", "Error parsing serving size: $servingSizeString", e)
            null
        }
    }
    
    /**
     * Gets a default serving size suggestion.
     * Always returns 100g as the standard nutrition label reference.
     * Users can easily adjust this using the ServingSizeDialog.
     */
    fun getSuggestedServingSize(foodInfo: FoodInfo): Pair<Double, ServingSizeUnit> {
        // Always default to 100g - the standard nutrition label reference
        // This is simple, predictable, and users can easily adjust it
        return Pair(100.0, ServingSizeUnit.GRAMS)
    }
}
