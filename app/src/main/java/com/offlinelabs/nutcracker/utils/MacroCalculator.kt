package com.offlinelabs.nutcracker.utils

import android.util.Log

/**
 * MacroCalculator - Bidirectional macro and calorie calculations
 * 
 * Supports both standard caloric densities and TEF-adjusted calculations.
 * Standard densities: Carbs 4, Protein 4, Fat 9 kcal/g
 * TEF-adjusted densities account for thermic effect of food.
 */
object MacroCalculator {
    private const val TAG = "MacroCalculator"
    
    // Standard caloric densities (kcal per gram)
    private const val CARBS_KCAL_PER_GRAM = 4.0
    private const val PROTEIN_KCAL_PER_GRAM = 4.0
    private const val FAT_KCAL_PER_GRAM = 9.0
    
    // AMDR (Acceptable Macronutrient Distribution Range) percentages
    private const val DEFAULT_CARBS_PERCENTAGE = 0.55  // 55%
    private const val DEFAULT_PROTEIN_PERCENTAGE = 0.175 // 17.5%
    private const val DEFAULT_FAT_PERCENTAGE = 0.275  // 27.5%
    
    /**
     * Data class representing macro proportions
     */
    data class MacroRatios(
        val carbsPercentage: Double,
        val proteinPercentage: Double,
        val fatPercentage: Double
    ) {
        init {
            val total = carbsPercentage + proteinPercentage + fatPercentage
            require(total > 0.99 && total < 1.01) { "Macro percentages must sum to 1.0, got $total" }
        }
    }
    
    /**
     * Data class representing macro values in grams
     */
    data class MacroValues(
        val carbsGrams: Double,
        val proteinGrams: Double,
        val fatGrams: Double
    )
    
    /**
     * Calculate current macro ratios from gram values
     * @return MacroRatios with current proportions
     */
    fun calculateCurrentRatios(
        carbsGrams: Double,
        proteinGrams: Double,
        fatGrams: Double
    ): MacroRatios {
        val totalCalories = calculateKcalFromMacros(carbsGrams, proteinGrams, fatGrams, useTEF = false)
        
        if (totalCalories <= 0) {
            return MacroRatios(
                carbsPercentage = DEFAULT_CARBS_PERCENTAGE,
                proteinPercentage = DEFAULT_PROTEIN_PERCENTAGE,
                fatPercentage = DEFAULT_FAT_PERCENTAGE
            )
        }
        
        val carbsCalories = carbsGrams * CARBS_KCAL_PER_GRAM
        val proteinCalories = proteinGrams * PROTEIN_KCAL_PER_GRAM
        val fatCalories = fatGrams * FAT_KCAL_PER_GRAM
        
        return MacroRatios(
            carbsPercentage = (carbsCalories / totalCalories).coerceIn(0.0, 1.0),
            proteinPercentage = (proteinCalories / totalCalories).coerceIn(0.0, 1.0),
            fatPercentage = (fatCalories / totalCalories).coerceIn(0.0, 1.0)
        ).let { ratios ->
            // Normalize to ensure they sum to 1.0
            val total = ratios.carbsPercentage + ratios.proteinPercentage + ratios.fatPercentage
            MacroRatios(
                carbsPercentage = ratios.carbsPercentage / total,
                proteinPercentage = ratios.proteinPercentage / total,
                fatPercentage = ratios.fatPercentage / total
            )
        }
    }
    
    /**
     * Calculate macros from target kcal using provided ratios
     * @param targetKcal Target daily calorie intake
     * @param ratios Macro proportions to apply
     * @param useTEF If true, uses TEF-adjusted calculation
     * @return MacroValues with calculated gram amounts
     */
    fun calculateMacrosFromKcal(
        targetKcal: Int,
        ratios: MacroRatios,
        useTEF: Boolean = false
    ): MacroValues {
        if (targetKcal <= 0) {
            return MacroValues(carbsGrams = 0.0, proteinGrams = 0.0, fatGrams = 0.0)
        }
        
        val effectiveCalories = if (useTEF) {
            // With TEF, we need to calculate backwards from effective calories
            // effective = target - TEF(macros)
            // This is iterative: we estimate macros, calculate TEF, and adjust
            iterativeMacroCalculation(targetKcal, ratios)
        } else {
            targetKcal.toDouble()
        }
        
        val carbsCalories = effectiveCalories * ratios.carbsPercentage
        val proteinCalories = effectiveCalories * ratios.proteinPercentage
        val fatCalories = effectiveCalories * ratios.fatPercentage
        
        return MacroValues(
            carbsGrams = carbsCalories / CARBS_KCAL_PER_GRAM,
            proteinGrams = proteinCalories / PROTEIN_KCAL_PER_GRAM,
            fatGrams = fatCalories / FAT_KCAL_PER_GRAM
        )
    }
    
