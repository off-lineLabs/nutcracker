package com.example.template.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.template.data.model.Piece

@Composable
fun AddPieceDialog(onDismiss: () -> Unit, onAddPiece: (Piece) -> Unit) {
	var nameInput by rememberSaveable { mutableStateOf("") }
	var valueInput by rememberSaveable { mutableStateOf("") }
	var nameError by remember { mutableStateOf<String?>(null) }
	var valueError by remember { mutableStateOf<String?>(null) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Add New Piece") },
		text = {
			Column {
				OutlinedTextField(
					value = nameInput,
					onValueChange = { nameInput = it; nameError = null },
					label = { Text("Piece Name") },
					isError = nameError != null
				)
				if (nameError != null) {
					Text(nameError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
				}
				Spacer(modifier = Modifier.height(8.dp))
				OutlinedTextField(
					value = valueInput,
					onValueChange = { valueInput = it; valueError = null },
					label = { Text("Piece Value") },
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
					isError = valueError != null
				)
				if (valueError != null) {
					Text(valueError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
				}
			}
		},
		confirmButton = {
			Button(
				onClick = {
					val name = nameInput.trim()
					val value = valueInput.toIntOrNull()
					var isValid = true
					if (name.isEmpty()) {
						nameError = "Name cannot be empty."
						isValid = false
					}
					if (value == null || value <= 0) {
						valueError = "Enter a valid positive value."
						isValid = false
					}

					if (isValid) {
						onAddPiece(Piece(name, value!!))
					}
				}
			) { Text("Add") }
		},
		dismissButton = {
			Button(onClick = onDismiss) { Text("Cancel") }
		}
	)
}


