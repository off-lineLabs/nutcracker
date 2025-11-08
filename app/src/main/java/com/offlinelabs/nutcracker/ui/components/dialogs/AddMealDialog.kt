package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
import com.offlinelabs.nutcracker.ui.theme.dialogOutlinedTextFieldColorsMaxContrast
import com.offlinelabs.nutcracker.ui.theme.dialogPrimaryColorMaxContrast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealDialog(
    onDismiss: () -> Unit,
    onAddMeal: (Meal) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var sodium by remember { mutableStateOf("") }
    
    // Serving size state
    var servingSizeValue by remember { mutableStateOf("100") }
    var selectedUnit by remember { mutableStateOf(ServingSizeUnit.GRAMS) }
    var expanded by remember { mutableStateOf(false) }

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
                    text = stringResource(R.string.add_new_meal),
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
                        colors = dialogTextFieldColors,
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
                            val servingSize = servingSizeValue.toDoubleOrNull() ?: 100.0
                            val meal = Meal(
                                name = name,
                                calories = calories.toIntOrNull() ?: 0,
                                carbohydrates_g = carbs.toDoubleOrNull() ?: 0.0,
                                protein_g = protein.toDoubleOrNull() ?: 0.0,
                                fat_g = fat.toDoubleOrNull() ?: 0.0,
                                fiber_g = fiber.toDoubleOrNull() ?: 0.0,
                                sodium_mg = sodium.toDoubleOrNull() ?: 0.0,
                                servingSize_value = servingSize,
                                servingSize_unit = selectedUnit
                            )
                            onAddMeal(meal)
                        },
                        enabled = name.isNotBlank() && calories.isNotBlank() && servingSizeValue.isNotBlank()
                    ) {
                        Text(stringResource(R.string.add_meal))
                    }
                }
            }
        }
    }
}
