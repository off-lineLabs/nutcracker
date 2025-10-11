package com.offlinelabs.nutcracker.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.offlinelabs.nutcracker.R
import androidx.compose.ui.text.style.TextAlign
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.ui.theme.getContrastingTextColor
import com.offlinelabs.nutcracker.data.model.ExerciseCategoryMapper
import com.offlinelabs.nutcracker.data.model.ExerciseType
import com.offlinelabs.nutcracker.ui.theme.*
import com.offlinelabs.nutcracker.ui.components.ExerciseImageIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectExerciseForCheckInDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onAddExercise: () -> Unit,
    onSelectExercise: (Exercise) -> Unit,
    onEditExercise: (Exercise) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.my_exercises),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Add new exercise button
                Button(
                    onClick = onAddExercise,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ballot),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.type_information))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Existing exercises list
                if (exercises.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_exercises_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(exercises) { exercise ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectExercise(exercise) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Exercise image or icon
                                    ExerciseImageIcon(
                                        exercise = exercise,
                                        size = 56.dp,
                                        showShadow = true
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exercise.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = when (ExerciseCategoryMapper.getExerciseType(exercise.category)) {
                                                ExerciseType.STRENGTH -> stringResource(R.string.strength)
                                                ExerciseType.CARDIO -> stringResource(R.string.cardio)
                                                ExerciseType.BODYWEIGHT -> stringResource(R.string.bodyweight)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    // Edit button with pencil icon on the right
                                    IconButton(
                                        onClick = { onEditExercise(exercise) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = stringResource(R.string.edit_exercise),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                )
            }
        }
    )
}
