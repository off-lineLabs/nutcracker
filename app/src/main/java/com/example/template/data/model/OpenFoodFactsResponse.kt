package com.example.template.data.model

import com.google.gson.annotations.SerializedName

data class OpenFoodFactsResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("status_verbose")
    val statusVerbose: String,
    @SerializedName("product")
    val product: Product?
)

data class Product(
    @SerializedName("product_name")
    val productName: String?,
    @SerializedName("product_name_en")
    val productNameEn: String?,
    @SerializedName("brands")
    val brands: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("image_front_url")
    val imageFrontUrl: String?,
    @SerializedName("image_nutrition_url")
    val imageNutritionUrl: String?,
    @SerializedName("nutriments")
    val nutriments: Nutriments?,
    @SerializedName("nova_group")
    val novaGroup: Int?,
    @SerializedName("nova_groups")
    val novaGroups: String?,
    @SerializedName("ecoscore_grade")
    val ecoscoreGrade: String?,
    @SerializedName("ecoscore_score")
    val ecoscoreScore: Int?,
    @SerializedName("nutriscore_grade")
    val nutriscoreGrade: String?,
    @SerializedName("nutriscore_score")
    val nutriscoreScore: Int?,
    @SerializedName("ingredients_text")
    val ingredientsText: String?,
    @SerializedName("ingredients_text_en")
    val ingredientsTextEn: String?,
    @SerializedName("categories")
    val categories: String?,
    @SerializedName("quantity")
    val quantity: String?,
    @SerializedName("serving_size")
    val servingSize: String?
)

data class Nutriments(
    // Energy - prioritize per 100g
    @SerializedName("energy-kcal_100g")
    val energyKcal100g: Double?,
    @SerializedName("energy_100g")
    val energy100g: Double?,
    @SerializedName("energy-kcal")
    val energyKcal: Double?,
    @SerializedName("energy")
    val energy: Double?,
    
    // Fat - prioritize per 100g
    @SerializedName("fat_100g")
    val fat100g: Double?,
    @SerializedName("fat")
    val fat: Double?,
    @SerializedName("saturated-fat_100g")
    val saturatedFat100g: Double?,
    @SerializedName("saturated-fat")
    val saturatedFat: Double?,
    
    // Carbohydrates - prioritize per 100g
    @SerializedName("carbohydrates_100g")
    val carbohydrates100g: Double?,
    @SerializedName("carbohydrates")
    val carbohydrates: Double?,
    @SerializedName("sugars_100g")
    val sugars100g: Double?,
    @SerializedName("sugars")
    val sugars: Double?,
    
    // Proteins - prioritize per 100g
    @SerializedName("proteins_100g")
    val proteins100g: Double?,
    @SerializedName("proteins")
    val proteins: Double?,
    
    // Sodium/Salt - prioritize per 100g
    @SerializedName("sodium_100g")
    val sodium100g: Double?,
    @SerializedName("sodium")
    val sodium: Double?,
    @SerializedName("salt_100g")
    val salt100g: Double?,
    @SerializedName("salt")
    val salt: Double?,
    @SerializedName("sodium_value")
    val sodiumValue: Double?,
    @SerializedName("salt_value")
    val saltValue: Double?,
    
    // Fiber - prioritize per 100g
    @SerializedName("fiber_100g")
    val fiber100g: Double?,
    @SerializedName("fiber")
    val fiber: Double?,
    
    // Additional nutrients - prioritize per 100g
    @SerializedName("cholesterol_100g")
    val cholesterol100g: Double?,
    @SerializedName("cholesterol")
    val cholesterol: Double?,
    @SerializedName("vitamin-c_100g")
    val vitaminC100g: Double?,
    @SerializedName("vitamin-c")
    val vitaminC: Double?,
    @SerializedName("vitamin-c_value")
    val vitaminCValue: Double?,
    @SerializedName("calcium_100g")
    val calcium100g: Double?,
    @SerializedName("calcium")
    val calcium: Double?,
    @SerializedName("calcium_value")
    val calciumValue: Double?,
    @SerializedName("iron_100g")
    val iron100g: Double?,
    @SerializedName("iron")
    val iron: Double?,
    @SerializedName("iron_value")
    val ironValue: Double?
)

data class FoodInfo(
    val name: String,
    val brand: String?,
    val imageUrl: String?,
    val nutritionImageUrl: String?,
    val nutrition: NutritionInfo,
    val novaClassification: NovaClassification,
    val greenScore: GreenScore?,
    val nutriscore: Nutriscore?,
    val ingredients: String?,
    val categories: String?,
    val quantity: String?,
    val servingSize: String?
)

data class NutritionInfo(
    // All values are standardized per 100g/100ml for consistency
    val calories: Double?, // kcal per 100g
    val fat: Double?, // g per 100g
    val saturatedFat: Double?, // g per 100g
    val carbohydrates: Double?, // g per 100g
    val proteins: Double?, // g per 100g
    val sodium: Double?, // mg per 100g
    val fiber: Double?, // g per 100g
    val sugars: Double?, // g per 100g
    val cholesterol: Double?, // mg per 100g
    val vitaminC: Double?, // mg per 100g
    val calcium: Double?, // mg per 100g
    val iron: Double? // mg per 100g
)

data class NovaClassification(
    val group: Int,
    val description: String
) {
    companion object {
        fun fromGroup(group: Int?): NovaClassification {
            return when (group) {
                1 -> NovaClassification(1, "Unprocessed or minimally processed foods")
                2 -> NovaClassification(2, "Processed culinary ingredients")
                3 -> NovaClassification(3, "Processed foods")
                4 -> NovaClassification(4, "Ultra-processed food and drink products")
                else -> NovaClassification(0, "Unknown classification")
            }
        }
    }
}

data class GreenScore(
    val grade: String,
    val score: Int?
) {
    val displayName: String
        get() = "Eco-Score: $grade"
}

data class Nutriscore(
    val grade: String,
    val score: Int?
) {
    val displayName: String
        get() = "Nutri-Score: $grade"
}
