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

@Composable
fun AddMealDialog(
    onDismiss: () -> Unit,
    /** The new meal and the ids of the meal types picked for it. */
    onAddMeal: (Meal, List<Long>) -> Unit,
    categories: List<MealCategory> = emptyList(),
    onManageCategories: () -> Unit = {}
) {
    val state = rememberMealFormState(original = null)
    var categoryIds by rememberSaveable { mutableStateOf(ArrayList<Long>()) }

    FormDialog(
        title = stringResource(R.string.add_new_meal),
        onDismissRequest = onDismiss,
        actions = {
            FormDialogActions(
                confirmText = stringResource(R.string.add_meal),
                onConfirm = { state.toResult()?.let { onAddMeal(it.toNewMeal(), categoryIds.toList()) } },
                onDismiss = onDismiss,
                confirmEnabled = state.isValid
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
