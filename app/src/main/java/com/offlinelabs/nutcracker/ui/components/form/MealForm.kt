package com.offlinelabs.nutcracker.ui.components.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.ServingSizeUnit
import com.offlinelabs.nutcracker.ui.theme.FoodLogTheme
import com.offlinelabs.nutcracker.ui.theme.brandAccentShade
import com.offlinelabs.nutcracker.ui.theme.getContrastingIconColor
import com.offlinelabs.nutcracker.ui.theme.nutrientCarbsColor
import com.offlinelabs.nutcracker.ui.theme.nutrientFatColor
import com.offlinelabs.nutcracker.ui.theme.nutrientFiberColor
import com.offlinelabs.nutcracker.ui.theme.nutrientProteinColor
import java.util.Locale

/**
 * The add/edit meal form body: Name, "Nutrition per <qty> <unit>" (with the link/rescale toggle
 * when editing), then the nutrition card (Calories, Carbs, Protein, Fat, Fiber in g, Salt in mg).
 * Place it inside a [FormDialog]; all state and validation live in [state].
 */
@Composable
fun MealForm(state: MealFormState, modifier: Modifier = Modifier) {
    val colors = FormDefaults.colors()

    @Composable
    fun errorFor(field: MealField): String? = state.visibleError(field)?.let { stringResource(it.messageRes) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FormTextField(
            value = state.text(MealField.Name),
            onValueChange = { state.onValueChange(MealField.Name, it) },
            label = stringResource(R.string.meal_name),
            placeholder = stringResource(R.string.meal_name_placeholder),
            errorText = errorFor(MealField.Name),
            colors = colors
        )

        Column {
            QuantityUnitInput(
                label = stringResource(R.string.meal_form_nutrition_per),
                amount = state.text(MealField.Quantity),
                onAmountChange = { state.onValueChange(MealField.Quantity, it) },
                unit = state.unit,
                onUnitChange = state::onUnitChange,
                errorText = errorFor(MealField.Quantity),
                trailing = if (state.canRescale) {
                    { RescaleToggle(linked = state.isLinked, onToggle = state::toggleLink, colors = colors) }
                } else {
                    null
                },
                colors = colors
            )
            if (state.isLinked) {
                Text(
                    text = stringResource(R.string.meal_form_rescale_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.contentSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }

        val sectionColor = colors.section
        FormSection(colors = colors) {
            NutrientRow(
                state, MealField.Calories, R.string.calories_label, R.string.unit_kcal,
                Icons.Filled.LocalFireDepartment, brandAccentShade(2), colors, emphasized = true,
                // Required: no "0" hint, so an empty field doesn't look filled in.
                placeholder = null
            )
            FormSectionDivider(colors = colors)
            NutrientRow(state, MealField.Carbs, R.string.carbohydrates_label, R.string.unit_g, Icons.Filled.Grain, nutrientCarbsColor(), colors)
            NutrientRow(state, MealField.Protein, R.string.protein_label, R.string.unit_g, Icons.Filled.FitnessCenter, nutrientProteinColor(), colors)
            NutrientRow(state, MealField.Fat, R.string.fat_label, R.string.unit_g, Icons.Filled.OilBarrel, nutrientFatColor(), colors)
            NutrientRow(state, MealField.Fiber, R.string.fiber_label, R.string.unit_g, Icons.Filled.Park, nutrientFiberColor(), colors)
            NutrientRow(
                // Salt in mg, as stored in Meal.sodium_mg.
                state, MealField.Salt, R.string.sodium_label, R.string.unit_mg,
                Icons.Filled.Water, getContrastingIconColor(sectionColor), colors, imeAction = ImeAction.Done
            )
        }
    }
}

@Composable
private fun NutrientRow(
    state: MealFormState,
    field: MealField,
    labelRes: Int,
    unitRes: Int,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    colors: FormColors,
    emphasized: Boolean = false,
    placeholder: String? = "0",
    imeAction: ImeAction = ImeAction.Next
) {
    NumericInputRow(
        label = stringResource(labelRes),
        value = state.text(field),
        onValueChange = { state.onValueChange(field, it) },
        unit = stringResource(unitRes),
        leading = { FormRowIcon(icon, iconTint) },
        errorText = state.visibleError(field)?.let { stringResource(it.messageRes) },
        emphasized = emphasized,
        placeholder = placeholder,
        imeAction = imeAction,
        colors = colors
    )
}

/** Link/unlink icon button: when linked, nutrients rescale with the serving quantity. */
@Composable
private fun RescaleToggle(linked: Boolean, onToggle: () -> Unit, colors: FormColors) {
    IconButton(onClick = onToggle) {
        Icon(
            painter = painterResource(if (linked) R.drawable.link else R.drawable.link_off),
            contentDescription = stringResource(if (linked) R.string.unlink else R.string.link),
            tint = if (linked) colors.accent else colors.contentSecondary,
            modifier = Modifier.size(22.dp)
        )
    }
}

// ----- Previews -----

private val previewMeal = Meal(
    id = 1,
    name = "Granola",
    calories = 250,
    carbohydrates_g = 30.0,
    protein_g = 12.5,
    fat_g = 8.0,
    fiber_g = 3.0,
    sodium_mg = 400.0,
    servingSize_value = 100.0,
    servingSize_unit = ServingSizeUnit.GRAMS
)

@Composable
private fun MealFormPreviewContent(state: MealFormState, title: String, onDelete: (() -> Unit)?) {
    // Same loose, centered constraints and margins FormDialog gives the card.
    Box(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
        FormDialogSurface(
            title = title,
            actions = {
                FormDialogActions(
                    confirmText = stringResource(if (onDelete == null) R.string.add_meal else R.string.save),
                    onConfirm = {},
                    onDismiss = {},
                    confirmEnabled = state.isValid,
                    onDelete = onDelete
                )
            }
        ) {
            MealForm(state)
        }
    }
}

@Preview(name = "Add meal - dark", widthDp = 360, heightDp = 720, showBackground = true, backgroundColor = 0xFF0B1426)
@Composable
private fun AddMealFormDarkPreview() {
    FoodLogTheme(darkTheme = true) {
        val state = MealFormState(null, Locale.US).apply {
            onValueChange(MealField.Calories, "abc")
        }
        MealFormPreviewContent(state, stringResource(R.string.add_new_meal), onDelete = null)
    }
}

@Preview(name = "Add meal - light", widthDp = 360, heightDp = 720, showBackground = true, backgroundColor = 0xFFFAFBFC)
@Composable
private fun AddMealFormLightPreview() {
    FoodLogTheme(darkTheme = false) {
        val state = MealFormState(null, Locale.US).apply {
            onValueChange(MealField.Name, "Granola")
            onValueChange(MealField.Calories, "250")
            onValueChange(MealField.Fat, "-3")
        }
        MealFormPreviewContent(state, stringResource(R.string.add_new_meal), onDelete = null)
    }
}

@Preview(name = "Edit meal - dark, pt-BR", widthDp = 360, heightDp = 720, showBackground = true, backgroundColor = 0xFF0B1426, locale = "pt-rBR")
@Composable
private fun EditMealFormDarkPreview() {
    FoodLogTheme(darkTheme = true) {
        val state = MealFormState(previewMeal, Locale.forLanguageTag("pt-BR")).apply { toggleLink() }
        MealFormPreviewContent(state, stringResource(R.string.edit_meal), onDelete = {})
    }
}

@Preview(name = "Edit meal - light", widthDp = 360, heightDp = 720, showBackground = true, backgroundColor = 0xFFFAFBFC)
@Composable
private fun EditMealFormLightPreview() {
    FoodLogTheme(darkTheme = false) {
        MealFormPreviewContent(MealFormState(previewMeal, Locale.US), stringResource(R.string.edit_meal), onDelete = {})
    }
}

// Narrowest supported phone: "Carbohidratos" / "Carboidratos" must stay on one line.
@Preview(name = "Edit meal - light, es-ES, 360dp", widthDp = 360, heightDp = 720, showBackground = true, backgroundColor = 0xFFFAFBFC, locale = "es-rES")
@Preview(name = "Edit meal - light, pt-BR, 360dp", widthDp = 360, heightDp = 720, showBackground = true, backgroundColor = 0xFFFAFBFC, locale = "pt-rBR")
@Preview(name = "Edit meal - light, es-ES, 360dp, font 1.3", widthDp = 360, heightDp = 720, showBackground = true, backgroundColor = 0xFFFAFBFC, locale = "es-rES", fontScale = 1.3f)
@Composable
private fun EditMealFormNarrowPreview() {
    FoodLogTheme(darkTheme = false) {
        MealFormPreviewContent(MealFormState(previewMeal, Locale.US), stringResource(R.string.edit_meal), onDelete = {})
    }
}

// Landscape with the keyboard open: title, content and actions scroll together.
@Preview(name = "Add meal - compact height", widthDp = 800, heightDp = 160, showBackground = true, backgroundColor = 0xFF0B1426)
@Composable
private fun AddMealFormCompactHeightPreview() {
    FoodLogTheme(darkTheme = true) {
        MealFormPreviewContent(MealFormState(null, Locale.US), stringResource(R.string.add_new_meal), onDelete = null)
    }
}
