package com.offlinelabs.nutcracker.ui.components.gym

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.data.model.Gym
import com.offlinelabs.nutcracker.ui.components.form.FormDefaults
import com.offlinelabs.nutcracker.ui.components.form.FormDialog
import com.offlinelabs.nutcracker.ui.components.form.FormDialogStackedActions
import com.offlinelabs.nutcracker.ui.components.form.FormSection
import com.offlinelabs.nutcracker.ui.components.form.FormSectionDivider
import com.offlinelabs.nutcracker.ui.components.form.FormStackedAction

/** Lists the visible [gyms] with edit and delete actions. */
@Composable
fun ManageGymsDialog(
    gyms: List<Gym>,
    onEdit: (Gym) -> Unit,
    onDelete: (Gym) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FormDefaults.colors()
    FormDialog(
        title = stringResource(R.string.gym_manage_title),
        onDismissRequest = onDismiss,
        actions = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)
                ) {
                    Text(stringResource(R.string.close), maxLines = 2, textAlign = TextAlign.Center)
                }
            }
        }
    ) {
        if (gyms.isEmpty()) {
            Text(
                text = stringResource(R.string.gym_manage_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.contentSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        } else {
            FormSection(modifier = Modifier.padding(bottom = 4.dp)) {
                gyms.forEachIndexed { index, gym ->
                    if (index > 0) FormSectionDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GymNameText(
                            name = gym.name,
                            unit = gym.unit,
                            nameStyle = MaterialTheme.typography.bodyLarge,
                            unitStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onEdit(gym) }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.gym_edit_description, gym.name),
                                tint = colors.accent
                            )
                        }
                        IconButton(onClick = { onDelete(gym) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.gym_delete_description, gym.name),
                                tint = colors.error
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Asks how to delete [gym]: only the gym (its exercises become shared), or the gym with its
 * exercises (hidden; their logs stay in the history), or cancel.
 */
@Composable
fun DeleteGymDialog(
    gym: Gym,
    onDeleteGymOnly: () -> Unit,
    onDeleteWithExercises: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = FormDefaults.colors()
    FormDialog(
        title = stringResource(R.string.gym_delete_title),
        onDismissRequest = onDismiss,
        actions = {
            FormDialogStackedActions(
                primaryText = stringResource(R.string.gym_delete_only),
                onPrimary = onDeleteGymOnly,
                secondary = listOf(
                    FormStackedAction(stringResource(R.string.gym_delete_with_exercises), onDeleteWithExercises)
                ),
                onDismiss = onDismiss,
                colors = colors
            )
        }
    ) {
        Text(
            text = stringResource(R.string.gym_delete_message, gym.name),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.contentSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}