    /**
     * Iteratively calculate effective calories accounting for TEF
     * Since TEF = (Protein_g × 4 × 0.25) + (Carbs_g × 4 × 0.075) + (Fat_g × 9 × 0.02)
     * And calories_net = Protein_g × 4 + Carbs_g × 4 + Fat_g × 9 - TEF
     * We need: target = calories_net, solve for macros
     */
    private fun iterativeMacroCalculation(targetKcal: Int, ratios: MacroRatios): Double {
        var effectiveCalories = targetKcal.toDouble()
        
        // Iterate 5 times for convergence
        repeat(5) {
            val carbsCalories = effectiveCalories * ratios.carbsPercentage
            val proteinCalories = effectiveCalories * ratios.proteinPercentage
            val fatCalories = effectiveCalories * ratios.fatPercentage
            
            val carbsGrams = carbsCalories / CARBS_KCAL_PER_GRAM
            val proteinGrams = proteinCalories / PROTEIN_KCAL_PER_GRAM
            val fatGrams = fatCalories / FAT_KCAL_PER_GRAM
            
            val tef = TEFCalculator.calculateTEFBonus(proteinGrams, carbsGrams, fatGrams)
            effectiveCalories = targetKcal + tef // Add TEF back to net calories to get gross intake
        }
        
        return effectiveCalories
    }
    
    /**
     * Calculate total kcal from macro grams
     * @param useTEF If true, includes TEF bonus in calculation
     * @return Total kcal as integer
     */
    fun calculateKcalFromMacros(
        carbsGrams: Double,
        proteinGrams: Double,
        fatGrams: Double,
        useTEF: Boolean = false
    ): Int {
        val baseCalories = (carbsGrams * CARBS_KCAL_PER_GRAM +
                proteinGrams * PROTEIN_KCAL_PER_GRAM +
                fatGrams * FAT_KCAL_PER_GRAM).toInt()
        
        return if (useTEF) {
            val tef = TEFCalculator.calculateTEFBonus(proteinGrams, carbsGrams, fatGrams).toInt()
            baseCalories + tef
        } else {
            baseCalories
        }
    }
    
    /**
     * Get default AMDR ratios (55% carbs, 17.5% protein, 27.5% fat)
     */
    fun getDefaultRatios(): MacroRatios {
        return MacroRatios(
            carbsPercentage = DEFAULT_CARBS_PERCENTAGE,
            proteinPercentage = DEFAULT_PROTEIN_PERCENTAGE,
            fatPercentage = DEFAULT_FAT_PERCENTAGE
        )
    }
    
