package com.offlinelabs.nutcracker.data.model

import com.offlinelabs.nutcracker.R

/**
 * Enum representing different serving size units for nutrition tracking
 */
enum class ServingSizeUnit(
    val stringResourceId: Int,
    val abbreviation: String,
    val isWeight: Boolean = true, // true for weight units, false for volume units
    val isMetric: Boolean = true // true for metric units, false for imperial
) {
    // Weight units
    GRAMS(R.string.unit_grams, "g", true, true),
    KILOGRAMS(R.string.unit_kilograms, "kg", true, true),
    POUNDS(R.string.unit_pounds, "lb", true, false),
    OUNCES(R.string.unit_ounces, "oz", true, false),
    
    // Volume units
    MILLILITERS(R.string.unit_milliliters, "ml", false, true),
    LITERS(R.string.unit_liters, "L", false, true),
    CUPS(R.string.unit_cups, "cup", false, false),
    FLUID_OUNCES(R.string.unit_fluid_ounces, "fl oz", false, false),
    TABLESPOONS(R.string.unit_tablespoons, "tbsp", false, false),
    TEASPOONS(R.string.unit_teaspoons, "tsp", false, false),
    
    // Count units
    PIECES(R.string.unit_pieces, "pcs", false, false),
    SLICES(R.string.unit_slices, "slice", false, false),
    UNITS(R.string.unit_units, "unit", false, false),
    
    // Special units
    SERVINGS(R.string.unit_servings, "serving", false, false),
    PORTIONS(R.string.unit_portions, "portion", false, false);
    
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
