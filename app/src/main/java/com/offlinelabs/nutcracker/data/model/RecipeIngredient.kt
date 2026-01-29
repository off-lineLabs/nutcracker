package com.offlinelabs.nutcracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE // Delete ingredients when recipe is deleted
        ),
        ForeignKey(
            entity = Meal::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.RESTRICT // Prevent deletion of meals that are used in recipes
        )
    ],
    indices = [Index("recipeId"), Index("mealId")]
)
@TypeConverters(ServingSizeUnitConverter::class)
data class RecipeIngredient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipeId: Long, // Foreign key to Recipe
    val mealId: Long, // Foreign key to Meal
    val quantity: Double, // Quantity of the meal ingredient
    val unit: ServingSizeUnit, // Unit for the quantity
    val order: Int = 0 // Display order in the recipe
)