    /**
     * Proportional Redistribution Algorithm - maintains fixed calorie total
     * When user drags one macro slider, the other two adjust proportionally
     * to "steal" calories and maintain the target total.
     * 
     * @param targetCalories Fixed target (e.g., 1800 kcal)
     * @param draggedType Which macro was dragged (CARBS, PROTEIN, or FAT)
     * @param draggedValue New value for the dragged macro in grams
     * @param currentCarbsG Current carbs in grams
     * @param currentProteinG Current protein in grams
     * @param currentFatG Current fat in grams
     * @return MacroValues with redistributed values maintaining targetCalories
     */
    fun redistributeMacrosProportionally(
        targetCalories: Int,
        draggedType: String, // "CARBS", "PROTEIN", or "FAT"
        draggedValue: Double,
        currentCarbsG: Double,
        currentProteinG: Double,
        currentFatG: Double
    ): MacroValues {
        Log.d(TAG, "===== REDISTRIBUTION START =====")
        Log.d(TAG, "Target: $targetCalories kcal")
        Log.d(TAG, "Dragged: $draggedType = $draggedValue g")
        Log.d(TAG, "Current: C=$currentCarbsG P=$currentProteinG F=$currentFatG")
        
        // Step 1: Calculate the cost of the dragged macro
        val draggedCost = when (draggedType) {
            "CARBS" -> draggedValue * CARBS_KCAL_PER_GRAM
            "PROTEIN" -> draggedValue * PROTEIN_KCAL_PER_GRAM
            "FAT" -> draggedValue * FAT_KCAL_PER_GRAM
            else -> 0.0
        }
        Log.d(TAG, "Dragged cost: $draggedCost kcal")
        
        // Step 2: Calculate remaining budget for other macros
        val remainingBudget = targetCalories - draggedCost
        Log.d(TAG, "Remaining budget: $remainingBudget kcal")
        
        // Step 3: Calculate current cost of the non-dragged macros
        val (nonDraggedMacro1Cost, nonDraggedMacro1Value, nonDraggedMacro2Cost, nonDraggedMacro2Value) = when (draggedType) {
            "CARBS" -> {
                val proteinCost = currentProteinG * PROTEIN_KCAL_PER_GRAM
                val fatCost = currentFatG * FAT_KCAL_PER_GRAM
                Quad(proteinCost, currentProteinG, fatCost, currentFatG)
            }
            "PROTEIN" -> {
                val carbsCost = currentCarbsG * CARBS_KCAL_PER_GRAM
                val fatCost = currentFatG * FAT_KCAL_PER_GRAM
                Quad(carbsCost, currentCarbsG, fatCost, currentFatG)
            }
            "FAT" -> {
                val carbsCost = currentCarbsG * CARBS_KCAL_PER_GRAM
                val proteinCost = currentProteinG * PROTEIN_KCAL_PER_GRAM
                Quad(carbsCost, currentCarbsG, proteinCost, currentProteinG)
            }
            else -> Quad(0.0, 0.0, 0.0, 0.0)
        }
        
        val totalOtherCost = nonDraggedMacro1Cost + nonDraggedMacro2Cost
        Log.d(TAG, "Total other cost: $totalOtherCost kcal (M1=$nonDraggedMacro1Cost, M2=$nonDraggedMacro2Cost)")
        
        // Step 4: Handle edge cases
        if (remainingBudget <= 0 || totalOtherCost == 0.0) {
            Log.d(TAG, "Edge case: remainingBudget=$remainingBudget, totalOtherCost=$totalOtherCost")
            val result = when (draggedType) {
                "CARBS" -> MacroValues(draggedValue, 0.0, 0.0)
                "PROTEIN" -> MacroValues(0.0, draggedValue, 0.0)
                "FAT" -> MacroValues(0.0, 0.0, draggedValue)
                else -> MacroValues(0.0, 0.0, 0.0)
            }
            Log.d(TAG, "Result: C=${result.carbsGrams} P=${result.proteinGrams} F=${result.fatGrams}")
            Log.d(TAG, "===== REDISTRIBUTION END =====")
            return result
        }
        
        // Step 5: Calculate proportional distribution ratios
        val macro1Ratio = nonDraggedMacro1Cost / totalOtherCost
        val macro2Ratio = nonDraggedMacro2Cost / totalOtherCost
        Log.d(TAG, "Ratios: M1=$macro1Ratio, M2=$macro2Ratio")
        
        val macro1NewCost = remainingBudget * macro1Ratio
        val macro2NewCost = remainingBudget * macro2Ratio
        Log.d(TAG, "New costs: M1=$macro1NewCost, M2=$macro2NewCost")
        
        // Step 6: Convert costs back to grams
        val result = when (draggedType) {
            "CARBS" -> MacroValues(
                carbsGrams = draggedValue,
                proteinGrams = macro1NewCost / PROTEIN_KCAL_PER_GRAM,
                fatGrams = macro2NewCost / FAT_KCAL_PER_GRAM
            )
            "PROTEIN" -> MacroValues(
                carbsGrams = macro1NewCost / CARBS_KCAL_PER_GRAM,
                proteinGrams = draggedValue,
                fatGrams = macro2NewCost / FAT_KCAL_PER_GRAM
            )
            "FAT" -> MacroValues(
                carbsGrams = macro1NewCost / CARBS_KCAL_PER_GRAM,
                proteinGrams = macro2NewCost / PROTEIN_KCAL_PER_GRAM,
                fatGrams = draggedValue
            )
            else -> MacroValues(0.0, 0.0, 0.0)
        }
        
        val resultKcal = (result.carbsGrams * 4) + (result.proteinGrams * 4) + (result.fatGrams * 9)
        Log.d(TAG, "Result: C=${result.carbsGrams} P=${result.proteinGrams} F=${result.fatGrams}")
        Log.d(TAG, "Result kcal: $resultKcal (target was $targetCalories)")
        Log.d(TAG, "===== REDISTRIBUTION END =====")
        
        return result
    }
    
    // Helper data class for quad tuple
    private data class Quad(val cost1: Double, val value1: Double, val cost2: Double, val value2: Double)
}
