package com.offlinelabs.nutcracker.ui.components.dialogs

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
import androidx.compose.ui.text.style.TextAlign
import com.offlinelabs.nutcracker.R
import com.offlinelabs.nutcracker.ui.theme.getContrastingTextColor
import java.util.Locale
import com.offlinelabs.nutcracker.ui.theme.getContrastingSliderColor
import com.offlinelabs.nutcracker.ui.theme.BrandGoldLight
import com.offlinelabs.nutcracker.ui.theme.BrandRedLight
import com.offlinelabs.nutcracker.ui.theme.NeutralLight100
import androidx.compose.ui.graphics.Color
import com.offlinelabs.nutcracker.data.model.Exercise
import com.offlinelabs.nutcracker.data.model.ExerciseLog
import com.offlinelabs.nutcracker.data.model.ExerciseType
import com.offlinelabs.nutcracker.data.model.ExerciseCategoryMapper
import com.offlinelabs.nutcracker.data.model.Meal
import com.offlinelabs.nutcracker.data.model.MealCheckIn
import com.offlinelabs.nutcracker.data.model.CheckInData
import com.offlinelabs.nutcracker.ui.components.DateTimePicker
import java.text.SimpleDateFormat
import java.util.Date

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
    
    // Date and time state
    var selectedDateTime by remember {
        mutableStateOf(
            if (isEditMode && existingExerciseLog != null) {
                // Parse existing timestamp
                try {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
                        .parse(existingExerciseLog.logDateTime) ?: Date()
                } catch (e: Exception) {
                    Date()
                }
            } else {
                Date()
            }
        )
    }

    // Calculate calories burned
    val caloriesBurned = remember(weight, reps, sets, exercise) {
        val exerciseType = ExerciseCategoryMapper.getExerciseType(exercise.category)
        when (exerciseType) {
            ExerciseType.STRENGTH -> {
                val setsVal = sets.toIntOrNull() ?: 0
                val kcalPerSet = exercise.kcalBurnedPerUnit ?: 0.0
                setsVal * kcalPerSet
            }
            ExerciseType.CARDIO -> {
                val repsVal = reps.toIntOrNull() ?: 0
                val kcalPerMinute = exercise.kcalBurnedPerUnit ?: 0.0
                // Assuming reps represent minutes for cardio
                repsVal * kcalPerMinute
            }
            ExerciseType.BODYWEIGHT -> {
                val repsVal = reps.toIntOrNull() ?: 0
                val kcalPerRep = exercise.kcalBurnedPerUnit ?: 0.0
                repsVal * kcalPerRep
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
                        color = getContrastingSliderColor(MaterialTheme.colorScheme.surface),
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
                // Date and Time Picker
                DateTimePicker(
                    selectedDateTime = selectedDateTime,
                    onDateTimeChanged = { selectedDateTime = it }
                )
                
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

                // Reps and Sets fields side by side
                if (exerciseType != ExerciseType.CARDIO) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Reps field
                        OutlinedTextField(
                            value = reps,
                            onValueChange = { reps = it },
                            label = { Text(stringResource(R.string.reps)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                        
                        // Sets field
                        OutlinedTextField(
                            value = sets,
                            onValueChange = { sets = it },
                            label = { Text(stringResource(R.string.sets)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                    }
                } else {
                    // For cardio, only show minutes field (full width)
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text(stringResource(R.string.minutes)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }

                CaloriesSummaryCard(
                    label = stringResource(R.string.calories_burned),
                    labelColor = BrandRedLight,
                    valueText = "${caloriesBurned.toInt()} kcal"
                )

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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                    )
                }
                Button(
                    onClick = {
                        val exerciseLog = if (isEditMode && existingExerciseLog != null) {
                            // Update existing log with new values including timestamp
                            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                            val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
                            existingExerciseLog.copy(
                                logDate = dateFormatter.format(selectedDateTime),
                                logDateTime = dateTimeFormatter.format(selectedDateTime),
                                weight = weight.toDoubleOrNull() ?: 0.0,
                                reps = reps.toIntOrNull() ?: 0,
                                sets = sets.toIntOrNull() ?: 0,
                                caloriesBurned = caloriesBurned,
                                notes = notes.takeIf { it.isNotBlank() }
                            )
                        } else {
                            // Create new log with custom timestamp
                            ExerciseLog.create(
                                exerciseId = exercise.id,
                                weight = weight.toDoubleOrNull() ?: 0.0,
                                reps = reps.toIntOrNull() ?: 0,
                                sets = sets.toIntOrNull() ?: 0,
                                caloriesBurned = caloriesBurned,
                                notes = notes.takeIf { it.isNotBlank() },
                                customDateTime = selectedDateTime
                            )
                        }
                        onCheckIn(exerciseLog)
                    },
                    enabled = weight.isNotBlank() && reps.isNotBlank() && sets.isNotBlank()
                ) {
                    Text(if (isEditMode) stringResource(R.string.update) else stringResource(R.string.check_in))
                }
            }
        },
        dismissButton = {
            // Delete button (only in edit mode) - positioned at left
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
        mutableDoubleStateOf(
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
    
    // Date and time state
    var selectedDateTime by remember {
        mutableStateOf(
            if (isEditMode && existingMealCheckIn != null) {
                // Parse existing timestamp
                try {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
                        .parse(existingMealCheckIn.checkInDateTime) ?: Date()
                } catch (e: Exception) {
                    Date()
                }
            } else {
                Date()
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
                // Date and Time Picker
                DateTimePicker(
                    selectedDateTime = selectedDateTime,
                    onDateTimeChanged = { selectedDateTime = it }
                )
                
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
                            text = stringResource(R.string.serving_multiplier_format, minRange),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = currentValue,
                            onValueChange = { servingSize = it.toDouble() },
                            valueRange = minRange..maxRange,
                            steps = maxOf(0, ((maxRange - minRange) / 0.1f).toInt() - 1), // Dynamic steps based on range
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = getContrastingSliderColor(MaterialTheme.colorScheme.surface),
                                activeTrackColor = getContrastingSliderColor(MaterialTheme.colorScheme.surface),
                                inactiveTrackColor = getContrastingSliderColor(MaterialTheme.colorScheme.surface).copy(alpha = 0.3f)
                            )
                        )
                        Text(
                            text = stringResource(R.string.serving_multiplier_format, maxRange),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = stringResource(R.string.serving_multiplier_format, servingSize),
                        style = MaterialTheme.typography.bodyMedium,
                        color = getContrastingTextColor(MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
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
                        manualInput = String.format(Locale.US, "%.1f", currentAmount)
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
                            stringResource(R.string.serving_size_placeholder)
                        )
                    },
                    placeholder = { 
                        Text(
                            stringResource(R.string.serving_size_placeholder)
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
                            manualInput = String.format(Locale.US, "%.1f", inputValue)
                        }
                    }
                }

                CaloriesSummaryCard(
                    label = stringResource(R.string.total_calories),
                    labelColor = BrandGoldLight,
                    valueText = "$totalCalories kcal"
                )

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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = getContrastingTextColor(MaterialTheme.colorScheme.surface)
                    )
                }
                Button(
                    onClick = {
                        val checkIn = if (isEditMode && existingMealCheckIn != null) {
                            // Update existing check-in with new values including timestamp
                            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                            val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
                            existingMealCheckIn.copy(
                                checkInDate = dateFormatter.format(selectedDateTime),
                                checkInDateTime = dateTimeFormatter.format(selectedDateTime),
                                servingSize = servingSize,
                                notes = notes.takeIf { it.isNotBlank() }
                            )
                        } else {
                            // Create new check-in with custom timestamp
                            MealCheckIn.create(
                                mealId = meal.id,
                                servingSize = servingSize,
                                notes = notes.takeIf { it.isNotBlank() },
                                customDateTime = selectedDateTime
                            )
                        }
                        onCheckIn(checkIn)
                    }
                ) {
                    Text(if (isEditMode) stringResource(R.string.update) else stringResource(R.string.check_in))
                }
            }
        },
        dismissButton = {
            // Delete button (only in edit mode) - positioned at left
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
        }
    )
}

@Composable
private fun CaloriesSummaryCard(
    label: String,
    labelColor: Color,
    valueText: String
) {
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
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NeutralLight100
            )
        }
    }
}
