package com.example.template.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.template.R
import com.example.template.data.model.Exercise
import com.example.template.data.model.ExerciseLog
import com.example.template.data.model.ExerciseType
import com.example.template.data.model.ExerciseCategoryMapper
import com.example.template.data.model.Meal
import com.example.template.data.model.MealCheckIn
import com.example.template.data.model.CheckInData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : CheckInData> UnifiedCheckInDialog(
    // Common parameters
    onDismiss: () -> Unit,
    onCheckIn: (T) -> Unit,
    onDelete: (() -> Unit)? = null,
    isEditMode: Boolean = false,
    
    // Exercise-specific parameters
    exercise: Exercise? = null,
    lastLog: ExerciseLog? = null,
    maxWeight: Double? = null,
    existingExerciseLog: ExerciseLog? = null,
    
    // Meal-specific parameters
    meal: Meal? = null,
    existingMealCheckIn: MealCheckIn? = null
) {
    // Determine if this is an exercise or meal check-in
    val isExercise = exercise != null
    val isMeal = meal != null
    
    require((isExercise && !isMeal) || (!isExercise && isMeal)) { "Exactly one of exercise or meal must be provided" }
    
    if (isExercise) {
        ExerciseCheckInContent(
            exercise = exercise!!,
            lastLog = lastLog,
            maxWeight = maxWeight,
            existingExerciseLog = existingExerciseLog,
            isEditMode = isEditMode,
            onDismiss = onDismiss,
            onCheckIn = { exerciseLog -> 
                @Suppress("UNCHECKED_CAST")
                onCheckIn(CheckInData.Exercise(exerciseLog) as T)
            },
            onDelete = onDelete
        )
    } else {
        MealCheckInContent(
            meal = meal!!,
            existingMealCheckIn = existingMealCheckIn,
            isEditMode = isEditMode,
            onDismiss = onDismiss,
            onCheckIn = { mealCheckIn -> 
                @Suppress("UNCHECKED_CAST")
                onCheckIn(CheckInData.Meal(mealCheckIn) as T)
            },
            onDelete = onDelete
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseCheckInContent(
    exercise: Exercise,
    lastLog: ExerciseLog?,
    maxWeight: Double?,
    existingExerciseLog: ExerciseLog?,
    isEditMode: Boolean,
    onDismiss: () -> Unit,
    onCheckIn: (ExerciseLog) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var weight by remember { 
        mutableStateOf(
            if (isEditMode && existingExerciseLog != null) {
                existingExerciseLog.weight.toString()
            } else {
                lastLog?.weight?.toString() ?: exercise.defaultWeight.toString()
            }
        )
    }
    var reps by remember { 
        mutableStateOf(
            if (isEditMode && existingExerciseLog != null) {
                existingExerciseLog.reps.toString()
            } else {
                lastLog?.reps?.toString() ?: exercise.defaultReps.toString()
            }
        )
    }
    var sets by remember { 
        mutableStateOf(
            if (isEditMode && existingExerciseLog != null) {
                existingExerciseLog.sets.toString()
            } else {
                lastLog?.sets?.toString() ?: exercise.defaultSets.toString()
            }
        )
    }
    var notes by remember { 
        mutableStateOf(
            if (isEditMode && existingExerciseLog != null) {
                existingExerciseLog.notes ?: ""
            } else {
                ""
            }
        )
    }

    // Calculate calories burned
    val caloriesBurned = remember(weight, reps, sets, exercise) {
        val exerciseType = ExerciseCategoryMapper.getExerciseType(exercise.category)
        when (exerciseType) {
            ExerciseType.STRENGTH -> {
                val setsVal = sets.toIntOrNull() ?: 0
                val kcalPerSet = exercise.kcalBurnedPerRep ?: 0.0 // This field now represents kcal per set
                setsVal * kcalPerSet
            }
            ExerciseType.CARDIO -> {
                val repsVal = reps.toIntOrNull() ?: 0
                val kcalPerMinute = exercise.kcalBurnedPerMinute ?: 0.0
                // Assuming reps represent minutes for cardio
                repsVal * kcalPerMinute
            }
            ExerciseType.BODYWEIGHT -> {
                val repsVal = reps.toIntOrNull() ?: 0
                val setsVal = sets.toIntOrNull() ?: 0
                val kcalPerRep = exercise.kcalBurnedPerRep ?: 0.0
                repsVal * setsVal * kcalPerRep
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.check_in_exercise_title, exercise.name),
                    style = MaterialTheme.typography.headlineSmall
                )
                // Show max weight if available
                maxWeight?.let { max ->
                    Text(
                        text = stringResource(R.string.max_weight_recorded, max),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Weight field (for strength exercises)
                val exerciseType = ExerciseCategoryMapper.getExerciseType(exercise.category)
                if (exerciseType == ExerciseType.STRENGTH) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text(stringResource(R.string.weight_kg)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        )
                    )
                }

                // Reps field
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { 
                        Text(
                            when (exerciseType) {
                                ExerciseType.CARDIO -> stringResource(R.string.minutes)
                                else -> stringResource(R.string.reps)
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )

                // Sets field (for strength and bodyweight exercises)
                if (exerciseType != ExerciseType.CARDIO) {
                    OutlinedTextField(
                        value = sets,
                        onValueChange = { sets = it },
                        label = { Text(stringResource(R.string.sets)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }

                // Calories burned display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.calories_burned),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${caloriesBurned.toInt()} kcal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Notes field
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
                    val exerciseLog = if (isEditMode && existingExerciseLog != null) {
                        // Update existing log with new values but preserve ID and timestamps
                        existingExerciseLog.copy(
                            weight = weight.toDoubleOrNull() ?: 0.0,
                            reps = reps.toIntOrNull() ?: 0,
                            sets = sets.toIntOrNull() ?: 0,
                            caloriesBurned = caloriesBurned,
                            notes = notes.takeIf { it.isNotBlank() }
                        )
                    } else {
                        // Create new log
                        ExerciseLog.create(
                            exerciseId = exercise.id,
                            weight = weight.toDoubleOrNull() ?: 0.0,
                            reps = reps.toIntOrNull() ?: 0,
                            sets = sets.toIntOrNull() ?: 0,
                            caloriesBurned = caloriesBurned,
                            notes = notes.takeIf { it.isNotBlank() }
                        )
                    }
                    onCheckIn(exerciseLog)
                },
                enabled = weight.isNotBlank() && reps.isNotBlank() && sets.isNotBlank()
            ) {
                Text(if (isEditMode) stringResource(R.string.update) else stringResource(R.string.check_in))
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete button (only in edit mode) - positioned at bottom-left
                if (isEditMode && onDelete != null) {
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
                
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealCheckInContent(
    meal: Meal,
    existingMealCheckIn: MealCheckIn?,
    isEditMode: Boolean,
    onDismiss: () -> Unit,
    onCheckIn: (MealCheckIn) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var servingSize by remember { 
        mutableStateOf(
            if (isEditMode && existingMealCheckIn != null) {
                existingMealCheckIn.servingSize
            } else {
                1.0
            }
        )
    }
    var notes by remember { 
        mutableStateOf(
            if (isEditMode && existingMealCheckIn != null) {
                existingMealCheckIn.notes ?: ""
            } else {
                ""
            }
        )
    }

    // Calculate total calories based on serving size
    val totalCalories = remember(servingSize, meal) {
        (meal.calories * servingSize).toInt()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.check_in_meal_title, meal.name),
                    style = MaterialTheme.typography.headlineSmall
                )
                // Show meal info
                Text(
                    text = stringResource(
                        R.string.meal_calories_info,
                        meal.calories,
                        meal.servingSize_value.toInt(),
                        meal.servingSize_unit.abbreviation
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Serving size slider
                Column {
                    Text(
                        text = stringResource(R.string.serving_size),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Calculate dynamic range based on current serving size
                    val minRange = 0.1f
                    val maxRange = maxOf(3.0f, (servingSize * 1.2f).toFloat()) // Expand range by 20% above current value
                    val currentValue = servingSize.toFloat().coerceIn(minRange, maxRange)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${minRange}x",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = currentValue,
                            onValueChange = { servingSize = it.toDouble() },
                            valueRange = minRange..maxRange,
                            steps = ((maxRange - minRange) / 0.1f).toInt() - 1, // Dynamic steps based on range
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${String.format("%.1f", maxRange)}x",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.serving_size_value,
                            servingSize,
                            totalCalories
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Manual serving size input
                var manualInput by remember { mutableStateOf("") }
                var isUserTyping by remember { mutableStateOf(false) }
                
                // Update manual input when slider changes (only if user is not typing)
                LaunchedEffect(servingSize) {
                    if (!isUserTyping) {
                        val currentAmount = servingSize * meal.servingSize_value
                        // Format to 1 decimal place to avoid float precision issues
                        manualInput = String.format("%.1f", currentAmount)
                    }
                }
                
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { newValue ->
                        isUserTyping = true
                        manualInput = newValue
                        // Convert manual input to serving size multiplier
                        val inputValue = newValue.toDoubleOrNull()
                        if (inputValue != null && inputValue > 0) {
                            // Calculate multiplier based on meal's base serving size
                            val baseServing = meal.servingSize_value
                            val multiplier = inputValue / baseServing
                            // No artificial cap - let users input any reasonable value
                            servingSize = maxOf(0.1, multiplier) // Only prevent negative values
                        }
                    },
                    label = { 
                        Text(
                            stringResource(
                                R.string.serving_size_placeholder,
                                meal.servingSize_unit.abbreviation
                            )
                        )
                    },
                    placeholder = { 
                        Text(
                            stringResource(
                                R.string.serving_size_placeholder,
                                meal.servingSize_unit.abbreviation
                            )
                        )
                    },
                    suffix = {
                        Text(
                            text = meal.servingSize_unit.abbreviation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
                
                // Reset typing flag after a delay to allow formatting
                LaunchedEffect(manualInput) {
                    if (isUserTyping) {
                        kotlinx.coroutines.delay(1000) // Wait 1 second after user stops typing
                        isUserTyping = false
                        val inputValue = manualInput.toDoubleOrNull()
                        if (inputValue != null && inputValue > 0) {
                            manualInput = String.format("%.1f", inputValue)
                        }
                    }
                }

                // Total calories display (matching exercise style)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.total_calories),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$totalCalories kcal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Notes field
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
                    val checkIn = if (isEditMode && existingMealCheckIn != null) {
                        // Update existing check-in with new values but preserve ID and timestamps
                        existingMealCheckIn.copy(
                            servingSize = servingSize,
                            notes = notes.takeIf { it.isNotBlank() }
                        )
                    } else {
                        // Create new check-in
                        MealCheckIn.create(
                            mealId = meal.id,
                            servingSize = servingSize,
                            notes = notes.takeIf { it.isNotBlank() }
                        )
                    }
                    onCheckIn(checkIn)
                }
            ) {
                Text(if (isEditMode) stringResource(R.string.update) else stringResource(R.string.check_in))
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete button (only in edit mode) - positioned at bottom-left
                if (isEditMode && onDelete != null) {
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
                
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}
