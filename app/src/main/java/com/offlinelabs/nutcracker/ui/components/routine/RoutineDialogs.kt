package com.offlinelabs.nutcracker.ui.components.routine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.gymLabel
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.Gym
import com.offlinelabs.nutcracker.data.model.Routine
import com.offlinelabs.nutcracker.data.model.RoutineExercise
import com.offlinelabs.nutcracker.ui.components.dialogs.foldForSearch
import com.offlinelabs.nutcracker.ui.components.form.ConfirmDeleteDialog
import com.offlinelabs.nutcracker.ui.components.form.FormColors
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import com.offlinelabs.nutcracker.ui.components.form.FormTextField
import com.offlinelabs.nutcracker.ui.components.gym.GymPickerField
import com.offlinelabs.nutcracker.ui.components.gym.gymPickerOptions
import com.offlinelabs.nutcracker.ui.components.lists.DraggableListRow
import com.offlinelabs.nutcracker.ui.components.lists.ListCard
import com.offlinelabs.nutcracker.ui.components.lists.ListDialogFrame
import com.offlinelabs.nutcracker.ui.components.lists.ListHint
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * The routines list: routines of [selectedGymId] (or all, with their gym, when null) in their order.
 * Tap one to edit it; drag to reorder ([onReorder] gets the shown ids in the new order).
 */
@Composable
fun RoutinesDialog(
    routines: List<Routine>,
    memberships: List<RoutineExercise>,
    gyms: List<Gym>,
    selectedGymId: Long?,
    onDismiss: () -> Unit,
    onNew: () -> Unit,
    onEdit: (Routine) -> Unit,
    onReorder: (List<Long>) -> Unit
) {
    val colors = FormDefaults.colors()
    val shown = remember(routines, selectedGymId) { routinesForGym(routines, selectedGymId) }
    var order by remember(shown) { mutableStateOf(shown) }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = order.indexOfFirst { "routine_${it.id}" == from.key }
        val toIndex = order.indexOfFirst { "routine_${it.id}" == to.key }
        if (fromIndex >= 0 && toIndex >= 0) order = order.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
    }
    val countByRoutine = remember(memberships) { memberships.groupingBy { it.routineId }.eachCount() }

    ListDialogFrame(
        title = stringResource(R.string.routines_title),
        onDismiss = onDismiss,
        listState = listState,
        actions = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onNew,
                    enabled = gyms.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.routine_new))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), color = colors.accent) }
            }
        }
    ) {
        when {
            gyms.isEmpty() -> item(key = "needs_gym") { ListHint(stringResource(R.string.routine_needs_gym), colors) }
            order.isEmpty() -> item(key = "empty") { ListHint(stringResource(R.string.routine_empty), colors) }
            else -> {
                if (order.size > 1) item(key = "hint") { ListHint(stringResource(R.string.routine_reorder_hint), colors) }
                items(order, key = { "routine_${it.id}" }) { routine ->
                    val count = countByRoutine[routine.id] ?: 0
                    val gym = gyms.firstOrNull { it.id == routine.gymId }
                    val subtitle = listOfNotNull(
                        pluralStringResource(R.plurals.routine_exercise_count, count, count),
                        gym?.takeIf { selectedGymId == null }?.let { gymLabel(it.name, it.unit) }
                    ).joinToString(" · ")
                    DraggableListRow(
                        state = reorderState,
                        key = "routine_${routine.id}",
                        enabled = true,
                        handleDescription = stringResource(R.string.reorder_handle_description, routine.name),
                        onDragStopped = { onReorder(order.map { it.id }) }
                    ) {
                        ListCard(routine.name, subtitle, colors, modifier = Modifier.clickable { onEdit(routine) })
                    }
                }
            }
        }
    }
}

/**
 * Create or edit a routine: a name, a gym (required) and an ordered list of exercises available at
 * that gym (its own and shared ones). Changing the gym drops exercises not available there.
 */
