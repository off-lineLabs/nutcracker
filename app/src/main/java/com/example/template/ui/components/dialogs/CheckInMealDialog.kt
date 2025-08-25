package com.example.template.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.template.R
import com.example.template.data.model.Meal
import com.example.template.data.model.MealCheckIn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInMealDialog(
    meal: Meal,
    onDismiss: () -> Unit,
    onCheckIn: (MealCheckIn) -> Unit
) {
    var servingSize by remember { mutableStateOf(1.0) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.check_in_meal_title, meal.name),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Meal info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = meal.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.meal_calories_info,
                                meal.calories,
                                meal.servingSize_value,
                                meal.servingSize_unit
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Serving size slider
                Column {
                    Text(
                        text = stringResource(R.string.serving_size),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "0.5x",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = servingSize.toFloat(),
                            onValueChange = { servingSize = it.toDouble() },
                            valueRange = 0.5f..3.0f,
                            steps = 24, // 0.1 increments
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "3.0x",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.serving_size_value,
                            servingSize,
                            (meal.calories * servingSize).toInt()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Notes field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val checkIn = MealCheckIn.create(
                        mealId = meal.id,
                        servingSize = servingSize,
                        notes = notes.takeIf { it.isNotBlank() }
                    )
                    onCheckIn(checkIn)
                }
            ) {
                Text(stringResource(R.string.check_in))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
