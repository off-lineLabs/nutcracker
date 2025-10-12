package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.UserGoal
import com.offlinelabs.nutcracker.ui.theme.getContrastingTextColor

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
    
    // Store string resources in variables to avoid calling stringResource in non-composable contexts
    val caloriesLabel = stringResource(R.string.calories_kcal)
    val carbsLabel = stringResource(R.string.carbohydrates_g)
    val proteinLabel = stringResource(R.string.protein_g_goal)
    val fatLabel = stringResource(R.string.fat_g_goal)
    val fiberLabel = stringResource(R.string.fiber_g_goal)
    val sodiumLabel = stringResource(R.string.sodium_mg_goal)
    
    val validationCaloriesPositive = stringResource(R.string.validation_calories_positive)
    val validationCarbsNonNegative = stringResource(R.string.validation_carbs_non_negative)
    val validationProteinNonNegative = stringResource(R.string.validation_protein_non_negative)
    val validationFatNonNegative = stringResource(R.string.validation_fat_non_negative)
    val validationFiberNonNegative = stringResource(R.string.validation_fiber_non_negative)
    val validationSodiumNonNegative = stringResource(R.string.validation_sodium_non_negative)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_your_daily_goals)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                GoalTextField(label = caloriesLabel, value = caloriesInput, onValueChange = { caloriesInput = it; inputError = null }, isError = inputError?.contains(caloriesLabel) == true)
                GoalTextField(label = carbsLabel, value = carbsInput, onValueChange = { carbsInput = it; inputError = null }, isError = inputError?.contains(carbsLabel) == true)
                GoalTextField(label = proteinLabel, value = proteinInput, onValueChange = { proteinInput = it; inputError = null }, isError = inputError?.contains(proteinLabel) == true)
                GoalTextField(label = fatLabel, value = fatInput, onValueChange = { fatInput = it; inputError = null }, isError = inputError?.contains(fatLabel) == true)
                GoalTextField(label = fiberLabel, value = fiberInput, onValueChange = { fiberInput = it; inputError = null }, isError = inputError?.contains(fiberLabel) == true)
                GoalTextField(label = sodiumLabel, value = sodiumInput, onValueChange = { sodiumInput = it; inputError = null }, isError = inputError?.contains(sodiumLabel) == true)

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
                        inputError = validationCaloriesPositive
                        return@Button
                    }
                    if (carbs == null || carbs < 0) {
                        inputError = validationCarbsNonNegative
                        return@Button
                    }
                    if (protein == null || protein < 0) {
                        inputError = validationProteinNonNegative
                        return@Button
                    }
                    if (fat == null || fat < 0) {
                        inputError = validationFatNonNegative
                        return@Button
                    }
                    if (fiber == null || fiber < 0) {
                        inputError = validationFiberNonNegative
                        return@Button
                    }
                    if (sodium == null || sodium < 0) {
                        inputError = validationSodiumNonNegative
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
            ) { Text(stringResource(R.string.set_goals)) }
        },
        dismissButton = {
            Button(onClick = onDismiss) { 
                Text(
                    text = stringResource(R.string.cancel),
                    color = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                )
            }
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
