package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
import com.offlinelabs.nutcracker.ui.theme.dialogOutlinedTextFieldColorsMaxContrast
import com.offlinelabs.nutcracker.ui.theme.dialogPrimaryColorMaxContrast
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMealDialog(
    meal: Meal,
    onDismiss: () -> Unit,
    onUpdateMeal: (Meal) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    // Store original values for proportional calculations
    val originalServingSize = remember { meal.servingSize_value }
    val originalCalories = remember { meal.calories }
    val originalProtein = remember { meal.protein_g }
    val originalCarbs = remember { meal.carbohydrates_g }
    val originalFat = remember { meal.fat_g }
    val originalFiber = remember { meal.fiber_g }
    val originalSodium = remember { meal.sodium_mg }
    
    // Initialize state with existing meal data
    var name by remember { mutableStateOf(meal.name) }
    var calories by remember { mutableStateOf(meal.calories.toString()) }
    var protein by remember { mutableStateOf(meal.protein_g.toString()) }
    var carbs by remember { mutableStateOf(meal.carbohydrates_g.toString()) }
    var fat by remember { mutableStateOf(meal.fat_g.toString()) }
    var fiber by remember { mutableStateOf(meal.fiber_g.toString()) }
    var sodium by remember { mutableStateOf(meal.sodium_mg.toString()) }
    
    // Serving size state
    var servingSizeValue by remember { mutableStateOf(meal.servingSize_value.toString()) }
    var selectedUnit by remember { mutableStateOf(meal.servingSize_unit) }
    var expanded by remember { mutableStateOf(false) }
    
    // Link toggle state
    var isLinked by remember { mutableStateOf(false) }

    // Get maximum contrast primary color for dialogs
    val dialogPrimary = dialogPrimaryColorMaxContrast()
    val dialogTextFieldColors = dialogOutlinedTextFieldColorsMaxContrast()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.edit_meal),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Name | Kcals row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.meal_name)) },
                        placeholder = { Text(stringResource(R.string.meal_name_placeholder)) },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = dialogTextFieldColors,
                        modifier = Modifier.weight(0.6f)
                    )
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it },
                        label = { Text(stringResource(R.string.calories)) },
                        placeholder = { Text(stringResource(R.string.calories_placeholder)) },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = dialogTextFieldColors,
                        modifier = Modifier.weight(0.4f)
                    )
                }

                // Carbs | Fat row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text(stringResource(R.string.carbs_g)) },
                        placeholder = { Text(stringResource(R.string.carbs_placeholder)) },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = dialogTextFieldColors,
                        modifier = Modifier.weight(0.6f)
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text(stringResource(R.string.fat_g)) },
                        placeholder = { Text(stringResource(R.string.fat_placeholder)) },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = dialogTextFieldColors,
                        modifier = Modifier.weight(0.4f)
                    )
                }

                // Fiber | Sodium row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fiber,
                        onValueChange = { fiber = it },
                        label = { Text(stringResource(R.string.fiber_g)) },
                        placeholder = { Text(stringResource(R.string.fiber_placeholder)) },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = dialogTextFieldColors,
                        modifier = Modifier.weight(0.6f)
                    )
                    OutlinedTextField(
                        value = sodium,
                        onValueChange = { sodium = it },
                        label = { Text(stringResource(R.string.sodium_mg)) },
                        placeholder = { Text(stringResource(R.string.sodium_placeholder)) },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = dialogTextFieldColors,
                        modifier = Modifier.weight(0.4f)
                    )
                }

                // Proteins row (full width)
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = { Text(stringResource(R.string.protein_g)) },
                    placeholder = { Text(stringResource(R.string.protein_placeholder)) },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = dialogTextFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp)
                )

                // Serving Size Section
                Text(
                    text = stringResource(R.string.serving_size_label),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Serving size value input with link toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = servingSizeValue,
                            onValueChange = { newValue ->
                                servingSizeValue = newValue
                                
                                // If linked, update all nutrition fields proportionally
                                if (isLinked) {
                                    val newServingSize = newValue.toDoubleOrNull() ?: originalServingSize
                                    if (newServingSize > 0 && originalServingSize > 0) {
                                        val ratio = newServingSize / originalServingSize
                                        
                                        calories = (originalCalories * ratio).toInt().toString()
                                        protein = String.format(Locale.US, "%.1f", originalProtein * ratio)
                                        carbs = String.format(Locale.US, "%.1f", originalCarbs * ratio)
                                        fat = String.format(Locale.US, "%.1f", originalFat * ratio)
                                        fiber = String.format(Locale.US, "%.1f", originalFiber * ratio)
                                        sodium = String.format(Locale.US, "%.1f", originalSodium * ratio)
                                    }
                                }
                            },
                            label = { Text(stringResource(R.string.serving_size_value_label)) },
                            placeholder = { Text(stringResource(R.string.serving_size_placeholder)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = dialogTextFieldColors,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Link toggle button
                        IconButton(
                            onClick = { 
                                val wasLinked = isLinked
                                isLinked = !isLinked
                                
                                // If toggle is being turned ON, recalculate values based on current serving size
                                if (!wasLinked && isLinked) {
                                    val currentServingSize = servingSizeValue.toDoubleOrNull() ?: originalServingSize
                                    if (currentServingSize > 0 && originalServingSize > 0) {
                                        val ratio = currentServingSize / originalServingSize
                                        
                                        calories = (originalCalories * ratio).toInt().toString()
                                        protein = String.format(Locale.US, "%.1f", originalProtein * ratio)
                                        carbs = String.format(Locale.US, "%.1f", originalCarbs * ratio)
                                        fat = String.format(Locale.US, "%.1f", originalFat * ratio)
                                        fiber = String.format(Locale.US, "%.1f", originalFiber * ratio)
                                        sodium = String.format(Locale.US, "%.1f", originalSodium * ratio)
                                    }
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (isLinked) R.drawable.link else R.drawable.link_off
                                ),
                                contentDescription = if (isLinked) "Unlink" else "Link",
                                tint = if (isLinked) 
                                    dialogPrimary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                    
                    // Unit dropdown (full width, stacked below)
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = stringResource(selectedUnit.stringResourceId),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.serving_size_unit_label)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            colors = dialogTextFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            ServingSizeUnit.values().forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(unit.stringResourceId)) },
                                    onClick = {
                                        selectedUnit = unit
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete button (only when onDelete is provided) - positioned at bottom-left
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        // Empty space to maintain layout when no delete button
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(contentColor = dialogPrimary)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val servingSize = servingSizeValue.toDoubleOrNull() ?: meal.servingSize_value
                                // Create updated meal preserving all additional fields
                                val updatedMeal = meal.copy(
                                    name = name,
                                    calories = calories.toIntOrNull() ?: meal.calories,
                                    carbohydrates_g = carbs.toDoubleOrNull() ?: meal.carbohydrates_g,
                                    protein_g = protein.toDoubleOrNull() ?: meal.protein_g,
                                    fat_g = fat.toDoubleOrNull() ?: meal.fat_g,
                                    fiber_g = fiber.toDoubleOrNull() ?: meal.fiber_g,
                                    sodium_mg = sodium.toDoubleOrNull() ?: meal.sodium_mg,
                                    servingSize_value = servingSize,
                                    servingSize_unit = selectedUnit
                                )
                                onUpdateMeal(updatedMeal)
                            },
                            enabled = name.isNotBlank() && calories.isNotBlank() && servingSizeValue.isNotBlank()
                        ) {
                            Text(stringResource(R.string.update_meal))
                        }
                    }
                }
            }
        }
    }
}
