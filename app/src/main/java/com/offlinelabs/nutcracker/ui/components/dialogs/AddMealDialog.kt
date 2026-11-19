package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.ui.components.form.FormDialog
import com.offlinelabs.nutcracker.ui.components.form.FormDialogActions
import com.offlinelabs.nutcracker.ui.components.form.MealForm
import com.offlinelabs.nutcracker.ui.components.form.rememberMealFormState

@Composable
fun AddMealDialog(
    onDismiss: () -> Unit,
    onAddMeal: (Meal) -> Unit
) {
    val state = rememberMealFormState(original = null)

    FormDialog(
        title = stringResource(R.string.add_new_meal),
        onDismissRequest = onDismiss,
        actions = {
            FormDialogActions(
                confirmText = stringResource(R.string.add_meal),
                onConfirm = { state.toResult()?.let { onAddMeal(it.toNewMeal()) } },
                onDismiss = onDismiss,
                confirmEnabled = state.isValid
            )
        }
    ) {
        MealForm(state)
    }
}
