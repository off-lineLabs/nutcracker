package com.example.template.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.template.R
import com.example.template.data.model.Exercise
import com.example.template.data.model.ExerciseType
import com.example.template.data.model.ExerciseCategoryMapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onAddExercise: (Exercise) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var exerciseType by remember { mutableStateOf(ExerciseType.STRENGTH) }
    var kcalPerRep by remember { mutableStateOf("") }
    var kcalPerMinute by remember { mutableStateOf("") }
    var defaultWeight by remember { mutableStateOf("") }
    var defaultReps by remember { mutableStateOf("") }
    var defaultSets by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.add_exercise),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Exercise name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.exercise_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Exercise type
                Column {
                    Text(
                        text = stringResource(R.string.exercise_type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExerciseType.values().forEach { type ->
                            FilterChip(
                                selected = exerciseType == type,
                                onClick = { exerciseType = type },
                                label = {
                                    Text(
                                        when (type) {
                                            ExerciseType.STRENGTH -> stringResource(R.string.strength)
                                            ExerciseType.CARDIO -> stringResource(R.string.cardio)
                                            ExerciseType.BODYWEIGHT -> stringResource(R.string.bodyweight)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                // Calorie burn rate fields based on exercise type
                when (exerciseType) {
                    ExerciseType.STRENGTH -> {
                        OutlinedTextField(
                            value = kcalPerRep,
                            onValueChange = { kcalPerRep = it },
                            label = { Text(stringResource(R.string.kcal_per_rep)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            )
                        )
                    }
                    ExerciseType.CARDIO -> {
                        OutlinedTextField(
                            value = kcalPerMinute,
                            onValueChange = { kcalPerMinute = it },
                            label = { Text(stringResource(R.string.kcal_per_minute)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            )
                        )
                    }
                    ExerciseType.BODYWEIGHT -> {
                        OutlinedTextField(
                            value = kcalPerRep,
                            onValueChange = { kcalPerRep = it },
                            label = { Text(stringResource(R.string.kcal_per_rep)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            )
                        )
                    }
                }

                // Default values (for strength exercises)
                if (exerciseType == ExerciseType.STRENGTH) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = defaultWeight,
                            onValueChange = { defaultWeight = it },
                            label = { Text(stringResource(R.string.default_weight_kg)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            )
                        )
                        OutlinedTextField(
                            value = defaultReps,
                            onValueChange = { defaultReps = it },
                            label = { Text(stringResource(R.string.default_reps)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                        OutlinedTextField(
                            value = defaultSets,
                            onValueChange = { defaultSets = it },
                            label = { Text(stringResource(R.string.default_sets)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val exercise = Exercise(
                        name = name.trim(),
                        category = ExerciseCategoryMapper.getCategory(exerciseType),
                        kcalBurnedPerRep = kcalPerRep.toDoubleOrNull(),
                        kcalBurnedPerMinute = kcalPerMinute.toDoubleOrNull(),
                        defaultWeight = defaultWeight.toDoubleOrNull() ?: 0.0,
                        defaultReps = defaultReps.toIntOrNull() ?: 0,
                        defaultSets = defaultSets.toIntOrNull() ?: 0,
                        notes = notes.takeIf { it.isNotBlank() }
                    )
                    onAddExercise(exercise)
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
