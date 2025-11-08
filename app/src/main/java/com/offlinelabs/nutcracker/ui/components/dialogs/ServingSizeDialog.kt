package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.FoodInfo
import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
import com.offlinelabs.nutcracker.data.mapper.FoodInfoToMealMapper
import androidx.compose.ui.res.stringResource
import com.offlinelabs.nutcracker.ui.theme.getContrastingTextColor
import com.offlinelabs.nutcracker.ui.theme.brandAccentShade
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServingSizeDialog(
    foodInfo: FoodInfo,
    barcode: String? = null,
    onDismiss: () -> Unit,
    onSaveAndCheckIn: (servingSizeValue: Double, servingSizeUnit: ServingSizeUnit) -> Unit,
    onJustSave: (servingSizeValue: Double, servingSizeUnit: ServingSizeUnit) -> Unit,
    onJustCheckIn: (servingSizeValue: Double, servingSizeUnit: ServingSizeUnit) -> Unit
) {
    var servingSizeValue by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf<ServingSizeUnit?>(null) }
    var showUnitSelector by remember { mutableStateOf(false) }
    
    // Initialize with suggested serving size
    LaunchedEffect(foodInfo) {
        val suggested = FoodInfoToMealMapper.getSuggestedServingSize(foodInfo)
        servingSizeValue = suggested.first.toString()
        selectedUnit = suggested.second
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.add_to_my_meals),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Food name
                Text(
                    text = foodInfo.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                foodInfo.brand?.let { brand ->
                    Text(
                        text = stringResource(R.string.brand_label, brand),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Serving size input
                Text(
                    text = stringResource(R.string.serving_size),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = servingSizeValue,
                        onValueChange = { servingSizeValue = it },
                        label = { Text(stringResource(R.string.amount)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        )
                    )
                    
                    OutlinedButton(
                        onClick = { showUnitSelector = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = selectedUnit?.abbreviation ?: stringResource(R.string.select_unit),
                            color = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                        )
                    }
                }
                
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
                            text = stringResource(R.string.nutrition_preview),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = brandAccentShade(4)
                        )
                        
                        val multiplier = try {
                            servingSizeValue.toDoubleOrNull() ?: 0.0
                        } catch (e: Exception) { 0.0 }
                        
                        val nutrition = foodInfo.nutrition
                        nutrition.calories?.let { 
                            Row {
                                Text(
                                    text = stringResource(R.string.calories_label) + ": ",
                                    color = brandAccentShade(2)
                                )
                                Text(
                                    text = "${(it * multiplier / 100).toInt()} kcal",
                                    color = brandAccentShade(3)
                                )
                            }
                        }
                        nutrition.proteins?.let { 
                            Row {
                                Text(
                                    text = stringResource(R.string.protein_label) + ": ",
                                    color = brandAccentShade(2)
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", it * multiplier / 100)}g",
                                    color = brandAccentShade(3)
                                )
                            }
                        }
                        nutrition.carbohydrates?.let { 
                            Row {
                                Text(
                                    text = stringResource(R.string.carbs_g) + ": ",
                                    color = brandAccentShade(2)
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", it * multiplier / 100)}g",
                                    color = brandAccentShade(3)
                                )
                            }
                        }
                        nutrition.fat?.let { 
                            Row {
                                Text(
                                    text = stringResource(R.string.fat_g) + ": ",
                                    color = brandAccentShade(2)
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", it * multiplier / 100)}g",
                                    color = brandAccentShade(3)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val parsedValue = servingSizeValue.toDoubleOrNull()
            val isEnabled = parsedValue != null && parsedValue > 0 && selectedUnit != null
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Save and Check-in button
                Button(
                    onClick = {
                        val value = parsedValue ?: 0.0
                        val unit = selectedUnit ?: ServingSizeUnit.GRAMS
                        if (value > 0) {
                            onSaveAndCheckIn(value, unit)
                        }
                    },
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brandAccentShade(0)
                    )
                ) {
                    Text(stringResource(R.string.save_and_check_in))
                }
                
                // Just Save button
                Button(
                    onClick = {
                        val value = parsedValue ?: 0.0
                        val unit = selectedUnit ?: ServingSizeUnit.GRAMS
                        if (value > 0) {
                            onJustSave(value, unit)
                        }
                    },
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brandAccentShade(1)
                    )
                ) {
                    Text(stringResource(R.string.just_save))
                }
                
                // Just Check-in button
                Button(
                    onClick = {
                        val value = parsedValue ?: 0.0
                        val unit = selectedUnit ?: ServingSizeUnit.GRAMS
                        if (value > 0) {
                            onJustCheckIn(value, unit)
                        }
                    },
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brandAccentShade(2)
                    )
                ) {
                    Text(stringResource(R.string.just_check_in))
                }
                
                // Cancel button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }
    )
    
    // Unit selector dialog
    if (showUnitSelector) {
        UnitSelectorDialog(
            onDismiss = { showUnitSelector = false },
            onUnitSelected = { unit ->
                selectedUnit = unit
                showUnitSelector = false
            }
        )
    }
}

@Composable
private fun UnitSelectorDialog(
    onDismiss: () -> Unit,
    onUnitSelected: (ServingSizeUnit) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_unit)) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                // Common units
                item {
                    Text(
                        text = stringResource(R.string.common_units),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(ServingSizeUnit.getCommonUnits()) { unit ->
                    UnitItem(
                        unit = unit,
                        onClick = { onUnitSelected(unit) }
                    )
                }
                
                // Weight units
                item {
                    Text(
                        text = stringResource(R.string.weight_units),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(ServingSizeUnit.getWeightUnits()) { unit ->
                    UnitItem(
                        unit = unit,
                        onClick = { onUnitSelected(unit) }
                    )
                }
                
                // Volume units
                item {
                    Text(
                        text = stringResource(R.string.volume_units),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(ServingSizeUnit.getVolumeUnits()) { unit ->
                    UnitItem(
                        unit = unit,
                        onClick = { onUnitSelected(unit) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                )
            }
        }
    )
}

@Composable
private fun UnitItem(
    unit: ServingSizeUnit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = unit.abbreviation,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = getUnitDescription(unit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun getUnitDescription(unit: ServingSizeUnit): String {
    return when (unit) {
        ServingSizeUnit.GRAMS -> stringResource(R.string.unit_grams)
        ServingSizeUnit.KILOGRAMS -> stringResource(R.string.unit_kilograms)
        ServingSizeUnit.POUNDS -> stringResource(R.string.unit_pounds)
        ServingSizeUnit.OUNCES -> stringResource(R.string.unit_ounces)
        ServingSizeUnit.MILLILITERS -> stringResource(R.string.unit_milliliters)
        ServingSizeUnit.LITERS -> stringResource(R.string.unit_liters)
        ServingSizeUnit.CUPS -> stringResource(R.string.unit_cups)
        ServingSizeUnit.FLUID_OUNCES -> stringResource(R.string.unit_fluid_ounces)
        ServingSizeUnit.TABLESPOONS -> stringResource(R.string.unit_tablespoons)
        ServingSizeUnit.TEASPOONS -> stringResource(R.string.unit_teaspoons)
        ServingSizeUnit.PIECES -> stringResource(R.string.unit_pieces)
        ServingSizeUnit.SLICES -> stringResource(R.string.unit_slices)
        ServingSizeUnit.UNITS -> stringResource(R.string.unit_units)
        ServingSizeUnit.SERVINGS -> stringResource(R.string.unit_servings)
        ServingSizeUnit.PORTIONS -> stringResource(R.string.unit_portions)
    }
}


