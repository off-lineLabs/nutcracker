package com.offlinelabs.nutcracker.ui.components.mealcategory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.MealCategory
import com.offlinelabs.nutcracker.data.model.MealCategoryDefaults
import com.offlinelabs.nutcracker.ui.components.form.ConfirmDeleteDialog
import com.offlinelabs.nutcracker.ui.components.form.FormColors
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import com.offlinelabs.nutcracker.ui.components.lists.DraggableListRow
import com.offlinelabs.nutcracker.ui.components.lists.ListCard
import com.offlinelabs.nutcracker.ui.components.lists.ListDialogFrame
import com.offlinelabs.nutcracker.ui.components.lists.ListHint
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Translated name of a built-in category ([defaultKey]), otherwise [name] as typed. */
@Composable
fun mealCategoryLabel(name: String, defaultKey: String?): String = when (defaultKey) {
    MealCategoryDefaults.BREAKFAST -> stringResource(R.string.meal_category_breakfast)
    MealCategoryDefaults.LUNCH -> stringResource(R.string.meal_category_lunch)
    MealCategoryDefaults.DINNER -> stringResource(R.string.meal_category_dinner)
    MealCategoryDefaults.SNACK -> stringResource(R.string.meal_category_snack)
    else -> name
}

@Composable
fun mealCategoryLabel(category: MealCategory): String = mealCategoryLabel(category.name, category.defaultKey)

/**
 * Meal-type chips for the food form (several can be selected) with an edit button that opens the
 * category manager via [onManage].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealCategoryPicker(
    categories: List<MealCategory>,
    selectedIds: List<Long>,
    onSelectedChange: (List<Long>) -> Unit,
    onManage: () -> Unit,
    colors: FormColors,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.meal_categories_label),
                style = MaterialTheme.typography.labelLarge,
                color = colors.contentSecondary,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            IconButton(onClick = onManage) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.meal_categories_manage),
                    tint = colors.contentSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { category ->
                val selected = category.id in selectedIds
                FilterChip(
                    selected = selected,
                    onClick = { onSelectedChange(if (selected) selectedIds - category.id else selectedIds + category.id) },
                    label = { Text(mealCategoryLabel(category)) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = colors.field,
                        labelColor = colors.content,
                        selectedContainerColor = colors.accent,
                        selectedLabelColor = colors.onAccent,
                        selectedLeadingIconColor = colors.onAccent
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = colors.fieldBorder,
                        selectedBorderColor = colors.accent
                    )
                )
            }
        }
    }
}

/**
 * Manage meal types: drag to reorder, tap the pencil to rename, the bin to delete, and add new ones
 * at the bottom. Renaming a built-in one keeps the typed text from then on.
 */
@Composable
fun MealCategoriesDialog(
    categories: List<MealCategory>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onRename: (MealCategory, String) -> Unit,
    onDelete: (MealCategory) -> Unit,
    onReorder: (List<Long>) -> Unit
) {
    val colors = FormDefaults.colors()
    var order by remember(categories) { mutableStateOf(categories) }
    var newName by rememberSaveable { mutableStateOf("") }
    var renamingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var deleting by remember { mutableStateOf<MealCategory?>(null) }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = order.indexOfFirst { "cat_${it.id}" == from.key }
        val toIndex = order.indexOfFirst { "cat_${it.id}" == to.key }
        if (fromIndex >= 0 && toIndex >= 0) order = order.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
    }

    fun submitNew() {
        val name = newName.trim()
        if (name.isNotEmpty()) onAdd(name)
        newName = ""
    }

    ListDialogFrame(
        title = stringResource(R.string.meal_categories_label),
        onDismiss = onDismiss,
        listState = listState,
        actions = {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), color = colors.accent) }
            }
        }
    ) {
        if (order.size > 1) item(key = "hint") { ListHint(stringResource(R.string.routine_reorder_hint), colors) }
        items(order, key = { "cat_${it.id}" }) { category ->
            val label = mealCategoryLabel(category)
            DraggableListRow(
                state = reorderState,
                key = "cat_${category.id}",
                enabled = renamingId == null,
                handleDescription = stringResource(R.string.reorder_handle_description, label),
                onDragStopped = { onReorder(order.map { it.id }) }
            ) {
                if (renamingId == category.id) {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            renameText.trim().takeIf { it.isNotEmpty() }?.let { onRename(category, it) }
                            renamingId = null
                        }),
                        trailingIcon = {
                            IconButton(onClick = {
                                renameText.trim().takeIf { it.isNotEmpty() }?.let { onRename(category, it) }
                                renamingId = null
                            }) { Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.save)) }
                        },
                        colors = FormDefaults.outlinedTextFieldColors(colors),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ListCard(
                        title = label,
                        subtitle = null,
                        colors = colors,
                        modifier = Modifier.clickable { renamingId = category.id; renameText = label },
                        trailing = {
                            IconButton(onClick = { renamingId = category.id; renameText = label }) {
                                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.meal_category_rename, label), tint = colors.contentSecondary)
                            }
                            IconButton(onClick = { deleting = category }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = colors.contentSecondary)
                            }
                        }
                    )
                }
            }
        }
        item(key = "new") {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = { Text(stringResource(R.string.meal_category_new_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submitNew() }),
                trailingIcon = {
                    TextButton(onClick = { submitNew() }, enabled = newName.isNotBlank()) {
                        Text(stringResource(R.string.add), color = colors.accent)
                    }
                },
                colors = FormDefaults.outlinedTextFieldColors(colors),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }

    deleting?.let { category ->
        val label = mealCategoryLabel(category)
        ConfirmDeleteDialog(
            title = stringResource(R.string.meal_category_delete_title),
            message = stringResource(R.string.meal_category_delete_message, label),
            onConfirm = { deleting = null; onDelete(category) },
            onDismiss = { deleting = null }
        )
    }
}
