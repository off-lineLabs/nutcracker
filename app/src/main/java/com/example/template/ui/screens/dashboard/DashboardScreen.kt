package com.example.template.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
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

	val lightGray50 = Color(0xFFF9FAFB)
	val lightGray100 = Color(0xFFF3F4F6)
	val darkGray800 = Color(0xFF1F2937)
	val darkGray900 = Color(0xFF111827)
	val textGray200 = Color(0xFFE5E7EB) // For text on dark backgrounds

	val gradientStartColor = if (isSystemInDarkTheme()) darkGray900 else lightGray50
	val gradientEndColor = if (isSystemInDarkTheme()) darkGray800 else lightGray100
	val innerContainerBackgroundColor = darkGray800 // rgb(31 41 55) which is gray-800

	Scaffold(
		topBar = {
			TopAppBar(
				title = {
					Text(
						text = stringResource(id = R.string.progress_title),
						fontSize = 24.sp,
						fontWeight = FontWeight.Bold,
						color = Color(0xFFFFFFFF), // Original color, consider making dynamic if needed
						textAlign = TextAlign.Center,
						modifier = Modifier.fillMaxWidth()
					)
				},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent // Make TopAppBar transparent to see gradient
                )
			)
		},
		floatingActionButton = {
			FloatingActionButton(onClick = { showAddPieceDialog = true }) {
				Icon(Icons.Filled.Add, contentDescription = "Add New Piece")
			}
		},
        // Make Scaffold background transparent to allow outer Box to control it
        containerColor = Color.Transparent
	) { scaffoldPaddingValues ->
        Box( // Outer container for gradient background and overall padding
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(gradientStartColor, gradientEndColor)
                    )
                )
                .padding(scaffoldPaddingValues) // Apply scaffold padding here for TopAppBar, FAB
                .padding(16.dp), // 1rem (16.dp) overall screen padding
            contentAlignment = Alignment.Center
        ) {
            Box( // Inner container with solid background, padding, and rounded corners
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = innerContainerBackgroundColor,
                        shape = RoundedCornerShape(24.dp) // 1.5rem (24.dp) border-radius
                    )
                    .clip(RoundedCornerShape(24.dp)) // Clip contents to rounded shape
                    .padding(24.dp) // 1.5rem (24.dp) internal padding
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Current Goal: ${'$'}goal",
                        style = MaterialTheme.typography.headlineSmall,
                        color = textGray200
                    )
                    Button(onClick = { showSetGoalDialog = true }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Set Goal") // Button text color should be handled by Button's own theming
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Your Pieces",
                        style = MaterialTheme.typography.titleMedium,
                        color = textGray200
                    )
                    if (pieces.isEmpty()) {
                        Text(
                            text = "No pieces added yet. Click the '+' button to add one.",
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Normal,
                            color = textGray200
                        )
                    } else {
                        LazyColumn(modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()) {
                            items(pieces, key = { it.id }) { piece ->
                                // Assuming PieceItem handles its own text colors appropriately for a dark bg
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

                    Text(
                        "Recent Check-ins",
                        style = MaterialTheme.typography.titleMedium,
                        color = textGray200
                    )
                    if (checkIns.isEmpty()) {
                        Text(
                            text = "No check-ins yet.",
                            modifier = Modifier.padding(8.dp),
                            fontStyle = FontStyle.Normal,
                            color = textGray200
                        )
                    } else {
                        LazyColumn(modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()) {
                            items(checkIns.take(5), key = { it.id }) { checkIn ->
                                // Assuming CheckInItem handles its own text colors
                                CheckInItem(checkIn = checkIn)
                            }
                        }
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
