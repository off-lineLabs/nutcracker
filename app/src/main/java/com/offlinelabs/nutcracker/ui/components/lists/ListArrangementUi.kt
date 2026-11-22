package com.offlinelabs.nutcracker.ui.components.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.ExerciseGrouping
import com.offlinelabs.nutcracker.data.GroupTitle
import com.offlinelabs.nutcracker.data.ListSort
import com.offlinelabs.nutcracker.data.MealGrouping
import com.offlinelabs.nutcracker.data.model.ExerciseType

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
        MealGrouping.NUTRI_SCORE -> R.string.group_nutri_score
    }
)

@Composable
fun exerciseGroupingLabel(grouping: ExerciseGrouping): String = stringResource(
    when (grouping) {
        ExerciseGrouping.NONE -> R.string.group_none
        ExerciseGrouping.ROUTINE -> R.string.group_routine
        ExerciseGrouping.MUSCLE -> R.string.group_muscle
        ExerciseGrouping.TYPE -> R.string.group_exercise_type
    }
)

@Composable
fun groupTitleText(title: GroupTitle): String = when (title) {
    is GroupTitle.NutriScore -> stringResource(R.string.group_nutri_grade, title.grade)
    GroupTitle.NoNutriScore -> stringResource(R.string.group_no_nutri_score)
    is GroupTitle.RoutineName -> title.name
    GroupTitle.NoRoutine -> stringResource(R.string.group_no_routine)
    is GroupTitle.Muscle -> title.muscle.replaceFirstChar { it.uppercase() }
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
 * "Sort: …" and "Group: …" chips, each opening a menu. [groupings] empty hides the group chip.
 * In custom order a hint explains how to reorder.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <G> ListArrangementBar(
    sort: ListSort,
    onSortChange: (ListSort) -> Unit,
    grouping: G,
    groupings: List<G>,
    groupingLabel: @Composable (G) -> String,
    onGroupingChange: (G) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuChip(
                text = stringResource(R.string.sort_chip_format, sortLabel(sort)),
                icon = Icons.AutoMirrored.Filled.Sort,
                options = ListSort.entries,
                selected = sort,
                optionLabel = { sortLabel(it) },
                onSelect = onSortChange
            )
            if (groupings.size > 1) {
                MenuChip(
                    text = stringResource(R.string.group_chip_format, groupingLabel(grouping)),
                    icon = Icons.Filled.Workspaces,
                    options = groupings,
                    selected = grouping,
                    optionLabel = groupingLabel,
                    onSelect = onGroupingChange
                )
            }
        }
        if (sort == ListSort.CUSTOM) {
            Text(
                text = stringResource(R.string.reorder_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun <T> MenuChip(
    text: String,
    icon: ImageVector,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(text, maxLines = 1) },
            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
            colors = AssistChipDefaults.assistChipColors(
                labelColor = MaterialTheme.colorScheme.onSurface,
                leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                trailingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
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
 * Title above a group of list items. Smaller than section titles such as "Shared" (titleSmall), so
 * a group inside a section reads as a sub-level.
 */
@Composable
fun ListGroupHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        modifier = modifier.padding(top = 6.dp, start = 4.dp)
    )
}