@Composable
fun RoutineEditorDialog(
    routine: Routine?,
    initialExerciseIds: List<Long>,
    gyms: List<Gym>,
    exercises: List<Exercise>,
    defaultGymId: Long?,
    onDismiss: () -> Unit,
    onSave: (Routine, List<Long>) -> Unit,
    onDelete: (() -> Unit)?
) {
    val colors = FormDefaults.colors()
    var name by rememberSaveable { mutableStateOf(routine?.name ?: "") }
    var nameTouched by rememberSaveable { mutableStateOf(false) }
    var gymId by rememberSaveable {
        mutableStateOf(routine?.gymId ?: defaultGymId?.takeIf { id -> gyms.any { it.id == id } } ?: gyms.firstOrNull()?.id)
    }
    var exerciseIds by rememberSaveable { mutableStateOf(ArrayList(initialExerciseIds)) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val byId = remember(exercises) { exercises.associateBy { it.id } }
    val chosen = exerciseIds.mapNotNull { byId[it] }
    val gymsById = remember(gyms) { gyms.associateBy { it.id } }
    val valid = name.isNotBlank() && gymId != null

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = exerciseIds.indexOfFirst { "re_$it" == from.key }
        val toIndex = exerciseIds.indexOfFirst { "re_$it" == to.key }
        if (fromIndex >= 0 && toIndex >= 0) exerciseIds = ArrayList(exerciseIds).apply { add(toIndex, removeAt(fromIndex)) }
    }

    ListDialogFrame(
        title = stringResource(if (routine == null) R.string.routine_new else R.string.routine_edit_title),
        onDismiss = onDismiss,
        listState = listState,
        actions = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = colors.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = colors.accent) }
                Button(
                    onClick = {
                        val gym = gymId ?: return@Button
                        val base = routine ?: Routine(name = "", gymId = gym)
                        onSave(base.copy(name = name.trim(), gymId = gym), exerciseIds.toList())
                    },
                    enabled = valid,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent)
                ) { Text(stringResource(R.string.save)) }
            }
        }
    ) {
        item(key = "name") {
            FormTextField(
                value = name,
                onValueChange = { name = it; nameTouched = true },
                label = stringResource(R.string.routine_name_label),
                errorText = if (nameTouched && name.isBlank()) stringResource(R.string.routine_name_required) else null
            )
        }
        item(key = "gym") {
            GymPickerField(
                options = gymPickerOptions(gyms, excludeShared = true),
                selectedGymId = gymId,
                onSelected = { newGym ->
                    gymId = newGym
                    // Only exercises of the new gym (or shared ones) can stay
                    exerciseIds = ArrayList(exerciseIds.filter { id -> byId[id]?.let { it.gymId == null || it.gymId == newGym } == true })
                },
                colors = colors
            )
        }
        item(key = "exercises_header") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(
                    text = stringResource(R.string.routine_exercises_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.contentSecondary,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                OutlinedButton(onClick = { showPicker = true }, enabled = gymId != null) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.accent)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.routine_add_exercises), color = colors.accent)
                }
            }
        }
        if (chosen.isEmpty()) {
            item(key = "no_exercises") { ListHint(stringResource(R.string.routine_no_exercises), colors) }
        } else if (chosen.size > 1) {
            item(key = "reorder_hint") { ListHint(stringResource(R.string.routine_reorder_hint), colors) }
        }
        items(chosen, key = { "re_${it.id}" }) { exercise ->
            DraggableListRow(
                state = reorderState,
                key = "re_${exercise.id}",
                enabled = true,
                handleDescription = stringResource(R.string.reorder_handle_description, exercise.name),
                onDragStopped = {}
            ) {
                ListCard(
                    title = exercise.name,
                    subtitle = exercise.gymId?.let { id -> gymsById[id]?.let { gymLabel(it.name, it.unit) } }
                        ?: stringResource(R.string.gym_shared_section),
                    colors = colors,
                    trailing = {
                        IconButton(onClick = { exerciseIds = ArrayList(exerciseIds - exercise.id) }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.routine_remove_exercise, exercise.name),
                                tint = colors.contentSecondary
                            )
                        }
                    }
                )
            }
        }
    }

    if (showPicker && gymId != null) {
        val candidates = remember(exercises, gymId, exerciseIds) {
            exercisesAvailableAtGym(exercises, gymId!!).filter { it.id !in exerciseIds }.sortedBy { foldForSearch(it.name) }
        }
        ExerciseMultiPickerSheet(
            candidates = candidates,
            gymsById = gymsById,
            onDismiss = { showPicker = false },
            onAdd = { ids ->
                exerciseIds = ArrayList(exerciseIds + ids)
                showPicker = false
            }
        )
    }

    if (confirmDelete && onDelete != null) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.routine_delete_title),
            message = stringResource(R.string.routine_delete_message, routine?.name ?: name),
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false }
        )
    }
}

/** Bottom sheet to pick several exercises (in tap order) from [candidates], with a search field. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseMultiPickerSheet(
    candidates: List<Exercise>,
    gymsById: Map<Long, Gym>,
    onDismiss: () -> Unit,
    onAdd: (List<Long>) -> Unit
) {
    val colors = FormDefaults.colors()
    var query by rememberSaveable { mutableStateOf("") }
    var picked by rememberSaveable { mutableStateOf(ArrayList<Long>()) }
    val shown = remember(candidates, query) {
        val q = foldForSearch(query)
        candidates.filter { q.isEmpty() || foldForSearch(it.name).contains(q) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        SheetContent {
            Text(
                text = stringResource(R.string.routine_add_exercises),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.routine_pick_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = FormDefaults.outlinedTextFieldColors(colors),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
            )
            if (candidates.isEmpty()) {
                Text(
                    stringResource(R.string.routine_pick_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                items(shown, key = { it.id }) { exercise ->
                    val checked = exercise.id in picked
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { picked = ArrayList(if (checked) picked - exercise.id else picked + exercise.id) }
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = colors.accent, checkmarkColor = colors.onAccent)
                        )
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(exercise.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = exercise.gymId?.let { id -> gymsById[id]?.let { gymLabel(it.name, it.unit) } }
                                    ?: stringResource(R.string.gym_shared_section),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = colors.accent) }
                Button(
                    onClick = { onAdd(picked.toList()) },
                    enabled = picked.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent)
                ) { Text(stringResource(R.string.routine_pick_add_format, picked.size)) }
            }
        }
    }
}

@Composable
private fun SheetContent(content: @Composable ColumnScope.() -> Unit) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.8f).dp
    Column(modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).navigationBarsPadding(), content = content)
}
