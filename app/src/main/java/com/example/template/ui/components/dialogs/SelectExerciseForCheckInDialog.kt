package com.example.template.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.template.R
import com.example.template.data.model.Exercise
import com.example.template.data.model.ExerciseCategoryMapper
import com.example.template.data.model.ExerciseType
import com.example.template.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectExerciseForCheckInDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onAddExercise: () -> Unit,
    onSelectExercise: (Exercise) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.select_exercise),
                style = MaterialTheme.typography.headlineSmall
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
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_new_exercise))
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
                    Text(
                        text = stringResource(R.string.select_existing_exercise),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
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
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Exercise icon with modern gradient and shadow - brand red
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .shadow(
                                                elevation = 6.dp,
                                                shape = RoundedCornerShape(16.dp),
                                                ambientColor = exerciseItemBackgroundColor().copy(alpha = 0.3f),
                                                spotColor = exerciseItemBackgroundColor().copy(alpha = 0.3f)
                                            )
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        exerciseItemBackgroundColor(),
                                                        exerciseItemBackgroundColor().copy(alpha = 0.8f)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (ExerciseCategoryMapper.getExerciseType(exercise.category)) {
                                                ExerciseType.CARDIO -> Icons.Filled.Favorite // ECG heart icon
                                                ExerciseType.BODYWEIGHT -> Icons.Filled.SportsGymnastics // Sports gymnastics icon
                                                ExerciseType.STRENGTH -> Icons.Filled.FitnessCenter // Exercise icon
                                            },
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
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
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
