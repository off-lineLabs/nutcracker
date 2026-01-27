package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.offlinelabs.nutcracker.R
import androidx.compose.ui.res.stringResource
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.Recipe
import com.offlinelabs.nutcracker.data.model.RecipeIngredient
import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
import com.offlinelabs.nutcracker.ui.components.items.RecipeIngredientItem
import com.offlinelabs.nutcracker.ui.theme.getContrastingTextColor
import com.offlinelabs.nutcracker.ui.theme.brandAccentShade
import java.util.Locale
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecipeDialog(
    recipe: Recipe? = null, // If provided, we're editing
    meals: List<Meal>,
    initialIngredients: List<Pair<RecipeIngredient, Meal?>> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Recipe, List<RecipeIngredient>) -> Unit,
    onSelectMeal: () -> Unit
) {
    var recipeName by remember { mutableStateOf(recipe?.name ?: "") }
    var recipeDescription by remember { mutableStateOf(recipe?.description ?: "") }
    var ingredients by remember { mutableStateOf(initialIngredients) }
    var editingIngredient by remember { mutableStateOf<RecipeIngredient?>(null) }
    var showMealSelector by remember { mutableStateOf(false) }
    
    // Update ingredients when initialIngredients change (for editing)
    LaunchedEffect(initialIngredients) {
        if (initialIngredients.isNotEmpty()) {
            ingredients = initialIngredients
        }
    }
    
    // Function to add ingredient
    fun addIngredient(meal: Meal, quantity: Double = meal.servingSize_value, unit: ServingSizeUnit = meal.servingSize_unit) {
        val newIngredient = RecipeIngredient(
            recipeId = recipe?.id ?: 0L,
            mealId = meal.id,
            quantity = quantity,
            unit = unit,
            order = ingredients.size
        )
        ingredients = ingredients + Pair(newIngredient, meal)
    }
    
    // Calculate nutrition totals
    val nutritionTotals = remember(ingredients) {
        var totalCalories = 0.0
        var totalCarbs = 0.0
        var totalProtein = 0.0
        var totalFat = 0.0
        var totalFiber = 0.0
        var totalSodium = 0.0
        
        ingredients.forEach { (ingredient, meal) ->
            meal?.let { m ->
                val multiplier = ingredient.quantity / m.servingSize_value
                totalCalories += m.calories * multiplier
                totalCarbs += m.carbohydrates_g * multiplier
                totalProtein += m.protein_g * multiplier
                totalFat += m.fat_g * multiplier
                totalFiber += m.fiber_g * multiplier
                totalSodium += m.sodium_mg * multiplier
            }
        }
        
        mapOf(
            "calories" to totalCalories,
            "carbs" to totalCarbs,
            "protein" to totalProtein,
            "fat" to totalFat,
            "fiber" to totalFiber,
            "sodium" to totalSodium
        )
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                        )
                    }
                    Text(
                        text = if (recipe != null) stringResource(R.string.edit_recipe) else stringResource(R.string.create_recipe),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Recipe name
                OutlinedTextField(
                    value = recipeName,
                    onValueChange = { recipeName = it },
                    label = { Text(stringResource(R.string.recipe_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Recipe description (optional)
                OutlinedTextField(
                    value = recipeDescription,
                    onValueChange = { recipeDescription = it },
                    label = { Text(stringResource(R.string.description_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Add ingredient button
                Button(
                    onClick = { showMealSelector = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brandAccentShade(0)
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_ingredient))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Ingredients list
                if (ingredients.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_ingredients_added),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ingredients.size) { index ->
                            val (ingredient, meal) = ingredients[index]
                            RecipeIngredientItem(
                                ingredient = ingredient,
                                meal = meal,
                                onEdit = { editingIngredient = it },
                                onDelete = {
                                    ingredients = ingredients.filter { it.first.id != ingredient.id }
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Nutrition preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.total_nutrition),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${stringResource(R.string.calories_label)}: ${nutritionTotals["calories"]?.toInt() ?: 0} kcal",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${stringResource(R.string.protein_label)}: ${String.format(Locale.US, "%.1f", nutritionTotals["protein"] ?: 0.0)}g",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${stringResource(R.string.carbs_g)}: ${String.format(Locale.US, "%.1f", nutritionTotals["carbs"] ?: 0.0)}g",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${stringResource(R.string.fat_g)}: ${String.format(Locale.US, "%.1f", nutritionTotals["fat"] ?: 0.0)}g",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Save button
                Button(
                    onClick = {
                        if (recipeName.isNotBlank() && ingredients.isNotEmpty()) {
                            val newRecipe = recipe?.copy(
                                name = recipeName,
                                description = recipeDescription.ifBlank { null }
                            ) ?: Recipe(
                                name = recipeName,
                                description = recipeDescription.ifBlank { null }
                            )
                            val ingredientList = ingredients.mapIndexed { index, (ingredient, _) ->
                                ingredient.copy(order = index)
                            }
                            onSave(newRecipe, ingredientList)
                        }
                    },
                    enabled = recipeName.isNotBlank() && ingredients.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brandAccentShade(0)
                    )
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
    
    // Ingredient quantity edit dialog
    editingIngredient?.let { ingredient ->
        EditIngredientQuantityDialog(
            ingredient = ingredient,
            meal = ingredients.find { it.first.id == ingredient.id }?.second,
            onDismiss = { editingIngredient = null },
            onSave = { updatedIngredient ->
                ingredients = ingredients.map { (ing, meal) ->
                    if (ing.id == updatedIngredient.id) {
                        Pair(updatedIngredient, meal)
                    } else {
                        Pair(ing, meal)
                    }
                }
                editingIngredient = null
            }
        )
    }
    
    // Meal selector for adding ingredients
    if (showMealSelector) {
        SelectMealForIngredientDialog(
            meals = meals,
            onDismiss = { showMealSelector = false },
            onMealSelected = { meal ->
                // Add ingredient with default quantity
                val newIngredient = RecipeIngredient(
                    recipeId = recipe?.id ?: 0L,
                    mealId = meal.id,
                    quantity = meal.servingSize_value,
                    unit = meal.servingSize_unit,
                    order = ingredients.size
                )
                ingredients = ingredients + Pair(newIngredient, meal)
                showMealSelector = false
            }
        )
    }
}

@Composable
private fun EditIngredientQuantityDialog(
    ingredient: RecipeIngredient,
    meal: Meal?,
    onDismiss: () -> Unit,
    onSave: (RecipeIngredient) -> Unit
) {
    var quantity by remember { mutableStateOf(ingredient.quantity.toString()) }
    var selectedUnit by remember { mutableStateOf(ingredient.unit) }
    var showUnitSelector by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_ingredient)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                meal?.let {
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text(stringResource(R.string.quantity)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        )
                    )
                    
                    OutlinedButton(
                        onClick = { showUnitSelector = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(selectedUnit.abbreviation)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toDoubleOrNull() ?: 0.0
                    if (qty > 0) {
                        onSave(ingredient.copy(quantity = qty, unit = selectedUnit))
                    }
                },
                enabled = quantity.toDoubleOrNull()?.let { it > 0 } ?: false
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
    
    if (showUnitSelector) {
        UnitSelectorDialog(
            onDismiss = { showUnitSelector = false },
            onUnitSelected = {
                selectedUnit = it
                showUnitSelector = false
            }
        )
    }
}

@Composable
private fun SelectMealForIngredientDialog(
    meals: List<Meal>,
    onDismiss: () -> Unit,
    onMealSelected: (Meal) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_ingredient)) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(meals) { meal ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onMealSelected(meal) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = meal.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${meal.calories} kcal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
