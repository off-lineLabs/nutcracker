package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.MealCategory
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import com.offlinelabs.nutcracker.ui.components.form.FormDialog
import com.offlinelabs.nutcracker.ui.components.form.FormDialogActions
import com.offlinelabs.nutcracker.ui.components.form.MealForm
import com.offlinelabs.nutcracker.ui.components.form.rememberMealFormState
import com.offlinelabs.nutcracker.ui.components.mealcategory.MealCategoryPicker

/**
 * Edits a meal definition. The link toggle next to the serving size rescales every nutrient
 * proportionally from the meal's original values (see MealFormState.toggleLink).
 * All fields not shown in the form (id, source, Open Food Facts data...) are preserved.
 */
@Composable
fun EditMealDialog(
    meal: Meal,
    onDismiss: () -> Unit,
    /** The updated meal and the ids of its meal types. */
    onUpdateMeal: (Meal, List<Long>) -> Unit,
    onDelete: (() -> Unit)? = null,
    categories: List<MealCategory> = emptyList(),
    /** The meal's current meal types (they may arrive after the dialog opens). */
    initialCategoryIds: List<Long> = emptyList(),
    onManageCategories: () -> Unit = {}
) {
    val state = rememberMealFormState(original = meal)
    var categoryIds by rememberSaveable(initialCategoryIds) { mutableStateOf(ArrayList(initialCategoryIds)) }

    FormDialog(
        title = stringResource(R.string.edit_meal),
        onDismissRequest = onDismiss,
        actions = {
            FormDialogActions(
                confirmText = stringResource(R.string.save),
                onConfirm = { state.toResult()?.let { onUpdateMeal(it.applyTo(meal), categoryIds.toList()) } },
                onDismiss = onDismiss,
                confirmEnabled = state.isValid,
                onDelete = onDelete
            )
        }
    ) {
        MealForm(state)
        Spacer(Modifier.height(12.dp))
        MealCategoryPicker(
            categories = categories,
            selectedIds = categoryIds,
            onSelectedChange = { categoryIds = ArrayList(it) },
            onManage = onManageCategories,
            colors = FormDefaults.colors()
        )
    }
}
