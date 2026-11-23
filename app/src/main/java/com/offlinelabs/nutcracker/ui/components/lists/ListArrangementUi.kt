package com.offlinelabs.nutcracker.ui.components.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.ExerciseGrouping
import com.offlinelabs.nutcracker.data.GroupTitle
import com.offlinelabs.nutcracker.data.ListSort
import com.offlinelabs.nutcracker.data.MealGrouping
import com.offlinelabs.nutcracker.data.gymLabel
import com.offlinelabs.nutcracker.data.model.ExerciseType
import com.offlinelabs.nutcracker.ui.components.muscleLabel
import com.offlinelabs.nutcracker.ui.components.mealcategory.mealCategoryLabel

@Composable
fun sortLabel(sort: ListSort): String = stringResource(
    when (sort) {
        ListSort.CUSTOM -> R.string.sort_custom
        ListSort.ALPHABETICAL -> R.string.sort_alphabetical
        ListSort.ADDED_DATE -> R.string.sort_added_date
        ListSort.LAST_CHECKED_IN -> R.string.sort_last_checked_in
    }
)

@Composable
fun mealGroupingLabel(grouping: MealGrouping): String = stringResource(
    when (grouping) {
        MealGrouping.NONE -> R.string.group_none
        MealGrouping.MEAL_TYPE -> R.string.group_meal_type
        MealGrouping.NOVA -> R.string.group_nova
    }
)

@Composable
fun exerciseGroupingLabel(grouping: ExerciseGrouping): String = stringResource(
    when (grouping) {
        ExerciseGrouping.NONE -> R.string.group_none
        ExerciseGrouping.GYM -> R.string.group_gym
        ExerciseGrouping.ROUTINE -> R.string.group_routine
        ExerciseGrouping.MUSCLE -> R.string.group_muscle
        ExerciseGrouping.TYPE -> R.string.group_exercise_type
    }
)

@Composable
fun groupTitleText(title: GroupTitle): String = when (title) {
    is GroupTitle.MealType -> mealCategoryLabel(title.name, title.defaultKey)
    GroupTitle.NoMealType -> stringResource(R.string.group_no_meal_type)
    is GroupTitle.Nova -> stringResource(R.string.group_nova_number, title.group)
    GroupTitle.NoNova -> stringResource(R.string.group_no_nova)
    is GroupTitle.GymName -> gymLabel(title.name, title.unit)
    GroupTitle.Shared -> stringResource(R.string.gym_shared_section)
    is GroupTitle.RoutineName -> title.gym?.let { "${title.name} · ${gymLabel(it.name, it.unit)}" } ?: title.name
    GroupTitle.NoRoutine -> stringResource(R.string.group_no_routine)
    is GroupTitle.Muscle -> muscleLabel(title.muscle)
    GroupTitle.NoMuscle -> stringResource(R.string.group_no_muscle)
    is GroupTitle.Type -> stringResource(
        when (title.type) {
            ExerciseType.STRENGTH -> R.string.strength
            ExerciseType.CARDIO -> R.string.cardio
            ExerciseType.BODYWEIGHT -> R.string.bodyweight
        }
    )
}

/**
 * One row: Sort, a small up/down direction toggle (only for sorts with a direction) and Group
 * (hidden when [groupings] has a single entry). Sort and Group show a small label above the value
 * and open a menu when tapped.
 */
@Composable
fun <G> ListArrangementBar(
    sort: ListSort,
    descending: Boolean,
    onSortChange: (ListSort) -> Unit,
    onDescendingChange: (Boolean) -> Unit,
    grouping: G,
    groupings: List<G>,
    groupingLabel: @Composable (G) -> String,
    onGroupingChange: (G) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MenuField(
                label = stringResource(R.string.sort_label),
                icon = Icons.AutoMirrored.Filled.Sort,
                options = ListSort.entries,
                selected = sort,
                optionLabel = { sortLabel(it) },
                onSelect = onSortChange,
                modifier = Modifier.weight(1f)
            )
            if (sort.hasDirection) {
                // A bare icon between the fields: discreet, and not mistaken for a dropdown
                Surface(
                    onClick = { onDescendingChange(!descending) },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(width = 32.dp, height = 48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (descending) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                            contentDescription = stringResource(
                                if (descending) R.string.sort_descending else R.string.sort_ascending
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            if (groupings.size > 1) {
                MenuField(
                    label = stringResource(R.string.group_label),
                    icon = Icons.Filled.Workspaces,
                    options = groupings,
                    selected = grouping,
                    optionLabel = groupingLabel,
                    onSelect = onGroupingChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun <T> MenuField(
    label: String,
    icon: ImageVector,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = optionLabel(selected),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    trailingIcon = if (option == selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

/**
 * Title above a group of list items: a small uppercase label, so groups read as a sub-level of
 * the screen's own titles.
 */
@Composable
fun ListGroupHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = TextUnit(1f, TextUnitType.Sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        modifier = modifier.padding(top = 6.dp, start = 4.dp)
    )
}
