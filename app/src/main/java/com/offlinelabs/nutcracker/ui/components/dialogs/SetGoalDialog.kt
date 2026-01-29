package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.UserGoal
import com.offlinelabs.nutcracker.ui.components.CombinedMacroBar
import com.offlinelabs.nutcracker.ui.theme.appTextPrimaryColor
import com.offlinelabs.nutcracker.ui.theme.appTextSecondaryColor
import com.offlinelabs.nutcracker.ui.theme.getContrastingTextColor
import com.offlinelabs.nutcracker.utils.MacroCalculator
import com.offlinelabs.nutcracker.utils.TEFCalculator
import kotlin.math.roundToInt

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
    
    // State for TEF
    var useTEFAdjustment by remember { mutableStateOf(false) }
    var inputError by remember { mutableStateOf<String?>(null) }
    
    // Store current macro ratios
    val currentCarbsGrams = carbsInput.toDoubleOrNull() ?: 0.0
    val currentProteinGrams = proteinInput.toDoubleOrNull() ?: 0.0
    val currentFatGrams = fatInput.toDoubleOrNull() ?: 0.0
    
    val currentRatios = remember(currentCarbsGrams, currentProteinGrams, currentFatGrams) {
        MacroCalculator.calculateCurrentRatios(currentCarbsGrams, currentProteinGrams, currentFatGrams)
    }
    
    // TEF bonus calculation
    val tefBonus = remember(currentCarbsGrams, currentProteinGrams, currentFatGrams) {
        if (useTEFAdjustment) {
            TEFCalculator.calculateTEFBonus(currentProteinGrams, currentCarbsGrams, currentFatGrams).roundToInt()
        } else {
            0
        }
    }
    
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
                // 1. Calorie Input at Top with actual calculated value
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = caloriesInput,
                        onValueChange = { newValue ->
                            caloriesInput = newValue
                            inputError = null
                        },
                        label = { Text(caloriesLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = inputError?.contains(caloriesLabel) == true,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Always show actual NET kcal to prevent layout shift
                    // With TEF: show NET calories (what body absorbs) = GROSS - TEF bonus
                    // Without TEF: show GROSS calories directly
                    val grossKcal = MacroCalculator.calculateKcalFromMacros(
                        currentCarbsGrams, 
                        currentProteinGrams, 
                        currentFatGrams, 
                        useTEF = false // Always calculate gross first
                    )
                    val actualKcal = if (useTEFAdjustment) {
                        // NET = GROSS - TEF bonus
                        val tefBonus = TEFCalculator.calculateTEFBonus(
                            currentProteinGrams,
                            currentCarbsGrams,
                            currentFatGrams
                        ).toInt()
                        grossKcal - tefBonus
                    } else {
                        grossKcal
                    }
                    val targetKcal = caloriesInput.toIntOrNull() ?: 0
                    val difference = actualKcal - targetKcal
                    val differenceText = if (difference > 0) "+$difference" else "$difference"
                    
                    // Always render to prevent layout shift, but hide text if matches
                    Text(
                        text = if (actualKcal != targetKcal && targetKcal > 0) {
                            "Actual: $actualKcal kcal ($differenceText)"
                        } else {
                            "" // Empty but still takes up space
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (kotlin.math.abs(difference) > 50) MaterialTheme.colorScheme.error else appTextSecondaryColor(),
                        modifier = Modifier
                            .padding(start = 16.dp, top = 4.dp)
                            .height(16.dp) // Fixed height to prevent layout shift
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Calculate suggested values for nudging
                val suggestedMacros = MacroCalculator.calculateMacrosFromKcal(
                    caloriesInput.toIntOrNull() ?: 2000,
                    currentRatios,
                    useTEF = useTEFAdjustment
                )
                
                // Combined Macro Bar with nudge markers
                CombinedMacroBar(
                    carbsGrams = currentCarbsGrams,
                    proteinGrams = currentProteinGrams,
                    fatGrams = currentFatGrams,
                    totalKcal = caloriesInput.toIntOrNull() ?: 2000,
                    suggestedCarbsGrams = suggestedMacros.carbsGrams,
                    suggestedProteinGrams = suggestedMacros.proteinGrams,
                    suggestedFatGrams = suggestedMacros.fatGrams,
                    showSuggested = true,
                    onMacrosChanged = { carbs, protein, fat ->
                        carbsInput = carbs.roundToInt().toString()
                        proteinInput = protein.roundToInt().toString()
                        fatInput = fat.roundToInt().toString()
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // TEF Toggle with Bonus Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.thermic_effect_tef),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = appTextPrimaryColor()
                        )
                        Text(
                            text = stringResource(R.string.higher_macros_same_kcal),
                            fontSize = 12.sp,
                            color = appTextSecondaryColor()
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (useTEFAdjustment && tefBonus > 0) {
                            Text(
                                text = "+$tefBonus kcal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(60.dp)
                            )
                        }
                        Switch(
                            checked = useTEFAdjustment,
                            onCheckedChange = { newValue ->
                                useTEFAdjustment = newValue
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Sodium and Fiber in Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoalTextField(
                        label = sodiumLabel,
                        value = sodiumInput,
                        onValueChange = { sodiumInput = it; inputError = null },
                        isError = inputError?.contains(sodiumLabel) == true,
                        modifier = Modifier.weight(1f)
                    )
                    GoalTextField(
                        label = fiberLabel,
                        value = fiberInput,
                        onValueChange = { fiberInput = it; inputError = null },
                        isError = inputError?.contains(fiberLabel) == true,
                        modifier = Modifier.weight(1f)
                    )
                }
                
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
private fun GoalTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = isError,
        singleLine = true,
        modifier = modifier
    )
}
