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
import com.offlinelabs.nutcracker.ui.components.items.MealItem
import com.offlinelabs.nutcracker.ui.theme.getContrastingTextColor
import com.offlinelabs.nutcracker.ui.theme.brandAccentShade
import com.offlinelabs.nutcracker.ui.theme.dialogOutlinedTextFieldColorsMaxContrast
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import com.offlinelabs.nutcracker.ui.components.form.FormDialog
import com.offlinelabs.nutcracker.ui.components.form.FormDialogActions
import com.offlinelabs.nutcracker.ui.components.form.FormDialogSurface
import com.offlinelabs.nutcracker.ui.components.form.QuantityUnitInput
import com.offlinelabs.nutcracker.ui.components.form.formatDecimal
import com.offlinelabs.nutcracker.ui.components.form.parseDecimal
import com.offlinelabs.nutcracker.ui.components.form.validateDecimal
import com.offlinelabs.nutcracker.ui.theme.FoodLogTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecipeDialog(
    recipe: Recipe? = null, // If provided, we're editing
    meals: List<Meal>,
    initialIngredients: List<Pair<RecipeIngredient, Meal?>> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Recipe, List<RecipeIngredient>) -> Unit
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
                val multiplier = ServingSizeUnit.servingMultiplier(
                    quantity = ingredient.quantity,
                    unit = ingredient.unit,
                    servingValue = m.servingSize_value,
                    servingUnit = m.servingSize_unit
                )
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
                    singleLine = true,
                    colors = dialogOutlinedTextFieldColorsMaxContrast()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Recipe description (optional)
                OutlinedTextField(
                    value = recipeDescription,
                    onValueChange = { recipeDescription = it },
                    label = { Text(stringResource(R.string.description_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = dialogOutlinedTextFieldColorsMaxContrast()
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
                                    ingredients = ingredients.filterIndexed { i, _ -> i != index }
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
                                text = "${stringResource(R.string.calories_label)}: ${stringResource(R.string.calories_value, nutritionTotals["calories"]?.toInt() ?: 0)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${stringResource(R.string.protein_label)}: ${stringResource(R.string.grams_value_format, String.format(Locale.getDefault(), "%.1f", nutritionTotals["protein"] ?: 0.0))}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${stringResource(R.string.carbs_g)}: ${stringResource(R.string.grams_value_format, String.format(Locale.getDefault(), "%.1f", nutritionTotals["carbs"] ?: 0.0))}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${stringResource(R.string.fat_g)}: ${stringResource(R.string.grams_value_format, String.format(Locale.getDefault(), "%.1f", nutritionTotals["fat"] ?: 0.0))}",
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
        val ingredientIndex = ingredients.indexOfFirst { it.first == ingredient }
        EditIngredientQuantityDialog(
            ingredient = ingredient,
            meal = if (ingredientIndex >= 0) ingredients[ingredientIndex].second else null,
            onDismiss = { editingIngredient = null },
            onSave = { updatedIngredient ->
                ingredients = ingredients.mapIndexed { index, (ing, meal) ->
                    if (index == ingredientIndex) {
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
    var quantity by rememberSaveable { mutableStateOf(formatDecimal(ingredient.quantity)) }
    var selectedUnit by rememberSaveable { mutableStateOf(ingredient.unit) }

    val error = validateDecimal(quantity, required = true, allowZero = false)
    val parsed = parseDecimal(quantity)
    val isValid = error == null && parsed != null && parsed > 0
    val save = {
        if (isValid && parsed != null) {
            onSave(ingredient.copy(quantity = parsed, unit = selectedUnit))
        }
    }

    FormDialog(
        title = stringResource(R.string.edit_ingredient),
        onDismissRequest = onDismiss,
        actions = {
            FormDialogActions(
                confirmText = stringResource(R.string.save),
                onConfirm = save,
                onDismiss = onDismiss,
                confirmEnabled = isValid
            )
        }
    ) {
        EditIngredientContent(
            mealName = meal?.name,
            quantity = quantity,
            onQuantityChange = { quantity = it },
            unit = selectedUnit,
            onUnitChange = { selectedUnit = it },
            errorText = error?.let { stringResource(it.messageRes) },
            onDone = save
        )
    }
}

@Composable
private fun EditIngredientContent(
    mealName: String?,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    unit: ServingSizeUnit,
    onUnitChange: (ServingSizeUnit) -> Unit,
    errorText: String?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FormDefaults.colors()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (mealName != null) {
            Text(
                text = mealName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.content,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        QuantityUnitInput(
            label = stringResource(R.string.quantity),
            amount = quantity,
            onAmountChange = onQuantityChange,
            unit = unit,
            onUnitChange = onUnitChange,
            errorText = errorText,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            colors = colors
        )
    }
}

@Composable
private fun EditIngredientPreviewContent(quantity: String, error: String?) {
    Box(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
        FormDialogSurface(
            title = stringResource(R.string.edit_ingredient),
            actions = {
                FormDialogActions(
                    confirmText = stringResource(R.string.save),
                    onConfirm = {},
                    onDismiss = {},
                    confirmEnabled = error == null
                )
            }
        ) {
            EditIngredientContent("Rolled oats", quantity, {}, ServingSizeUnit.GRAMS, {}, error, {})
        }
    }
}

@Preview(name = "Edit ingredient - light", widthDp = 360, heightDp = 400, showBackground = true, backgroundColor = 0xFFFAFBFC)
@Composable
private fun EditIngredientLightPreview() {
    FoodLogTheme(darkTheme = false) { EditIngredientPreviewContent("50", null) }
}

@Preview(name = "Edit ingredient - dark, error", widthDp = 360, heightDp = 400, showBackground = true, backgroundColor = 0xFF0B1426)
@Composable
private fun EditIngredientDarkErrorPreview() {
    FoodLogTheme(darkTheme = true) {
        EditIngredientPreviewContent("abc", stringResource(R.string.form_error_invalid_number))
    }
}

@Composable
private fun SelectMealForIngredientDialog(
    meals: List<Meal>,
    onDismiss: () -> Unit,
    onMealSelected: (Meal) -> Unit
) {
    // Filter to only show visible meals
    val visibleMeals = meals.filter { it.isVisible }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_ingredient)) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(visibleMeals) { meal ->
                    MealItem(
                        meal = meal,
                        onMealClick = { onMealSelected(meal) }
                    )
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
