package com.example.template.data.mapper

import com.example.template.data.model.*

object FoodInfoMapper {
    
    fun mapToFoodInfo(response: OpenFoodFactsResponse): FoodInfo? {
        val product = response.product ?: return null
        
        return FoodInfo(
            name = product.productNameEn ?: product.productName ?: "Unknown Product",
            brand = product.brands,
            imageUrl = product.imageFrontUrl ?: product.imageUrl,
            nutritionImageUrl = product.imageNutritionUrl,
            nutrition = mapNutritionInfo(product.nutriments),
            novaClassification = NovaClassification.fromGroup(product.novaGroup),
            greenScore = product.ecoscoreGrade?.let { grade ->
                GreenScore(grade.uppercase(), product.ecoscoreScore)
            },
            nutriscore = product.nutriscoreGrade?.let { grade ->
                Nutriscore(grade.uppercase(), product.nutriscoreScore)
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
            sodium = getPer100gValue(
                primary = nutriments.sodium100g,
                fallback = nutriments.sodium
            ),
            fiber = getPer100gValue(
                primary = nutriments.fiber100g,
                fallback = nutriments.fiber
            ),
            sugars = getPer100gValue(
                primary = nutriments.sugars100g,
                fallback = nutriments.sugars
            ),
            salt = getPer100gValue(
                primary = nutriments.salt100g,
                fallback = nutriments.salt
            ),
            cholesterol = getPer100gValue(
                primary = nutriments.cholesterol100g,
                fallback = nutriments.cholesterol
            ),
            vitaminC = getPer100gValue(
                primary = nutriments.vitaminC100g,
                fallback = nutriments.vitaminC
            ),
            calcium = getPer100gValue(
                primary = nutriments.calcium100g,
                fallback = nutriments.calcium
            ),
            iron = getPer100gValue(
                primary = nutriments.iron100g,
                fallback = nutriments.iron
            )
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
            salt = null,
            cholesterol = null,
            vitaminC = null,
            calcium = null,
            iron = null
        )
    }
}
