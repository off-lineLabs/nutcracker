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
            return NutritionInfo(
                calories = null,
                fat = null,
                carbohydrates = null,
                proteins = null,
                sodium = null,
                fiber = null,
                sugars = null,
                salt = null
            )
        }
        
        return NutritionInfo(
            calories = nutriments.energyKcal100g ?: nutriments.energyKcal,
            fat = nutriments.fat100g ?: nutriments.fat,
            carbohydrates = nutriments.carbohydrates100g ?: nutriments.carbohydrates,
            proteins = nutriments.proteins100g ?: nutriments.proteins,
            sodium = nutriments.sodium100g ?: nutriments.sodium,
            fiber = nutriments.fiber100g ?: nutriments.fiber,
            sugars = nutriments.sugars100g ?: nutriments.sugars,
            salt = nutriments.salt100g ?: nutriments.salt
        )
    }
}
