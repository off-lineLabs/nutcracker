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

        /**
         * How many of a meal's servings an ingredient amount represents.
         *
         * Example: meal serving = 100 g, ingredient = 0.5 kg -> 5.0.
         *
         * - Same unit: plain ratio.
         * - Ingredient in [SERVINGS]/[PORTIONS]: the quantity is the number of the meal's servings.
         * - Both weight units, or both volume units: converted with standard (US customary) factors.
         * - Anything else (e.g. g <-> ml needs a density, pieces/slices have no size): the quantity is
         *   assumed to already be in the meal's serving unit, which was the previous behaviour.
         *
         * Returns 0.0 for a non-positive serving size instead of dividing by zero.
         */
        fun servingMultiplier(
            quantity: Double,
            unit: ServingSizeUnit,
            servingValue: Double,
            servingUnit: ServingSizeUnit
        ): Double {
            if (servingValue <= 0.0) return 0.0
            if (unit == servingUnit) return quantity / servingValue
            if (unit == SERVINGS || unit == PORTIONS) return quantity

            val from = unit.baseAmount()
            val to = servingUnit.baseAmount()
            if (from != null && to != null && from.first == to.first) {
                return (quantity * from.second) / (servingValue * to.second)
            }
            return quantity / servingValue
        }

        private enum class Dimension { MASS, VOLUME }

        /** Size of one unit in its dimension's base unit (grams or millilitres), or null if not convertible. */
        private fun ServingSizeUnit.baseAmount(): Pair<Dimension, Double>? = when (this) {
            GRAMS -> Dimension.MASS to 1.0
            KILOGRAMS -> Dimension.MASS to 1000.0
            POUNDS -> Dimension.MASS to 453.59237
            OUNCES -> Dimension.MASS to 28.349523125
            MILLILITERS -> Dimension.VOLUME to 1.0
            LITERS -> Dimension.VOLUME to 1000.0
            CUPS -> Dimension.VOLUME to 236.5882365
            FLUID_OUNCES -> Dimension.VOLUME to 29.5735295625
            TABLESPOONS -> Dimension.VOLUME to 14.78676478125
            TEASPOONS -> Dimension.VOLUME to 4.92892159375
            PIECES, SLICES, UNITS, SERVINGS, PORTIONS -> null
        }
    }
}
