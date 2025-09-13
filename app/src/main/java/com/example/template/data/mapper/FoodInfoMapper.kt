package com.example.template.data.mapper

import com.example.template.data.model.*
import android.util.Log

object FoodInfoMapper {
    
    fun mapToFoodInfo(response: OpenFoodFactsResponse, currentLanguage: com.example.template.data.AppLanguage? = null): FoodInfo? {
        val product = response.product ?: return null
        
        // Use localized name if language is provided, otherwise fall back to English
        val productName = if (currentLanguage != null) {
            product.getLocalizedProductName(currentLanguage)
        } else {
            product.productNameEn ?: product.productName ?: "Unknown Product"
        }
        
        return FoodInfo(
            name = productName,
            brand = product.brands,
            imageUrl = product.imageFrontUrl ?: product.imageUrl,
            nutritionImageUrl = product.imageNutritionUrl,
            nutrition = mapNutritionInfo(product.nutriments),
            novaClassification = NovaClassification.fromGroup(product.novaGroup),
            greenScore = product.ecoscoreGrade?.let { grade ->
                if (grade.uppercase() != "UNKNOWN" && grade.uppercase() != "NOT-APPLICABLE" && grade.isNotBlank()) {
                    GreenScore(grade.uppercase(), product.ecoscoreScore)
                } else null
            },
            nutriscore = product.nutriscoreGrade?.let { grade ->
                if (grade.uppercase() != "UNKNOWN" && grade.isNotBlank()) {
                    Nutriscore(grade.uppercase(), product.nutriscoreScore)
                } else null
            },
            ingredients = product.ingredientsTextEn ?: product.ingredientsText,
            categories = product.categories,
            quantity = product.quantity,
            servingSize = product.servingSize
        )
    }
    
    private fun mapNutritionInfo(nutriments: Nutriments?): NutritionInfo {
        if (nutriments == null) {
            return createEmptyNutritionInfo()
        }
        
        // Debug logging to see what values we're getting
        Log.d("FoodInfoMapper", "Sodium values - 100g: ${nutriments.sodium100g}, regular: ${nutriments.sodium}, value: ${nutriments.sodiumValue}")
        Log.d("FoodInfoMapper", "Vitamin C values - 100g: ${nutriments.vitaminC100g}, regular: ${nutriments.vitaminC}, value: ${nutriments.vitaminCValue}")
        Log.d("FoodInfoMapper", "Calcium values - 100g: ${nutriments.calcium100g}, regular: ${nutriments.calcium}, value: ${nutriments.calciumValue}")
        Log.d("FoodInfoMapper", "Iron values - 100g: ${nutriments.iron100g}, regular: ${nutriments.iron}, value: ${nutriments.ironValue}")
        
        return NutritionInfo(
            // Always prioritize per-100g values for consistency
            calories = getPer100gValue(
                primary = nutriments.energyKcal100g,
                fallback = nutriments.energy100g,
                tertiary = nutriments.energyKcal,
                quaternary = nutriments.energy
            ),
            fat = getPer100gValue(
                primary = nutriments.fat100g,
                fallback = nutriments.fat
            ),
            saturatedFat = getPer100gValue(
                primary = nutriments.saturatedFat100g,
                fallback = nutriments.saturatedFat
            ),
            carbohydrates = getPer100gValue(
                primary = nutriments.carbohydrates100g,
                fallback = nutriments.carbohydrates
            ),
            proteins = getPer100gValue(
                primary = nutriments.proteins100g,
                fallback = nutriments.proteins
            ),
            sodium = convertGramsToMilligrams(getPer100gValue(
                primary = nutriments.sodium100g,
                fallback = nutriments.sodium,
                tertiary = nutriments.sodiumValue
            )),
            fiber = getPer100gValue(
                primary = nutriments.fiber100g,
                fallback = nutriments.fiber
            ),
            sugars = getPer100gValue(
                primary = nutriments.sugars100g,
                fallback = nutriments.sugars
            ),
            cholesterol = getPer100gValue(
                primary = nutriments.cholesterol100g,
                fallback = nutriments.cholesterol
            ),
            vitaminC = convertGramsToMilligrams(getPer100gValue(
                primary = nutriments.vitaminC100g,
                fallback = nutriments.vitaminC,
                tertiary = nutriments.vitaminCValue
            )),
            calcium = convertGramsToMilligrams(getPer100gValue(
                primary = nutriments.calcium100g,
                fallback = nutriments.calcium,
                tertiary = nutriments.calciumValue
            )),
            iron = convertGramsToMilligrams(getPer100gValue(
                primary = nutriments.iron100g,
                fallback = nutriments.iron,
                tertiary = nutriments.ironValue
            ))
        )
    }
    
    /**
     * Helper function to prioritize per-100g values over other units.
     * This ensures we always get standardized values per 100g/100ml when available.
     */
    private fun getPer100gValue(
        primary: Double?,
        fallback: Double? = null,
        tertiary: Double? = null,
        quaternary: Double? = null
    ): Double? {
        return primary ?: fallback ?: tertiary ?: quaternary
    }
    
    /**
     * Helper function to convert grams to milligrams for display.
     * Many Open Food Facts values are in grams but we display in mg.
     */
    private fun convertGramsToMilligrams(grams: Double?): Double? {
        return grams?.let { it * 1000 }
    }
    
    private fun createEmptyNutritionInfo(): NutritionInfo {
        return NutritionInfo(
            calories = null,
            fat = null,
            saturatedFat = null,
            carbohydrates = null,
            proteins = null,
            sodium = null,
            fiber = null,
            sugars = null,
            cholesterol = null,
            vitaminC = null,
            calcium = null,
            iron = null
        )
    }
}
