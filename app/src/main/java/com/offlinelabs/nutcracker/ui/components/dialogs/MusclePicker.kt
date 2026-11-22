package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.ui.components.form.FormColors
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults

/**
 * Multi-select chips for an exercise's primary muscle groups. [options] are the choices (see
 * [muscleOptions]); "Other…" reveals a field to type a muscle that is not listed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MusclePicker(
    options: List<String>,
    selected: List<String>,
    onSelectedChange: (List<String>) -> Unit,
    colors: FormColors,
    modifier: Modifier = Modifier
) {
    var showCustom by rememberSaveable { mutableStateOf(false) }
    var custom by rememberSaveable { mutableStateOf("") }

    fun addCustom() {
        val value = normalizeMuscle(custom)
        if (value.isNotEmpty() && value !in selected) onSelectedChange(selected + value)
        custom = ""
        showCustom = false
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.muscle_groups_label),
            style = MaterialTheme.typography.labelLarge,
            color = colors.contentSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        val chipColors = FilterChipDefaults.filterChipColors(
            containerColor = colors.field,
            labelColor = colors.content,
            selectedContainerColor = colors.accent,
            selectedLabelColor = colors.onAccent,
            selectedLeadingIconColor = colors.onAccent
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            options.forEach { muscle ->
                val isSelected = muscle in selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectedChange(toggleMuscle(selected, muscle)) },
                    label = { Text(muscle.replaceFirstChar { it.uppercase() }) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                    colors = chipColors,
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = colors.fieldBorder,
                        selectedBorderColor = colors.accent
                    )
                )
            }
            if (!showCustom) {
                AssistChip(
                    onClick = { showCustom = true },
                    label = { Text(stringResource(R.string.muscle_other)) },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = colors.content,
                        leadingIconContentColor = colors.content
                    ),
                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = colors.fieldBorder)
                )
            }
        }
        if (showCustom) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it },
                    label = { Text(stringResource(R.string.muscle_custom_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { addCustom() }),
                    colors = FormDefaults.outlinedTextFieldColors(colors),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { addCustom() },
                    enabled = custom.isNotBlank(),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = colors.accent),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(stringResource(R.string.add))
                }
            }
        }
    }
}
