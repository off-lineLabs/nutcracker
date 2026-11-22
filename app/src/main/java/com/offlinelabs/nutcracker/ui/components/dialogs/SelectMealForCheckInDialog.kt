package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.offlinelabs.nutcracker.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.LayoutCoordinates
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.ui.components.items.MealItem
import com.offlinelabs.nutcracker.data.ListGroup
import com.offlinelabs.nutcracker.ui.components.lists.DraggableListRow
import com.offlinelabs.nutcracker.ui.components.lists.ListGroupHeader
import com.offlinelabs.nutcracker.ui.components.lists.groupTitleText
import com.offlinelabs.nutcracker.ui.components.lists.rememberGroupedReorderState
import androidx.compose.foundation.lazy.rememberLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.offlinelabs.nutcracker.ui.theme.brandAccentShade

/**
 * Inline meal picker shown on the dashboard's Food tab. It fills the space it is given and has no
 * dialog chrome (no card, no back arrow); the whole list scrolls together with the action buttons.
 */
@Composable
fun MealPickerContent(
    meals: List<Meal>,
    onAddMeal: () -> Unit,
    onSelectMeal: (Meal) -> Unit,
    onEditMeal: (Meal) -> Unit = {},
    onSearchMeal: () -> Unit = {},
    onScanBarcode: () -> Unit = {},
    onSelectRecipe: () -> Unit = {},
    modifier: Modifier = Modifier,
    registerElementCoordinates: ((String, Offset, androidx.compose.ui.geometry.Size) -> Unit)? = null,
    /** [meals] sorted and grouped for display; defaults to one ungrouped group in the given order. */
    groups: List<ListGroup<Meal>> = listOf(ListGroup("all", null, meals)),
    /** True in custom order: rows can be dragged within their group. */
    reorderEnabled: Boolean = false,
    /** New order of the dragged group's meal ids, called when a row is dropped. */
    onReorder: (List<Long>) -> Unit = {},
    /** Sort/group controls shown above the list. */
    arrangementBar: @Composable () -> Unit = {}
) {
    // Only the id is saved so the details dialog survives rotation; the meal is looked up again.
    var selectedMealId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedMeal = selectedMealId?.let { id -> meals.find { it.id == id } }
    val density = LocalDensity.current

    selectedMeal?.let { meal ->
        UnifiedMealDetailsDialog(
            meal = meal,
            onBack = { selectedMealId = null },
            onEdit = { mealToEdit ->
                selectedMealId = null
                onEditMeal(mealToEdit)
            },
            onCheckIn = {
                onSelectMeal(meal)
                selectedMealId = null
            }
        )
    }

    val reorder = rememberGroupedReorderState(groups, prefix = "meal") { it.id }
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        reorder.move(from.key, to.key)
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "scan_barcode") {
            Button(
                onClick = onScanBarcode,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates: LayoutCoordinates ->
                        val bounds = coordinates.boundsInWindow()
                        val center = bounds.center
                        val padding = with(density) { 8.dp.toPx() }
                        val size = androidx.compose.ui.geometry.Size(
                            width = bounds.width + padding * 2,
                            height = bounds.height + padding * 2
                        )
                        registerElementCoordinates?.invoke("barcode_button", center, size)
                    },
                colors = ButtonDefaults.buttonColors(containerColor = brandAccentShade(0))
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.scan_barcode))
            }
        }

        item(key = "search_database") {
            Button(
                onClick = onSearchMeal,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = brandAccentShade(1))
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.search_meal_database))
            }
        }

        item(key = "type_information") {
            Button(
                onClick = onAddMeal,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = brandAccentShade(2))
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ballot),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.type_information))
            }
        }

        item(key = "recipes") {
            Button(
                onClick = onSelectRecipe,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = brandAccentShade(3))
            ) {
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.my_recipes))
            }
        }

        if (meals.isEmpty()) {
            item(key = "no_meals") {
                Text(
                    text = stringResource(R.string.no_meals_added),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            item(key = "arrangement_bar") {
                Box(modifier = Modifier.padding(top = 8.dp)) { arrangementBar() }
            }
            reorder.groups.forEach { group ->
                group.title?.let { title ->
                    item(key = "meal_header_${group.key}") { ListGroupHeader(groupTitleText(title)) }
                }
                items(group.items, key = { reorder.itemKey(group.key, it) }) { meal ->
                    DraggableListRow(
                        state = reorderableState,
                        key = reorder.itemKey(group.key, meal),
                        enabled = reorderEnabled,
                        handleDescription = stringResource(R.string.reorder_handle_description, meal.name),
                        onDragStopped = { onReorder(reorder.groupOrder(group.key)) }
                    ) {
                        MealItem(
                            meal = meal,
                            onMealClick = { selectedMealId = meal.id }
                        )
                    }
                }
            }
        }
    }
}
