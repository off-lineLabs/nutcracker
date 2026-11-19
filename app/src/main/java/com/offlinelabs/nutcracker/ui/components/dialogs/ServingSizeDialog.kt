package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.FoodInfo
import com.offlinelabs.nutcracker.data.model.NovaClassification
import com.offlinelabs.nutcracker.data.model.NutritionInfo
import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
import com.offlinelabs.nutcracker.data.mapper.FoodInfoToMealMapper
import androidx.compose.ui.res.stringResource
import com.offlinelabs.nutcracker.ui.components.form.FormColors
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import com.offlinelabs.nutcracker.ui.components.form.FormDialog
import com.offlinelabs.nutcracker.ui.components.form.FormDialogStackedActions
import com.offlinelabs.nutcracker.ui.components.form.FormDialogSurface
import com.offlinelabs.nutcracker.ui.components.form.FormRowIcon
import com.offlinelabs.nutcracker.ui.components.form.FormSection
import com.offlinelabs.nutcracker.ui.components.form.FormSectionDivider
import com.offlinelabs.nutcracker.ui.components.form.FormStackedAction
import com.offlinelabs.nutcracker.ui.components.form.QuantityUnitInput
import com.offlinelabs.nutcracker.ui.components.form.formatDecimal
import com.offlinelabs.nutcracker.ui.components.form.parseDecimal
import com.offlinelabs.nutcracker.ui.components.form.validateDecimal
import com.offlinelabs.nutcracker.ui.theme.FoodLogTheme
import com.offlinelabs.nutcracker.ui.theme.getContrastingTextColor
import com.offlinelabs.nutcracker.ui.theme.brandAccentShade
import com.offlinelabs.nutcracker.ui.theme.nutrientCarbsColor
import com.offlinelabs.nutcracker.ui.theme.nutrientFatColor
import com.offlinelabs.nutcracker.ui.theme.nutrientProteinColor

@Composable
fun ServingSizeDialog(
    foodInfo: FoodInfo,
    barcode: String? = null,
    onDismiss: () -> Unit,
    onSaveAndCheckIn: (servingSizeValue: Double, servingSizeUnit: ServingSizeUnit) -> Unit,
    onJustSave: (servingSizeValue: Double, servingSizeUnit: ServingSizeUnit) -> Unit,
    onJustCheckIn: (servingSizeValue: Double, servingSizeUnit: ServingSizeUnit) -> Unit
) {
    // Initialize with the suggested serving size; survives rotation.
    val suggested = remember(foodInfo) { FoodInfoToMealMapper.getSuggestedServingSize(foodInfo) }
    var amount by rememberSaveable(foodInfo) { mutableStateOf(formatDecimal(suggested.first)) }
    var unit by rememberSaveable(foodInfo) { mutableStateOf(suggested.second) }

    val error = validateDecimal(amount, required = true, allowZero = false)
    val parsed = parseDecimal(amount)
    val isValid = error == null && parsed != null && parsed > 0

    fun confirm(callback: (Double, ServingSizeUnit) -> Unit) {
        val value = parsed ?: return
        if (isValid) callback(value, unit)
    }

    FormDialog(
        title = stringResource(R.string.add_to_my_meals),
        onDismissRequest = onDismiss,
        actions = {
            FormDialogStackedActions(
                primaryText = stringResource(R.string.save_and_check_in),
                onPrimary = { confirm(onSaveAndCheckIn) },
                secondary = listOf(
                    FormStackedAction(stringResource(R.string.just_save)) { confirm(onJustSave) },
                    FormStackedAction(stringResource(R.string.just_check_in)) { confirm(onJustCheckIn) }
                ),
                onDismiss = onDismiss,
                confirmEnabled = isValid
            )
        }
    ) {
        ServingSizeContent(
            foodInfo = foodInfo,
            amount = amount,
            onAmountChange = { amount = it },
            unit = unit,
            onUnitChange = { unit = it },
            errorText = error?.let { stringResource(it.messageRes) }
        )
    }
}

