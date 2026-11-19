package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.UserGoal
import com.offlinelabs.nutcracker.ui.components.CombinedMacroBar
import com.offlinelabs.nutcracker.ui.components.form.FormColors
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import com.offlinelabs.nutcracker.ui.components.form.FormDialog
import com.offlinelabs.nutcracker.ui.components.form.FormDialogActions
import com.offlinelabs.nutcracker.ui.components.form.FormDialogSurface
import com.offlinelabs.nutcracker.ui.components.form.FormSection
import com.offlinelabs.nutcracker.ui.components.form.FormSectionDivider
import com.offlinelabs.nutcracker.ui.components.form.NumericInputRow
import com.offlinelabs.nutcracker.ui.theme.FoodLogTheme
import com.offlinelabs.nutcracker.utils.MacroCalculator
import com.offlinelabs.nutcracker.utils.TEFCalculator
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SetGoalDialog(
    currentUserGoal: UserGoal,
    onDismiss: () -> Unit,
    onSetGoal: (UserGoal) -> Unit
) {
    val state = rememberGoalFormState(currentUserGoal)
    val colors = FormDefaults.colors()
    val goal = state.buildGoal(currentUserGoal.id)

    FormDialog(
        title = stringResource(R.string.set_your_daily_goals),
        onDismissRequest = onDismiss,
        actions = {
            FormDialogActions(
                confirmText = stringResource(R.string.set_goals),
                onConfirm = { goal?.let(onSetGoal) },
                onDismiss = onDismiss,
                confirmEnabled = goal != null
            )
        }
    ) {
        SetGoalBody(state, colors)
    }
}

/** Editable state of the goal form. */
private class GoalFormState(goal: UserGoal) {
    var calories by mutableStateOf(wholeGoalText(goal.caloriesGoal))
    var carbs by mutableIntStateOf(goal.carbsGoal_g)
    var protein by mutableIntStateOf(goal.proteinGoal_g)
    var fat by mutableIntStateOf(goal.fatGoal_g)
    var fiber by mutableStateOf(wholeGoalText(goal.fiberGoal_g))
    var salt by mutableStateOf(saltMgToGramsText(goal.sodiumGoal_mg))
    var useTef by mutableStateOf(false)

    fun buildGoal(id: Int): UserGoal? = buildUserGoal(id, calories, carbs, protein, fat, fiber, salt)
}

@Composable
private fun rememberGoalFormState(goal: UserGoal): GoalFormState = remember { GoalFormState(goal) }

