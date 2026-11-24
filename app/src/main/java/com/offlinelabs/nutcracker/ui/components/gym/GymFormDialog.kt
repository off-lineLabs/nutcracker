package com.offlinelabs.nutcracker.ui.components.gym

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.Gym
import com.offlinelabs.nutcracker.ui.components.form.FormDialog
import com.offlinelabs.nutcracker.ui.components.form.FormDialogActions
import com.offlinelabs.nutcracker.ui.components.form.FormSection
import com.offlinelabs.nutcracker.ui.components.form.FormTextField

/**
 * Add ([gym] == null) or edit a gym. Typed values survive rotation. [onSave] receives the gym with
 * trimmed fields; on edit it keeps the id and the (UI-less) maps fields of [gym].
 */
@Composable
fun GymFormDialog(
    gym: Gym?,
    onDismiss: () -> Unit,
    onSave: (Gym) -> Unit
) {
    var name by rememberSaveable(gym?.id) { mutableStateOf(gym?.name.orEmpty()) }
    var unit by rememberSaveable(gym?.id) { mutableStateOf(gym?.unit.orEmpty()) }
    var nameTouched by rememberSaveable(gym?.id) { mutableStateOf(false) }
    val nameValid = isGymNameValid(name)
    val save = { if (nameValid) onSave(buildGymFromForm(gym, name, unit)) }

    FormDialog(
        title = stringResource(if (gym == null) R.string.gym_form_add_title else R.string.gym_form_edit_title),
        onDismissRequest = onDismiss,
        actions = {
            FormDialogActions(
                confirmText = stringResource(R.string.save),
                onConfirm = save,
                onDismiss = onDismiss,
                confirmEnabled = nameValid
            )
        }
    ) {
        FormSection(modifier = Modifier.padding(bottom = 4.dp)) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FormTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameTouched = true
                    },
                    label = stringResource(R.string.gym_name_label),
                    errorText = if (nameTouched && !nameValid) stringResource(R.string.gym_name_required) else null,
                    capitalization = KeyboardCapitalization.Words
                )
                FormTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = stringResource(R.string.gym_unit_label),
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    capitalization = KeyboardCapitalization.Words
                )
            }
        }
    }
}
