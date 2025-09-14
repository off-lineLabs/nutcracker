package com.example.template.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.template.data.model.FoodInfo
import com.example.template.data.model.ServingSizeUnit
import com.example.template.data.mapper.FoodInfoToMealMapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServingSizeDialog(
    foodInfo: FoodInfo,
    barcode: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (servingSizeValue: Double, servingSizeUnit: ServingSizeUnit) -> Unit
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
                text = "Add to My Meals",
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
                        text = "Brand: $brand",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Serving size input
                Text(
                    text = "Serving Size",
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
                        label = { Text("Amount") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        )
                    )
                    
                    OutlinedButton(
                        onClick = { showUnitSelector = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(selectedUnit?.abbreviation ?: "Select Unit")
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
                            text = "Nutrition Preview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        val multiplier = try {
                            servingSizeValue.toDoubleOrNull() ?: 0.0
                        } catch (e: Exception) { 0.0 }
                        
                        val nutrition = foodInfo.nutrition
                        nutrition.calories?.let { 
                            Text("Calories: ${(it * multiplier / 100).toInt()} kcal")
                        }
                        nutrition.proteins?.let { 
                            Text("Protein: ${String.format("%.1f", it * multiplier / 100)}g")
                        }
                        nutrition.carbohydrates?.let { 
                            Text("Carbs: ${String.format("%.1f", it * multiplier / 100)}g")
                        }
                        nutrition.fat?.let { 
                            Text("Fat: ${String.format("%.1f", it * multiplier / 100)}g")
                        }
                    }
                }
            }
        },
        confirmButton = {
            val parsedValue = servingSizeValue.toDoubleOrNull()
            Button(
                onClick = {
                    val value = parsedValue ?: 0.0
                    val unit = selectedUnit ?: ServingSizeUnit.GRAMS
                    if (value > 0) {
                        onConfirm(value, unit)
                    }
                },
                enabled = parsedValue != null && parsedValue > 0 && selectedUnit != null
            ) {
                Text("Add to My Meals")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
        title = { Text("Select Unit") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                // Common units
                item {
                    Text(
                        text = "Common Units",
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
                        text = "Weight Units",
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
                        text = "Volume Units",
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
                Text("Cancel")
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

private fun getUnitDescription(unit: ServingSizeUnit): String {
    return when (unit) {
        ServingSizeUnit.GRAMS -> "Grams"
        ServingSizeUnit.KILOGRAMS -> "Kilograms"
        ServingSizeUnit.POUNDS -> "Pounds"
        ServingSizeUnit.OUNCES -> "Ounces"
        ServingSizeUnit.MILLILITERS -> "Milliliters"
        ServingSizeUnit.LITERS -> "Liters"
        ServingSizeUnit.CUPS -> "Cups"
        ServingSizeUnit.FLUID_OUNCES -> "Fluid Ounces"
        ServingSizeUnit.TABLESPOONS -> "Tablespoons"
        ServingSizeUnit.TEASPOONS -> "Teaspoons"
        ServingSizeUnit.PIECES -> "Pieces"
        ServingSizeUnit.SLICES -> "Slices"
        ServingSizeUnit.UNITS -> "Units"
        ServingSizeUnit.SERVINGS -> "Servings"
        ServingSizeUnit.PORTIONS -> "Portions"
    }
}
