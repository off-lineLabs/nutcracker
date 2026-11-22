package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.ui.components.form.FormColors
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import com.offlinelabs.nutcracker.ui.components.muscleLabel
import com.offlinelabs.nutcracker.ui.components.rememberMuscleLabeler

/**
 * Compact multi-select for an exercise's primary muscles: the selected ones as removable chips, a
 * search field, and one horizontally scrolling row of suggestions that narrows as you type
 * (translated names and stored keys both match). Typing a muscle that isn't listed offers to add
 * it as a custom one; Done adds the best match.
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
    var query by rememberSaveable { mutableStateOf("") }
    val label = rememberMuscleLabeler()
    val suggestions = remember(options, selected, query) { muscleSuggestions(options, selected, query, label) }
    val typed = remember(options, query) { resolveTypedMuscle(options, query, label) }
    // Offer "Add …" when the typed text is not one of the listed muscles
    val customToAdd = typed?.takeIf { it !in options && it !in selected }

    fun add(muscle: String) {
        if (muscle !in selected) onSelectedChange(selected + muscle)
        query = ""
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.muscle_groups_label),
            style = MaterialTheme.typography.labelLarge,
            color = colors.contentSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        if (selected.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                selected.forEach { muscle ->
                    val name = muscleLabel(muscle)
                    InputChip(
                        selected = true,
                        onClick = { onSelectedChange(selected - muscle) },
                        label = { Text(name) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.muscle_remove_description, name),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = colors.accent,
                            selectedLabelColor = colors.onAccent,
                            selectedTrailingIconColor = colors.onAccent
                        ),
                        border = null
                    )
                }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.muscle_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                (suggestions.firstOrNull()?.takeIf { query.isNotBlank() } ?: typed)?.let(::add)
            }),
            colors = FormDefaults.outlinedTextFieldColors(colors),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
        if (suggestions.isNotEmpty() || customToAdd != null) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                customToAdd?.let { custom ->
                    item(key = "custom") {
                        AssistChip(
                            onClick = { add(custom) },
                            label = { Text(stringResource(R.string.muscle_add_custom, query.trim())) },
                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = colors.content,
                                leadingIconContentColor = colors.content
                            ),
                            border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = colors.accent)
                        )
                    }
                }
                items(suggestions, key = { it }) { muscle ->
                    SuggestionChip(
                        onClick = { add(muscle) },
                        label = { Text(muscleLabel(muscle)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(labelColor = colors.content),
                        border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = colors.fieldBorder)
                    )
                }
            }
        }
    }
}
