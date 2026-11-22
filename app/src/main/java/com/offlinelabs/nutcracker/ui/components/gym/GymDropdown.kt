package com.offlinelabs.nutcracker.ui.components.gym

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.Gym

/**
 * Gym selector at the top of the Exercise tab. The whole header is the tap target: it shows the
 * selected gym's name (bold) with its optional unit below, or "All gyms" when [selectedGym] is null.
 * The menu lists "All gyms", each gym of [gyms], then "Add gym…" and "Manage gyms…".
 */
@Composable
fun GymDropdown(
    selectedGym: Gym?,
    gyms: List<Gym>,
    onSelectAll: () -> Unit,
    onSelectGym: (Long) -> Unit,
    onAddGym: () -> Unit,
    onManageGyms: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val clickLabel = stringResource(R.string.gym_dropdown_click_label)
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClickLabel = clickLabel, role = Role.Button) { expanded = true }
                .heightIn(min = 48.dp)
                .padding(start = 4.dp, end = 0.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GymNameText(
                name = selectedGym?.name ?: stringResource(R.string.gym_all_gyms),
                unit = selectedGym?.unit,
                nameStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                unitStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 200.dp)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.gym_all_gyms),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                trailingIcon = { if (selectedGym == null) SelectedCheck() },
                onClick = {
                    expanded = false
                    onSelectAll()
                }
            )
            gyms.forEach { gym ->
                DropdownMenuItem(
                    text = {
                        GymNameText(
                            name = gym.name,
                            unit = gym.unit,
                            nameStyle = MaterialTheme.typography.bodyLarge,
                            unitStyle = MaterialTheme.typography.bodySmall
                        )
                    },
                    trailingIcon = { if (gym.id == selectedGym?.id) SelectedCheck() },
                    onClick = {
                        expanded = false
                        onSelectGym(gym.id)
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.gym_menu_add), maxLines = 2) },
                onClick = {
                    expanded = false
                    onAddGym()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.gym_menu_manage), maxLines = 2) },
                onClick = {
                    expanded = false
                    onManageGyms()
                }
            )
        }
    }
}

@Composable
private fun SelectedCheck() {
    Icon(
        imageVector = Icons.Filled.Check,
        contentDescription = stringResource(R.string.gym_selected),
        // primary is navy on the dark menu surface; onSurface reads in both themes
        tint = MaterialTheme.colorScheme.onSurface
    )
}

/** Gym name with the unit (if any) on a smaller second line. Text is shown as typed. */
@Composable
fun GymNameText(
    name: String,
    unit: String?,
    nameStyle: androidx.compose.ui.text.TextStyle,
    unitStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        Text(
            text = name,
            style = nameStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!unit.isNullOrBlank()) {
            Text(
                text = unit,
                style = unitStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
