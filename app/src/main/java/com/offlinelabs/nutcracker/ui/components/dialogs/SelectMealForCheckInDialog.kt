package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.ListGroup
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.ui.components.ValueWheel
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import com.offlinelabs.nutcracker.ui.components.formatWheelValue
import com.offlinelabs.nutcracker.ui.components.lists.DraggableListRow
import com.offlinelabs.nutcracker.ui.components.lists.ListGroupHeader
import com.offlinelabs.nutcracker.ui.components.lists.QuickCheckRow
import com.offlinelabs.nutcracker.ui.components.lists.QuickControls
import com.offlinelabs.nutcracker.ui.components.lists.SwipeAction
import com.offlinelabs.nutcracker.ui.components.lists.groupTitleText
import com.offlinelabs.nutcracker.ui.components.lists.listSidePadding
import com.offlinelabs.nutcracker.ui.components.lists.rememberGroupedReorderState
import com.offlinelabs.nutcracker.ui.theme.brandAccentShade
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Inline meal picker shown on the dashboard's Food tab. It fills the space it is given and has no
 * dialog chrome (no card, no back arrow); the whole list scrolls together with the action buttons.
 * Foods are full-width rows with a portion wheel and a check-in box.
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
    /** Whether rows of a group can be dragged (custom order). */
    canReorder: (ListGroup<Meal>) -> Boolean = { false },
    /** New order of a dragged group's meal ids, called with the group key when a row is dropped. */
    onReorder: (groupKey: String, ids: List<Long>) -> Unit = { _, _ -> },
    /** Sort/group controls shown above the list. */
    arrangementBar: @Composable () -> Unit = {},
    /** Rows put their controls (portion wheel, check-in box) on the left, away from a scrolling right thumb. */
    controlsFirst: Boolean = false,
    /** Foods with a check-in on the day shown (ticked outside meal type groups). */
    checkedInIds: Set<Long> = emptySet(),
    /**
     * (meal id, meal type id) of the day's check-ins; the type is null when checked in outside a
     * meal type group. A meal type group ticks its own check-ins and those without a type.
     */
    checkedInCategoryKeys: Set<Pair<Long, Long?>> = emptySet(),
    /** Serving multiplier of each food's last check-in (where its portion wheel starts). */
    lastServings: Map<Long, Double> = emptyMap(),
    /** Checks the food in with the portion wheel (as a serving multiplier) under the group's meal type (or null). */
    onQuickCheckIn: (meal: Meal, servingMultiplier: Double, mealCategoryId: Long?) -> Unit = { _, _, _ -> },
    /** Removes the food's check-ins of the day shown (only those of the meal type when given). */
    onRemoveCheckIns: (meal: Meal, mealCategoryId: Long?) -> Unit = { _, _ -> },
    /** Delete from the row's swipe panel (the caller confirms). */
    onDeleteMeal: (Meal) -> Unit = {}
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
    // Only one row's swipe panel is open at a time
    var openRowKey by remember { mutableStateOf<String?>(null) }
    // Portions survive rows scrolling out of view (reset when the tab leaves composition)
    val portions = remember { mutableStateMapOf<Long, Double>() }

    // Rows span the full width; the other items keep the screen's side padding
    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item(key = "scan_barcode") {
            Button(
                onClick = onScanBarcode,
                modifier = Modifier
                    .fillMaxWidth()
                    .listSidePadding()
                    .onGloballyPositioned { coordinates: LayoutCoordinates ->
                        val bounds = coordinates.boundsInWindow()
                        val padding = with(density) { 8.dp.toPx() }
                        val size = androidx.compose.ui.geometry.Size(
                            width = bounds.width + padding * 2,
                            height = bounds.height + padding * 2
                        )
                        registerElementCoordinates?.invoke("barcode_button", bounds.center, size)
                    },
                colors = ButtonDefaults.buttonColors(containerColor = brandAccentShade(0))
            ) {
                Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.scan_barcode))
            }
        }

        item(key = "search_database") {
            Button(
                onClick = onSearchMeal,
                modifier = Modifier.fillMaxWidth().listSidePadding().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = brandAccentShade(1))
            ) {
                Icon(imageVector = Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.search_meal_database))
            }
        }

        item(key = "type_information") {
            Button(
                onClick = onAddMeal,
                modifier = Modifier.fillMaxWidth().listSidePadding().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = brandAccentShade(2))
            ) {
                Icon(painter = painterResource(R.drawable.ic_ballot), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.type_information))
            }
        }

        item(key = "recipes") {
            Button(
                onClick = onSelectRecipe,
                modifier = Modifier.fillMaxWidth().listSidePadding().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = brandAccentShade(3))
            ) {
                Icon(imageVector = Icons.Filled.Restaurant, contentDescription = null, modifier = Modifier.size(20.dp))
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
                    modifier = Modifier.listSidePadding().padding(top = 16.dp)
                )
            }
        } else {
            item(key = "arrangement_bar") {
                Box(modifier = Modifier.listSidePadding().padding(top = 16.dp, bottom = 8.dp)) { arrangementBar() }
            }
            reorder.groups.forEach { group ->
                group.title?.let { title ->
                    item(key = "meal_header_${group.key}") {
                        ListGroupHeader(groupTitleText(title), Modifier.listSidePadding().padding(top = 12.dp, bottom = 6.dp))
                    }
                }
                val draggable = canReorder(group)
                // A meal type group counts the check-ins made under it and those without a type
                val categoryId = group.mealCategoryId
                items(group.items, key = { reorder.itemKey(group.key, it) }) { meal ->
                    val rowKey = reorder.itemKey(group.key, meal)
                    DraggableListRow(
                        state = reorderableState,
                        key = rowKey,
                        enabled = draggable,
                        handleDescription = stringResource(R.string.reorder_handle_description, meal.name),
                        onDragStopped = { onReorder(group.key, reorder.groupOrder(group.key)) },
                        // The handle lives in the swipe panel instead of beside the row
                        showHandle = false
                    ) { handleModifier ->
                        val start = portionWheelStart(meal, lastServings[meal.id])
                        val amount = portions[meal.id] ?: start
                        MealRow(
                            meal = meal,
                            controlsFirst = controlsFirst,
                            open = openRowKey == rowKey,
                            onOpenChange = { open -> openRowKey = if (open) rowKey else openRowKey.takeIf { it != rowKey } },
                            draggable = draggable,
                            handleModifier = handleModifier,
                            portionStart = start,
                            portion = amount,
                            onPortionChange = { portions[meal.id] = it },
                            checkedIn = if (categoryId != null) {
                                (meal.id to categoryId) in checkedInCategoryKeys || (meal.id to null) in checkedInCategoryKeys
                            } else {
                                meal.id in checkedInIds
                            },
                            onCheckedChange = { checked ->
                                if (checked) onQuickCheckIn(meal, portionMultiplier(portions[meal.id] ?: start, meal), categoryId)
                                else onRemoveCheckIns(meal, categoryId)
                            },
                            onClick = { selectedMealId = meal.id },
                            onEdit = { openRowKey = null; onEditMeal(meal) },
                            onDelete = { openRowKey = null; onDeleteMeal(meal) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * One food as a full-width row: image, name and the kcal of the chosen portion, a portion wheel
 * in the food's unit and the check-in box. Tap opens the details; swipe left reveals Edit and
 * Delete (plus the drag handle in custom order).
 */
@Composable
private fun MealRow(
    meal: Meal,
    controlsFirst: Boolean,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    draggable: Boolean,
    handleModifier: Modifier,
    portionStart: Double,
    portion: Double,
    onPortionChange: (Double) -> Unit,
    checkedIn: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = FormDefaults.colors()
    val unit = stringResource(meal.servingSize_unit.abbreviationResId)
    val steps = remember(portionStart, meal.servingSize_unit) { portionWheelValues(portionStart, meal.servingSize_unit) }
    val kcal = meal.calories * portionMultiplier(portion, meal)

    QuickCheckRow(
        controlsFirst = controlsFirst,
        open = open,
        onOpenChange = onOpenChange,
        actionCount = if (draggable) 3 else 2,
        actions = {
            if (draggable) {
                SwipeAction(
                    icon = Icons.Filled.DragIndicator,
                    label = stringResource(R.string.reorder_handle_description, meal.name),
                    background = MaterialTheme.colorScheme.outline,
                    content = MaterialTheme.colorScheme.surface,
                    modifier = handleModifier,
                    onClick = {}
                )
            }
            SwipeAction(Icons.Filled.Edit, stringResource(R.string.edit), colors.accent, colors.onAccent, onClick = onEdit)
            SwipeAction(Icons.Filled.Delete, stringResource(R.string.delete), MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError, onClick = onDelete)
        },
        onClick = onClick,
        controls = {
            QuickControls(
                controlsFirst = controlsFirst,
                checked = checkedIn,
                onCheckedChange = onCheckedChange,
                checkLabel = stringResource(if (checkedIn) R.string.quick_remove_check_in else R.string.quick_check_in, meal.name)
            ) {
                // Re-created when the start moves (the last portion loads), so it scrolls there
                key(steps) {
                    ValueWheel(
                        values = steps,
                        value = portion,
                        onValueChange = onPortionChange,
                        unit = unit,
                        contentDescription = stringResource(R.string.quick_wheel_description, meal.name, formatWheelValue(portion), unit),
                        width = 60.dp
                    )
                }
            }
        }
    ) {
        // The name takes the full width above the picture and the kcal
        Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(
                text = meal.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                MealThumbnail(meal)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.kcal_value_format, kotlin.math.round(kcal).toLong().toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Only real NOVA groups (1–4); some imported foods carry a 0
                meal.novaClassification?.takeIf { it.group in 1..4 }?.let { nova ->
                    Text(
                        text = " · " + stringResource(R.string.nova_group_format, nova.group),
                        style = MaterialTheme.typography.bodySmall,
                        color = novaColor(nova.group)
                    )
                }
            }
        }
    }
}

/** The food's picture, or a neutral plate icon so the names line up. */
@Composable
private fun MealThumbnail(meal: Meal) {
    val model = meal.localImagePath ?: meal.imageUrl
    val shape = RoundedCornerShape(8.dp)
    if (model != null) {
        AsyncImage(
            model = model,
            contentDescription = stringResource(R.string.food_image),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(40.dp).clip(shape)
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(Icons.Filled.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

/** NOVA 1 green … 4 red, as on the food details. */
private fun novaColor(group: Int): Color = when (group) {
    1 -> Color(0xFF4CAF50)
    2 -> Color(0xFF8BC34A)
    3 -> Color(0xFFFF9800)
    4 -> Color(0xFFF44336)
    else -> Color(0xFF9E9E9E)
}
