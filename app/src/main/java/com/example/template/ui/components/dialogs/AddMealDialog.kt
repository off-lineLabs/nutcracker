package com.example.template.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.template.R
import com.example.template.data.model.Meal

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

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Meal Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("Calories") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text("Protein (g)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text("Carbs (g)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text("Fat (g)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fiber,
                        onValueChange = { fiber = it },
                        label = { Text("Fiber (g)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = sodium,
                    onValueChange = { sodium = it },
                    label = { Text("Sodium (mg)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val meal = Meal(
                                name = name,
                                calories = calories.toIntOrNull() ?: 0,
                                carbohydrates_g = carbs.toDoubleOrNull() ?: 0.0,
                                protein_g = protein.toDoubleOrNull() ?: 0.0,
                                fat_g = fat.toDoubleOrNull() ?: 0.0,
                                fiber_g = fiber.toDoubleOrNull() ?: 0.0,
                                sodium_mg = sodium.toDoubleOrNull() ?: 0.0,
                                servingSize_value = 100,
                                servingSize_unit = "g"
                            )
                            onAddMeal(meal)
                        },
                        enabled = name.isNotBlank() && calories.isNotBlank()
                    ) {
                        Text("Add Meal")
                    }
                }
            }
        }
    }
}
