package com.example.template.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.template.data.model.UserGoal

@Composable
fun SetGoalDialog(
    currentUserGoal: UserGoal,
    onDismiss: () -> Unit,
    onSetGoal: (UserGoal) -> Unit
) {
    var caloriesInput by remember { mutableStateOf(currentUserGoal.caloriesGoal.toString()) }
    var carbsInput by remember { mutableStateOf(currentUserGoal.carbsGoal_g.toString()) }
    var proteinInput by remember { mutableStateOf(currentUserGoal.proteinGoal_g.toString()) }
    var fatInput by remember { mutableStateOf(currentUserGoal.fatGoal_g.toString()) }
    var fiberInput by remember { mutableStateOf(currentUserGoal.fiberGoal_g.toString()) }
    var sodiumInput by remember { mutableStateOf(currentUserGoal.sodiumGoal_mg.toString()) }

    var inputError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Your Daily Goals") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                GoalTextField(label = "Calories (kcal)", value = caloriesInput, onValueChange = { caloriesInput = it; inputError = null }, isError = inputError?.contains("Calories") == true)
                GoalTextField(label = "Carbohydrates (g)", value = carbsInput, onValueChange = { carbsInput = it; inputError = null }, isError = inputError?.contains("Carbs") == true)
                GoalTextField(label = "Protein (g)", value = proteinInput, onValueChange = { proteinInput = it; inputError = null }, isError = inputError?.contains("Protein") == true)
                GoalTextField(label = "Fat (g)", value = fatInput, onValueChange = { fatInput = it; inputError = null }, isError = inputError?.contains("Fat") == true)
                GoalTextField(label = "Fiber (g)", value = fiberInput, onValueChange = { fiberInput = it; inputError = null }, isError = inputError?.contains("Fiber") == true)
                GoalTextField(label = "Sodium (mg)", value = sodiumInput, onValueChange = { sodiumInput = it; inputError = null }, isError = inputError?.contains("Sodium") == true)

                if (inputError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(inputError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val calories = caloriesInput.toIntOrNull()
                    val carbs = carbsInput.toIntOrNull()
                    val protein = proteinInput.toIntOrNull()
                    val fat = fatInput.toIntOrNull()
                    val fiber = fiberInput.toIntOrNull()
                    val sodium = sodiumInput.toIntOrNull()

                    if (calories == null || calories <= 0) {
                        inputError = "Calories must be a positive number."
                        return@Button
                    }
                    if (carbs == null || carbs < 0) {
                        inputError = "Carbs must be a non-negative number."
                        return@Button
                    }
                    if (protein == null || protein < 0) {
                        inputError = "Protein must be a non-negative number."
                        return@Button
                    }
                    if (fat == null || fat < 0) {
                        inputError = "Fat must be a non-negative number."
                        return@Button
                    }
                    if (fiber == null || fiber < 0) {
                        inputError = "Fiber must be a non-negative number."
                        return@Button
                    }
                    if (sodium == null || sodium < 0) {
                        inputError = "Sodium must be a non-negative number."
                        return@Button
                    }

                    onSetGoal(
                        UserGoal(
                            id = currentUserGoal.id, // Keep the same ID
                            caloriesGoal = calories,
                            carbsGoal_g = carbs,
                            proteinGoal_g = protein,
                            fatGoal_g = fat,
                            fiberGoal_g = fiber,
                            sodiumGoal_mg = sodium
                        )
                    )
                }
            ) { Text("Set Goals") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun GoalTextField(label: String, value: String, onValueChange: (String) -> Unit, isError: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = isError,
        singleLine = true
    )
    Spacer(modifier = Modifier.height(8.dp))
}
