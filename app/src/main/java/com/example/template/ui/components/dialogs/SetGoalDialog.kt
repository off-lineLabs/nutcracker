package com.example.template.ui.components.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun SetGoalDialog(currentGoal: Int, onDismiss: () -> Unit, onSetGoal: (Int) -> Unit) {
	var goalInput by rememberSaveable { mutableStateOf(currentGoal.toString()) }
	var inputError by remember { mutableStateOf<String?>(null) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Set Your Goal") },
		text = {
			Column {
				OutlinedTextField(
					value = goalInput,
					onValueChange = {
						goalInput = it
						inputError = null
					},
					label = { Text("Goal Value") },
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
					isError = inputError != null
				)
				if (inputError != null) {
					Text(inputError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
				}
			}
		},
		confirmButton = {
			Button(
				onClick = {
					val newGoal = goalInput.toIntOrNull()
					if (newGoal != null && newGoal > 0) {
						onSetGoal(newGoal)
					} else {
						inputError = "Please enter a valid positive number."
					}
				}
			) { Text("Set") }
		},
		dismissButton = {
			Button(onClick = onDismiss) { Text("Cancel") }
		}
	)
}


