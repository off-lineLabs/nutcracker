package com.example.template.data.model

/**
 * Enum representing different serving size units for nutrition tracking
 */
enum class ServingSizeUnit(
    val displayName: String,
    val abbreviation: String,
    val isWeight: Boolean = true, // true for weight units, false for volume units
    val isMetric: Boolean = true // true for metric units, false for imperial
) {
    // Weight units
    GRAMS("Grams", "g", true, true),
    KILOGRAMS("Kilograms", "kg", true, true),
    POUNDS("Pounds", "lb", true, false),
    OUNCES("Ounces", "oz", true, false),
    
    // Volume units
    MILLILITERS("Milliliters", "ml", false, true),
    LITERS("Liters", "L", false, true),
    CUPS("Cups", "cup", false, false),
    FLUID_OUNCES("Fluid Ounces", "fl oz", false, false),
    TABLESPOONS("Tablespoons", "tbsp", false, false),
    TEASPOONS("Teaspoons", "tsp", false, false),
    
    // Count units
    PIECES("Pieces", "pcs", false, false),
    SLICES("Slices", "slice", false, false),
    UNITS("Units", "unit", false, false),
    
    // Special units
    SERVINGS("Servings", "serving", false, false),
    PORTIONS("Portions", "portion", false, false);
    
    companion object {
        /**
         * Get all weight units
         */
        fun getWeightUnits(): List<ServingSizeUnit> = values().filter { it.isWeight }
        
        /**
         * Get all volume units
         */
        fun getVolumeUnits(): List<ServingSizeUnit> = values().filter { !it.isWeight }
        
        /**
         * Get all metric units
         */
        fun getMetricUnits(): List<ServingSizeUnit> = values().filter { it.isMetric }
        
        /**
         * Get all imperial units
         */
        fun getImperialUnits(): List<ServingSizeUnit> = values().filter { !it.isMetric }
        
        /**
         * Get the most common units for quick selection
         */
        fun getCommonUnits(): List<ServingSizeUnit> = listOf(
            GRAMS, MILLILITERS, CUPS, PIECES, SLICES, SERVINGS
        )
        
        /**
         * Find unit by abbreviation
         */
        fun fromAbbreviation(abbreviation: String): ServingSizeUnit? {
            return values().find { it.abbreviation.equals(abbreviation, ignoreCase = true) }
        }
        
        /**
         * Get default unit (grams)
         */
        fun getDefault(): ServingSizeUnit = GRAMS
    }
}