@Composable
private fun ColumnScope.SetGoalBody(state: GoalFormState, colors: FormColors) {
    val carbsGrams = state.carbs.toDouble()
    val proteinGrams = state.protein.toDouble()
    val fatGrams = state.fat.toDouble()
    val targetKcal = parseWholeGoal(state.calories)?.takeIf { it > 0 }

    val currentRatios = remember(carbsGrams, proteinGrams, fatGrams) {
        MacroCalculator.calculateCurrentRatios(carbsGrams, proteinGrams, fatGrams)
    }
    val tefBonus = remember(carbsGrams, proteinGrams, fatGrams, state.useTef) {
        if (state.useTef) {
            TEFCalculator.calculateTEFBonus(proteinGrams, carbsGrams, fatGrams).roundToInt()
        } else {
            0
        }
    }

    val caloriesError = validateCaloriesGoal(state.calories)?.let {
        stringResource(R.string.validation_calories_positive)
    }
    val fiberError = validateFiberGoal(state.fiber)?.let {
        stringResource(R.string.validation_fiber_non_negative)
    }
    val saltError = validateSaltGoal(state.salt)?.let {
        stringResource(R.string.validation_sodium_non_negative)
    }

    // Calories, with the kcal the chosen macros actually add up to.
    FormSection {
        NumericInputRow(
            label = stringResource(R.string.calories_label),
            value = state.calories,
            onValueChange = { state.calories = it },
            unit = stringResource(R.string.unit_kcal),
            emphasized = true,
            errorText = caloriesError
        )
        // Gross kcal from macros; with TEF, net = gross - TEF bonus.
        val grossKcal = MacroCalculator.calculateKcalFromMacros(carbsGrams, proteinGrams, fatGrams, useTEF = false)
        val actualKcal = if (state.useTef) grossKcal - tefBonus else grossKcal
        val difference = actualKcal - (targetKcal ?: 0)
        val differenceText = if (difference > 0) "+$difference" else "$difference"
        // Always rendered with a fixed height so the dialog doesn't jump.
        Text(
            text = if (targetKcal != null && actualKcal != targetKcal) {
                stringResource(R.string.actual_calories, actualKcal, differenceText)
            } else {
                ""
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (abs(difference) > 50) colors.error else colors.contentSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 16.dp)
                .padding(horizontal = 4.dp)
        )
    }

    Spacer(Modifier.height(12.dp))

    // Macros (drag the bar) and TEF.
    val suggested = MacroCalculator.calculateMacrosFromKcal(
        targetKcal ?: 2000,
        currentRatios,
        useTEF = state.useTef
    )
    FormSection {
        CombinedMacroBar(
            carbsGrams = carbsGrams,
            proteinGrams = proteinGrams,
            fatGrams = fatGrams,
            totalKcal = targetKcal ?: 2000,
            suggestedCarbsGrams = suggested.carbsGrams,
            suggestedProteinGrams = suggested.proteinGrams,
            suggestedFatGrams = suggested.fatGrams,
            showSuggested = true,
            onMacrosChanged = { carbs, protein, fat ->
                state.carbs = carbs.roundToInt().coerceAtLeast(0)
                state.protein = protein.roundToInt().coerceAtLeast(0)
                state.fat = fat.roundToInt().coerceAtLeast(0)
            }
        )
        FormSectionDivider()
        TefRow(
            checked = state.useTef,
            onCheckedChange = { state.useTef = it },
            bonus = tefBonus,
            colors = colors
        )
    }

    Spacer(Modifier.height(12.dp))

    // Fiber (g) and salt (shown in g, stored in mg like the meal form and dashboard).
    FormSection {
        NumericInputRow(
            label = stringResource(R.string.fiber_label),
            value = state.fiber,
            onValueChange = { state.fiber = it },
            unit = stringResource(R.string.unit_g),
            errorText = fiberError
        )
        FormSectionDivider()
        NumericInputRow(
            label = stringResource(R.string.sodium_label),
            value = state.salt,
            onValueChange = { state.salt = it },
            unit = stringResource(R.string.unit_g),
            errorText = saltError
        )
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun TefRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit, bonus: Int, colors: FormColors) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.thermic_effect_tef),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.content
            )
            Text(
                text = stringResource(R.string.higher_macros_same_kcal),
                style = MaterialTheme.typography.bodySmall,
                color = colors.contentSecondary
            )
        }
        if (checked && bonus > 0) {
            Text(
                text = stringResource(R.string.kcal_value_format, "+$bonus"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.accent,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colors.accent,
                checkedThumbColor = colors.onAccent
            )
        )
    }
}

// ----- Previews -----

@Composable
private fun SetGoalPreview(goal: UserGoal, invalidCalories: Boolean) {
    val state = remember { GoalFormState(goal) }
    if (invalidCalories) state.calories = "abc"
    Box(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
        FormDialogSurface(
            title = stringResource(R.string.set_your_daily_goals),
            actions = {
                FormDialogActions(
                    confirmText = stringResource(R.string.set_goals),
                    onConfirm = {},
                    onDismiss = {},
                    confirmEnabled = !invalidCalories
                )
            }
        ) {
            SetGoalBody(state, FormDefaults.colors())
        }
    }
}

@Preview(name = "Set goals - light", widthDp = 360, heightDp = 780, showBackground = true, backgroundColor = 0xFFFAFBFC)
@Composable
private fun SetGoalLightPreview() {
    FoodLogTheme(darkTheme = false) { SetGoalPreview(UserGoal.default(), false) }
}

@Preview(name = "Set goals - dark, error", widthDp = 360, heightDp = 780, showBackground = true, backgroundColor = 0xFF0B1426)
@Composable
private fun SetGoalDarkErrorPreview() {
    FoodLogTheme(darkTheme = true) { SetGoalPreview(UserGoal.default(), true) }
}
