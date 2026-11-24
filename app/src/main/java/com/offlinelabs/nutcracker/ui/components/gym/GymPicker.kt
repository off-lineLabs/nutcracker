package com.offlinelabs.nutcracker.ui.components.gym

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.gymLabel
import com.offlinelabs.nutcracker.data.model.Gym
import com.offlinelabs.nutcracker.ui.components.form.FormColors
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults

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

private val GymPickerOption.icon: ImageVector
    get() = if (gym == null) Icons.Filled.Public else Icons.Filled.FitnessCenter

@Composable
private fun optionTitle(option: GymPickerOption): String =
    option.gym?.name ?: stringResource(R.string.gym_all_gyms)

@Composable
private fun optionSubtitle(option: GymPickerOption): String? =
    if (option.gym == null) stringResource(R.string.gym_shared_option_hint) else option.gym.unit?.takeIf { it.isNotBlank() }

/**
 * Bottom sheet listing gym targets (e.g. for "Copy to another gym"). Each row shows the gym name
 * with its unit below. Tapping a row calls [onSelected]; swiping down or tapping outside dismisses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymPickerSheet(
    title: String,
    options: List<GymPickerOption>,
    onSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    subtitle: String? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                items(options, key = { it.gymId ?: -1L }) { option ->
                    ListItem(
                        headlineContent = {
                            Text(optionTitle(option), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        supportingContent = optionSubtitle(option)?.let { sub ->
                            { Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        },
                        leadingContent = {
                            Icon(option.icon, contentDescription = null)
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            headlineColor = MaterialTheme.colorScheme.onSurface,
                            supportingColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable { onSelected(option.gymId) }
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Form field for choosing an exercise's gym: an M3 exposed dropdown anchored to the field. The
 * menu shows each gym's name with its unit on a second line, and a check on the current choice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymPickerField(
    options: List<GymPickerOption>,
    selectedGymId: Long?,
    onSelected: (Long?) -> Unit,
    colors: FormColors,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selected = options.firstOrNull { it.gymId == selectedGymId } ?: options.firstOrNull { it.gymId == null }
    val fieldText = selected?.gym?.let { gymLabel(it.name, it.unit) } ?: stringResource(R.string.gym_all_gyms_shared)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = fieldText,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.gym_picker_label)) },
            leadingIcon = { Icon((selected ?: GymPickerOption(null)).icon, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = FormDefaults.outlinedTextFieldColors(colors),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                val isSelected = option.gymId == selected?.gymId
                DropdownMenuItem(
                    text = {
                        GymNameText(
                            name = optionTitle(option),
                            unit = optionSubtitle(option),
                            nameStyle = MaterialTheme.typography.bodyLarge,
                            unitStyle = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = { Icon(option.icon, contentDescription = null) },
                    trailingIcon = if (isSelected) {
                        { Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.gym_selected)) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onSelected(option.gymId)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
