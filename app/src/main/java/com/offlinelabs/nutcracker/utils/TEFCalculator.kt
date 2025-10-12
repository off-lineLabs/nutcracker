package com.offlinelabs.nutcracker.utils

import com.offlinelabs.nutcracker.data.dao.DailyTotals

/**
 * TEF (Thermic Effect of Food) Calculator
 * 
 * Calculates the thermal effect of food based on macronutrient composition.
 * The body burns additional calories during digestion and absorption of nutrients.
 * 
 * TEF percentages:
 * - Protein: 25% (20-30% range, using practical average)
 * - Carbohydrates: 7.5% (5-10% range, using practical average)  
 * - Fat: 2% (0-3% range, using practical average)
 * 
 * Formula: TEF = (Protein_g × 4 × 0.25) + (Carbs_g × 4 × 0.075) + (Fat_g × 9 × 0.02)
 */
object TEFCalculator {
    
    // TEF percentages for each macronutrient
    private const val PROTEIN_TEF_PERCENTAGE = 0.25  // 25%
    private const val CARBS_TEF_PERCENTAGE = 0.075   // 7.5%
    private const val FAT_TEF_PERCENTAGE = 0.02      // 2%
    
    // Calories per gram for each macronutrient
    private const val PROTEIN_CALORIES_PER_GRAM = 4.0
    private const val CARBS_CALORIES_PER_GRAM = 4.0
    private const val FAT_CALORIES_PER_GRAM = 9.0
    
    /**
     * Calculate TEF bonus calories from macronutrient weights in grams
     * 
     * @param proteinGrams Protein weight in grams
     * @param carbsGrams Carbohydrates weight in grams  
     * @param fatGrams Fat weight in grams
     * @return TEF bonus calories burned during digestion
     */
    fun calculateTEFBonus(
        proteinGrams: Double,
        carbsGrams: Double,
        fatGrams: Double
    ): Double {
        // Convert grams to calories
        val proteinCalories = proteinGrams * PROTEIN_CALORIES_PER_GRAM
        val carbsCalories = carbsGrams * CARBS_CALORIES_PER_GRAM
        val fatCalories = fatGrams * FAT_CALORIES_PER_GRAM
        
        // Apply TEF percentages
        val proteinTEF = proteinCalories * PROTEIN_TEF_PERCENTAGE
        val carbsTEF = carbsCalories * CARBS_TEF_PERCENTAGE
        val fatTEF = fatCalories * FAT_TEF_PERCENTAGE
        
        return proteinTEF + carbsTEF + fatTEF
    }
    
    /**
     * Calculate TEF bonus from DailyTotals object
     * 
     * @param dailyTotals Daily nutrient totals
     * @return TEF bonus calories burned during digestion
     */
    fun calculateTEFBonus(dailyTotals: DailyTotals): Double {
        return calculateTEFBonus(
            proteinGrams = dailyTotals.totalProtein,
            carbsGrams = dailyTotals.totalCarbohydrates,
            fatGrams = dailyTotals.totalFat
        )
    }
    
    /**
     * Calculate net calories after TEF adjustment
     * 
     * @param grossCalories Total calories consumed
     * @param dailyTotals Daily nutrient totals
     * @return Net calories available after TEF burn
     */
    fun calculateNetCalories(grossCalories: Double, dailyTotals: DailyTotals): Double {
        val tefBonus = calculateTEFBonus(dailyTotals)
        return grossCalories - tefBonus
    }
}
