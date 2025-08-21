package com.example.template.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.example.template.data.model.Piece

@Composable
fun CheckInPieceDialog(piece: Piece, onDismiss: () -> Unit, onCheckIn: (Piece, Double) -> Unit) {
	var percentageInput by rememberSaveable { mutableStateOf("") }
	var inputError by remember { mutableStateOf<String?>(null) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Check in ${'$'}{piece.name}") },
		text = {
			Column {
				Text("Current value: ${'$'}{piece.value}")
				OutlinedTextField(
					value = percentageInput,
					onValueChange = { percentageInput = it; inputError = null },
					label = { Text("Percentage (0-100)") },
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
					val percentage = percentageInput.toDoubleOrNull()
					if (percentage != null && percentage in 0.0..100.0) {
						onCheckIn(piece, percentage)
					} else {
						inputError = "Enter a valid percentage (0-100)."
					}
				}
			) { Text("Check In") }
		},
		dismissButton = {
			Button(onClick = onDismiss) { Text("Cancel") }
		}
	)
}


