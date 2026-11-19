package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.ui.components.form.FormDialog
import com.offlinelabs.nutcracker.ui.components.form.FormDialogActions
import com.offlinelabs.nutcracker.ui.components.form.MealForm
import com.offlinelabs.nutcracker.ui.components.form.rememberMealFormState

/**
 * Edits a meal definition. The link toggle next to the serving size rescales every nutrient
 * proportionally from the meal's original values (see MealFormState.toggleLink).
 * All fields not shown in the form (id, source, Open Food Facts data...) are preserved.
 */
@Composable
fun EditMealDialog(
    meal: Meal,
    onDismiss: () -> Unit,
    onUpdateMeal: (Meal) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val state = rememberMealFormState(original = meal)

    FormDialog(
        title = stringResource(R.string.edit_meal),
        onDismissRequest = onDismiss,
        actions = {
            FormDialogActions(
                confirmText = stringResource(R.string.save),
                onConfirm = { state.toResult()?.let { onUpdateMeal(it.applyTo(meal)) } },
                onDismiss = onDismiss,
                confirmEnabled = state.isValid,
                onDelete = onDelete
            )
        }
    ) {
        MealForm(state)
    }
}
