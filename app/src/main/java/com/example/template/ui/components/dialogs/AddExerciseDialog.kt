package com.example.template.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.template.ui.theme.getContrastingTextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    externalExercise: ExternalExercise? = null,
    existingExercise: Exercise? = null,
    onDismiss: () -> Unit,
    onAddExercise: (Exercise) -> Unit,
    onDelete: (() -> Unit)? = null,
    onSaveAndCheckIn: ((Exercise) -> Unit)? = null,
    onJustSave: ((Exercise) -> Unit)? = null,
    onJustCheckIn: ((Exercise) -> Unit)? = null
) {
    var name by remember { mutableStateOf("") }
    var exerciseType by remember { mutableStateOf(ExerciseType.STRENGTH) }
    var kcalPerUnit by remember { mutableStateOf("") }
    var defaultWeight by remember { mutableStateOf("") }
    var defaultReps by remember { mutableStateOf("") }
    var defaultSets by remember { mutableStateOf("") }
    var defaultMinutes by remember { mutableStateOf("") } // For Cardio exercises
    var defaultBodyweightReps by remember { mutableStateOf("") } // For Bodyweight exercises
    var notes by remember { mutableStateOf("") }

    // Pre-populate fields when external exercise or existing exercise is provided
    LaunchedEffect(externalExercise, existingExercise) {
        AppLogger.i("AddExerciseDialog", "LaunchedEffect triggered with externalExercise: ${externalExercise?.name}, existingExercise: ${existingExercise?.name}")
        // Handle existing exercise (edit mode)
        existingExercise?.let { exercise ->
            name = exercise.name
            exerciseType = ExerciseCategoryMapper.getExerciseType(exercise.category)
            kcalPerUnit = exercise.kcalBurnedPerUnit?.toString() ?: ""
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
                    kcalPerUnit = "8.0"
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = if (existingExercise != null) stringResource(R.string.edit_exercise) else stringResource(R.string.add_exercise),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }
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

                // Calorie burn rate field with appropriate label based on exercise type
                OutlinedTextField(
                    value = kcalPerUnit,
                    onValueChange = { kcalPerUnit = it },
                    label = { 
                        Text(
                            when (exerciseType) {
                                ExerciseType.STRENGTH -> stringResource(R.string.kcal_per_set)
                                ExerciseType.CARDIO -> stringResource(R.string.kcal_per_minute)
                                ExerciseType.BODYWEIGHT -> stringResource(R.string.kcal_per_rep)
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )

                // Default values (for strength exercises) - 2x2 layout
                if (exerciseType == ExerciseType.STRENGTH) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // First row: Weight and Reps
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
                        }
                        // Second row: Sets and empty space
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                            // Empty space to maintain 2x2 layout
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                
                // Additional fields for Cardio and Bodyweight exercises
                when (exerciseType) {
                    ExerciseType.CARDIO -> {
                        OutlinedTextField(
                            value = defaultMinutes,
                            onValueChange = { defaultMinutes = it },
                            label = { Text("Default Minutes") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                    }
                    ExerciseType.BODYWEIGHT -> {
                        OutlinedTextField(
                            value = defaultBodyweightReps,
                            onValueChange = { defaultBodyweightReps = it },
                            label = { Text("Default Reps") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                    }
                    ExerciseType.STRENGTH -> {
                        // No additional fields for Strength exercises
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
                
                // Create exercise helper function
                fun createExercise(): Exercise {
                    AppLogger.i("AddExerciseDialog", "Creating exercise with externalExercise: ${externalExercise?.name}")
                    return if (existingExercise != null) {
                        // When editing, preserve all non-editable fields from the existing exercise
                        existingExercise.copy(
                            name = name.trim(),
                            category = ExerciseCategoryMapper.getCategory(exerciseType),
                            kcalBurnedPerUnit = kcalPerUnit.toDoubleOrNull(),
                            defaultWeight = defaultWeight.toDoubleOrNull() ?: 0.0,
                            defaultReps = when (exerciseType) {
                                ExerciseType.CARDIO -> defaultMinutes.toIntOrNull() ?: 0 // Minutes for cardio
                                ExerciseType.BODYWEIGHT -> defaultBodyweightReps.toIntOrNull() ?: 0 // Reps for bodyweight
                                ExerciseType.STRENGTH -> defaultReps.toIntOrNull() ?: 0 // Reps for strength
                            },
                            defaultSets = when (exerciseType) {
                                ExerciseType.CARDIO -> 1 // Always 1 set for cardio
                                ExerciseType.BODYWEIGHT -> 1 // Always 1 set for bodyweight
                                ExerciseType.STRENGTH -> defaultSets.toIntOrNull() ?: 0 // Sets for strength
                            },
                            notes = notes.takeIf { it.isNotBlank() },
                            imagePaths = existingExercise.imagePaths // Preserve existing image paths
                        )
                    } else {
                        // When creating new exercise, use the provided external exercise data if available
                        val baseExercise = externalExercise?.toInternalExercise() ?: Exercise(name = "")
                        baseExercise.copy(
                            name = name.trim(),
                            category = ExerciseCategoryMapper.getCategory(exerciseType),
                            kcalBurnedPerUnit = kcalPerUnit.toDoubleOrNull(),
                            defaultWeight = defaultWeight.toDoubleOrNull() ?: 0.0,
                            defaultReps = when (exerciseType) {
                                ExerciseType.CARDIO -> defaultMinutes.toIntOrNull() ?: 0 // Minutes for cardio
                                ExerciseType.BODYWEIGHT -> defaultBodyweightReps.toIntOrNull() ?: 0 // Reps for bodyweight
                                ExerciseType.STRENGTH -> defaultReps.toIntOrNull() ?: 0 // Reps for strength
                            },
                            defaultSets = when (exerciseType) {
                                ExerciseType.CARDIO -> 1 // Always 1 set for cardio
                                ExerciseType.BODYWEIGHT -> 1 // Always 1 set for bodyweight
                                ExerciseType.STRENGTH -> defaultSets.toIntOrNull() ?: 0 // Sets for strength
                            },
                            notes = notes.takeIf { it.isNotBlank() }
                        )
                    }
                }
                
                // Check if we should show three-button structure (when external exercise is provided and not editing)
                val showThreeButtons = externalExercise != null && existingExercise == null && 
                    onSaveAndCheckIn != null && onJustSave != null && onJustCheckIn != null
                
                if (showThreeButtons) {
                    // Three-button structure for external exercises
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Save and Check-in button
                        Button(
                            onClick = { onSaveAndCheckIn(createExercise()) },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(stringResource(R.string.save_and_check_in))
                        }
                        
                        // Just Save button
                        Button(
                            onClick = { onJustSave(createExercise()) },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(stringResource(R.string.just_save))
                        }
                        
                        // Just Check-in button
                        Button(
                            onClick = { onJustCheckIn(createExercise()) },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text(stringResource(R.string.just_check_in))
                        }
                    }
                } else {
                    // Original single button for regular add/edit
                    Button(
                        onClick = { onAddExercise(createExercise()) },
                        enabled = name.isNotBlank()
                    ) {
                        Text(if (existingExercise != null) stringResource(R.string.update) else stringResource(R.string.add))
                    }
                }
            }
        }
    )
}
