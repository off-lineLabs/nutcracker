package com.example.template.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.template.R
import com.example.template.data.model.Meal
import com.example.template.data.model.ServingSizeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMealDialog(
    meal: Meal,
    onDismiss: () -> Unit,
    onUpdateMeal: (Meal) -> Unit
) {
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
                    text = "Edit Meal",
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
                        modifier = Modifier.weight(0.6f)
                    )
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it },
                        label = { Text(stringResource(R.string.calories)) },
                        placeholder = { Text(stringResource(R.string.calories_placeholder)) },
                        textStyle = MaterialTheme.typography.bodyMedium,
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
                        modifier = Modifier.weight(0.6f)
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text(stringResource(R.string.fat_g)) },
                        placeholder = { Text(stringResource(R.string.fat_placeholder)) },
                        textStyle = MaterialTheme.typography.bodyMedium,
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
                        modifier = Modifier.weight(0.6f)
                    )
                    OutlinedTextField(
                        value = sodium,
                        onValueChange = { sodium = it },
                        label = { Text(stringResource(R.string.sodium_mg)) },
                        placeholder = { Text(stringResource(R.string.sodium_placeholder)) },
                        textStyle = MaterialTheme.typography.bodyMedium,
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
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Serving size value input (40% of width)
                    OutlinedTextField(
                        value = servingSizeValue,
                        onValueChange = { servingSizeValue = it },
                        label = { Text(stringResource(R.string.serving_size_value_label)) },
                        placeholder = { Text(stringResource(R.string.serving_size_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(0.4f)
                    )
                    
                    // Unit dropdown (60% of width)
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(0.6f)
                    ) {
                        OutlinedTextField(
                            value = stringResource(selectedUnit.stringResourceId),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.serving_size_unit_label)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
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
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
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
                        Text("Update Meal")
                    }
                }
            }
        }
    }
}
