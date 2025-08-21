package com.example.template.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.template.R
import com.example.template.data.model.CheckIn
import com.example.template.data.model.Piece
import com.example.template.data.repo.InMemoryRepository
import com.example.template.data.repo.Repository
import com.example.template.ui.components.dialogs.AddPieceDialog
import com.example.template.ui.components.dialogs.CheckInPieceDialog
import com.example.template.ui.components.dialogs.SetGoalDialog
import com.example.template.ui.components.items.CheckInItem
import com.example.template.ui.components.items.PieceItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(repository: Repository? = null) {
	val repo = repository ?: remember { InMemoryRepository() }
	var goal by rememberSaveable { mutableIntStateOf(repo.getGoal()) }
	var pieces by rememberSaveable { mutableStateOf(repo.getPieces()) }
	var checkIns by rememberSaveable { mutableStateOf(repo.getCheckIns()) }

	var showSetGoalDialog by remember { mutableStateOf(false) }
	var showAddPieceDialog by remember { mutableStateOf(false) }
	var showCheckInDialog by remember { mutableStateOf<Piece?>(null) }

	Scaffold(
		topBar = { TopAppBar(title = {
			Text(
				text = stringResource(id = R.string.progress_title),
				fontSize = 24.sp, // Equivalent to text-2xl (1.5rem * 16px/rem)
				fontWeight = FontWeight.Bold, // Equivalent to font-bold
				color = Color(0xFF1F2937) // Equivalent to text-gray-900. For dark mode, you'd need to handle theme changes.
				// dark:text-white would require observing the current theme and setting color accordingly.
				// For simplicity, this example uses the light mode color.
			) })
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
			Text("Current Goal: ${'$'}goal", style = MaterialTheme.typography.headlineSmall)
			Button(onClick = { showSetGoalDialog = true }, modifier = Modifier.padding(top = 8.dp)) {
				Text("Set Goal")
			}
			Spacer(modifier = Modifier.height(16.dp))

			Text("Your Pieces", style = MaterialTheme.typography.titleMedium)
			if (pieces.isEmpty()) {
				Text("No pieces added yet. Click the '+' button to add one.", modifier = Modifier.padding(8.dp))
			} else {
				LazyColumn(modifier = Modifier
					.weight(1f)
					.fillMaxWidth()) {
					items(pieces, key = { it.id }) { piece ->
						PieceItem(piece = piece, onCheckInClick = { showCheckInDialog = piece })
					}
				}
			}
			Button(
				onClick = { if (pieces.isNotEmpty()) showCheckInDialog = pieces.first() },
				enabled = pieces.isNotEmpty(),
				modifier = Modifier.padding(top = 8.dp)
			) {
				Text("Check In Piece")
			}

			Spacer(modifier = Modifier.height(16.dp))

			Text("Recent Check-ins", style = MaterialTheme.typography.titleMedium)
			if (checkIns.isEmpty()) {
				Text("No check-ins yet.", modifier = Modifier.padding(8.dp))
			} else {
				LazyColumn(modifier = Modifier
					.weight(1f)
					.fillMaxWidth()) {
					items(checkIns.take(5), key = { it.id }) { checkIn ->
						CheckInItem(checkIn = checkIn)
					}
				}
			}
		}
	}

	if (showSetGoalDialog) {
		SetGoalDialog(
			currentGoal = goal,
			onDismiss = { showSetGoalDialog = false },
			onSetGoal = { newGoal ->
				repo.setGoal(newGoal)
				goal = repo.getGoal()
				showSetGoalDialog = false
			}
		)
	}

	if (showAddPieceDialog) {
		AddPieceDialog(
			onDismiss = { showAddPieceDialog = false },
			onAddPiece = { newPiece ->
				repo.addPiece(newPiece)
				pieces = repo.getPieces()
				showAddPieceDialog = false
			}
		)
	}

	showCheckInDialog?.let { pieceToCheckIn ->
		CheckInPieceDialog(
			piece = pieceToCheckIn,
			onDismiss = { showCheckInDialog = null },
			onCheckIn = { piece, percentage ->
				val checkInValue = (piece.value * (percentage / 100.0)).toInt()
				val newGoal = goal - checkInValue
				repo.setGoal(newGoal)
				goal = repo.getGoal()

				val timestamp = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date())
				val newCheckIn = CheckIn(piece.name, piece.id, percentage, timestamp)
				repo.addCheckIn(newCheckIn)
				checkIns = repo.getCheckIns()

				showCheckInDialog = null
			}
		)
	}
}