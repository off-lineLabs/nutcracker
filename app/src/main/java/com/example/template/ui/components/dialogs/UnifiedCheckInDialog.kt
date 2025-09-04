package com.example.template.ui.components.dialogs

import androidx.compose.foundation.layout.*
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
import com.example.template.data.model.Meal
import com.example.template.data.model.MealCheckIn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedCheckInDialog(
    // Common parameters
    onDismiss: () -> Unit,
    onCheckIn: (Any) -> Unit, // Will be ExerciseLog or MealCheckIn
    
    // Exercise-specific parameters
    exercise: Exercise? = null,
    lastLog: ExerciseLog? = null,
    maxWeight: Double? = null,
    
    // Meal-specific parameters
    meal: Meal? = null
) {
    // Determine if this is an exercise or meal check-in
    val isExercise = exercise != null
    val isMeal = meal != null
    
    require(isExercise != isMeal) { "Exactly one of exercise or meal must be provided" }
    
    if (isExercise) {
        ExerciseCheckInContent(
            exercise = exercise!!,
            lastLog = lastLog,
            maxWeight = maxWeight,
            onDismiss = onDismiss,
            onCheckIn = onCheckIn
        )
    } else {
        MealCheckInContent(
            meal = meal!!,
            onDismiss = onDismiss,
            onCheckIn = onCheckIn
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseCheckInContent(
    exercise: Exercise,
    lastLog: ExerciseLog?,
    maxWeight: Double?,
    onDismiss: () -> Unit,
    onCheckIn: (Any) -> Unit
) {
    var weight by remember { mutableStateOf(lastLog?.weight?.toString() ?: exercise.defaultWeight.toString()) }
    var reps by remember { mutableStateOf(lastLog?.reps?.toString() ?: exercise.defaultReps.toString()) }
    var sets by remember { mutableStateOf(lastLog?.sets?.toString() ?: exercise.defaultSets.toString()) }
    var notes by remember { mutableStateOf("") }

    // Calculate calories burned
    val caloriesBurned = remember(weight, reps, sets, exercise) {
        when (exercise.exerciseType) {
            ExerciseType.STRENGTH -> {
                val weightVal = weight.toDoubleOrNull() ?: 0.0
                val repsVal = reps.toIntOrNull() ?: 0
                val setsVal = sets.toIntOrNull() ?: 0
                val kcalPerRep = exercise.kcalBurnedPerRep ?: 0.0
                weightVal * repsVal * setsVal * kcalPerRep
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
                if (exercise.exerciseType == ExerciseType.STRENGTH) {
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
                            when (exercise.exerciseType) {
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
                if (exercise.exerciseType != ExerciseType.CARDIO) {
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
                    val exerciseLog = ExerciseLog.create(
                        exerciseId = exercise.id,
                        weight = weight.toDoubleOrNull() ?: 0.0,
                        reps = reps.toIntOrNull() ?: 0,
                        sets = sets.toIntOrNull() ?: 0,
                        caloriesBurned = caloriesBurned,
                        notes = notes.takeIf { it.isNotBlank() }
                    )
                    onCheckIn(exerciseLog)
                },
                enabled = weight.isNotBlank() && reps.isNotBlank() && sets.isNotBlank()
            ) {
                Text(stringResource(R.string.check_in))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealCheckInContent(
    meal: Meal,
    onDismiss: () -> Unit,
    onCheckIn: (Any) -> Unit
) {
    var servingSize by remember { mutableStateOf(1.0) }
    var notes by remember { mutableStateOf("") }

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
                        meal.servingSize_value,
                        meal.servingSize_unit
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "0.5x",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = servingSize.toFloat(),
                            onValueChange = { servingSize = it.toDouble() },
                            valueRange = 0.5f..3.0f,
                            steps = 24, // 0.1 increments
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "3.0x",
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
                    val checkIn = MealCheckIn.create(
                        mealId = meal.id,
                        servingSize = servingSize,
                        notes = notes.takeIf { it.isNotBlank() }
                    )
                    onCheckIn(checkIn)
                }
            ) {
                Text(stringResource(R.string.check_in))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