/** Body of [ServingSizeDialog]: food header, amount + unit and the nutrition preview. */
@Composable
private fun ServingSizeContent(
    foodInfo: FoodInfo,
    amount: String,
    onAmountChange: (String) -> Unit,
    unit: ServingSizeUnit,
    onUnitChange: (ServingSizeUnit) -> Unit,
    errorText: String?,
    modifier: Modifier = Modifier
) {
    val colors = FormDefaults.colors()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = foodInfo.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.content,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            foodInfo.brand?.let { brand ->
                Text(
                    text = stringResource(R.string.brand_label, brand),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.contentSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        QuantityUnitInput(
            label = stringResource(R.string.serving_size),
            amount = amount,
            onAmountChange = onAmountChange,
            unit = unit,
            onUnitChange = onUnitChange,
            errorText = errorText,
            imeAction = ImeAction.Done,
            colors = colors
        )

        // Same math as before: values are relative to the amount typed, from per-100 data.
        val multiplier = parseDecimal(amount) ?: 0.0
        val nutrition = foodInfo.nutrition
        FormSection(header = stringResource(R.string.nutrition_preview), colors = colors) {
            var first = true
            @Composable
            fun row(content: @Composable () -> Unit) {
                if (!first) FormSectionDivider(colors = colors)
                first = false
                content()
            }
            nutrition.calories?.let {
                row {
                    PreviewValueRow(
                        stringResource(R.string.calories_label),
                        (it * multiplier / 100).toInt().toString(),
                        stringResource(R.string.unit_kcal),
                        Icons.Filled.LocalFireDepartment, brandAccentShade(2), colors
                    )
                }
            }
            nutrition.carbohydrates?.let {
                row {
                    PreviewValueRow(
                        stringResource(R.string.carbohydrates_label),
                        formatDecimal(it * multiplier / 100, maxFractionDigits = 1),
                        stringResource(R.string.unit_g),
                        Icons.Filled.Grain, nutrientCarbsColor(), colors
                    )
                }
            }
            nutrition.proteins?.let {
                row {
                    PreviewValueRow(
                        stringResource(R.string.protein_label),
                        formatDecimal(it * multiplier / 100, maxFractionDigits = 1),
                        stringResource(R.string.unit_g),
                        Icons.Filled.FitnessCenter, nutrientProteinColor(), colors
                    )
                }
            }
            nutrition.fat?.let {
                row {
                    PreviewValueRow(
                        stringResource(R.string.fat_label),
                        formatDecimal(it * multiplier / 100, maxFractionDigits = 1),
                        stringResource(R.string.unit_g),
                        Icons.Filled.OilBarrel, nutrientFatColor(), colors
                    )
                }
            }
        }
    }
}

/** Read-only counterpart of a numeric input row: `[icon] Label ........ value unit`. */
@Composable
private fun PreviewValueRow(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    iconTint: Color,
    colors: FormColors
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(FormDefaults.LeadingSize), contentAlignment = Alignment.Center) {
            FormRowIcon(icon, iconTint)
        }
        Spacer(Modifier.width(if (FormDefaults.isCompactWidth) 8.dp else 10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.content,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$value $unit",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.content,
            maxLines = 1
        )
    }
}

@Composable
fun UnitSelectorDialog(
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
                    text = stringResource(unit.abbreviationResId),
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

// ----- Previews -----

@Composable
private fun ServingSizePreviewContent(amount: String, error: String?) {
    val food = FoodInfo(
        name = "Crunchy granola with almonds and honey",
        brand = "Nature Co.",
        imageUrl = null,
        nutritionImageUrl = null,
        nutrition = NutritionInfo(
            calories = 450.0, fat = 16.5, saturatedFat = null, carbohydrates = 62.0, proteins = 10.0,
            sodium = null, fiber = null, sugars = null, cholesterol = null,
            vitaminC = null, calcium = null, iron = null
        ),
        novaClassification = NovaClassification(1, ""),
        greenScore = null,
        nutriscore = null,
        ingredients = null,
        quantity = null,
        servingSize = null
    )
    Box(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
        FormDialogSurface(
            title = stringResource(R.string.add_to_my_meals),
            actions = {
                FormDialogStackedActions(
                    primaryText = stringResource(R.string.save_and_check_in),
                    onPrimary = {},
                    secondary = listOf(
                        FormStackedAction(stringResource(R.string.just_save)) {},
                        FormStackedAction(stringResource(R.string.just_check_in)) {}
                    ),
                    onDismiss = {},
                    confirmEnabled = error == null
                )
            }
        ) {
            ServingSizeContent(food, amount, {}, ServingSizeUnit.GRAMS, {}, error)
        }
    }
}

@Preview(name = "Serving size - light", widthDp = 360, heightDp = 720, showBackground = true, backgroundColor = 0xFFFAFBFC)
@Composable
private fun ServingSizeLightPreview() {
    FoodLogTheme(darkTheme = false) { ServingSizePreviewContent("100", null) }
}

@Preview(name = "Serving size - dark, error", widthDp = 360, heightDp = 720, showBackground = true, backgroundColor = 0xFF0B1426)
@Composable
private fun ServingSizeDarkErrorPreview() {
    FoodLogTheme(darkTheme = true) { ServingSizePreviewContent("0", stringResource(R.string.form_error_must_be_positive)) }
}
