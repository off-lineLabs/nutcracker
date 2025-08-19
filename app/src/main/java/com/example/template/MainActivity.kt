package com.example.template

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.template.ui.theme.FoodLogTheme // Use your theme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Data classes remain the same
data class Piece(val name: String, val value: Double, val id: String = UUID.randomUUID().toString())
data class CheckIn(val pieceName: String, val pieceId: String, val percentage: Double, val timestamp: String, val id: String = UUID.randomUUID().toString())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodLogTheme { // Apply your app's theme
                CalorieAppScreen()
            }
        }
    }
}

// --------- ViewModel (Optional but Recommended for complex state) --------
// For simplicity in this example, state is managed directly in Composables
// For a more robust app, consider using a ViewModel:
// class CalorieViewModel : ViewModel() {
//    private val _goal = mutableStateOf(0.0)
//    val goal: State<Double> = _goal
//    // ... other state and logic
// }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalorieAppScreen() {
    // State variables
    var goal by rememberSaveable { mutableDoubleStateOf(2000.0) } // Example initial goal
    var pieces by rememberSaveable { mutableStateOf(listOf<Piece>()) }
    var checkIns by rememberSaveable { mutableStateOf(listOf<CheckIn>()) }

    var showSetGoalDialog by remember { mutableStateOf(false) }
    var showAddPieceDialog by remember { mutableStateOf(false) }
    var showCheckInDialog by remember { mutableStateOf<Piece?>(null) } // Holds the piece to check in

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Calorie Tracker") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddPieceDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add New Piece")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Goal Section
            Text("Current Goal: ${String.format("%.2f", goal)}", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { showSetGoalDialog = true }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Set Goal")
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Pieces Section
            Text("Your Pieces", style = MaterialTheme.typography.titleMedium)
            if (pieces.isEmpty()) {
                Text("No pieces added yet. Click the '+' button to add one.", modifier = Modifier.padding(8.dp))
            } else {
                LazyColumn(modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()) {
                    items(pieces, key = { it.id }) { piece ->
                        PieceItem(piece = piece, onCheckInClick = { showCheckInDialog = piece })
                        HorizontalDivider()
                    }
                }
            }
            Button(
                onClick = { if (pieces.isNotEmpty()) showCheckInDialog = pieces.first() /* Example: auto-select first or let user choose from list */ },
                enabled = pieces.isNotEmpty(),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Check In Piece")
            }


            Spacer(modifier = Modifier.height(16.dp))

            // Recent Check-ins Section
            Text("Recent Check-ins", style = MaterialTheme.typography.titleMedium)
            if (checkIns.isEmpty()) {
                Text("No check-ins yet.", modifier = Modifier.padding(8.dp))
            } else {
                LazyColumn(modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()) {
                    items(checkIns.take(5), key = { it.id }) { checkIn -> // Display latest 5
                        CheckInItem(checkIn = checkIn)
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    // --- Dialogs ---
    if (showSetGoalDialog) {
        SetGoalDialog(
            currentGoal = goal,
            onDismiss = { showSetGoalDialog = false },
            onSetGoal = { newGoal ->
                goal = newGoal
                showSetGoalDialog = false
            }
        )
    }

    if (showAddPieceDialog) {
        AddPieceDialog(
            onDismiss = { showAddPieceDialog = false },
            onAddPiece = { newPiece ->
                pieces = pieces + newPiece // Add to the list
                showAddPieceDialog = false
            }
        )
    }

    showCheckInDialog?.let { pieceToCheckIn ->
        CheckInPieceDialog(
            piece = pieceToCheckIn,
            onDismiss = { showCheckInDialog = null },
            onCheckIn = { piece, percentage ->
                val checkInValue = piece.value * (percentage / 100.0)
                goal -= checkInValue // Direct mutation; consider ViewModel for complex logic

                val timestamp = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date())
                val newCheckIn = CheckIn(piece.name, piece.id, percentage, timestamp)
                checkIns = listOf(newCheckIn) + checkIns // Add to the beginning of the list

                showCheckInDialog = null
            }
        )
    }
}

@Composable
fun PieceItem(piece: Piece, onCheckInClick: (Piece) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckInClick(piece) } // Make the whole item clickable for check-in
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("${piece.name}: ${String.format("%.2f", piece.value)}")
        // Optionally, add a specific "Check In" button per item if preferred
        // Button(onClick = { onCheckInClick(piece) }) { Text("Check In") }
    }
}

@Composable
fun CheckInItem(checkIn: CheckIn) {
    Text(
        text = "${checkIn.timestamp}: ${checkIn.percentage}% of ${checkIn.pieceName}",
        modifier = Modifier.padding(vertical = 8.dp)
    )
}


// --- Dialog Composable Functions ---

@Composable
fun SetGoalDialog(currentGoal: Double, onDismiss: () -> Unit, onSetGoal: (Double) -> Unit) {
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
                        inputError = null // Clear error on change
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
                    val newGoal = goalInput.toDoubleOrNull()
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
                    val value = valueInput.toDoubleOrNull()
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

@Composable
fun CheckInPieceDialog(piece: Piece, onDismiss: () -> Unit, onCheckIn: (Piece, Double) -> Unit) {
    var percentageInput by rememberSaveable { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }


    // If you want a list selection first (if `showCheckInDialog` was just a boolean)
    // You would manage a separate state for the selected piece from a list of pieces.
    // For this example, we assume `piece` is already the selected one.

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Check in ${piece.name}") },
        text = {
            Column {
                Text("Current value: ${piece.value}")
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


// --- Previews ---
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FoodLogTheme {
        CalorieAppScreen()
    }
}

@Preview(showBackground = true, widthDp = 200)
@Composable
fun PieceItemPreview() {
    FoodLogTheme {
        PieceItem(piece = Piece("Apple", 95.0), onCheckInClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun CheckInItemPreview() {
    FoodLogTheme {
        CheckInItem(checkIn = CheckIn("Apple", "id1", 50.0, "12/25 10:00"))
    }
}

@Preview(showBackground = true)
@Composable
fun SetGoalDialogPreview() {
    FoodLogTheme {
        SetGoalDialog(currentGoal = 2000.0, onDismiss = {}, onSetGoal = {})
    }
}

@Preview(showBackground = true)
@Composable
fun AddPieceDialogPreview() {
    FoodLogTheme {
        AddPieceDialog(onDismiss = {}, onAddPiece = {})
    }
}
