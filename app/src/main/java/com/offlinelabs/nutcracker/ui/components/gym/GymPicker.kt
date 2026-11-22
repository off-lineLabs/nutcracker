package com.offlinelabs.nutcracker.ui.components.gym

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.gymLabel
import com.offlinelabs.nutcracker.data.model.Gym
import com.offlinelabs.nutcracker.ui.components.form.FormColors

/** One choice of a gym picker; [gym] null means "All gyms (shared)". */
data class GymPickerOption(val gym: Gym?) {
    val gymId: Long? get() = gym?.id
}

/**
 * Options of a gym picker: "All gyms" first (unless [excludeShared]), then [gyms] without
 * [excludeGymId]. Hidden gyms are only included if the caller passes them in [gyms].
 */
fun gymPickerOptions(
    gyms: List<Gym>,
    excludeGymId: Long? = null,
    excludeShared: Boolean = false
): List<GymPickerOption> =
    (if (excludeShared) emptyList() else listOf(GymPickerOption(null))) +
        gyms.filter { it.id != excludeGymId }.map { GymPickerOption(it) }

/** [gyms] plus the gym with [currentGymId] from [hidden] if it is not already listed (hidden gym of an edited exercise). */
fun gymsIncludingCurrent(gyms: List<Gym>, currentGymId: Long?, hidden: Gym?): List<Gym> =
    if (currentGymId != null && hidden != null && hidden.id == currentGymId && gyms.none { it.id == currentGymId }) {
        gyms + hidden
    } else {
        gyms
    }

@Composable
private fun optionText(option: GymPickerOption): String =
    option.gym?.let { gymLabel(it.name, it.unit) } ?: stringResource(R.string.gym_all_gyms_shared)

/** Radio-list dialog. [selectedGymId] is only marked when [showSelection] is true. Back and tapping outside dismiss it. */
@Composable
fun GymPickerDialog(
    title: String,
    options: List<GymPickerOption>,
    selectedGymId: Long?,
    showSelection: Boolean,
    onSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                items(options, key = { it.gymId ?: -1L }) { option ->
                    val selected = showSelection && option.gymId == selectedGymId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelected(option.gymId) })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showSelection) RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(end = 12.dp))
                        Text(
                            text = optionText(option),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/** Form field that shows the chosen gym and opens a [GymPickerDialog]. */
@Composable
fun GymPickerField(
    options: List<GymPickerOption>,
    selectedGymId: Long?,
    onSelected: (Long?) -> Unit,
    colors: FormColors,
    modifier: Modifier = Modifier
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val selected = options.firstOrNull { it.gymId == selectedGymId } ?: options.firstOrNull { it.gymId == null }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.gym_picker_label),
            style = MaterialTheme.typography.labelLarge,
            color = colors.contentSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.field,
            border = BorderStroke(1.dp, colors.fieldBorder),
            modifier = Modifier.fillMaxWidth().clickable(onClickLabel = stringResource(R.string.gym_dropdown_click_label)) { open = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = selected?.let { optionText(it) } ?: stringResource(R.string.gym_all_gyms_shared),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.content,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = colors.contentSecondary)
            }
        }
    }
    if (open) {
        GymPickerDialog(
            title = stringResource(R.string.gym_picker_label),
            options = options,
            selectedGymId = selectedGymId,
            showSelection = true,
            onSelected = { open = false; onSelected(it) },
            onDismiss = { open = false }
        )
    }
}
