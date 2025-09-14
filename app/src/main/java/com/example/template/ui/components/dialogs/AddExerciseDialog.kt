package com.example.template.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.template.R
import com.example.template.data.model.Exercise
import com.example.template.data.model.ExternalExercise
import com.example.template.data.model.ExerciseType
import com.example.template.data.model.ExerciseCategoryMapper
import com.example.template.data.model.toInternalExercise
import com.example.template.util.logger.AppLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    externalExercise: ExternalExercise? = null,
    existingExercise: Exercise? = null,
    onDismiss: () -> Unit,
    onAddExercise: (Exercise) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf("") }
    var exerciseType by remember { mutableStateOf(ExerciseType.STRENGTH) }
    var kcalPerRep by remember { mutableStateOf("") }
    var kcalPerMinute by remember { mutableStateOf("") }
    var defaultWeight by remember { mutableStateOf("") }
    var defaultReps by remember { mutableStateOf("") }
    var defaultSets by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Pre-populate fields when external exercise or existing exercise is provided
    LaunchedEffect(externalExercise, existingExercise) {
        AppLogger.i("AddExerciseDialog", "LaunchedEffect triggered with externalExercise: ${externalExercise?.name}, existingExercise: ${existingExercise?.name}")
        // Handle existing exercise (edit mode)
        existingExercise?.let { exercise ->
            name = exercise.name
            exerciseType = ExerciseCategoryMapper.getExerciseType(exercise.category)
            kcalPerRep = exercise.kcalBurnedPerRep?.toString() ?: ""
            kcalPerMinute = exercise.kcalBurnedPerMinute?.toString() ?: ""
            defaultWeight = exercise.defaultWeight.toString()
            defaultReps = exercise.defaultReps.toString()
            defaultSets = exercise.defaultSets.toString()
            notes = exercise.notes ?: ""
        }
        
        // Handle external exercise (import mode)
        externalExercise?.let { exercise ->
            name = exercise.name
            exerciseType = ExerciseCategoryMapper.getExerciseType(exercise.category)
            
            // Set default values based on exercise type and category
            when (exerciseType) {
                ExerciseType.STRENGTH -> {
                    defaultWeight = when (exercise.equipment?.lowercase()) {
                        "barbell", "dumbbell", "kettlebells" -> "20.0"
                        "body only" -> "0.0"
                        else -> "0.0"
                    }
                    defaultReps = when (exercise.category.lowercase()) {
                        "strength", "strongman", "olympic weightlifting" -> "8"
                        else -> "8"
                    }
                    defaultSets = when (exercise.category.lowercase()) {
                        "strength", "strongman", "olympic weightlifting" -> "3"
                        else -> "3"
                    }
                }
                ExerciseType.CARDIO -> {
                    // Default kcal per minute for cardio
                    kcalPerMinute = "8.0"
                }
                ExerciseType.BODYWEIGHT -> {
                    defaultReps = when (exercise.category.lowercase()) {
                        "stretching" -> "1"
                        "cardio", "plyometrics" -> "10"
                        else -> "10"
                    }
                    defaultSets = when (exercise.category.lowercase()) {
                        "stretching" -> "1"
                        "cardio", "plyometrics" -> "1"
                        else -> "1"
                    }
                }
            }
            
            // Don't copy instructions to notes - let user add their own notes
            // Instructions will be stored separately in the instructions field
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingExercise != null) stringResource(R.string.edit_exercise) else stringResource(R.string.add_exercise),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.exercise_type),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (exerciseType) {
                                ExerciseType.STRENGTH -> stringResource(R.string.strength)
                                ExerciseType.CARDIO -> stringResource(R.string.cardio)
                                ExerciseType.BODYWEIGHT -> stringResource(R.string.bodyweight)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExerciseType.values().forEach { type ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (exerciseType == type) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { exerciseType = type }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (type) {
                                        ExerciseType.CARDIO -> Icons.Filled.Favorite
                                        ExerciseType.BODYWEIGHT -> Icons.Filled.SportsGymnastics
                                        ExerciseType.STRENGTH -> Icons.Filled.FitnessCenter
                                    },
                                    contentDescription = null,
                                    tint = if (exerciseType == type) 
                                        Color.White 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Calorie burn rate fields based on exercise type
                when (exerciseType) {
                    ExerciseType.STRENGTH -> {
                        OutlinedTextField(
                            value = kcalPerRep,
                            onValueChange = { kcalPerRep = it },
                            label = { Text(stringResource(R.string.kcal_per_set)) },
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
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = defaultWeight,
                            onValueChange = { defaultWeight = it },
                            label = { Text(stringResource(R.string.default_weight_kg)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            )
                        )
                        OutlinedTextField(
                            value = defaultReps,
                            onValueChange = { defaultReps = it },
                            label = { Text(stringResource(R.string.default_reps)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                        OutlinedTextField(
                            value = defaultSets,
                            onValueChange = { defaultSets = it },
                            label = { Text(stringResource(R.string.default_sets)) },
                            modifier = Modifier.fillMaxWidth(),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete button (only in edit mode when onDelete is provided) - positioned at left
                if (existingExercise != null && onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    // Empty space to maintain layout when no delete button
                    Spacer(modifier = Modifier.size(48.dp))
                }
                
                // Cancel and Add/Update buttons side by side
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            AppLogger.i("AddExerciseDialog", "Creating exercise with externalExercise: ${externalExercise?.name}")
                            val exercise = if (existingExercise != null) {
                                // When editing, preserve all non-editable fields from the existing exercise
                                existingExercise.copy(
                                    name = name.trim(),
                                    category = ExerciseCategoryMapper.getCategory(exerciseType),
                                    kcalBurnedPerRep = kcalPerRep.toDoubleOrNull(),
                                    kcalBurnedPerMinute = kcalPerMinute.toDoubleOrNull(),
                                    defaultWeight = defaultWeight.toDoubleOrNull() ?: 0.0,
                                    defaultReps = defaultReps.toIntOrNull() ?: 0,
                                    defaultSets = defaultSets.toIntOrNull() ?: 0,
                                    notes = notes.takeIf { it.isNotBlank() },
                                    imagePaths = existingExercise.imagePaths // Preserve existing image paths
                                )
                            } else {
                                // When creating new exercise, use the provided external exercise data if available
                                val baseExercise = externalExercise?.toInternalExercise() ?: Exercise(name = "")
                                baseExercise.copy(
                                    name = name.trim(),
                                    category = ExerciseCategoryMapper.getCategory(exerciseType),
                                    kcalBurnedPerRep = kcalPerRep.toDoubleOrNull(),
                                    kcalBurnedPerMinute = kcalPerMinute.toDoubleOrNull(),
                                    defaultWeight = defaultWeight.toDoubleOrNull() ?: 0.0,
                                    defaultReps = defaultReps.toIntOrNull() ?: 0,
                                    defaultSets = defaultSets.toIntOrNull() ?: 0,
                                    notes = notes.takeIf { it.isNotBlank() }
                                )
                            }
                            onAddExercise(exercise)
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text(if (existingExercise != null) stringResource(R.string.update) else stringResource(R.string.add))
                    }
                }
            }
        }
    )
}
